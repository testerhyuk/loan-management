package com.loan.loan_management.delinquency.domain;

public enum InstallmentDelinquencyStatus {
    OPEN, // 해당 회차의 미납금이 남아 있어 현재 연체 중
    RESOLVED // 해당 회차의 미납금이 모두 납부되어 연체가 해소됨
}
