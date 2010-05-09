package com.bitmechanic.jocko.authorizenet;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 3, 2010
 */
public class APIUtilities {

    // TODO: change to https://api.authorize.net/xml/v1/request.api
    //
    public static final String API_URL = "https://apitest.authorize.net/xml/v1/request.api";

    // TODO: Specify your API login name
    //
    public static final String API_LOGIN_NAME = "YourApiLoginName";

    // TODO: Specify you API key
    //
    public static final String API_KEY = "YourApiKey";

    public static String prepareXml(String xml) {
        xml = xml.replace("$xmldecl$", "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml = xml.replace("$xmlns$", "xmlns=\"AnetApi/xml/v1/schema/AnetApiSchema.xsd\"");
        xml = xml.replace("$merchauth$", "<merchantAuthentication><name>" + API_LOGIN_NAME + "</name><transactionKey>" + API_KEY + "</transactionKey></merchantAuthentication>");
        return xml;
    }

    // If you have org.apache.commons.lang.StringEscapeUtils, use that instead.

    public static String escapeXml(String str) {
        str = str.replace("&", "&amp;");
        str = str.replace("<", "&lt;");
        str = str.replace(">", "&gt;");
        return str;
    }

    public static java.util.Random rand = new java.util.Random();

    public static org.w3c.dom.Document sendRequest(String xml) {
        System.out.println("Posting data to " + API_URL);
        System.out.println(xml);
        try {
            java.net.URL url = new java.net.URL(API_URL);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setAllowUserInteraction(false);
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
            System.out.println("Response:");
            System.out.println(aResponse.toString());
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
                System.out.println("The API call was not successful. Error list:");
                org.w3c.dom.NodeList nodes = (org.w3c.dom.NodeList) xpath.evaluate("/*/messages/message", doc, javax.xml.xpath.XPathConstants.NODESET);
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        org.w3c.dom.Node node = nodes.item(i);
                        String code = "", text = "";
                        code = xpath.evaluate("code/text()", node);
                        text = xpath.evaluate("text/text()", node);
                        System.out.println("[" + code + "] " + text);
                    }
                }
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

}
