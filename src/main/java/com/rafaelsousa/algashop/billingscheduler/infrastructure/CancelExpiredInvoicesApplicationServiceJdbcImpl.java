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
    private final FastpayPaymentApiClient fastpayPaymentApiClient;

    private static final int BATCH_LIMIT = 50;
    private static final String UNPAID_STATUS = "UNPAID";
    private static final String CANCELED_STATUS = "CANCELED";
    private static final String CANCEL_REASON = "Invoice expired";
    private static final Duration EXPIRED_SINCE = Duration.ofDays(1);
    private static final String SELECT_EXPIRED_INVOICES_SQL = String.format("""
            SELECT i.id, ps.gateway_code
            FROM invoice i
            INNER JOIN payment_settings ps ON i.payment_settings_id = ps.id
            WHERE i.expires_at <= NOW() - INTERVAL '%d days'
              AND i.status = ?
            ORDER BY i.expires_at ASC
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
        transactionTemplate.execute(_ -> {
            List<InvoiceProjection> expiredInvoicesIds = fetchExpiredInvoicesIds();
            log.info("Task - Total invoices fetched: {}", expiredInvoicesIds.size());

            if (expiredInvoicesIds.isEmpty()) {
                log.info("Task - No expired invoices found for cancellation");

                return true;
            }

            int totalCanceledInvoices = updateInvoiceStatus(expiredInvoicesIds);
            log.info("Task - Total invoices canceled: {}", totalCanceledInvoices);

            return true;
        });
    }

    private List<InvoiceProjection> fetchExpiredInvoicesIds() {
        PreparedStatementSetter preparedStatementSetter = ps -> {
            ps.setString(1, UNPAID_STATUS);
            ps.setInt(2, BATCH_LIMIT);
        };
        RowMapper<InvoiceProjection> rowMapper = (resultSet, _) -> InvoiceProjection.builder()
                .id(resultSet.getObject("id", UUID.class))
                .paymentGatewayCode(resultSet.getString("gateway_code"))
                .build();

        return jdbcOperations.query(SELECT_EXPIRED_INVOICES_SQL, preparedStatementSetter, rowMapper);
    }

    private int updateInvoiceStatus(List<InvoiceProjection> invoices) {
        List<InvoiceProjection> cancelledInvoices = invoices.stream().filter(invoiceProjection -> {
            try {
                fastpayPaymentApiClient.cancel(invoiceProjection.getPaymentGatewayCode());
                log.info("Task - Invoice {} has the payment {} cancelled on gateway"
                        , invoiceProjection.getId(), invoiceProjection.getPaymentGatewayCode());

                return true;
            } catch (Exception ex) {
                log.error("Task - Failed to cancel invoice {} payment {} on the gateway"
                        , invoiceProjection.getId(), invoiceProjection.getPaymentGatewayCode());

                return false;
            }
        }).toList();

        List<UUID> cancelledInvoicesIds = cancelledInvoices.stream().map(InvoiceProjection::getId).toList();

        try {
            jdbcOperations.batchUpdate(UPDATE_INVOICE_STATUS_SQL,
                    cancelledInvoices,
                    cancelledInvoices.size(),
                    (ps, invoiceProjection) -> {
                        ps.setString(1, CANCELED_STATUS);
                        ps.setString(2, CANCEL_REASON);
                        ps.setObject(3, invoiceProjection.getId());
                    }
                );

            if (!cancelledInvoicesIds.isEmpty()) {
                log.info("Task - Invoices canceled IDs {}", cancelledInvoicesIds);
            }

            return cancelledInvoices.size();
        } catch (DataAccessException ex) {
            log.error("Task - Failed to cancel invoices with IDs {}", cancelledInvoicesIds, ex);

            return 0;
        }
    }
}