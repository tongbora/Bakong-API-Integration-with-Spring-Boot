package com.tongbora.bakongapiintergration.service;

import com.tongbora.bakongapiintergration.dto.BakongRequest;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import org.springframework.http.ResponseEntity;

public interface BakongService {

    KHQRResponse<KHQRData> generateQR(BakongRequest request);
    ResponseEntity<byte[]> getQRImage(KHQRData qr);
    ResponseEntity<?> checkTransactionByMD5(String md5);
}
