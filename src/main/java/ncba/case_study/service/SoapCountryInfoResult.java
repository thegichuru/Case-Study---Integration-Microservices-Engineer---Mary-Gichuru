package ncba.case_study.service;

import java.util.List;

public record SoapCountryInfoResult(
        String isoCode,
        String name,
        String capitalCity,
        String phoneCode,
        String continentCode,
        String currencyIsoCode,
        String countryFlagUrl,
        List<SoapLanguageResult> languages
) {
}

