package ncba.case_study.dto;

import java.util.List;

public record CountryUpdateRequest(
        String isoCode,
        String name,
        String capitalCity,
        String phoneCode,
        String continentCode,
        String currencyIsoCode,
        String countryFlagUrl,
        List<LanguageRequest> languages
) {
}

