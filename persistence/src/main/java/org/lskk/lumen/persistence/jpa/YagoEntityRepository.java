package org.lskk.lumen.persistence.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Created by ceefour on 13/02/2016.
 */
@Repository
public interface YagoEntityRepository extends PagingAndSortingRepository<YagoEntity, Integer> {

    YagoEntity findOneByNn(String nn);

    @Modifying @Query("UPDATE YagoEntity SET prefLabel=:prefLabel WHERE nn=:nn")
    void updatePrefLabel(@Param("nn") String nn, @Param("prefLabel") String prefLabel);
    @Modifying @Query("UPDATE YagoEntity SET isPreferredMeaningOf=:isPreferredMeaningOf WHERE nn=:nn")
    void updateIsPreferredMeaningOf(@Param("nn") String nn, @Param("isPreferredMeaningOf") String isPreferredMeaningOf);
    @Modifying @Query("UPDATE YagoEntity SET hasGivenName=:hasGivenName WHERE nn=:nn")
    void updateHasGivenName(@Param("nn") String nn, @Param("hasGivenName") String hasGivenName);
    @Modifying @Query("UPDATE YagoEntity SET hasFamilyName=:hasFamilyName WHERE nn=:nn")
    void updateHasFamilyName(@Param("nn") String nn, @Param("hasFamilyName") String hasFamilyName);
    @Modifying @Query("UPDATE YagoEntity SET hasGloss=:hasGloss WHERE nn=:nn")
    void updateHasGloss(@Param("nn") String nn, @Param("hasGloss") String hasGloss);
    @Modifying @Query("UPDATE YagoEntity SET redirectedFrom=:redirectedFrom WHERE nn=:nn")
    void updateRedirectedFrom(@Param("nn") String nn, @Param("redirectedFrom") String redirectedFrom);

    @Modifying @Query(value = "INSERT INTO lumen.yagoentity_superclasses VALUES ( (SELECT id FROM lumen.yagoentity WHERE nn=:nn), (SELECT id FROM lumen.yagoentity WHERE nn=:nnSuperClass) )",
        nativeQuery = true)
    void addSuperClass(@Param("nn") String nn, @Param("nnSuperClass") String nnSuperClass);
}
