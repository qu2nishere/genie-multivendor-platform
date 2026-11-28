package com.genie.catalog.proposal.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryProposalRepository extends JpaRepository<CategoryProposal, Long> {

    /** Khóa dòng khi duyệt / từ chối — hai nhân viên bấm cùng lúc thì người sau nhận 409 (plan C1 D20). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM CategoryProposal p WHERE p.id = :id")
    Optional<CategoryProposal> findByIdForUpdate(@Param("id") Long id);
}
