package com.tongbora.bakongapiintergration.dto;

import jakarta.validation.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public record BakongRequest(
        @NotBlank
        Double amount
) {
}
