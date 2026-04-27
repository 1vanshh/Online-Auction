package com.auction.biddingservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FinishLotBiddingRequest(
        @Min(value = 1, message = "Payment deadline days must be at least 1")
        @Max(value = 30, message = "Payment deadline days must be at most 30")
        Integer paymentDeadlineDays
) {
    public int effectivePaymentDeadlineDays() {
        return paymentDeadlineDays == null ? 3 : paymentDeadlineDays;
    }
}
