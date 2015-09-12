package id.ac.itb.lumen.persistence;

import org.apache.jena.graph.Node_Literal;
import org.apache.jena.riot.system.FastAbbreviatingPrefixMap;
import org.apache.jena.vocabulary.XSD;

/**
 * Created by ceefour on 24/01/15.
 */
public class RdfUtils {
    public static String abbrevDatatype(Node_Literal lit) {
        if (lit.getLiteralDatatypeURI() != null) {
            if (lit.getLiteralDatatypeURI().startsWith(XSD.getURI())) {
                return lit.getLiteralDatatypeURI().replace(XSD.getURI(), "xsd:");
            } else {
                return "yago:" + lit.getLiteralDatatypeURI();
            }

        } else {
            return null;
        }

    }

    public static String getYAGO_NAMESPACE() {
        return YAGO_NAMESPACE;
    }

    public static String getLUMEN_NAMESPACE() {
        return LUMEN_NAMESPACE;
    }

    public static FastAbbreviatingPrefixMap getPREFIX_MAP() {
        return PREFIX_MAP;
    }

    private static final String YAGO_NAMESPACE = "http://yago-knowledge.org/resource/";
    private static final String LUMEN_NAMESPACE = "http://lumen.lskk.ee.itb.ac.id/resource/";
    private static final FastAbbreviatingPrefixMap PREFIX_MAP;
}
