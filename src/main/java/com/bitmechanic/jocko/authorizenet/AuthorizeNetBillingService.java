package com.bitmechanic.jocko.authorizenet;

import com.bitmechanic.jocko.*;
import com.bitmechanic.util.Contract;
import com.bitmechanic.util.StringUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BillingService impl that uses the Authorize.net CIM
 * See docs here: http://developer.authorize.net/api/cim/
 *
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 3, 2010
 */
public class AuthorizeNetBillingService implements BillingService {

    private static Log log = LogFactory.getLog(AbstractBlobBackedPersistenceService.class);
    
    private static final String AUTHORIZE_NET_DUPLICATED_RECORD_ERROR_CODE = "E00039";

    private String authorizeName;
    private String authorizeKey;
    private String authorizeURL;

    public AuthorizeNetBillingService(String authorizeName, String authorizeKey, String authorizeURL) {
        Contract.notNullOrEmpty(authorizeName, "authorizeName cannot be empty");
        Contract.notNullOrEmpty(authorizeKey, "authorizeKey cannot be empty");
        Contract.notNullOrEmpty(authorizeURL, "authorizeURL cannot be empty");

        this.authorizeName = authorizeName;
        this.authorizeKey = authorizeKey;
        this.authorizeURL = authorizeURL;
    }

    ////////////////////////////////////
    // Profile methods //
    /////////////////////

    public String createProfile(String description) throws BillingServiceException {
        // define mandatory profile fields here
        Contract.notNullOrEmpty(description, "description cannot be empty");
        // start xml
        StringBuilder xmlBuilder = startXML();
        // open profile node
        xmlBuilder.append("<profile>");
        // we map our client UUID to the profile description field which serves as a primary key
        xmlBuilder.append("<description>").append(StringEscapeUtils.escapeXml(description)).append("</description>");
        // close profile
        xmlBuilder.append("</profile>");
        // close document root
        String xml = closeXML(xmlBuilder);
        xml = prepareXml(xml, "createCustomerProfileRequest");

        String customerProfileId = null;
        org.w3c.dom.Document doc = null;

        try {
            doc = sendRequest(xml);
        } catch (BillingServiceException bse) {
            // we are interested on some particular exceptions like duplicated records
            if (null != bse.getErrors() && bse.getErrors().containsKey(AUTHORIZE_NET_DUPLICATED_RECORD_ERROR_CODE)){
                String msg = bse.getErrors().get(AUTHORIZE_NET_DUPLICATED_RECORD_ERROR_CODE);
                Pattern pattern = Pattern.compile("[0-9]+");
                Matcher matcher = pattern.matcher(msg);
                if (matcher.find()) {
                    customerProfileId = matcher.group();
                    return customerProfileId;
                }
            }
            // fallback throw
            throw bse;
        }

        if (doc != null) {
            try {
                javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
                customerProfileId = xpath.evaluate("/*/customerProfileId/text()", doc);
            }
            catch (Exception ex) {
                throw new BillingServiceException(ex);
            }
        }

        if (customerProfileId == null)
            throw new BillingServiceException("Unable to parse profileId from response");
        else
            return customerProfileId;

    }

    public void deleteProfile(String profileId) throws BillingServiceException {
        Contract.notNullOrEmpty(profileId, "profileId cannot be empty");
        // start xml
        StringBuilder xmlBuilder = startXML();
        // profile id
        xmlBuilder.append("<customerProfileId>").append(StringEscapeUtils.escapeXml(profileId)).append("</customerProfileId>");
        // close document root
        String xml = closeXML(xmlBuilder);
        xml = prepareXml(xml, "deleteCustomerProfileRequest");
        // sendRequest checks for 'ok' in the response, which is all we care about
        sendRequest(xml);
    }

