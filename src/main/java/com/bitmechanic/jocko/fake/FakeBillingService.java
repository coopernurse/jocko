package com.bitmechanic.jocko.fake;

import com.bitmechanic.jocko.BillingService;
import com.bitmechanic.jocko.BillingServiceException;
import com.bitmechanic.jocko.CreateTransactionRequest;
import com.bitmechanic.jocko.CreateTransactionResponse;
import com.bitmechanic.jocko.PaymentMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Apr 9, 2010
 */
public class FakeBillingService implements BillingService {

    private List<CreateTransactionRequest> createAuthTrans;
    private Set<String> profileIds;

    private int profileCount;

    public FakeBillingService() {
        createAuthTrans = new ArrayList<CreateTransactionRequest>();
        profileIds = new TreeSet<String>();
        profileCount = 0;
    }

    public List<CreateTransactionRequest> getCreateAuthTrans() {
        return createAuthTrans;
    }

    //////////////////////////////////

    @Override
    public CreateTransactionResponse createAuthTransaction(CreateTransactionRequest request) throws BillingServiceException {
        createAuthTrans.add(request);
        return null;
    }

    @Override
    public String createProfile(String description) throws BillingServiceException {
        profileCount++;
        String profileId = String.valueOf(profileCount);
        profileIds.add(profileId);
        return profileId;
    }

    @Override
    public void deleteProfile(String profileId) throws BillingServiceException {
    }

    @Override
    public String createPaymentMethod(PaymentMethod method) throws BillingServiceException {
        return null;
    }

    @Override
    public void updatePaymentMethod(PaymentMethod method) throws BillingServiceException {
    }

    @Override
    public void deletePaymentMethod(String billingProfileId, String paymentMethodId) throws BillingServiceException {
    }

    @Override
    public List<PaymentMethod> getPaymentMethodsByProfileId(String profileId) throws BillingServiceException {
        return null;
    }
}
