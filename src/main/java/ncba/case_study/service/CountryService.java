package ncba.case_study.service;

import ncba.case_study.dto.CountryImportResponse;
import ncba.case_study.dto.CountryResponse;
import ncba.case_study.dto.CountryUpdateRequest;
import ncba.case_study.dto.LanguageResponse;
import ncba.case_study.entity.CountryInfo;
import ncba.case_study.entity.Language;
import ncba.case_study.exception.ResourceNotFoundException;
import ncba.case_study.repository.CountryInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CountryService {

    private static final Logger log = LoggerFactory.getLogger(CountryService.class);
    private final CountryInfoRepository countryInfoRepository;
    private final SoapCountryClient soapCountryClient;

    public CountryService(CountryInfoRepository countryInfoRepository, SoapCountryClient soapCountryClient) {
        this.countryInfoRepository = countryInfoRepository;
        this.soapCountryClient = soapCountryClient;
    }

    @Transactional
    public CountryImportResponse importCountry(String countryName) {
        String normalizedCountryName = normalizeName(countryName);
        log.info("Import country request received rawName={} normalizedName={}", countryName, normalizedCountryName);

        return countryInfoRepository.findByNameIgnoreCase(normalizedCountryName)
                .map(countryInfo -> {
                    log.info("Country information served from local cache countryId={} name={}", countryInfo.getId(), countryInfo.getName());
                    return new CountryImportResponse(false, "DATABASE_CACHE", toResponse(countryInfo));
                })
                .orElseGet(() -> fetchAndPersistCountry(normalizedCountryName));
    }

    public List<CountryResponse> getAllCountries() {
        return countryInfoRepository.findAll().stream()
                .sorted(Comparator.comparing(CountryInfo::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    public CountryResponse getCountryById(Long id) {
        return toResponse(getCountryEntity(id));
    }

    @Transactional
    public CountryResponse updateCountry(Long id, CountryUpdateRequest request) {
        CountryInfo countryInfo = getCountryEntity(id);
        applyManualUpdate(countryInfo, request);
        CountryInfo savedCountry = countryInfoRepository.save(countryInfo);
        log.info("Country information updated countryId={} isoCode={}", savedCountry.getId(), savedCountry.getIsoCode());
        return toResponse(savedCountry);
    }

    @Transactional
    public void deleteCountry(Long id) {
        CountryInfo countryInfo = getCountryEntity(id);
        countryInfoRepository.delete(countryInfo);
        log.info("Country information deleted countryId={} isoCode={}", countryInfo.getId(), countryInfo.getIsoCode());
    }

    private CountryImportResponse fetchAndPersistCountry(String normalizedCountryName) {
        String isoCode = soapCountryClient.fetchCountryIsoCode(normalizedCountryName);
        SoapCountryInfoResult soapCountryInfoResult = soapCountryClient.fetchFullCountryInfo(isoCode);

        CountryInfo countryInfo = countryInfoRepository.findByIsoCodeIgnoreCase(soapCountryInfoResult.isoCode())
                .orElseGet(CountryInfo::new);
        boolean created = countryInfo.getId() == null;

        applySoapResult(countryInfo, soapCountryInfoResult);
        CountryInfo savedCountry = countryInfoRepository.save(countryInfo);
        log.info("Country information persisted countryId={} isoCode={} created={}", savedCountry.getId(), savedCountry.getIsoCode(), created);
        return new CountryImportResponse(created, "SOAP", toResponse(savedCountry));
    }

    private CountryInfo getCountryEntity(Long id) {
        return countryInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country with id " + id + " was not found."));
    }

    private void applySoapResult(CountryInfo countryInfo, SoapCountryInfoResult soapResult) {
        countryInfo.setIsoCode(normalizeCode(soapResult.isoCode()));
        countryInfo.setName(normalizeName(soapResult.name()));
        countryInfo.setCapitalCity(normalizeName(soapResult.capitalCity()));
        countryInfo.setPhoneCode(normalizeValue(soapResult.phoneCode()));
        countryInfo.setContinentCode(normalizeCode(soapResult.continentCode()));
        countryInfo.setCurrencyIsoCode(normalizeCode(soapResult.currencyIsoCode()));
        countryInfo.setCountryFlagUrl(normalizeValue(soapResult.countryFlagUrl()));
        countryInfo.replaceLanguages(soapResult.languages().stream()
                .map(language -> createLanguage(language.isoCode(), language.name()))
                .toList());
    }

    private void applyManualUpdate(CountryInfo countryInfo, CountryUpdateRequest request) {
        countryInfo.setIsoCode(normalizeCode(request.isoCode()));
        countryInfo.setName(normalizeName(request.name()));
        countryInfo.setCapitalCity(normalizeName(request.capitalCity()));
        countryInfo.setPhoneCode(normalizeValue(request.phoneCode()));
        countryInfo.setContinentCode(normalizeCode(request.continentCode()));
        countryInfo.setCurrencyIsoCode(normalizeCode(request.currencyIsoCode()));
        countryInfo.setCountryFlagUrl(normalizeValue(request.countryFlagUrl()));
        countryInfo.replaceLanguages(request.languages().stream()
                .map(languageRequest -> createLanguage(languageRequest.isoCode(), languageRequest.name()))
                .toList());
    }

    private Language createLanguage(String isoCode, String name) {
        Language language = new Language();
        language.setIsoCode(normalizeLanguageCode(isoCode));
        language.setName(normalizeName(name));
        return language;
    }

    private CountryResponse toResponse(CountryInfo countryInfo) {
        return new CountryResponse(
                countryInfo.getId(),
                countryInfo.getIsoCode(),
                countryInfo.getName(),
                countryInfo.getCapitalCity(),
                countryInfo.getPhoneCode(),
                countryInfo.getContinentCode(),
                countryInfo.getCurrencyIsoCode(),
                countryInfo.getCountryFlagUrl(),
                countryInfo.getLanguages().stream()
                        .sorted(Comparator.comparing(Language::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(this::toLanguageResponse)
                        .toList(),
                countryInfo.getCreatedAt(),
                countryInfo.getUpdatedAt()
        );
    }

    private LanguageResponse toLanguageResponse(Language language) {
        return new LanguageResponse(language.getId(), language.getIsoCode(), language.getName());
    }

    private String normalizeName(String value) {
        String trimmedValue = normalizeValue(value).replaceAll("\\s+", " ");
        StringBuilder builder = new StringBuilder(trimmedValue.length());
        boolean capitalizeNext = true;

        for (char character : trimmedValue.toCharArray()) {
            if (Character.isLetter(character)) {
                builder.append(capitalizeNext ? Character.toTitleCase(character) : Character.toLowerCase(character));
                capitalizeNext = false;
            } else {
                builder.append(character);
                capitalizeNext = Character.isWhitespace(character) || character == '-' || character == '\'';
            }
        }

        return builder.toString();
    }

    private String normalizeCode(String value) {
        return normalizeValue(value).toUpperCase();
    }

    private String normalizeLanguageCode(String value) {
        return normalizeValue(value).toLowerCase();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

}
