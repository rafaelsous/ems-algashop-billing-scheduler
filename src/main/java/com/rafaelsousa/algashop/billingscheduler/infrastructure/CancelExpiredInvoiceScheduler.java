package com.rafaelsousa.algashop.billingscheduler.infrastructure;

import com.rafaelsousa.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelExpiredInvoiceScheduler {
    private final CancelExpiredInvoicesApplicationService cancelExpiredInvoicesApplicationService;

    @Scheduled(fixedRate = 1000 * 5) // 5 seconds
    public void runTask() {
        log.info("Task started - Cancelling expired invoices.");

        cancelExpiredInvoicesApplicationService.cancelExpiredInvoices();

        log.info("Task ended - Expired invoices.");
    }
}
