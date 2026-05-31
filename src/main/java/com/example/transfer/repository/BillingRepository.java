package com.example.transfer.repository;

import com.example.transfer.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BillingRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT t FROM Transfer t WHERE (t.fromAccount = :accountId OR t.toAccount = :accountId) " +
           "AND t.date >= :fromDate AND t.date <= :toDate ORDER BY t.date DESC")
    List<Transfer> findTransfersForAccountInDateRange(
            @Param("accountId") String accountId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}