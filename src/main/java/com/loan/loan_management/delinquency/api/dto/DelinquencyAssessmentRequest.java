package com.loan.loan_management.delinquency.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DelinquencyAssessmentRequest(
        @NotBlank String loanId,
        @Positive int installmentNumber,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal scheduledAmount,
        @NotNull @DecimalMin("0.0") BigDecimal paidAmount
) {
}
