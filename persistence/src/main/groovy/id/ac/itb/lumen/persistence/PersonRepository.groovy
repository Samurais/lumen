package id.ac.itb.lumen.persistence

import groovy.transform.CompileStatic
import org.springframework.data.neo4j.repository.GraphRepository
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Budhi on 21/01/2015.
 */
@CompileStatic
@Transactional
interface PersonRepository extends GraphRepository<Person> {

}