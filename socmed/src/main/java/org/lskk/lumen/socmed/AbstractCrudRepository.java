package org.lskk.lumen.socmed;

import org.springframework.data.repository.CrudRepository;

import java.io.Serializable;

/**
 * Created by ceefour on 1/19/15.
 */
public abstract class AbstractCrudRepository<T, ID extends Serializable> implements CrudRepository<T, ID> {
}
