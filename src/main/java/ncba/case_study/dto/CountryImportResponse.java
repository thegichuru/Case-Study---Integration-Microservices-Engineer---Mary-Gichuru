package ncba.case_study.dto;

public record CountryImportResponse(
        boolean created,
        String source,
        CountryResponse country
) {
}

