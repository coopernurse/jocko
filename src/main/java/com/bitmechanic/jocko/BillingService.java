package com.bitmechanic.jocko;

import java.util.List;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 3, 2010
 */
public interface BillingService {

    public String createProfile(String description) throws BillingServiceException;
    public void deleteProfile(String profileId) throws BillingServiceException;

    public String createPaymentMethod(PaymentMethod method) throws BillingServiceException;
    public void updatePaymentMethod(PaymentMethod method) throws BillingServiceException;
    public void deletePaymentMethod(String billingProfileId, String paymentMethodId) throws BillingServiceException;

    public List<PaymentMethod> getPaymentMethodsByProfileId(String profileId) throws BillingServiceException;

    public CreateTransactionResponse createAuthTransaction(CreateTransactionRequest request) throws BillingServiceException;
    
}
