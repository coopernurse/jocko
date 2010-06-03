package com.bitmechanic.jocko.fake;

import com.bitmechanic.jocko.BillingService;
import com.bitmechanic.jocko.BillingServiceException;
import com.bitmechanic.jocko.CreateTransactionRequest;
import com.bitmechanic.jocko.CreateTransactionResponse;
import com.bitmechanic.jocko.PaymentMethod;
import com.bitmechanic.util.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Apr 9, 2010
 */
public class FakeBillingService implements BillingService {

    private List<CreateTransactionRequest> createAuthTrans;
    private Set<String> profileIds;

    private List<PaymentMethod> paymentMethods;

    private int profileCount;

    public FakeBillingService() {
        createAuthTrans = new ArrayList<CreateTransactionRequest>();
        profileIds = new TreeSet<String>();
        paymentMethods = new ArrayList<PaymentMethod>();
        profileCount = 0;
    }

    public List<CreateTransactionRequest> getCreateAuthTrans() {
        return createAuthTrans;
    }

    //////////////////////////////////

    @Override
    public CreateTransactionResponse createAuthTransaction(CreateTransactionRequest request) throws BillingServiceException {
        createAuthTrans.add(request);
        CreateTransactionResponse response = new CreateTransactionResponse();
        response.setDateCreated(System.currentTimeMillis());
        response.setRawResponse("response");
        response.setStatus(CreateTransactionResponse.Status.Approved);
        response.setStatusDescription(response.getStatus().toString());
        response.setTransactionId(UUID.randomUUID().toString());
        return response;
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
        profileIds.remove(profileId);
    }

    @Override
    public String createPaymentMethod(PaymentMethod method) throws BillingServiceException {
        Contract.notNullOrEmpty(method.getBillingProfileId(), "billingProfileId cannot be empty");

        String paymentMethodId = UUID.randomUUID().toString();
        method.setPaymentMethodId(paymentMethodId);
        paymentMethods.add(method);
        return paymentMethodId;
    }

    @Override
    public void updatePaymentMethod(PaymentMethod method) throws BillingServiceException {
        Contract.notNullOrEmpty(method.getPaymentMethodId(), "paymentMethodId cannot be empty");

        PaymentMethod oldMethod = findPaymentMethodById(method.getPaymentMethodId());
        
        if (oldMethod != null) {
            paymentMethods.remove(oldMethod);
            paymentMethods.add(method);
        }
    }

    @Override
    public void deletePaymentMethod(String billingProfileId, String paymentMethodId) throws BillingServiceException {
        PaymentMethod method = findPaymentMethodById(paymentMethodId);
        if (method != null) {
            paymentMethods.remove(method);
        }
    }

    @Override
    public List<PaymentMethod> getPaymentMethodsByProfileId(String profileId) throws BillingServiceException {
        List<PaymentMethod> methodsForProfile = new ArrayList<PaymentMethod>();
        for (PaymentMethod pm : paymentMethods) {
            if (pm.getBillingProfileId().equals(profileId)) {
                methodsForProfile.add(pm);
            }
        }

        return methodsForProfile;
    }

    private PaymentMethod findPaymentMethodById(String paymentMethodId) {
        for (PaymentMethod pm : paymentMethods) {
            if (pm.getPaymentMethodId().equals(paymentMethodId)) {
                return pm;
            }
        }

        // not found
        return null;
    }
}
