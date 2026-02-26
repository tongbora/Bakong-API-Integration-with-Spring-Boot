package com.tongbora.bakongapiintergration.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tongbora.bakongapiintergration.dto.BakongRequest;
import com.tongbora.bakongapiintergration.service.BakongService;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.MerchantInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BakongServiceImpl implements BakongService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Value("${bakong.account-id}")
    private String bakongAccountId;
    @Value("${bakong.acquiring-bank}")
    private String acquiringBank;
    @Value("${bakong.merchant-name}")
    private String merchantName;
    @Value("${bakong.mobile-number}")
    private String mobileNumber;
    @Value("${bakong.store-label}")
    private String storeLabel;
    @Value("${bakong.base-url}")
    private String baseUrl;
    @Value("${bakong.bearer-token}")
    private String bearerToken;

    @Override
    public KHQRResponse<KHQRData> generateQR(BakongRequest request) {

        // You Can Customize MerchantInfo Based on Your Requirement

        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setBakongAccountId(bakongAccountId);
        merchantInfo.setMerchantId("123456");
        merchantInfo.setAcquiringBank(acquiringBank);
        merchantInfo.setCurrency(KHQRCurrency.USD);
        merchantInfo.setAmount(request.amount());
        merchantInfo.setMerchantName(merchantName);
        merchantInfo.setMerchantCity("PHNOM PENH");
        merchantInfo.setBillNumber("#12345");
        merchantInfo.setMobileNumber(mobileNumber);
        merchantInfo.setStoreLabel(storeLabel);
        merchantInfo.setUpiAccountInformation("KH123456789");
        merchantInfo.setMerchantAlternateLanguagePreference("km");
        merchantInfo.setMerchantNameAlternateLanguage("តុងបូរា");
        merchantInfo.setMerchantCityAlternateLanguage("ភ្នំពញ");
        KHQRResponse<KHQRData> response = BakongKHQR.generateMerchant(merchantInfo);
        return response;
    }

    @Override
    public ResponseEntity<byte[]> getQRImage(KHQRData qr) {
        try {
            // Validate input
            if (qr == null || qr.getQr() == null || qr.getQr().isBlank()) {
                return ResponseEntity.badRequest()
                        .body("QR code data is missing or empty".getBytes(StandardCharsets.UTF_8));
            }

            String qrCodeText = qr.getQr();

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qrcode.png\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(pngOutputStream.toByteArray());

        } catch (WriterException e) {
            // Thrown by QRCodeWriter.encode() if encoding fails (e.g., invalid data)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid QR code data".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Thrown by MatrixToImageWriter.writeToStream()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating QR image".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback for any unexpected error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public ResponseEntity<?> checkTransactionByMD5(String md5) {
        try {
            // Validation
            if (md5 == null || md5.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "md5 is required"));
            }
//            if (!md5Pattern.matcher(md5).matches()) {
//                return ResponseEntity.badRequest().body(Map.of("message", "md5 must be a 32-character hex string"));
//            }

//            String baseUrl = baseUrl;
//            String bearerToken = config.getBearerToken();

            if (baseUrl == null || bearerToken == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Bakong is not configured"));
            }

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(bearerToken);
//            headers.set("User-Agent", "exstad-backend/1.0");

            // Request body
            Map<String, String> requestBody = Map.of("md5", md5);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            String url = baseUrl.replaceAll("/+$", "") + "/v1/check_transaction_by_md5";

            System.out.println("Sending request to Bakong: " + url);
            // Execute request
            ResponseEntity<String> upstreamResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Dynamic JSON parsing
            try {
                JsonNode json = mapper.readTree(upstreamResponse.getBody());
                return ResponseEntity.status(upstreamResponse.getStatusCode())
                        .body(mapper.convertValue(json, Object.class));
            } catch (Exception e) {
                return ResponseEntity.status(upstreamResponse.getStatusCode())
                        .body(Map.of("message", upstreamResponse.getBody()));
            }

        } catch (HttpClientErrorException.Forbidden e) {
            System.out.println("Bakong returned 403 Forbidden: " + e.getStatusCode());
            System.out.println("Bakong returned 403 Forbidden: " + e.getMessage());
            System.out.println("Bakong returned 403 Forbidden: " + e.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Bakong returned 403 Forbidden",
                            "details", e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(Map.of("message", "Upstream timeout"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Upstream error: " + e.getMessage()));
        }
    }
}
