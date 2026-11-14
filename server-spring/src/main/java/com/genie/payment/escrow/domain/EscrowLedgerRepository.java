package com.genie.payment.escrow.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EscrowLedgerRepository extends JpaRepository<EscrowLedger, Long> {
}