    public List<PaymentMethod> getPaymentMethodsByProfileId(String profileId) throws BillingServiceException {
        Contract.notNullOrEmpty(profileId, "profileId cannot be empty");

        List<PaymentMethod> paymentMethods = new ArrayList<PaymentMethod>();

        StringBuilder xmlBuilder = startXML();
        // profile id
        xmlBuilder.append("<customerProfileId>");
        xmlBuilder.append(StringEscapeUtils.escapeXml(profileId));
        xmlBuilder.append("</customerProfileId>");
        // close document root
        String xml = closeXML(xmlBuilder);
        xml = prepareXml(xml, "getCustomerProfileRequest");

        org.w3c.dom.Document doc;

        try {
            doc = sendRequest(xml);
        } catch (BillingServiceException e) {
            // this is the error code for not found - not in love with this code either
            if (null != e.getMessage() && e.getMessage().contains("[E00040]")) {
                return paymentMethods;
            } else {
                throw e;
            }
        }

        if (doc != null) {

            try {
                javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
                String customerProfileId = xpath.evaluate("/*/profile/customerProfileId/text()", doc);
                // success - process all incoming fields
                org.w3c.dom.NodeList customerPaymentProfileList = (org.w3c.dom.NodeList) xpath.evaluate("/*/profile/paymentProfiles", doc, javax.xml.xpath.XPathConstants.NODESET);
                if (customerPaymentProfileList != null) {

                    for (int i = 0; i < customerPaymentProfileList.getLength(); i++) {
                        org.w3c.dom.Node ccNode = customerPaymentProfileList.item(i);
                        // we ha ve only credit cards as payment methods right now.
                        CreditCardPaymentMethod cc = new CreditCardPaymentMethod();
                        cc.setBillingProfileId(customerProfileId);
                        cc.setPaymentMethodId(xpath.evaluate("customerPaymentProfileId/text()", ccNode));
                        cc.setCardNumber(xpath.evaluate("payment/creditCard/cardNumber/text()", ccNode));
                        cc.setExpirationDate(xpath.evaluate("payment/creditCard/expirationDate/text()", ccNode));

                        org.w3c.dom.Node billTo = (org.w3c.dom.Node) xpath.evaluate("billTo", customerPaymentProfileList.item(i), javax.xml.xpath.XPathConstants.NODE);
                        cc.setFirstName(xpath.evaluate("firstName/text()", billTo));
                        cc.setLastName(xpath.evaluate("lastName/text()", billTo));
                        cc.setCompany(xpath.evaluate("company/text()", billTo));
                        cc.setAddress(xpath.evaluate("address/text()", billTo));
                        cc.setCity(xpath.evaluate("city/text()", billTo));
                        cc.setState(xpath.evaluate("state/text()", billTo));
                        cc.setZip(xpath.evaluate("zip/text()", billTo));
                        cc.setCountry(xpath.evaluate("country/text()", billTo));

                        paymentMethods.add(cc);
                    }
                }
            }
            catch (Exception ex) {
                throw new BillingServiceException(ex);
            }
        }
        return paymentMethods;
    }

    ////////////////////////////////////
    // Payment Methods //
    /////////////////////

