package com.tongbora.bakongapiintergration.controller;


import com.tongbora.bakongapiintergration.dto.BakongRequest;
import com.tongbora.bakongapiintergration.service.BakongService;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bakong")
@RequiredArgsConstructor
public class BakongController {

    private final BakongService service;

    @GetMapping("/generate-qr")
    public KHQRResponse<KHQRData> generateQR(@RequestBody BakongRequest request){
        return service.generateQR(request);
    }

    @GetMapping("/get-qr-image")
    public ResponseEntity<byte[]> getQRImage(@RequestBody KHQRData qr) {
        return service.getQRImage(qr);
    }

    @PostMapping("/check-transaction")
    public ResponseEntity<?> checkTransaction(@RequestBody Map<String, String> body) {
        String md5 = body.get("md5");
        return service.checkTransactionByMD5(md5);
    }

}
