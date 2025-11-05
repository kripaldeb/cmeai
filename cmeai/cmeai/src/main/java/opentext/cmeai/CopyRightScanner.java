package opentext.cmeai;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Lists;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Multi-image copyright and sensitivity scanner using Google Gemini (Vertex AI).
 * Analyzes multiple images and returns a structured JSON report with risk levels.
 */
public class CopyRightScanner {

    // === CONFIGURATION CONSTANTS ===
    private static final String PROJECT_ID = "otl-cem-teamsite";
    private static final String LOCATION = "us-central1";
    private static final String CREDENTIAL_PATH = "C:\\Users\\sandras\\Downloads\\newkey.json";

    // === GEMINI CLIENT CREATION ===
    private static Client createGeminiClient() throws IOException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(CREDENTIAL_PATH))
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

        return Client.builder()
                .project(PROJECT_ID)
                .location(LOCATION)
                .vertexAI(true)
                .credentials(credentials)
                .build();
    }

    // === MAIN MULTI-IMAGE SCANNER ===
    public static JSONObject scanImages(Map<String,byte[]> imageFiles) throws Exception {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("No images provided for scanning.");
        }

        Client client = createGeminiClient();

        // Build text instruction
        Part instruction = Part.fromText(buildPrompt());

        // Prepare input parts (prompt + images)
        List<Part> parts = new ArrayList<>();
        parts.add(instruction);
        for (Map.Entry<String, byte[]> entry : imageFiles.entrySet()) {
            String imageName = entry.getKey();
            byte[] imageBytes = entry.getValue();
            parts.add(Part.fromText("Image file name: " + imageName));
            parts.add(Part.fromBytes(imageBytes, "image/jpeg"));
        }


        // Send request to Gemini
        Content content = Content.builder().role("user").parts(parts).build();
        GenerateContentResponse response = client.models.generateContent(
                "gemini-2.5-pro", content, GenerateContentConfig.builder().build()
        );

        // Parse and evaluate response
        String rawText = response.text();
        JSONObject geminiJson = extractJsonSafely(rawText);
        return CopyrightRiskClassifier.evaluateMultiImageRisk(geminiJson);
    }

    // === PROMPT BUILDER ===
    private static String buildPrompt() {
        return """
                Analyze each image for copyright or sensitive content.
                Respond strictly in valid JSON with this structure:
                {
                  "images": [
                    {
                      "fileName": "...",
                      "copyrightDetected": boolean,
                      "visualIndicators": {
                        "watermarkDescription": "...",
                        "csymbolFound": boolean,
                        "logoOrBrandDetected": boolean
                      },
                      "inferredCopyrightMetadata": {
                        "metadataLikelyPresent": boolean,
                        "inferredCopyrightHolder": "...",
                        "imageProfessionalAssessment": "..."
                      },
                      "detectedText": ["..."],
                      "detectedLogos": ["..."],
                      "inappropriateContent": {
                        "vulgar": boolean,
                        "insensitive": boolean,
                        "violenceGory": boolean
                      }
                    }
                  ],
                  "summary": {
                    "overallRisk": "low|medium|high",
                    "justification": "..."
                  }
                }
                """;
    }
    // === SAFE JSON EXTRACTION ===
    private static JSONObject extractJsonSafely(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Empty model response.");
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("No valid JSON found in response: " + text);
        }

        String json = text.substring(start, end + 1);
        return new JSONObject(json);
    }
}