    public String createPaymentMethod(PaymentMethod method) throws BillingServiceException {
        // define mandatory profile fields here
        Contract.notNull(method, "profile cannot be null");
        Contract.notNullOrEmpty(method.getBillingProfileId(), "method.billingProfileId cannot be empty");
        Contract.notNullOrEmpty(method.getAddress(), "method.address cannot be empty");
        Contract.notNullOrEmpty(method.getCity(), "method.city cannot be empty");
        Contract.notNullOrEmpty(method.getCountry(), "method.country cannot be empty");
        Contract.notNullOrEmpty(method.getFirstName(), "method.firstName cannot be empty");
        Contract.notNullOrEmpty(method.getLastName(), "method.lastName cannot be empty");
        Contract.notNullOrEmpty(method.getState(), "method.state cannot be empty");
        Contract.notNullOrEmpty(method.getZip(), "method.zip cannot be empty");
        // start xml
        StringBuilder xmlBuilder = startXML();
        xmlBuilder.append("<customerProfileId>");
        xmlBuilder.append(StringEscapeUtils.escapeXml(method.getBillingProfileId()));
        xmlBuilder.append("</customerProfileId>");
        // open profile node
        xmlBuilder.append("<paymentProfile>");
        xmlBuilder.append(getPaymentMethodAsXmlNode(method));
        xmlBuilder.append("</paymentProfile>");
        // close document root
        String xml = closeXML(xmlBuilder);
        xml = prepareXml(xml, "createCustomerPaymentProfileRequest");

        String customerPaymentProfileId = null;
        org.w3c.dom.Document doc = sendRequest(xml);

        if (doc != null) {
            try {
                javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
                customerPaymentProfileId = xpath.evaluate("/*/customerPaymentProfileId/text()", doc);
            }
            catch (Exception ex) {
                throw new BillingServiceException(ex);
            }
        }

        if (customerPaymentProfileId == null) {
            throw new BillingServiceException("Unable to parse customerPaymentProfileId from response");
        } else {
            method.setPaymentMethodId(customerPaymentProfileId);
            return customerPaymentProfileId;
        }
    }

    public void updatePaymentMethod(PaymentMethod method) throws BillingServiceException {
        // define mandatory profile fields here
        Contract.notNull(method, "profile cannot be null");
        Contract.notNullOrEmpty(method.getBillingProfileId(), "profile.billingProfileId cannot be empty");
        Contract.notNullOrEmpty(method.getPaymentMethodId(), "profile.paymentMethodId cannot be empty");
        Contract.notNullOrEmpty(method.getAddress(), "profile.address cannot be empty");
        Contract.notNullOrEmpty(method.getCity(), "profile.city cannot be empty");
        Contract.notNullOrEmpty(method.getCountry(), "profile.country cannot be empty");
        Contract.notNullOrEmpty(method.getFirstName(), "profile.firstName cannot be empty");
        Contract.notNullOrEmpty(method.getLastName(), "profile.lastName cannot be empty");
        Contract.notNullOrEmpty(method.getState(), "profile.state cannot be empty");
        Contract.notNullOrEmpty(method.getZip(), "profile.zip cannot be empty");

            StringBuilder xmlBuilder = startXML();
            xmlBuilder.append("<customerProfileId>").append(StringEscapeUtils.escapeXml(method.getBillingProfileId())).append("</customerProfileId>");
            xmlBuilder.append("<paymentProfile>");
            xmlBuilder.append(getPaymentMethodAsXmlNode(method));
            xmlBuilder.append("<customerPaymentProfileId>");
            xmlBuilder.append(StringEscapeUtils.escapeXml(method.getPaymentMethodId()));
            xmlBuilder.append("</customerPaymentProfileId>");
            xmlBuilder.append("</paymentProfile>");
            String xml = closeXML(xmlBuilder);
            xml = prepareXml(xml, "updateCustomerPaymentProfileRequest");


            org.w3c.dom.Document doc = sendRequest(xml);

            if (doc != null && !StringUtil.hasText(method.getPaymentMethodId())) {
                try {
                    javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
                    String customerPaymentProfileId = xpath.evaluate("/*/customerPaymentProfileId/text()", doc);
                    method.setPaymentMethodId(customerPaymentProfileId);
                }
                catch (Exception ex) {
                    throw new BillingServiceException(ex);
                }
            }
    }

