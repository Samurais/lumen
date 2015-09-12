package id.ac.itb.lumen.persistence;

import org.apache.jena.graph.Node_Literal;
import org.apache.jena.riot.system.FastAbbreviatingPrefixMap;
import org.apache.jena.vocabulary.*;

/**
 * Created by ceefour on 24/01/15.
 */
public class RdfUtils {

    public static final String SCHEMA_NAMESPACE = "http://schema.org/";
    public static final String YAGO_NAMESPACE = "http://yago-knowledge.org/resource/";
    public static final String LUMEN_NAMESPACE = "http://lumen.lskk.ee.itb.ac.id/resource/";
    private static final FastAbbreviatingPrefixMap PREFIX_MAP;

    static {
        PREFIX_MAP = new FastAbbreviatingPrefixMap();
        PREFIX_MAP.add("rdf", RDF.getURI());
        PREFIX_MAP.add("rdfs", RDFS.getURI());
        PREFIX_MAP.add("xsd", XSD.getURI());
        PREFIX_MAP.add("owl", OWL.getURI());
        PREFIX_MAP.add("skos", SKOS.getURI());
        PREFIX_MAP.add("skosxl", SKOSXL.getURI());
        PREFIX_MAP.add("schema", SCHEMA_NAMESPACE);
        PREFIX_MAP.add("yago", YAGO_NAMESPACE);
        PREFIX_MAP.add("lumen", LUMEN_NAMESPACE);
    }

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

    public static FastAbbreviatingPrefixMap getPREFIX_MAP() {
        return PREFIX_MAP;
    }

}
