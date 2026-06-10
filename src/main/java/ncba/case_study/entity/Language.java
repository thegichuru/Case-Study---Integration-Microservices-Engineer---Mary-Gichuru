package ncba.case_study.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Setter;

@Entity
@Table(name = "language", uniqueConstraints = {
        @UniqueConstraint(name = "uk_language_country_iso_code", columnNames = {"country_info_id", "iso_code"})
})
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "iso_code", nullable = false, length = 20)
    private String isoCode;

    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "country_info_id", nullable = false)
    private CountryInfo countryInfo;

    public Long getId() {
        return id;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getName() {
        return name;
    }

    public CountryInfo getCountryInfo() {
        return countryInfo;
    }

}

