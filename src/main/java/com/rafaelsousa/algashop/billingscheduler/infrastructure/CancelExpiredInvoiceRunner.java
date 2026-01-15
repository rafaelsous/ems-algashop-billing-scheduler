package com.rafaelsousa.algashop.billingscheduler.infrastructure;

import com.rafaelsousa.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelExpiredInvoiceRunner implements ApplicationRunner {
    private final CancelExpiredInvoicesApplicationService cancelExpiredInvoicesApplicationService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Task started - Cancelling expired invoices.");

        cancelExpiredInvoicesApplicationService.cancelExpiredInvoices();

        log.info("Task ended - Expired invoices.");
    }
}