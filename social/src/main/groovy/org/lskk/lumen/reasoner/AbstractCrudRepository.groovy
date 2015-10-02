package org.lskk.lumen.reasoner

import org.springframework.data.repository.CrudRepository

/**
 * Created by ceefour on 1/19/15.
 */
abstract class AbstractCrudRepository<T, ID extends Serializable> implements CrudRepository<T, ID> {
    @Override
    def <S extends T> S save(S entity) {
        return null
    }

    @Override
    def <S extends T> Iterable<S> save(Iterable<S> entities) {
        return null
    }

    @Override
    T findOne(ID id) {
        return null
    }

    @Override
    boolean exists(ID id) {
        return false
    }

    @Override
    Iterable<T> findAll() {
        return null
    }

    @Override
    Iterable<T> findAll(Iterable<ID> ids) {
        return null
    }

    @Override
    long count() {
        return 0
    }

    @Override
    void delete(ID id) {

    }

    @Override
    void delete(T entity) {

    }

    @Override
    void delete(Iterable<? extends T> entities) {

    }

    @Override
    void deleteAll() {

    }
}
