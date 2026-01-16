package com.rafaelsousa.algashop.billingscheduler.infrastructure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceProjection {
    private UUID id;
    private String paymentGatewayCode;
}