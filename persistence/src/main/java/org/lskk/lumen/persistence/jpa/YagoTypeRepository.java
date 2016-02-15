package org.lskk.lumen.persistence.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by ceefour on 13/02/2016.
 */
@Repository
public interface YagoTypeRepository extends PagingAndSortingRepository<YagoType, Integer> {

    YagoType findOneByNn(String nn);
    List<YagoType> findAllByPrefLabelOrIsPreferredMeaningOf(String upPrefLabel, String upIsPreferredMeaningOf);
    @Query("SELECT t FROM YagoType t LEFT JOIN FETCH t.superClasses WHERE t.prefLabel=:upLabel OR t.isPreferredMeaningOf=:upLabel")
    List<YagoType> findAllByPrefLabelOrIsPreferredMeaningOfEager(@Param("upLabel") String upLabel);

    @Modifying @Query("UPDATE YagoType SET prefLabel=:prefLabel WHERE nn=:nn")
    void updatePrefLabel(@Param("nn") String nn, @Param("prefLabel") String prefLabel);
    @Modifying @Query("UPDATE YagoType SET isPreferredMeaningOf=:isPreferredMeaningOf WHERE nn=:nn")
    void updateIsPreferredMeaningOf(@Param("nn") String nn, @Param("isPreferredMeaningOf") String isPreferredMeaningOf);
    @Modifying @Query("UPDATE YagoType SET hasGivenName=:hasGivenName WHERE nn=:nn")
    void updateHasGivenName(@Param("nn") String nn, @Param("hasGivenName") String hasGivenName);
    @Modifying @Query("UPDATE YagoType SET hasFamilyName=:hasFamilyName WHERE nn=:nn")
    void updateHasFamilyName(@Param("nn") String nn, @Param("hasFamilyName") String hasFamilyName);
    @Modifying @Query("UPDATE YagoType SET hasGloss=:hasGloss WHERE nn=:nn")
    void updateHasGloss(@Param("nn") String nn, @Param("hasGloss") String hasGloss);
    @Modifying @Query("UPDATE YagoType SET redirectedFrom=:redirectedFrom WHERE nn=:nn")
    void updateRedirectedFrom(@Param("nn") String nn, @Param("redirectedFrom") String redirectedFrom);

    @Modifying @Query(value = "INSERT INTO lumen.yagotype_superclasses VALUES ( (SELECT id FROM lumen.yagotype WHERE nn=:nn), (SELECT id FROM lumen.yagotype WHERE nn=:nnSuperClass) )",
        nativeQuery = true)
    void addSuperClass(@Param("nn") String nn, @Param("nnSuperClass") String nnSuperClass);
}
