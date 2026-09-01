package com.loan.loan_management.delinquency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "installment_delinquency",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_installment_delinquency_loan_installment",
                        columnNames = {"loan_id", "installment_number"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstallmentDelinquencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private String loanId;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "scheduled_amount", nullable = false)
    private BigDecimal scheduledAmount;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount;

    @Column(name = "overdue_amount", nullable = false)
    private BigDecimal overdueAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstallmentDelinquencyStatus status;

    @Column(name = "first_delinquency_date", nullable = false)
    private LocalDate firstDelinquencyDate;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Column(name = "last_assessed_date", nullable = false)
    private LocalDate lastAssessedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static InstallmentDelinquencyEntity create(
            String loanId,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal scheduledAmount,
            BigDecimal paidAmount,
            LocalDate lastAssessedDate
    ) {
        validateRequiredValues(
                loanId,
                installmentNumber,
                dueDate,
                scheduledAmount,
                paidAmount,
                lastAssessedDate
        );

        BigDecimal overdueAmount = scheduledAmount.subtract(paidAmount);

        validateDelinquency(
                dueDate,
                lastAssessedDate,
                overdueAmount
        );

        LocalDateTime now = LocalDateTime.now();

        InstallmentDelinquencyEntity delinquency =
                new InstallmentDelinquencyEntity();

        delinquency.loanId = loanId;
        delinquency.installmentNumber = installmentNumber;
        delinquency.dueDate = dueDate;
        delinquency.scheduledAmount = scheduledAmount;
        delinquency.paidAmount = paidAmount;
        delinquency.overdueAmount = overdueAmount;
        delinquency.status = InstallmentDelinquencyStatus.OPEN;
        delinquency.firstDelinquencyDate = dueDate.plusDays(1);
        delinquency.resolvedDate = null;
        delinquency.lastAssessedDate = lastAssessedDate;
        delinquency.createdAt = now;
        delinquency.updatedAt = now;

        return delinquency;
    }

    public void updateAsDelinquent(
            BigDecimal scheduledAmount,
            BigDecimal paidAmount,
            LocalDate lastAssessedDate
    ) {
        validateAssessmentValues(scheduledAmount, paidAmount, lastAssessedDate);

        BigDecimal overdueAmount = scheduledAmount.subtract(paidAmount);
        validateDelinquency(dueDate, lastAssessedDate, overdueAmount);

        this.scheduledAmount = scheduledAmount;
        this.paidAmount = paidAmount;
        this.overdueAmount = overdueAmount;
        this.status = InstallmentDelinquencyStatus.OPEN;
        this.resolvedDate = null;
        this.lastAssessedDate = lastAssessedDate;
    }

    public void resolve(
            BigDecimal scheduledAmount,
            BigDecimal paidAmount,
            LocalDate lastAssessedDate
    ) {
        validateAssessmentValues(scheduledAmount, paidAmount, lastAssessedDate);

        BigDecimal overdueAmount = scheduledAmount.subtract(paidAmount);
        if (overdueAmount.signum() > 0) {
            throw new IllegalArgumentException(
                    "미납 금액이 남아 있어 연체를 해소할 수 없습니다."
            );
        }

        this.scheduledAmount = scheduledAmount;
        this.paidAmount = paidAmount;
        this.overdueAmount = BigDecimal.ZERO;

        if (this.status == InstallmentDelinquencyStatus.OPEN) {
            this.resolvedDate = lastAssessedDate;
        }

        this.status = InstallmentDelinquencyStatus.RESOLVED;
        this.lastAssessedDate = lastAssessedDate;
    }

    private static void validateRequiredValues(
            String loanId,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal scheduledAmount,
            BigDecimal paidAmount,
            LocalDate lastAssessedDate
    ) {
        if (loanId == null || loanId.isBlank()) {
            throw new IllegalArgumentException("loanId는 필수입니다.");
        }

        if (installmentNumber < 1) {
            throw new IllegalArgumentException(
                    "상환 회차는 1 이상이어야 합니다."
            );
        }

        if (dueDate == null
                || scheduledAmount == null
                || paidAmount == null
                || lastAssessedDate == null) {
            throw new IllegalArgumentException(
                    "상환 예정일, 예정 금액, 납부 금액, 마지막 판정 기준일은 필수입니다."
            );
        }

        if (scheduledAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "예정 금액은 0보다 커야 합니다."
            );
        }

        if (paidAmount.signum() < 0) {
            throw new IllegalArgumentException(
                    "납부 금액은 0 이상이어야 합니다."
            );
        }
    }

    private static void validateDelinquency(
            LocalDate dueDate,
            LocalDate lastAssessedDate,
            BigDecimal overdueAmount
    ) {
        if (!dueDate.isBefore(lastAssessedDate)) {
            throw new IllegalArgumentException(
                    "상환 예정일이 지나지 않았습니다."
            );
        }

        if (overdueAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "미납 금액이 존재하지 않습니다."
            );
        }
    }

    private static void validateAssessmentValues(
            BigDecimal scheduledAmount,
            BigDecimal paidAmount,
            LocalDate lastAssessedDate
    ) {
        if (scheduledAmount == null
                || paidAmount == null
                || lastAssessedDate == null) {
            throw new IllegalArgumentException(
                    "예정 금액, 납부 금액, 마지막 판정 기준일은 필수입니다."
            );
        }

        if (scheduledAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "예정 금액은 0보다 커야 합니다."
            );
        }

        if (paidAmount.signum() < 0) {
            throw new IllegalArgumentException(
                    "납부 금액은 0 이상이어야 합니다."
            );
        }
    }

    @PreUpdate
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
