package com.rafaelsousa.algashop.billingscheduler.infrastructure;

import com.rafaelsousa.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CancelExpiredInvoicesApplicationServiceJdbcImpl implements CancelExpiredInvoicesApplicationService {

    @Override
    public void cancelExpiredInvoices() {
        log.info("Canceling expired invoices...");
    }
}