    public void deletePaymentMethod(String billingProfileId, String paymentMethodId) throws BillingServiceException {
        Contract.notNullOrEmpty(billingProfileId, "billingProfileId cannot be empty");
        Contract.notNullOrEmpty(paymentMethodId, "paymentMethodId cannot be empty");

        StringBuilder xmlBuilder = startXML();
        // billing profile id
        xmlBuilder.append("<customerProfileId>");
        xmlBuilder.append(StringEscapeUtils.escapeXml(billingProfileId));
        xmlBuilder.append("</customerProfileId>");
        // paymentMethodId id
        xmlBuilder.append("<customerPaymentProfileId>");
        xmlBuilder.append(StringEscapeUtils.escapeXml(paymentMethodId));
        xmlBuilder.append("</customerPaymentProfileId>");
        // close document root
        String xml = closeXML(xmlBuilder);
        xml = prepareXml(xml, "deleteCustomerPaymentProfileRequest");

        // sendRequest checks for 'ok' in the response, which is all we care about
        sendRequest(xml);
    }


    ////////////////////////////////////
    // Transaction //
    /////////////////

    public CreateTransactionResponse createAuthTransaction(CreateTransactionRequest request) throws BillingServiceException {
        Contract.notNull(request, "request cannot be null");
        Contract.notNull(request.getType(), "request.type cannot be null");
        Contract.notNullOrEmpty(request.getBillingProfileId(), "request.billingProfileId cannot be empty");
        Contract.notNullOrEmpty(request.getBillingPaymentMethodId(), "request.billingPaymentMethodId cannot be empty");
        Contract.ensure(request.getTotalAmount() > 0, "request.totalAmount cannot be zero");
        //Contract.ensure(request.getTaxAmount() > 0, "request.taxAmount cannot be zero");

        DecimalFormat amountFormat = new DecimalFormat("#0.0000");

        StringBuilder xmlBuilder = startXML();
        xmlBuilder.append("<transaction>");
        // use request.type to determine whether to do:
        // <profileTransAuthOnly> or <profileTransAuthCapture>
        if (request.getType().equals(CreateTransactionRequest.Type.AuthOnly)) {
            xmlBuilder.append("<profileTransAuthOnly>");
        } else if (request.getType().equals(CreateTransactionRequest.Type.AuthCapture)) {
            xmlBuilder.append("<profileTransAuthCapture>");
        }
        // total amount including tax
        xmlBuilder.append("<amount>");
        xmlBuilder.append(amountFormat.format(request.getTotalAmount()));
        xmlBuilder.append("</amount>");
        // tax amount
        xmlBuilder.append("<tax>");
        xmlBuilder.append("<amount>");
        xmlBuilder.append(amountFormat.format(request.getTaxAmount()));
        xmlBuilder.append("</amount>");
        xmlBuilder.append("</tax>");

        // customerProfileId
        xmlBuilder.append("<customerProfileId>");
        xmlBuilder.append(StringEscapeUtils.escapeXml(request.getBillingProfileId()));
        xmlBuilder.append("</customerProfileId>");
        // customerPaymentProfileId
        xmlBuilder.append("<customerPaymentProfileId>");
        xmlBuilder.append(StringEscapeUtils.escapeXml(request.getBillingPaymentMethodId()));
        xmlBuilder.append("</customerPaymentProfileId>");
        // external order id
        xmlBuilder.append("<order><description>");
        xmlBuilder.append(StringEscapeUtils.escapeXml(request.getExternalSystemOrderId()));
        xmlBuilder.append("</description></order>");

        if (request.getType().equals(CreateTransactionRequest.Type.AuthOnly)) {
            xmlBuilder.append("</profileTransAuthOnly>");
        } else if (request.getType().equals(CreateTransactionRequest.Type.AuthCapture)) {
            xmlBuilder.append("</profileTransAuthCapture>");
        }

        xmlBuilder.append("</transaction>");

        // close document root
        String xml = closeXML(xmlBuilder);
        xml = prepareXml(xml, "createCustomerProfileTransactionRequest");
        org.w3c.dom.Document doc = sendRequest(xml);

        CreateTransactionResponse response = null;

        if (doc != null) {
            try {
                javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
                //   We would set response.rawResponse to the contents of <directResponse>
                //   That would let us parse additional info later if we needed to
                //     - example: the address verification results
                //   For now I just want to keep that raw data, but I don't see a need to parse other fields yet
                String rawResponse = xpath.evaluate("/*/directResponse/text()", doc);
                response = parseTransactionRawResponse(rawResponse);
            }
            catch (Exception ex) {
                throw new BillingServiceException(ex);
            }
        }

        return response;
    }

