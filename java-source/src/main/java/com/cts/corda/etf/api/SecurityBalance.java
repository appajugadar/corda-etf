package com.cts.corda.etf.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class SecurityBalance {
    private String securityName;
    private Long quantity;
}