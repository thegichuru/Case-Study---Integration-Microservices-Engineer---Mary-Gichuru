package ncba.case_study.service;

import ncba.case_study.config.SoapCountryInfoProperties;
import ncba.case_study.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SoapCountryClientTest {

    private SoapCountryClient soapCountryClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();

        SoapCountryInfoProperties properties = new SoapCountryInfoProperties();
        properties.setBaseUrl("http://localhost/soap");
        properties.setConnectionTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));

        soapCountryClient = new SoapCountryClient(restTemplate, properties);
    }

    @Test
    void fetchCountryIsoCode_returnsParsedIsoCode() {
        server.expect(once(), requestTo("http://localhost/soap"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("SOAPAction", "\"http://www.oorsprong.org/websamples.countryinfo/CountryISOCode\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<web:sCountryName>Kenya</web:sCountryName>")))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="utf-8"?>
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                          <soap:Body>
                            <m:CountryISOCodeResponse xmlns:m="http://www.oorsprong.org/websamples.countryinfo">
                              <m:CountryISOCodeResult>KE</m:CountryISOCodeResult>
                            </m:CountryISOCodeResponse>
                          </soap:Body>
                        </soap:Envelope>
                        """, MediaType.TEXT_XML));

        String isoCode = soapCountryClient.fetchCountryIsoCode("Kenya");

        assertThat(isoCode).isEqualTo("KE");
        server.verify();
    }

    @Test
    void fetchFullCountryInfo_returnsParsedCountryInfo() {
        server.expect(once(), requestTo("http://localhost/soap"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("SOAPAction", "\"http://www.oorsprong.org/websamples.countryinfo/FullCountryInfo\""))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="utf-8"?>
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                          <soap:Body>
                            <m:FullCountryInfoResponse xmlns:m="http://www.oorsprong.org/websamples.countryinfo">
                              <m:FullCountryInfoResult>
                                <m:sISOCode>KE</m:sISOCode>
                                <m:sName>Kenya</m:sName>
                                <m:sCapitalCity>Nairobi</m:sCapitalCity>
                                <m:sPhoneCode>254</m:sPhoneCode>
                                <m:sContinentCode>AF</m:sContinentCode>
                                <m:sCurrencyISOCode>KES</m:sCurrencyISOCode>
                                <m:sCountryFlag>http://example.com/kenya.png</m:sCountryFlag>
                                <m:Languages>
                                  <m:tLanguage>
                                    <m:sISOCode>swa</m:sISOCode>
                                    <m:sName>Swahili</m:sName>
                                  </m:tLanguage>
                                  <m:tLanguage>
                                    <m:sISOCode>eng</m:sISOCode>
                                    <m:sName>English</m:sName>
                                  </m:tLanguage>
                                </m:Languages>
                              </m:FullCountryInfoResult>
                            </m:FullCountryInfoResponse>
                          </soap:Body>
                        </soap:Envelope>
                        """, MediaType.TEXT_XML));

        SoapCountryInfoResult result = soapCountryClient.fetchFullCountryInfo("KE");

        assertThat(result.name()).isEqualTo("Kenya");
        assertThat(result.capitalCity()).isEqualTo("Nairobi");
        assertThat(result.languages()).extracting(SoapLanguageResult::isoCode).containsExactly("swa", "eng");
        server.verify();
    }

    @Test
    void fetchCountryIsoCode_throwsWhenSoapCallFails() {
        server.expect(once(), requestTo("http://localhost/soap"))
                .andRespond(withServerError());

        assertThrows(ExternalServiceException.class, () -> soapCountryClient.fetchCountryIsoCode("Kenya"));
    }
}

