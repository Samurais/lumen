package id.ac.itb.lumen.persistence

import org.apache.jena.graph.Node_Literal
import org.apache.jena.riot.system.FastAbbreviatingPrefixMap
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.XSD

/**
 * Created by ceefour on 24/01/15.
 */
class RdfUtils {

    static final String YAGO_NAMESPACE = 'http://yago-knowledge.org/resource/'
    static final String LUMEN_NAMESPACE = 'http://lumen.lskk.ee.itb.ac.id/resource/'
    static final FastAbbreviatingPrefixMap PREFIX_MAP

    static {
        PREFIX_MAP = new FastAbbreviatingPrefixMap()
        PREFIX_MAP.add('rdf', RDF.getURI())
        PREFIX_MAP.add('rdfs', RDFS.getURI())
        PREFIX_MAP.add('xsd', XSD.getURI())
        PREFIX_MAP.add('owl', OWL.getURI())
        PREFIX_MAP.add('yago', YAGO_NAMESPACE)
        PREFIX_MAP.add('lumen', LUMEN_NAMESPACE)
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
