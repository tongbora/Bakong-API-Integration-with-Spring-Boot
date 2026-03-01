package com.tongbora.bakongapiintergration.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.tongbora.bakongapiintergration.dto.BakongRequest;
import com.tongbora.bakongapiintergration.dto.BakongResponse;
import com.tongbora.bakongapiintergration.dto.CheckTransactionRequest;
import com.tongbora.bakongapiintergration.service.BakongService;
import jakarta.validation.Valid;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bakong")
@RequiredArgsConstructor
public class BakongController {

    private final BakongService service;

    @PostMapping("/generate-qr")
    public KHQRResponse<KHQRData> generateQR(@RequestBody BakongRequest request){
        return service.generateQR(request);
    }

    @PostMapping("/get-qr-image")
    public ResponseEntity<byte[]> getQRImage(@RequestBody KHQRData qr) {
        byte[] imageBytes = service.getQRImage(qr);

        return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qrcode.png\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
    }

    @PostMapping("/check-transaction")
    public ResponseEntity<BakongResponse> checkTransaction(
            @Valid @RequestBody CheckTransactionRequest request) {

        BakongResponse response = service.checkTransactionByMD5(request);
        return ResponseEntity.ok(response);
    }

}
