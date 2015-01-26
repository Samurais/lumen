package id.ac.itb.lumen.persistence

import com.hp.hpl.jena.graph.Node
import com.hp.hpl.jena.graph.Node_Literal
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra
import com.hp.hpl.jena.vocabulary.XSD
import groovy.transform.CompileStatic
import org.apache.jena.riot.system.PrefixMap
import org.apache.jena.riot.system.PrefixMapStd
import org.junit.Assert
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by ceefour on 24/01/15.
 */
@CompileStatic
class XsdDataTypeTest {

    private static final Logger log = LoggerFactory.getLogger(XsdDataTypeTest)

    @Test
    void dataTypes() {
        final str1 = '"1966-12-01"^^xsd:date'
        final lit1 = (Node_Literal) NodeFactoryExtra.parseNode(str1, RdfUtils.PREFIX_MAP)
        final abbr1 = RdfUtils.abbrevDatatype(lit1)
        log.info('Type 1: {} {} » {}', lit1, lit1.literalDatatypeURI, abbr1)
        Assert.assertEquals('xsd:date', abbr1)

        final str2 = '"44.083333333333336"^^<degrees>'
        final lit2 = (Node_Literal) NodeFactoryExtra.parseNode(str2, RdfUtils.PREFIX_MAP)
        final abbr2 = RdfUtils.abbrevDatatype(lit2)
        log.info('Type 2: {} {} » {}', lit2, lit2.literalDatatypeURI, abbr2)
        Assert.assertEquals('yago:degrees', abbr2)
    }

    @Test
    void uri() {
        // https://issues.apache.org/jira/browse/JENA-862
        Node uri
        uri = NodeFactoryExtra.parseNode('<hasPopulationDensity>', RdfUtils.PREFIX_MAP)
        log.info('Parsed: {}. localName={} nameSpace={}', uri, uri.localName, uri.nameSpace)

        uri = NodeFactoryExtra.parseNode('owl:Thing', RdfUtils.PREFIX_MAP)
        log.info('Parsed: {}. localName={} nameSpace={}', uri, uri.localName, uri.nameSpace)
    }

}
