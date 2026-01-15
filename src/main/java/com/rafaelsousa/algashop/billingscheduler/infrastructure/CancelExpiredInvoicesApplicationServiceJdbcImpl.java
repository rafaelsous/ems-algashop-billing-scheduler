package com.rafaelsousa.algashop.billingscheduler.infrastructure;

import com.rafaelsousa.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelExpiredInvoicesApplicationServiceJdbcImpl implements CancelExpiredInvoicesApplicationService {
    private final JdbcOperations jdbcOperations;
    private final TransactionTemplate transactionTemplate;

    private static final int BATCH_LIMIT = 5;
    private static final String UNPAID_STATUS = "UNPAID";
    private static final String CANCELED_STATUS = "CANCELED";
    private static final String CANCEL_REASON = "Invoice expired";
    private static final Duration EXPIRED_SINCE = Duration.ofDays(1);
    private static final String SELECT_EXPIRED_INVOICES_SQL = String.format("""
            SELECT id
            FROM invoice i
            WHERE i.expires_at <= NOW() - INTERVAL '%d days'
              AND i.status = ?
            LIMIT ?
            FOR UPDATE
            SKIP LOCKED
        """, EXPIRED_SINCE.toDays());
    private static final String UPDATE_INVOICE_STATUS_SQL = """
            UPDATE invoice
                SET status = ?,
                canceled_at = now(),
                cancel_reason = ?
            WHERE id = ?
        """;

    @Override
    public void cancelExpiredInvoices() {
        transactionTemplate.execute(status -> {
            List<UUID> expiredInvoicesIds = fetchExpiredInvoicesIds();
            log.info("Task - Total invoices fetched: {}", expiredInvoicesIds.size());

            int totalCanceledInvoices = updateInvoiceStatus(expiredInvoicesIds);
            log.info("Task - Total invoices canceled: {}", totalCanceledInvoices);

            return true;
        });
    }

    private List<UUID> fetchExpiredInvoicesIds() {
        PreparedStatementSetter preparedStatementSetter = ps -> {
            ps.setString(1, UNPAID_STATUS);
            ps.setInt(2, BATCH_LIMIT);
        };
        RowMapper<UUID> rowMapper = (resultSet, _) -> resultSet.getObject("id", UUID.class);

        return jdbcOperations.query(SELECT_EXPIRED_INVOICES_SQL, preparedStatementSetter, rowMapper);
    }

    private int updateInvoiceStatus(List<UUID> invoicesId) {
        int updatedInvoices = 0;

        for (UUID invoiceId: invoicesId) {
            try {
                jdbcOperations.update(UPDATE_INVOICE_STATUS_SQL, CANCELED_STATUS, CANCEL_REASON, invoiceId);
                updatedInvoices += 1;

                log.info("Task - Invoice canceled ID {}", invoiceId);
            } catch (DataAccessException ex) {
                log.error("Task - Failed to cancel invoice with ID {}", invoiceId, ex);
            }
        }

        return updatedInvoices;
    }
}