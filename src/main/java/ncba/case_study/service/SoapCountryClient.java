package ncba.case_study.service;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import ncba.case_study.config.SoapCountryInfoProperties;
import ncba.case_study.exception.ExternalServiceException;
import ncba.case_study.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class SoapCountryClient {

    private static final Logger log = LoggerFactory.getLogger(SoapCountryClient.class);
    private static final String SOAP_NAMESPACE = "http://www.oorsprong.org/websamples.countryinfo";

    private final RestTemplate restTemplate;
    private final SoapCountryInfoProperties properties;

    public SoapCountryClient(
            RestTemplate soapRestTemplate,
            SoapCountryInfoProperties properties
    ) {
        this.restTemplate = soapRestTemplate;
        this.properties = properties;
    }

    public String fetchCountryIsoCode(String countryName) {
        log.info("Calling SOAP CountryISOCode for countryName={}", countryName);
        String responseBody = sendSoapRequest(
                "CountryISOCode",
                "<web:CountryISOCode><web:sCountryName>%s</web:sCountryName></web:CountryISOCode>"
                        .formatted(escapeXml(countryName))
        );

        String isoCode = readText(responseBody, "//*[local-name()='CountryISOCodeResult']/text()");
        if (isoCode == null || isoCode.isBlank()) {
            throw new ResourceNotFoundException("No ISO code found for country '" + countryName + "'.");
        }

        return isoCode.trim().toUpperCase();
    }

    public SoapCountryInfoResult fetchFullCountryInfo(String countryIsoCode) {
        log.info("Calling SOAP FullCountryInfo for countryIsoCode={}", countryIsoCode);
        String responseBody = sendSoapRequest(
                "FullCountryInfo",
                "<web:FullCountryInfo><web:sCountryISOCode>%s</web:sCountryISOCode></web:FullCountryInfo>"
                        .formatted(escapeXml(countryIsoCode))
        );

        String basePath = "//*[local-name()='FullCountryInfoResult']";
        String isoCode = readText(responseBody, basePath + "/*[local-name()='sISOCode']/text()");
        if (isoCode == null || isoCode.isBlank()) {
            throw new ResourceNotFoundException("No full country information found for ISO code '" + countryIsoCode + "'.");
        }

        List<SoapLanguageResult> languages = readLanguages(responseBody, basePath + "/*[local-name()='Languages']/*[local-name()='tLanguage']");
        return new SoapCountryInfoResult(
                isoCode.trim().toUpperCase(),
                safeText(readText(responseBody, basePath + "/*[local-name()='sName']/text()")),
                safeText(readText(responseBody, basePath + "/*[local-name()='sCapitalCity']/text()")),
                safeText(readText(responseBody, basePath + "/*[local-name()='sPhoneCode']/text()")),
                safeText(readText(responseBody, basePath + "/*[local-name()='sContinentCode']/text()")),
                safeText(readText(responseBody, basePath + "/*[local-name()='sCurrencyISOCode']/text()")),
                safeText(readText(responseBody, basePath + "/*[local-name()='sCountryFlag']/text()")),
                languages
        );
    }

    private String sendSoapRequest(String operation, String payload) {
        String requestBody = buildEnvelope(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/xml; charset=UTF-8"));
        headers.setAccept(List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML));
        headers.add("SOAPAction", '"' + SOAP_NAMESPACE + "/" + operation + '"');

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getBaseUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody.getBytes(StandardCharsets.UTF_8), headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                throw new ExternalServiceException("SOAP operation '" + operation + "' returned an empty or unsuccessful response.");
            }

            return response.getBody();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("SOAP operation '" + operation + "' failed.", ex);
        }
    }

    private String buildEnvelope(String payload) {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:web="http://www.oorsprong.org/websamples.countryinfo">
                  <soap:Body>
                    %s
                  </soap:Body>
                </soap:Envelope>
                """.formatted(payload);
    }

    private List<SoapLanguageResult> readLanguages(String xml, String expression) {
        Document document = parseXml(xml);

        try {
            NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath()
                    .evaluate(expression, document, XPathConstants.NODESET);
            List<SoapLanguageResult> languages = new ArrayList<>();
            for (int index = 0; index < nodes.getLength(); index++) {
                Node node = nodes.item(index);
                String isoCode = readRelativeText(node, "*[local-name()='sISOCode']/text()");
                String name = readRelativeText(node, "*[local-name()='sName']/text()");
                if (isoCode != null && !isoCode.isBlank() && name != null && !name.isBlank()) {
                    languages.add(new SoapLanguageResult(isoCode.trim().toLowerCase(), name.trim()));
                }
            }
            return languages;
        } catch (Exception ex) {
            throw new ExternalServiceException("Unable to parse SOAP language response.", ex);
        }
    }

    private String readText(String xml, String expression) {
        try {
            return (String) XPathFactory.newInstance().newXPath()
                    .evaluate(expression, parseXml(xml), XPathConstants.STRING);
        } catch (Exception ex) {
            throw new ExternalServiceException("Unable to parse SOAP response body.", ex);
        }
    }

    private String readRelativeText(Node node, String expression) {
        try {
            return (String) XPathFactory.newInstance().newXPath().evaluate(expression, node, XPathConstants.STRING);
        } catch (Exception ex) {
            throw new ExternalServiceException("Unable to parse SOAP language node.", ex);
        }
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw new ExternalServiceException("Unable to parse SOAP XML document.", ex);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}


