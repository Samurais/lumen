package org.lskk.lumen.persistence.neo4j;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by ceefour on 29/02/2016.
 */
public interface ThingLabelRepository extends PagingAndSortingRepository<ThingLabel, Long> {

//    @Query("MATCH (n:owl_Thing {nn: {nn}}) WHERE n._partition IN ['lumen_yago', 'lumen_common', 'lumen_var']\n" +
//            "MERGE (n) -[:{property}]-> (label:lumen_Label {l: {inLanguage}, v: {value}, _partition: {partitionKey}})\n" +
//            "SET label.tv={tv}, label.m={metaphone}\n" +
//            "RETURN label")
//    ThingLabel assertLabel(@Param("partitionKey") PartitionKey partitionKey,
//                           @Param("nn") String nn,
//                           @Param("property") String property,
//                           @Param("inLanguage") String inLanguage,
//                           @Param("value") String value,
//                           @Param("tv") float[] tv,
//                           @Param("metaphone") String metaphone);

}
