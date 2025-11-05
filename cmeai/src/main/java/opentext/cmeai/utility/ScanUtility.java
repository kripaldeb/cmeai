package opentext.cmeai.utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ScanUtility {

    public static Map<String, byte[]> scanPage(String url) throws Exception
    {
        Map<String,byte[]> imageUrls = new HashMap<>();
        try {
            Document doc = Jsoup.connect(url).get();
            Elements imgTags = doc.select("img");

            for (Element img : imgTags) {
                String src = img.attr("abs:src"); // absolute URL
                if (src != null && !src.isEmpty()) {


                    byte[] imageBytes = null;

                    try
                    {
                        // Open connection to the URL
                        URL urlPath = new URL(src);
                        HttpURLConnection connection = (HttpURLConnection) urlPath.openConnection();
                        connection.setRequestMethod("GET");

                        // Read the input stream
                        try (InputStream inputStream = connection.getInputStream();
                             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

                            byte[] data = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, bytesRead);
                            }

                            imageBytes = buffer.toByteArray();
                            System.out.println("Image downloaded. Byte array length: " + imageBytes.length);
                        }

                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    imageUrls.put(src.substring(src.lastIndexOf('/') + 1), imageBytes);
                }
            }
        } catch (IOException e)
        {
            System.err.println("Error fetching images: " + e.getMessage());
        }

        return imageUrls;
    }
}



