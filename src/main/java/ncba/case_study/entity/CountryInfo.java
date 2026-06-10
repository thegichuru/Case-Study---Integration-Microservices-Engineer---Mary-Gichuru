package ncba.case_study.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Setter;

@Entity
@Table(name = "country_info", uniqueConstraints = {
        @UniqueConstraint(name = "uk_country_info_iso_code", columnNames = "iso_code"),
        @UniqueConstraint(name = "uk_country_info_name", columnNames = "name")
})
public class CountryInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "iso_code", nullable = false, length = 10)
    private String isoCode;

    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Setter
    @Column(name = "capital_city", nullable = false, length = 120)
    private String capitalCity;

    @Setter
    @Column(name = "phone_code", nullable = false, length = 20)
    private String phoneCode;

    @Setter
    @Column(name = "continent_code", nullable = false, length = 10)
    private String continentCode;

    @Setter
    @Column(name = "currency_iso_code", nullable = false, length = 10)
    private String currencyIsoCode;

    @Setter
    @Column(name = "country_flag_url", nullable = false, length = 500)
    private String countryFlagUrl;

    @OrderBy("name ASC")
    @OneToMany(mappedBy = "countryInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Language> languages = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getName() {
        return name;
    }

    public String getCapitalCity() {
        return capitalCity;
    }

    public String getPhoneCode() {
        return phoneCode;
    }

    public String getContinentCode() {
        return continentCode;
    }

    public String getCurrencyIsoCode() {
        return currencyIsoCode;
    }

    public String getCountryFlagUrl() {
        return countryFlagUrl;
    }

    public List<Language> getLanguages() {
        return languages;
    }

    public void replaceLanguages(List<Language> languages) {
        this.languages.clear();
        if (languages == null) {
            return;
        }

        for (Language language : languages) {
            addLanguage(language);
        }
    }

    public void addLanguage(Language language) {
        language.setCountryInfo(this);
        this.languages.add(language);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

