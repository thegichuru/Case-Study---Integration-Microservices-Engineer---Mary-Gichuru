package ncba.case_study.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import ncba.case_study.dto.CountryImportResponse;
import ncba.case_study.dto.CountryResponse;
import ncba.case_study.dto.LanguageResponse;
import ncba.case_study.exception.ResourceNotFoundException;
import ncba.case_study.service.CountryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CountryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CountryService countryService;

    @InjectMocks
    private CountryController countryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(countryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void importCountry_returnsCreatedResponse() throws Exception {
        CountryResponse country = sampleCountryResponse();
        when(countryService.importCountry("Kenya"))
                .thenReturn(new CountryImportResponse(true, "SOAP", country));

        mockMvc.perform(post("/api/v1/countries/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Kenya"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.source").value("SOAP"))
                .andExpect(jsonPath("$.country.name").value("Kenya"));
    }

    @Test
    void importCountry_returnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/countries/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Country name is required"));
    }

    @Test
    void getAllCountries_returnsList() throws Exception {
        when(countryService.getAllCountries()).thenReturn(List.of(sampleCountryResponse()));

        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isoCode").value("KE"));
    }

    @Test
    void getCountryById_returnsNotFoundWhenMissing() throws Exception {
        when(countryService.getCountryById(44L))
                .thenThrow(new ResourceNotFoundException("Country with id 44 was not found."));

        mockMvc.perform(get("/api/v1/countries/44"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Country with id 44 was not found."));
    }

    @Test
    void updateCountry_returnsUpdatedEntity() throws Exception {
        when(countryService.updateCountry(eq(1L), any()))
                .thenReturn(sampleCountryResponse());

        mockMvc.perform(put("/api/v1/countries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isoCode":"KE",
                                  "name":"Kenya",
                                  "capitalCity":"Nairobi",
                                  "phoneCode":"254",
                                  "continentCode":"AF",
                                  "currencyIsoCode":"KES",
                                  "countryFlagUrl":"https://example.com/kenya.png",
                                  "languages":[{"isoCode":"swa","name":"Swahili"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kenya"));
    }

    @Test
    void deleteCountry_returnsNoContent() throws Exception {
        doNothing().when(countryService).deleteCountry(1L);

        mockMvc.perform(delete("/api/v1/countries/1"))
                .andExpect(status().isNoContent());
    }

    private CountryResponse sampleCountryResponse() {
        return new CountryResponse(
                1L,
                "KE",
                "Kenya",
                "Nairobi",
                "254",
                "AF",
                "KES",
                "https://example.com/kenya.png",
                List.of(new LanguageResponse(1L, "swa", "Swahili")),
                Instant.parse("2026-06-10T10:15:30Z"),
                Instant.parse("2026-06-10T10:15:30Z")
        );
    }
}




