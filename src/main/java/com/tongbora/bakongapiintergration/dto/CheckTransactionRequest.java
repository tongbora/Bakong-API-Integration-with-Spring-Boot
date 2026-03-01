package com.tongbora.bakongapiintergration.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckTransactionRequest(
        @NotBlank
        String md5
) {
}
