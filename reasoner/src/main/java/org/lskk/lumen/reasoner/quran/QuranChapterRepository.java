package org.lskk.lumen.reasoner.quran;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by aina on 18/11/2015.
 */
public interface QuranChapterRepository extends PagingAndSortingRepository<QuranChapter, String> {
    @Query(value="SELECT id FROM sanad.quranchapter WHERE regexp_replace(lower(namewithtashkeel), '[^a-z]', '', 'g') = regexp_replace(lower(:q), '[^a-z]', '', 'g');",
    nativeQuery = true)
    String getIdByName(@Param("q") String q);
}
