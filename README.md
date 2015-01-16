# Lumen Persistence

Persistence module for Lumen Social Robot.

## Overview

Persistence handles the following general categories, each one is handled differently:

1. **Journal**. Records and provides daily activities of each robot. This is *not* the system log.
   Each journal is local to a robot.
   
2. **Facts**. Records information related to people, objects, and other robots (collectively called _entities_);
   and their resource and literal properties (collectively called _facts_).
   
   Facts are scoped:
   
   1. _global_ facts, applies to all robots
   2. _group_ facts, applies to some robots joining a specific group
   3. _instance_ facts, applies to an individual robot
   
   Writes to _group_ and _instance_ facts are normally also written to audit history,
   in order to provide insight who, when, and why was the fact recorded.
   
3. **Knowledge**. Records semantic meanings and inferences.

## Journal Persistence

TODO

## Fact Persistence

Stored in Neo4j. All facts are indexed for quick lookup.
Fact persistence also have quick graph traversal performance, because backed by a graph database.

### Sample storable facts

For person B.J. Habibie (taken from https://gate.d5.mpi-inf.mpg.de/webyago3spotlx/Browser?entity=%3CB._J._Habibie%3E):

| Fact ID                | Subject             | Property      | Object                                 | Time       | Location  | Keywords                      |
|------------------------|---------------------|---------------|----------------------------------------|------------|-----------|-------------------------------|
| id_1xidad2_1xk_uv85ns  | <yago:B.J._Habibie> | wasBornOnDate | 1936-06-25                             | 1936-06-25 | Pare-pare | Hasri Ainun Besari, Gorontalo |
|                        | <yago:B.J._Habibie> | label         | "Bacharuddin Jusuf Habibie"@ind        |            |           |                               |
| id_1xidad2_1sz_1iw0bpy | <yago:B.J._Habibie> | prefLabel     | "B.J. Habibie"@eng                     |            |           |                               |
| id_1xidad2_10x_1m2huro | <yago:B.J._Habibie> | graduatedFrom | <yago:Bandung_Institute_of_Technology> |            | Bandung   |                               |
| id_1xidad2_16x_n6kx1s  | <yago:B.J._Habibie> | isMarriedTo   | <yago:Hasri_Ainun_Habibie>             |            |           |                               |
| id_1xidad2_p3m_zkjp59  | <yago:B.J._Habibie> | hasGender     | male                                   |            |           |                               |

### Facts as Relationships

A simple `Fact` is direct relationship from a subject node to object node.
A `Fact` relationship has a stable referenceable `id` (the Fact ID, usually a UUID), and may contain any number of additional
metadata (temporal and spatial) as Neo4j properties.

### Future Consideration: Reified Facts

Since a `Fact` is a relationship and not a node, it cannot be connected to any
other node (perhaps another reified fact). If that is required, a `Fact` can be turned into a
[singleton property](http://mor.nlm.nih.gov/pubs/pdf/2014-www-vn.pdf) fact or via RDF-style reification.

Meta-Property Relationships connects a (singleton property) Fact node with its subject, time, location, or keywords.

| No. | Yago2s Property          | Neo4j Relationship Type   |
|-----|--------------------------|---------------------------|
|   1 | `rdf:type`               | `type`                    |
|   2 | `extractionSource`       | `extractionSource`        |
|   3 | `occursIn`               | `occursIn`                |
|   4 | `placedIn`               | `placedIn`                |
|   5 | `occursSince`            | `occursSince`             |
|   6 | `occursUntil`            | `occursUntil`             |
|   7 | `startsExistingOnDate`   | `startsExistingOnDate`    |
|   8 | `endsExistingOnDate`     | `endsExistingOnDate`      |

### LiteralFact

A `LiteralFact` has `type`, `value`, and `language` as Neo4j properties.

### Recognized Properties

Here are the recognized properties along with Yago2s Semantic Structure Mapping to Neo4j Relationship Types.

| No. | Yago2s Property          | Neo4j Relationship Type |
|-----|--------------------------|-------------------------|
|   1 | `rdfs:label`             | `label`                 |
|   2 | `skos:prefLabel`         | `prefLabel`             |
|   4 | `graduatedFrom`          | `graduatedFrom`         |
|   5 | `hasGender`              | `hasGender`             |
|   6 | `isMarriedTo`            | `isMarriedTo`           |
|   7 | `wasBornOnDate`          | `wasBornOnDate`         |

### Messaging Topics & Queues

#### /topic/lumen.AGENT_ID.persistence.fact

TODO.

#### /queue/lumen.AGENT_ID.persistence.fact

##### Ask with Single Answer

Ask and require a single `Fact` answer: (`replyTo` required)

```json
{
  "@type": "Question",
  "multipleAnswers": false,
  "subject": {
    "@id": "http://yago-knowledge.org/resource/#B.J._Habibie"
  },
  "property": {
    "@id": "http://yago-knowledge.org/resource/wasBornOnDate"
  }
}
```

Reply:

```json
{
  "@type": "Fact",
  "id": "id_1xidad2_1xk_uv85ns", 
  "subject": {
    "@type": "SemanticEntity",
    "@id": "http://yago-knowledge.org/resource/#B.J._Habibie",
    "rdfs:label": "Bacharuddin Jusuf Habibie",
    "skos:prefLabel": "Bacharuddin Jusuf Habibie"
  },
  "property": {
    "@type": "SemanticProperty",
    "@id": "http://yago-knowledge.org/resource/wasBornOnDate",
  },
  "object": {
    "@type": "LocalDate",
    "value": "1936-06-25"
  }
}
```

##### Assert A Fact

```json
{
  "@type": "Fact",
  "subject": {
    "@id": "http://yago-knowledge.org/resource/#B.J._Habibie"
  },
  "property": {
    "@id": "http://yago-knowledge.org/resource/wasBornOnDate",
  },
  "object": {
    "@type": "LocalDate",
    "value": "1936-06-25"
  }
}
```

If `replyTo` is given, will reply will a `Fact` summary:

```json
{
  "@type": "Fact",
  "id": "id_1xidad2_1xk_uv85ns", 
  "subject": {
    "@type": "SemanticEntity",
    "@id": "http://yago-knowledge.org/resource/#B.J._Habibie"
    "rdfs:label": "Bacharuddin Jusuf Habibie",
    "skos:prefLabel": "Bacharuddin Jusuf Habibie"
  },
  "property": {
    "@type": "SemanticProperty",
    "@id": "http://yago-knowledge.org/resource/wasBornOnDate",
  },
  "object": {
    "@type": "LocalDate",
    "value": "1936-06-25"
  }
}
```

## Knowledge Persistence

TODO.

Stored in Neo4j. Mimics [OpenCog AtomSpace Nodes and Links](http://wiki.opencog.org/w/AtomSpace).

It may be useful to reuse [Suggested Upper Merged Ontology (SUMO)](http://www.adampease.org/OP/)'s
_axiomatic knowledge_, which can be integrated with YAGO, see [YAGO-SUMO](http://people.mpi-inf.mpg.de/~gdemelo/yagosumo/).