    ////////////////////////////////////
    // Private //
    /////////////

    private CreateTransactionResponse parseTransactionRawResponse(String rawResponse) {
        CreateTransactionResponse response = new CreateTransactionResponse();
        response.setRawResponse(rawResponse);
        //   Fields we are interested in include:
        //     0 - Response Code (maps to status)
        //     3 - Reason text (maps to statusDescription)
        //     6 - transaction id (maps to transactionId)
        String[] fields = rawResponse.split(",");
        switch (Integer.parseInt(fields[0])) {
            case 1:
                response.setStatus(CreateTransactionResponse.Status.Approved);
                break;
            case 2:
                response.setStatus(CreateTransactionResponse.Status.Declined);
                break;
            default:
            response.setStatus(CreateTransactionResponse.Status.Error);
                break;
        }
        response.setStatusDescription(fields[3]);
        response.setTransactionId(fields[6]);
        response.setDateCreated(System.currentTimeMillis());
        return response;
    }

    private String prepareXml(String xml, String apiMethod) {
        xml = xml.replace("$xmldecl$", "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml = xml.replace("$xmlns$", "xmlns=\"AnetApi/xml/v1/schema/AnetApiSchema.xsd\"");
        xml = xml.replace("$merchauth$", "<merchantAuthentication><name>" + authorizeName + "</name><transactionKey>" + authorizeKey + "</transactionKey></merchantAuthentication>");
        xml = xml.replace("$apiMethod$", apiMethod);
        return xml;
    }

