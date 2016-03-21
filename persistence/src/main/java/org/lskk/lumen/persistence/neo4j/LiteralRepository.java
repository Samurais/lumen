package org.lskk.lumen.persistence.neo4j;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by ceefour on 14/02/2016.
 */
@Repository
public interface LiteralRepository extends PagingAndSortingRepository<Literal, Long> {

}
