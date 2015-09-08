package id.ac.itb.lumen.persistence;

import groovy.transform.CompileStatic;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ceefour on 24/01/15.
 */
@CompileStatic
public class XsdDataTypeTest {
        @Test
        public void dataTypes() {
        final String str1 = "\"1966-12-01\"^^xsd:date";
        final Node_Literal lit1 = (Node_Literal) NodeFactoryExtra.parseNode(str1, RdfUtils.getPREFIX_MAP());
        final String abbr1 = RdfUtils.abbrevDatatype(lit1);
        log.info("Type 1: {} {} » {}", lit1, lit1.getLiteralDatatypeURI(), abbr1);
        Assert.assertEquals("xsd:date", abbr1);

        final String str2 = "\"44.083333333333336\"^^<degrees>";
        final Node_Literal lit2 = (Node_Literal) NodeFactoryExtra.parseNode(str2, RdfUtils.getPREFIX_MAP());
        final String abbr2 = RdfUtils.abbrevDatatype(lit2);
        log.info("Type 2: {} {} » {}", lit2, lit2.getLiteralDatatypeURI(), abbr2);
        Assert.assertEquals("yago:degrees", abbr2);
    }

        @Test
        public void uri() {
        // https://issues.apache.org/jira/browse/JENA-862
        Node uri;
        uri = NodeFactoryExtra.parseNode("<hasPopulationDensity>", RdfUtils.getPREFIX_MAP());
        log.info("Parsed: {}. localName={} nameSpace={}", uri, uri.getLocalName(), uri.getNameSpace());

        uri = NodeFactoryExtra.parseNode("owl:Thing", RdfUtils.getPREFIX_MAP());
        log.info("Parsed: {}. localName={} nameSpace={}", uri, uri.getLocalName(), uri.getNameSpace());
    }

        private static final Logger log = LoggerFactory.getLogger(XsdDataTypeTest.class);
}
