package ncba.case_study.repository;

import java.util.Optional;

import ncba.case_study.entity.CountryInfo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryInfoRepository extends JpaRepository<CountryInfo, Long> {

    @EntityGraph(attributePaths = "languages")
    Optional<CountryInfo> findById(Long id);

    @EntityGraph(attributePaths = "languages")
    Optional<CountryInfo> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "languages")
    Optional<CountryInfo> findByIsoCodeIgnoreCase(String isoCode);
}

