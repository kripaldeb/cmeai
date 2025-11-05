package opentext.cmeai.controller;

import opentext.cmeai.CopyRightScanner;
import opentext.cmeai.utility.ScanUtility;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/scanImages")
    public String hello(@RequestParam("url") String url) throws Exception {
        CopyRightScanner copyRightScanner = new CopyRightScanner();

        Map<String, byte[]> imageBytes = ScanUtility.scanPage(url);
        JSONObject result = copyRightScanner.scanImages(imageBytes);

        System.out.println(result);
        return result.toString();
    }

}