    private org.w3c.dom.Document sendRequest(String xml) throws BillingServiceException {
        log.debug("Posting data to " + authorizeURL);
        // mask sensible data before logging xml
        log.debug(maskSensitiveXmlData(xml));

        try {
            java.net.URL url = new java.net.URL(authorizeURL);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setAllowUserInteraction(true);
            // POST the data
            java.io.OutputStreamWriter sw = new java.io.OutputStreamWriter(conn.getOutputStream(), "UTF8");
            sw.write(xml);
            sw.flush();
            sw.close();

            // Get the Response
            java.io.InputStream resultStream = conn.getInputStream();
            java.io.BufferedReader aReader = new java.io.BufferedReader(new java.io.InputStreamReader(resultStream, "UTF8"));
            StringBuffer aResponse = new StringBuffer();
            String aLine = aReader.readLine();
            while (aLine != null) {
                aResponse.append(aLine);
                aLine = aReader.readLine();
            }
            resultStream.close();

            log.debug("Response: " + aResponse.toString());

            // Remove BOM because the current version of InputStreamReader doesn't do that for you as it should.
            if (aResponse.length() > 0 && (int) aResponse.charAt(0) == 0xFEFF) {
                aResponse.deleteCharAt(0);
            }

            // Parse the Response
            javax.xml.parsers.DocumentBuilder docBuilder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            java.io.StringReader stringReader = new java.io.StringReader(aResponse.toString());
            org.w3c.dom.Document doc = docBuilder.parse(new org.xml.sax.InputSource(stringReader));
            stringReader.close();

            // Check API response for errors
            javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xpathFactory.newXPath();
            String resultCode = xpath.evaluate("/*/messages/resultCode/text()", doc);

            if ("Ok".equalsIgnoreCase(resultCode)) {
                // API call was successful
                return doc;
            } else {
                // Print errors
                log.error("The API call was not successful.");

                StringBuilder errs = new StringBuilder("Authorize.net request failed.  Error list: ");
                Map<String, String> errors = new HashMap<String, String>();
                org.w3c.dom.NodeList nodes = (org.w3c.dom.NodeList) xpath.evaluate("/*/messages/message", doc, javax.xml.xpath.XPathConstants.NODESET);
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        org.w3c.dom.Node node = nodes.item(i);
                        String code = xpath.evaluate("code/text()", node);
                        String text = xpath.evaluate("text/text()", node);
                        errs.append("[").append(code).append("] ").append(text).append(" \t");
                        errors.put(code, text);
                    }
                }
                throw new BillingServiceException(errs.toString(), errors);
            }

        } catch (BillingServiceException be) {
            throw be;
        } catch (Exception ex) {
            throw new BillingServiceException(ex);
        }
    }

    private String getPaymentMethodAsXmlNode(PaymentMethod method) {
        StringBuilder b = new StringBuilder();

        // add billing address
        b.append("<billTo>");

        b.append("<firstName>").append(StringEscapeUtils.escapeXml(method.getFirstName())).append("</firstName>");
        b.append("<lastName>").append(StringEscapeUtils.escapeXml(method.getLastName())).append("</lastName>");
        if (StringUtil.hasText(method.getCompany()))
            b.append("<company>").append(StringEscapeUtils.escapeXml(method.getCompany())).append("</company>");
        b.append("<address>").append(StringEscapeUtils.escapeXml(method.getAddress())).append("</address>");
        b.append("<city>").append(StringEscapeUtils.escapeXml(method.getCity())).append("</city>");
        b.append("<state>").append(StringEscapeUtils.escapeXml(method.getState())).append("</state>");
        b.append("<zip>").append(StringEscapeUtils.escapeXml(method.getZip())).append("</zip>");
        b.append("<country>").append(StringEscapeUtils.escapeXml(method.getCountry())).append("</country>");

        b.append("</billTo>");

        b.append("<payment>");

        // I'm not in love with this code, but it keeps the
        // marshalling in one place.

        if (method instanceof CreditCardPaymentMethod) {
            CreditCardPaymentMethod card = (CreditCardPaymentMethod) method;

            b.append("<creditCard>");

            b.append("<cardNumber>");
            b.append(StringEscapeUtils.escapeXml(card.getCardNumber()));
            b.append("</cardNumber>");

            b.append("<expirationDate>");
            b.append(StringEscapeUtils.escapeXml(card.getExpirationDate()));
            b.append("</expirationDate>");

            // if we are updating don't include the node
            if (null != card.getCardCode()) {
                b.append("<cardCode>");
                b.append(StringEscapeUtils.escapeXml(card.getCardCode()));
                b.append("</cardCode>");
            }

            b.append("</creditCard>");
        } else {
            throw new IllegalArgumentException("Unsupported PaymentMethod: " + method.getClass().getCanonicalName());
        }

        b.append("</payment>");

        return b.toString();
    }

    private String maskSensitiveXmlData(String xml) {
        // mask merchant auth info
        xml = xml.replaceAll("<merchantAuthentication>.*</merchantAuthentication>", "<merchantAuthentication>XXXXXXXX</merchantAuthentication>");
        // mask credit card data
        xml = xml.replaceAll("<cardNumber>.*</cardNumber>", "<cardNumber>XXXX-XXXX-XXXX-XXXX</cardNumber>");
        xml = xml.replaceAll("<expirationDate>.*</expirationDate>", "<expirationDate>XX-XX</expirationDate>");
        xml = xml.replaceAll("<cardCode>.*</cardCode>", "<cardCode>XXX</cardCode>");
        return xml;
    }

    private StringBuilder startXML() {
        // xmlBuilder declaration and document root
        StringBuilder xmlBuilder = new StringBuilder("$xmldecl$\r\n<$apiMethod$ $xmlns$>");
        // merchant authorization info
        xmlBuilder.append("$merchauth$");
        return xmlBuilder;
    }

    private String closeXML(StringBuilder xml) {
        return xml.append("</$apiMethod$>").toString();
    }

}
