package org.lskk.lumen.reasoner.quran;

import org.springframework.data.repository.PagingAndSortingRepository;

/**
 *  tujuannya untuk melakukan manipulasi ditabel sesuai dengan entity, select, update, insert dan delete
 * Created by aina on 25/11/2015.
 */
public interface LiteralRepository extends PagingAndSortingRepository<Literal, String> {
}
