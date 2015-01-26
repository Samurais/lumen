package id.ac.itb.lumen.persistence

import com.hp.hpl.jena.graph.Node_Literal
import com.hp.hpl.jena.vocabulary.OWL
import com.hp.hpl.jena.vocabulary.XSD
import org.apache.jena.riot.system.FastAbbreviatingPrefixMap
import org.apache.jena.riot.system.PrefixMap
import org.apache.jena.riot.system.PrefixMapStd

/**
 * Created by ceefour on 24/01/15.
 */
class RdfUtils {

    static final FastAbbreviatingPrefixMap PREFIX_MAP

    static {
        PREFIX_MAP = new FastAbbreviatingPrefixMap()
        PREFIX_MAP.add('xsd', XSD.getURI())
        PREFIX_MAP.add('owl', OWL.getURI())
        PREFIX_MAP.add('yago', 'http://yago-knowledge.org/resource/')
        PREFIX_MAP.add('lumen', 'http://lumen.lskk.ee.itb.ac.id/resource/')
    }

    static String abbrevDatatype(Node_Literal lit) {
        if (lit.literalDatatypeURI != null) {
            if (lit.literalDatatypeURI.startsWith(XSD.getURI())) {
                lit.literalDatatypeURI.replace(XSD.getURI(), 'xsd:')
            } else {
                'yago:' + lit.literalDatatypeURI
            }
        } else {
            null
        }
    }

}
