package opentext.cmeai.controller;

import opentext.cmeai.CopyRightScanner;
import opentext.cmeai.utility.ScanUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class ScanImagesController {

    @GetMapping("/scanImages")
    public String hello(@RequestParam("url") String url, Model model) throws Exception {
        CopyRightScanner copyRightScanner = new CopyRightScanner();

        Map<String, byte[]> imageBytes = ScanUtility.scanPage(url);
        JSONObject result = copyRightScanner.scanImages(imageBytes);

        JSONObject summary = result.optJSONObject("summary");
        if (summary != null) {
            model.addAttribute("overallRisk", summary.optString("overallRisk"));
            model.addAttribute("justification", summary.optString("justification"));
        }

        // --- Extract evaluated images ---
        JSONArray imagesArray = result.optJSONArray("evaluatedImages");
        List<Map<String, String>> images1 = new ArrayList<>();
        if (imagesArray != null) {
            for (int i = 0; i < imagesArray.length(); i++) {
                JSONObject img = imagesArray.getJSONObject(i);
                images1.add(Map.of(
                        "reason", img.optString("reason"),
                        "fileName", img.optString("fileName"),
                        "copyrightRisk", img.optString("copyrightRisk")
                ));
            }
        }

        model.addAttribute("images", images1);

        System.out.println(result);
        return "scanResult";
    }

}

