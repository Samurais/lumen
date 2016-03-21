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
public interface YagoLabelRepository extends PagingAndSortingRepository<YagoLabel, Integer> {

    @Modifying
    @Query(value = "INSERT INTO lumen.yagolabel (type_id, inlanguage, value) VALUES ( (SELECT id FROM lumen.yagotype WHERE nn=:nn), :inLanguage, :value )",
            nativeQuery = true)
    void addLabel(@Param("nn") String nn, @Param("inLanguage") String inLanguage, @Param("value") String value);
}
