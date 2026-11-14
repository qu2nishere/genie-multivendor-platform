package com.genie.payment.ledger.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialLedgerRepository extends JpaRepository<FinancialLedger, Long> {
}
