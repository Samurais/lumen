package org.lskk.lumen.persistence.neo4j;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by ceefour on 14/02/2016.
 */
@Repository
public interface ThingRepository extends PagingAndSortingRepository<Thing, Long> {

    Thing findOneByNn(String nn);

}
