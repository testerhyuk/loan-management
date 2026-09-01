package com.loan.loan_management.delinquency.application;

import com.loan.loan_management.delinquency.api.dto.DelinquencyAssessmentRequest;
import com.loan.loan_management.delinquency.api.dto.DelinquencyAssessmentResponse;
import com.loan.loan_management.delinquency.domain.AssessmentResult;
import com.loan.loan_management.delinquency.domain.DelinquencyStatus;
import com.loan.loan_management.delinquency.domain.InstallmentDelinquencyEntity;
import com.loan.loan_management.delinquency.domain.InstallmentDelinquencyStatus;
import com.loan.loan_management.delinquency.infrastructure.InstallmentDelinquencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DelinquencyAssessmentService {
    private final InstallmentDelinquencyRepository installmentDelinquencyRepository;

    @Transactional
    public DelinquencyAssessmentResponse assessment(DelinquencyAssessmentRequest request) {
        BigDecimal unpaidAmount = request.scheduledAmount()
                .subtract(request.paidAmount())
                .max(BigDecimal.ZERO);

        LocalDate assessmentDate = LocalDate.now();
        AssessmentResult result = determineAssessmentResult(
                request.dueDate(),
                assessmentDate,
                unpaidAmount
        );

        BigDecimal installmentOverdueAmount =
                result == AssessmentResult.DELINQUENT
                        ? unpaidAmount
                        : BigDecimal.ZERO;

        Optional<InstallmentDelinquencyEntity> existing = installmentDelinquencyRepository.findByLoanIdAndInstallmentNumber(
                request.loanId(),
                request.installmentNumber()
        );

        if (result == AssessmentResult.DELINQUENT) {
            handleDelinquentAssessment(existing, request, assessmentDate);
        } else {
            handleNormalAssessment(existing, request, assessmentDate);
        }

        DelinquencyStatus loanDelinquencyStatus =
                determineLoanDelinquencyStatus(request.loanId());

        return DelinquencyAssessmentResponse.toResponse(
                request,
                assessmentDate,
                result,
                installmentOverdueAmount,
                loanDelinquencyStatus
        );
    }

    private void handleDelinquentAssessment(
            Optional<InstallmentDelinquencyEntity> existing,
            DelinquencyAssessmentRequest request,
            LocalDate assessmentDate
    ) {
        if (existing.isEmpty()) {
            InstallmentDelinquencyEntity entity =
                    InstallmentDelinquencyEntity.create(
                            request.loanId(),
                            request.installmentNumber(),
                            request.dueDate(),
                            request.scheduledAmount(),
                            request.paidAmount(),
                            assessmentDate
                    );

            installmentDelinquencyRepository.save(entity);
        } else {
            InstallmentDelinquencyEntity delinquency = existing.get();

            delinquency.updateAsDelinquent(
                    request.scheduledAmount(),
                    request.paidAmount(),
                    assessmentDate
            );
        }
    }

    private void handleNormalAssessment(
            Optional<InstallmentDelinquencyEntity> existing,
            DelinquencyAssessmentRequest request,
            LocalDate assessmentDate
    ) {
        existing.ifPresent(delinquency ->
                delinquency.resolve(
                        request.scheduledAmount(),
                        request.paidAmount(),
                        assessmentDate
                )
        );
    }

    private AssessmentResult determineAssessmentResult(
            LocalDate dueDate,
            LocalDate assessmentDate,
            BigDecimal overdueAmount
    ) {
        if (dueDate.isBefore(assessmentDate) && overdueAmount.signum() > 0) {
            return AssessmentResult.DELINQUENT;
        }

        return AssessmentResult.NORMAL;
    }

    private DelinquencyStatus determineLoanDelinquencyStatus(String loanId) {
        boolean hasOpenDelinquency = installmentDelinquencyRepository
                .existsByLoanIdAndStatus(
                        loanId,
                        InstallmentDelinquencyStatus.OPEN
                );

        if (hasOpenDelinquency) {
            return DelinquencyStatus.DELINQUENT;
        }

        if (installmentDelinquencyRepository.existsByLoanId(loanId)) {
            return DelinquencyStatus.CURED;
        }

        return DelinquencyStatus.NORMAL;
    }
}
