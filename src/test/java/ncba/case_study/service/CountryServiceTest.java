package ncba.case_study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import ncba.case_study.dto.CountryImportResponse;
import ncba.case_study.dto.CountryUpdateRequest;
import ncba.case_study.dto.LanguageRequest;
import ncba.case_study.entity.CountryInfo;
import ncba.case_study.exception.ResourceNotFoundException;
import ncba.case_study.repository.CountryInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private CountryInfoRepository countryInfoRepository;

    @Mock
    private SoapCountryClient soapCountryClient;

    @InjectMocks
    private CountryService countryService;

    private SoapCountryInfoResult kenyaInfo;

    @BeforeEach
    void setUp() {
        kenyaInfo = new SoapCountryInfoResult(
                "KE",
                "Kenya",
                "Nairobi",
                "254",
                "AF",
                "KES",
                "http://example.com/kenya.png",
                List.of(new SoapLanguageResult("swa", "Swahili"))
        );
    }

    @Test
    void importCountry_fetchesFromSoapAndPersistsWhenNotCached() {
        when(countryInfoRepository.findByNameIgnoreCase("Kenya")).thenReturn(Optional.empty());
        when(countryInfoRepository.findByIsoCodeIgnoreCase("KE")).thenReturn(Optional.empty());
        when(soapCountryClient.fetchCountryIsoCode("Kenya")).thenReturn("KE");
        when(soapCountryClient.fetchFullCountryInfo("KE")).thenReturn(kenyaInfo);
        when(countryInfoRepository.save(any(CountryInfo.class))).thenAnswer(invocation -> {
            CountryInfo countryInfo = invocation.getArgument(0);
            setId(countryInfo, 1L);
            return countryInfo;
        });

        CountryImportResponse response = countryService.importCountry("kenya");

        assertThat(response.created()).isTrue();
        assertThat(response.source()).isEqualTo("SOAP");
        assertThat(response.country().name()).isEqualTo("Kenya");
        assertThat(response.country().isoCode()).isEqualTo("KE");
        assertThat(response.country().languages()).hasSize(1);
        assertThat(response.country().languages().get(0).name()).isEqualTo("Swahili");

        ArgumentCaptor<CountryInfo> captor = ArgumentCaptor.forClass(CountryInfo.class);
        verify(countryInfoRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Kenya");
        assertThat(captor.getValue().getLanguages()).hasSize(1);
    }

    @Test
    void importCountry_returnsCachedCountryWithoutCallingSoap() {
        CountryInfo existingCountry = new CountryInfo();
        setId(existingCountry, 7L);
        existingCountry.setIsoCode("KE");
        existingCountry.setName("Kenya");
        existingCountry.setCapitalCity("Nairobi");
        existingCountry.setPhoneCode("254");
        existingCountry.setContinentCode("AF");
        existingCountry.setCurrencyIsoCode("KES");
        existingCountry.setCountryFlagUrl("http://example.com/kenya.png");
        existingCountry.replaceLanguages(List.of(createLanguage("swa", "Swahili")));
        setTimestamps(existingCountry);

        when(countryInfoRepository.findByNameIgnoreCase("Kenya")).thenReturn(Optional.of(existingCountry));

        CountryImportResponse response = countryService.importCountry("Kenya");

        assertThat(response.created()).isFalse();
        assertThat(response.source()).isEqualTo("DATABASE_CACHE");
        verify(soapCountryClient, times(0)).fetchCountryIsoCode(any());
    }

    @Test
    void updateCountry_updatesAllFieldsAndLanguages() {
        CountryInfo existingCountry = new CountryInfo();
        setId(existingCountry, 1L);
        existingCountry.setIsoCode("KE");
        existingCountry.setName("Kenya");
        existingCountry.setCapitalCity("Nairobi");
        existingCountry.setPhoneCode("254");
        existingCountry.setContinentCode("AF");
        existingCountry.setCurrencyIsoCode("KES");
        existingCountry.setCountryFlagUrl("http://example.com/kenya.png");
        existingCountry.replaceLanguages(List.of(createLanguage("swa", "Swahili")));
        setTimestamps(existingCountry);

        CountryUpdateRequest request = new CountryUpdateRequest(
                "TZ",
                "tanzania",
                "dodoma",
                "255",
                "AF",
                "TZS",
                "https://example.com/tz.png",
                List.of(
                        new LanguageRequest("swa", "swahili"),
                        new LanguageRequest("eng", "english")
                )
        );

        when(countryInfoRepository.findById(1L)).thenReturn(Optional.of(existingCountry));
        when(countryInfoRepository.save(any(CountryInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = countryService.updateCountry(1L, request);

        assertThat(response.isoCode()).isEqualTo("TZ");
        assertThat(response.name()).isEqualTo("Tanzania");
        assertThat(response.capitalCity()).isEqualTo("Dodoma");
        assertThat(response.languages()).extracting(language -> language.name())
                .containsExactly("English", "Swahili");
    }

    @Test
    void getCountryById_throwsWhenCountryDoesNotExist() {
        when(countryInfoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> countryService.getCountryById(99L));
    }

    @Test
    void deleteCountry_deletesExistingEntity() {
        CountryInfo existingCountry = new CountryInfo();
        setId(existingCountry, 11L);
        existingCountry.setIsoCode("KE");
        existingCountry.setName("Kenya");
        setTimestamps(existingCountry);

        when(countryInfoRepository.findById(11L)).thenReturn(Optional.of(existingCountry));

        countryService.deleteCountry(11L);

        verify(countryInfoRepository).delete(eq(existingCountry));
    }

    private ncba.case_study.entity.Language createLanguage(String isoCode, String name) {
        ncba.case_study.entity.Language language = new ncba.case_study.entity.Language();
        language.setIsoCode(isoCode);
        language.setName(name);
        return language;
    }

    private void setId(CountryInfo countryInfo, Long id) {
        try {
            var field = CountryInfo.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(countryInfo, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setTimestamps(CountryInfo countryInfo) {
        try {
            var createdAt = CountryInfo.class.getDeclaredField("createdAt");
            var updatedAt = CountryInfo.class.getDeclaredField("updatedAt");
            createdAt.setAccessible(true);
            updatedAt.setAccessible(true);
            java.time.Instant now = java.time.Instant.now();
            createdAt.set(countryInfo, now);
            updatedAt.set(countryInfo, now);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}


