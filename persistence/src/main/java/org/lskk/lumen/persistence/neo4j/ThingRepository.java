package org.lskk.lumen.persistence.neo4j;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by ceefour on 14/02/2016.
 */
@Repository
public interface ThingRepository extends PagingAndSortingRepository<Thing, Long> {

    Thing findOneByPartitionAndNn(PartitionKey _partition, String nn);
    List<Thing> findAllByPrefLabelOrIsPreferredMeaningOf(String upLabel);

}
