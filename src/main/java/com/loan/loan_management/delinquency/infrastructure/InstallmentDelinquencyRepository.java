package com.loan.loan_management.delinquency.infrastructure;

import com.loan.loan_management.delinquency.domain.InstallmentDelinquencyEntity;
import com.loan.loan_management.delinquency.domain.InstallmentDelinquencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstallmentDelinquencyRepository extends JpaRepository<InstallmentDelinquencyEntity, Long> {
    Optional<InstallmentDelinquencyEntity> findByLoanIdAndInstallmentNumber(String loanId, int installmentNumber);

    boolean existsByLoanIdAndStatus(String loanId, InstallmentDelinquencyStatus status);

    boolean existsByLoanId(String loanId);
}
