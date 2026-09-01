package com.loan.loan_management.delinquency.application;

import com.loan.loan_management.delinquency.api.dto.DelinquencyAssessmentRequest;
import com.loan.loan_management.delinquency.api.dto.DelinquencyAssessmentResponse;
import com.loan.loan_management.delinquency.domain.AssessmentResult;
import com.loan.loan_management.delinquency.domain.DelinquencyStatus;
import com.loan.loan_management.delinquency.domain.InstallmentDelinquencyEntity;
import com.loan.loan_management.delinquency.domain.InstallmentDelinquencyStatus;
import com.loan.loan_management.delinquency.infrastructure.InstallmentDelinquencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DelinquencyAssessmentServiceTest {

    private static final BigDecimal SCHEDULED_AMOUNT = new BigDecimal("100000");

    @Autowired
    private DelinquencyAssessmentService service;

    @Autowired
    private InstallmentDelinquencyRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void returnsNormalWithoutSavingWhenThereIsNoDelinquencyHistory() {
        DelinquencyAssessmentRequest request = request(
                "LOAN-NORMAL",
                1,
                SCHEDULED_AMOUNT
        );

        DelinquencyAssessmentResponse response = service.assessment(request);

        assertThat(response.result()).isEqualTo(AssessmentResult.NORMAL);
        assertThat(response.loanDelinquencyStatus()).isEqualTo(DelinquencyStatus.NORMAL);
        assertThat(response.overdueAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(repository.count()).isZero();
    }

    @Test
    void createsOpenDelinquencyAndReturnsDelinquentLoanStatus() {
        DelinquencyAssessmentRequest request = request(
                "LOAN-DELINQUENT",
                1,
                BigDecimal.ZERO
        );

        DelinquencyAssessmentResponse response = service.assessment(request);
        InstallmentDelinquencyEntity saved = repository
                .findByLoanIdAndInstallmentNumber("LOAN-DELINQUENT", 1)
                .orElseThrow();

        assertThat(response.result()).isEqualTo(AssessmentResult.DELINQUENT);
        assertThat(response.loanDelinquencyStatus()).isEqualTo(DelinquencyStatus.DELINQUENT);
        assertThat(response.overdueAmount()).isEqualByComparingTo(SCHEDULED_AMOUNT);
        assertThat(saved.getStatus()).isEqualTo(InstallmentDelinquencyStatus.OPEN);
        assertThat(saved.getOverdueAmount()).isEqualByComparingTo(SCHEDULED_AMOUNT);
        assertThat(saved.getLastAssessedDate()).isEqualTo(response.assessmentDate());
    }

    @Test
    void updatesCurrentAmountsWhenOpenDelinquencyIsAssessedAgain() {
        service.assessment(request("LOAN-PARTIAL", 1, BigDecimal.ZERO));

        DelinquencyAssessmentResponse response = service.assessment(
                request("LOAN-PARTIAL", 1, new BigDecimal("30000"))
        );
        InstallmentDelinquencyEntity saved = repository
                .findByLoanIdAndInstallmentNumber("LOAN-PARTIAL", 1)
                .orElseThrow();

        assertThat(response.result()).isEqualTo(AssessmentResult.DELINQUENT);
        assertThat(response.overdueAmount()).isEqualByComparingTo("70000");
        assertThat(saved.getPaidAmount()).isEqualByComparingTo("30000");
        assertThat(saved.getOverdueAmount()).isEqualByComparingTo("70000");
        assertThat(saved.getStatus()).isEqualTo(InstallmentDelinquencyStatus.OPEN);
    }

    @Test
    void resolvesLastOpenInstallmentAndReturnsCuredLoanStatus() {
        service.assessment(request("LOAN-CURED", 1, BigDecimal.ZERO));

        DelinquencyAssessmentResponse response = service.assessment(
                request("LOAN-CURED", 1, SCHEDULED_AMOUNT)
        );
        InstallmentDelinquencyEntity saved = repository
                .findByLoanIdAndInstallmentNumber("LOAN-CURED", 1)
                .orElseThrow();

        assertThat(response.result()).isEqualTo(AssessmentResult.NORMAL);
        assertThat(response.loanDelinquencyStatus()).isEqualTo(DelinquencyStatus.CURED);
        assertThat(response.overdueAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getStatus()).isEqualTo(InstallmentDelinquencyStatus.RESOLVED);
        assertThat(saved.getOverdueAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getResolvedDate()).isEqualTo(response.assessmentDate());
    }

    @Test
    void reopensResolvedInstallmentWhenUnpaidAmountAppearsAgain() {
        service.assessment(request("LOAN-REOPEN", 1, BigDecimal.ZERO));
        service.assessment(request("LOAN-REOPEN", 1, SCHEDULED_AMOUNT));

        DelinquencyAssessmentResponse response = service.assessment(
                request("LOAN-REOPEN", 1, new BigDecimal("50000"))
        );
        InstallmentDelinquencyEntity saved = repository
                .findByLoanIdAndInstallmentNumber("LOAN-REOPEN", 1)
                .orElseThrow();

        assertThat(response.result()).isEqualTo(AssessmentResult.DELINQUENT);
        assertThat(response.loanDelinquencyStatus()).isEqualTo(DelinquencyStatus.DELINQUENT);
        assertThat(saved.getStatus()).isEqualTo(InstallmentDelinquencyStatus.OPEN);
        assertThat(saved.getOverdueAmount()).isEqualByComparingTo("50000");
        assertThat(saved.getResolvedDate()).isNull();
    }

    @Test
    void keepsLoanDelinquentWhileAnotherInstallmentIsStillOpen() {
        service.assessment(request("LOAN-MULTIPLE", 1, BigDecimal.ZERO));
        service.assessment(request("LOAN-MULTIPLE", 2, BigDecimal.ZERO));

        DelinquencyAssessmentResponse response = service.assessment(
                request("LOAN-MULTIPLE", 2, SCHEDULED_AMOUNT)
        );

        assertThat(response.result()).isEqualTo(AssessmentResult.NORMAL);
        assertThat(response.loanDelinquencyStatus()).isEqualTo(DelinquencyStatus.DELINQUENT);
    }

    private DelinquencyAssessmentRequest request(
            String loanId,
            int installmentNumber,
            BigDecimal paidAmount
    ) {
        return new DelinquencyAssessmentRequest(
                loanId,
                installmentNumber,
                LocalDate.now().minusDays(1),
                SCHEDULED_AMOUNT,
                paidAmount
        );
    }
}
