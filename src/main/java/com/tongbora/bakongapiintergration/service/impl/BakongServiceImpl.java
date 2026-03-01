package com.tongbora.bakongapiintergration.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tongbora.bakongapiintergration.dto.BakongRequest;
import com.tongbora.bakongapiintergration.dto.BakongResponse;
import com.tongbora.bakongapiintergration.dto.CheckTransactionRequest;
import com.tongbora.bakongapiintergration.service.BakongService;
import com.tongbora.bakongapiintergration.service.BakongTokenService;
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
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
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
    private final BakongTokenService bakongTokenService;

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
//    @Value("${bakong.bearer-token}")
//    private String bearerToken;

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
        return BakongKHQR.generateMerchant(merchantInfo);
    }

    @Override
    public byte[] getQRImage(KHQRData qr) {
        try {
            // Validate input
            if (qr == null || qr.getQr() == null || qr.getQr().isBlank()) {
                return "Invalid QR data".getBytes(StandardCharsets.UTF_8);
            }

            String qrCodeText = qr.getQr();

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();

        } catch (WriterException e) {
            // Thrown by QRCodeWriter.encode() if encoding fails (e.g., invalid data)
            return "Error encoding QR data".getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback for any unexpected error
            return ("Unexpected error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public BakongResponse checkTransactionByMD5(CheckTransactionRequest request) {
        String bearerToken = bakongTokenService.getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);

        Map<String, String> requestBody = Map.of("md5", request.md5());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        String url = baseUrl.replaceAll("/+$", "") + "/v1/check_transaction_by_md5";

        ResponseEntity<String> upstream = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );
        log.info("Data response from Bakong API: {}", upstream);

        try {
            // Deserialize data into BakongResponse
            return mapper.readValue(upstream.getBody(), BakongResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid upstream response", e);
        }
    }
}
