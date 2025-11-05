package opentext.cmeai;

import org.json.JSONArray;
import org.json.JSONObject;
public class CopyrightRiskClassifier {

    // === EVALUATION OF MULTI-IMAGE RISK ===
    public static JSONObject evaluateMultiImageRisk(JSONObject geminiResponse) {
        JSONArray images = geminiResponse.optJSONArray("images");
        if (images == null) {
            throw new IllegalArgumentException("Gemini response missing 'images' array.");
        }

        JSONArray evaluatedImages = new JSONArray();
        int high = 0, medium = 0, low = 0;

        for (int i = 0; i < images.length(); i++) {
            JSONObject img = images.getJSONObject(i);
            JSONObject evaluated = evaluateSingleImage(img);
            evaluated.put("fileName", img.optString("fileName", "image_" + (i + 1)));
            evaluatedImages.put(evaluated);

            switch (evaluated.getString("copyrightRisk")) {
                case "high" -> high++;
                case "medium" -> medium++;
                default -> low++;
            }
        }

        // Determine overall summary
        String overallRisk = high > 0 ? "high" : medium > 0 ? "medium" : "low";

        JSONObject summary = new JSONObject()
                .put("overallRisk", overallRisk)
                .put("justification", String.format(
                        "Out of %d images: %d high, %d medium, %d low risk.",
                        images.length(), high, medium, low
                ));

        return new JSONObject()
                .put("evaluatedImages", evaluatedImages)
                .put("summary", summary);
    }

    // === PER-IMAGE RISK EVALUATION ===
    private static JSONObject evaluateSingleImage(JSONObject img) {
        JSONObject visual = img.optJSONObject("visualIndicators");
        JSONObject meta = img.optJSONObject("inferredCopyrightMetadata");
        JSONObject inappropriate = img.optJSONObject("inappropriateContent");

        boolean copyrightDetected = img.optBoolean("copyrightDetected", false);
        String watermark = visual != null ? visual.optString("watermarkDescription", "None visible") : "None visible";
        boolean cSymbol = visual != null && visual.optBoolean("csymbolFound", false);
        boolean logo = visual != null && visual.optBoolean("logoOrBrandDetected", false);
        boolean metadata = meta != null && meta.optBoolean("metadataLikelyPresent", false);
        String holder = meta != null ? meta.optString("inferredCopyrightHolder", "Unknown") : "Unknown";
        String assessment = meta != null ? meta.optString("imageProfessionalAssessment", "").toLowerCase() : "";

        JSONArray logos = img.optJSONArray("detectedLogos");
        JSONArray text = img.optJSONArray("detectedText");

        boolean vulgar = inappropriate != null && inappropriate.optBoolean("vulgar", false);
        boolean insensitive = inappropriate != null && inappropriate.optBoolean("insensitive", false);
        boolean violent = inappropriate != null && inappropriate.optBoolean("violenceGory", false);

        // --- Classification ---
        String risk = "low";
        StringBuilder reason = new StringBuilder();

        if (vulgar || insensitive || violent) {
            risk = "medium";
            reason.append("Contains sensitive content. ");
        }

        if (!copyrightDetected) {
            reason.append("No copyright indicators found.");
        } else if (logo || (!"unknown".equalsIgnoreCase(holder) && !holder.isEmpty())) {
            if (assessment.matches(".*(software|ui|interface|screenshot|product).*")) {
                risk = "high";
                reason.append("Branded or product-related content detected (").append(holder).append("). ");
            } else {
                risk = "medium";
                reason.append("Brand/logo elements found (").append(holder).append("). ");
            }
        } else if (cSymbol || metadata) {
            risk = "medium";
            reason.append("Copyright symbols or metadata present. ");
        } else if (!"None visible".equalsIgnoreCase(watermark) && !watermark.isBlank()) {
            risk = "medium";
            reason.append("Watermark detected: ").append(watermark).append(". ");
        } else if (text != null && text.length() > 0) {
            risk = "medium";
            reason.append("Text content may indicate licensed material. ");
        }

        if ("high".equals(risk) && logos != null && logos.length() > 0) {
            reason.append("Logos detected: ").append(logos.join(", ")).append(". ");
        }

        if ("low".equals(risk) && assessment.contains("amateur")) {
            reason.append("Appears self-generated or amateur.");
        }

        return new JSONObject()
                .put("copyrightRisk", risk)
                .put("reason", reason.toString().trim());
    }


}
