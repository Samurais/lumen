package id.ac.itb.lumen.persistence

import org.springframework.data.neo4j.repository.GraphRepository

/**
 * Created by Budhi on 21/01/2015.
 */
interface PersonRepository extends GraphRepository<Person> {

}