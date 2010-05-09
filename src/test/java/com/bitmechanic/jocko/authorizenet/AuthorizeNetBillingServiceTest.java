package com.bitmechanic.jocko.authorizenet;

import com.bitmechanic.jocko.*;
import com.bitmechanic.util.PropertyUtil;
import junit.framework.TestCase;

import java.util.*;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 3, 2010
 */
public class AuthorizeNetBillingServiceTest extends TestCase {

    AuthorizeNetBillingService service;
    List<String> profileIdsToCleanUp = new ArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Properties props = PropertyUtil.loadProperties("jocko-test.properties");

        service = new AuthorizeNetBillingService(props.getProperty("authorizeName"), props.getProperty("authorizeKey"), props.getProperty("authorizeURL"));
    }

    public void testPaymentMethodLifecycle() throws Exception {
        // create test profile
        String profileId = service.createProfile(UUID.randomUUID().toString());
        assertNotNull(profileId);
        profileIdsToCleanUp.add(profileId);
        // create payment method
        CreditCardPaymentMethod method = new CreditCardPaymentMethod();
        // billing profile id
        method.setBillingProfileId(profileId);
        // address
        method.setAddress("666 Hell Street");
        method.setCity("Troya");
        method.setCompany("Hungry Vulture LLC");
        method.setCountry("Columbia");
        method.setFirstName("John");
        method.setLastName("Smith");
        method.setState("Death Valley");
        method.setZip("54321");
        // credit card info
        method.setCardNumber("4111111111111111");
        method.setExpirationDate("2015-12");
        method.setCardCode("123");
        // create it
        String paymentMethodId = service.createPaymentMethod(method);
        assertNotNull(paymentMethodId);
        // reload and compare
        List<PaymentMethod> methods = service.getPaymentMethodsByProfileId(profileId);
        assertNotNull(methods);
        assertEquals(1, methods.size());
        PaymentMethod method2 = methods.get(0);
        assertPaymentMethodsEquals(method, method2);
        // update method2
        method2.setFirstName("Mike");
        method2.setLastName("Hawk");
        method2.setCompany("Spark Central LLC");
        method2.setAddress("123 Rhythm Street");
        method2.setCity("Ontario");
        method2.setState("New Guinea");
        method2.setZip("12345");
        method2.setCountry("Canada");
        // update it
        service.updatePaymentMethod(method2);
        // reload and compare
        methods = service.getPaymentMethodsByProfileId(profileId);
        assertNotNull(methods);
        assertEquals(1, methods.size());
        PaymentMethod method3 = methods.get(0);
        assertPaymentMethodsEquals(method2, method3);
        // delete - and verify it was deleted
        service.deletePaymentMethod(profileId, method3.getPaymentMethodId());
        methods = service.getPaymentMethodsByProfileId(profileId);
        assertNotNull(methods);
        assertEquals(0, methods.size());
    }

    public void testCreateAuthTransaction() throws Exception {
        // create test profile
        String profileId = service.createProfile(UUID.randomUUID().toString());
        assertNotNull(profileId);
        profileIdsToCleanUp.add(profileId);
        // create test payment method
        PaymentMethod method = createTestPaymentMethod(profileId);
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(CreateTransactionRequest.Type.AuthOnly);
        request.setBillingProfileId(method.getBillingProfileId());
        request.setBillingPaymentMethodId(method.getPaymentMethodId());
        request.setTaxAmount(Math.random() * 10);
        request.setTotalAmount(Math.random() * 100 + 10);
        request.setExternalSystemOrderId(UUID.randomUUID().toString());

        CreateTransactionResponse response = service.createAuthTransaction(request);
        assertNotNull(response);
        assertNotNull(response.getTransactionId());
        assertNotNull(response.getRawResponse());
        assertEquals(CreateTransactionResponse.Status.Approved, response.getStatus());
        assertEquals("This transaction has been approved.", response.getStatusDescription());
    }

    private PaymentMethod createTestPaymentMethod(String profileId) throws Exception {
        CreditCardPaymentMethod method = new CreditCardPaymentMethod();

        method.setBillingProfileId(profileId);
        method.setAddress("666 Hell Street");
        method.setCity("Troya");
        method.setCompany("Hungry Vulture LLC");
        method.setCountry("Columbia");
        method.setFirstName("John");
        method.setLastName("Smith");
        method.setState("Death Valley");
        method.setZip("54321");
        method.setCardNumber("4111111111111111");
        method.setExpirationDate("2015-12");
        method.setCardCode("123");

        String paymentMethodId = service.createPaymentMethod(method);
        assertNotNull(paymentMethodId);
        assertEquals(paymentMethodId, method.getPaymentMethodId());

        return method;
    }

    private void assertPaymentMethodsEquals(PaymentMethod method1, PaymentMethod method2) {
        assertNotNull(method1);
        assertNotNull(method2);
        // billing address
        assertEquals(method1.getBillingProfileId(), method2.getBillingProfileId());
        assertEquals(method1.getPaymentMethodId(), method2.getPaymentMethodId());
        assertEquals(method1.getAddress(), method2.getAddress());
        assertEquals(method1.getCity(), method2.getCity());
        assertEquals(method1.getCompany(), method2.getCompany());
        assertEquals(method1.getCountry(), method2.getCountry());
        assertEquals(method1.getFirstName(), method2.getFirstName());
        assertEquals(method1.getLastName(), method2.getLastName());
        assertEquals(method1.getState(), method2.getState());
        assertEquals(method1.getZip(), method2.getZip());
        // credit card (masked)
        assertTrue(method1 instanceof CreditCardPaymentMethod);
        assertTrue(method2 instanceof CreditCardPaymentMethod);
        CreditCardPaymentMethod cc1 = (CreditCardPaymentMethod) method1;
        CreditCardPaymentMethod cc2 = (CreditCardPaymentMethod) method2;
        String ccNumber1 = cc1.getCardNumber();
        String ccNumber2 = cc2.getCardNumber();
        assertEquals(ccNumber1.substring(ccNumber1.length() - 4), ccNumber2.substring(ccNumber2.length() - 4));
    }

    @Override
    protected void tearDown() throws Exception {
        for (String profile : profileIdsToCleanUp) {
            try {
                service.deleteProfile(profile);
            } catch (BillingServiceException e) {
                // this is the error code for not found - not in love with this code either
                if (null != e.getMessage() && e.getMessage().contains("[E00040]")) {
                    // Ignore it
                } else {
                    throw e;
                }
            }
        }
        super.tearDown();
    }
}
