package com.pe.pivot.account.domain.port.out;

import com.pe.pivot.account.domain.model.CustomerSummary;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Output Port for communicating reactively with ms-customer.
 */
public interface CustomerClientPort {
    Maybe<CustomerSummary> findCustomerById(String customerId);
}
