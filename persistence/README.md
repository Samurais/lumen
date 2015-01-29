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

### Sample Data (new)

    MERGE (subj:Entity {uri: 'http://lumen.lskk.ee.itb.ac.id/resource/Budhi_Yulianto'})
    ON CREATE SET subj.qName = 'lumen:Budhi_Yulianto'
    MERGE (obj:Entity {uri: 'http://yago-knowledge.org/resource/Bandung'})
    ON CREATE SET obj.qName = 'yago:Bandung'
    MERGE (subj) -[:wasBornIn {factId: '123'}]-> (obj)
    RETURN subj

### Sample Data (old)

    CREATE (wordnet_person_100007846:Class {uri: 'http://yago-knowledge.org/resource/wordnet_person_100007846', label: 'person'})
    CREATE (wordnet_city_108524735:Class {uri: 'http://yago-knowledge.org/resource/wordnet_city_108524735', label: 'city'})
    CREATE (Budhi_Yulianto:wordnet_person_100007846 {uri: 'http://lumen.lskk.ee.itb.ac.id/resource/Budhi_Yulianto', label: 'Budhi Yulianto'})
    CREATE (Budhi_Yulianto_label:Text {value: 'Budhi Yulianto', language: 'ind'})
    CREATE (Bandung:wordnet_city_108524735 {uri: 'http://yago-knowledge.org/resource/Bandung', label: 'Bandung'})
    CREATE (Bandung_label:Text {value: 'Bandung', language: 'ind'})
    CREATE (Bandung_label2:Text {value: 'Parijs van Java', language: 'ind'})

    CREATE (Budhi_Yulianto) -[:type]-> (wordnet_person_100007846)
    CREATE (Budhi_Yulianto) -[:label]-> (Budhi_Yulianto_label)
    CREATE (Bandung) -[:type]-> (wordnet_city_108524735)
    CREATE (Bandung) -[:label]-> (Bandung_label)
    CREATE (Bandung) -[:label]-> (Bandung_label2)
    CREATE (Budhi_Yulianto) -[:wasBornIn]-> (Bandung)
    
    RETURN Budhi_Yulianto

### To Delete All
    
    /* to delete all
    MATCH (a)-[r]-(b) DELETE r
    MATCH n DELETE n
    */

### Sample to Create from Existing Nodes
    
    /* sample to create from existing nodes
    MATCH Person WHERE Person.uri = 'http://yago-knowledge.org/resource/Person'
    MATCH Budhi_Yulianto WHERE Budhi_Yulianto.uri = 'http://lumen.lskk.ee.itb.ac.id/resource/Budhi_Yulianto'
    CREATE (Budhi_Yulianto)-[:instanceOf]->(Person) 
    RETURN Budhi_Yulianto
    */

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
    "@id": "http://yago-knowledge.org/resource/B.J._Habibie"
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
    "@id": "http://yago-knowledge.org/resource/B.J._Habibie",
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
    "@id": "http://yago-knowledge.org/resource/B.J._Habibie"
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

## Knowledge Persistence

TODO.

Stored in Neo4j. Mimics [OpenCog AtomSpace Nodes and Links](http://wiki.opencog.org/w/AtomSpace).

It may be useful to reuse [Suggested Upper Merged Ontology (SUMO)](http://www.adampease.org/OP/)'s
_axiomatic knowledge_, which can be integrated with YAGO, see [YAGO-SUMO](http://people.mpi-inf.mpg.de/~gdemelo/yagosumo/).

## Neo4j Browser

It's nice to be able to use Neo4j Browser for the data, but you can't run both
Lumen Persistence and Neo4j Server at the same time.

    cp -va /var/lib/neo4j ~/neo4j-lumen
    rm -rv ~/neo4j-lumen/data
    mkdir -v ~/neo4j-lumen/data
    rm -v ~/neo4j-lumen/conf
    mkdir -v ~/neo4j-lumen/conf
    sudo cp -v /etc/neo4j/* ~/neo4j-lumen/conf/
    sudo chown -Rc ceefour:ceefour ~/neo4j-lumen

Edit `~/neo4j-lumen/conf/neo4j-server.properties`:

    org.neo4j.server.database.location=/home/ceefour/lumen_lumen_dev/neo4j/graph.db

Then run:

    sudo service neo4j-service stop
    ~/neo4j-lumen/bin/neo4j console

## conf/neo4j-wrapper.conf

You **will** need to configure `wrapper.java.maxmemory` to 1024, otherwise you'll get `OutOfMemoryError` with at least step 4 DB.

## Tuning Performance

During import and also for production server, you need to tweak user limit.

Create `/etc/security/limits.d/neo4j.conf` :

    neo4j   soft    nofile  40000
    neo4j   hard    nofile  40000
    ceefour soft    nofile  40000
    ceefour hard    nofile  40000

Edit `/etc/pam.d/su` and uncomment:

    session    required   pam_limits.so

Then restart your computer.

Now `ulimit -n` should show `40000`.

During import, maximize the RAM before writing disk:

    # Ubuntu's default: vm.dirty_background_ratio = 5 vm.dirty_ratio = 10
    sysctl vm.dirty_background_ratio vm.dirty_ratio
    # Set new values
    sudo sysctl vm.dirty_background_ratio=50 vm.dirty_ratio=80

Your heap (`-Xmx`) should be large, i.e. 75% of RAM.

For more info, see (Neo4j Linux Performance Guide](http://neo4j.com/docs/stable/linux-performance-guide.html).

## Steps to Import from Yago

This normally should not be required, and only used when database needs to be refreshed or there's a new Yago
version.

**Important:** Before importing, make sure to tweak Linux kernel `vm.*` options above!
Mount your Neo4j DB in `tmpfs` (4000M) to get ~1500 inserts/s (using 6 workers in 8 CPU), otherwise in HDD you get ~200/s,
but my testing in HDD can get to ~20/s on step 6.

tmpfs/SSD works well with multithreading (individual transaction per thread), while HDD probably want serialized better.

1. Index Labels -> 550 MiB `yago2s/yagoLabels.jsonset` (Hadoop-style Ctrl+A-separated JSON). ~2 mins on SSD
    TODO: this needs to index *all* labels across all files, not just `yagoLabels.tsv`, so next importers
    don't need to check for label properties
2. Import Labels -> 1.5 GiB Initial Neo4j database (including href constraint, Resource indexes, and Label.v indexes) using BatchInserter
    ~5 mins on SSD
    Run once: `neo4j-shell ~/lumen_lumen_dev/neo4j/graph.db` to "fix incorrect shutdown"
3. Next steps are to import other files, recommended to be in order.
    Import `yagoLabels.tsv` (3 special label properties will be ignored, it will only import regular labels like `hasFamilyName` etc.)
    ~1 hour on `tmpfs`, probably ~8 hrs on HDD (SSD crashed on me). Result: 3000 MiB DB.
4. Import `yagoLiteralFacts.tsv` # test first, but move after types & taxonomy when done
    Source is 321 MiB, 3.353.659 statements. ~45 mins SSD. Result is 5.034 MiB DB.
5. Import `yagoFacts.tsv`. Source: 321 MiB, 4.484.914 statements. ~45 mins SSD, result 7.042 MiB.
6. Import `yagoImportantTypes.tsv`. 169 MiB. 2,723,628 statements. ~1 hr SSD, result 8.096 MiB.
7. Import `yagoSimpleTypes.tsv`. 316 MiB, 5,437,179 statements. ~1 hr SSD, result 10.081 MiB.
8. Import `yagoTypes.tsv`. 821 MiB, 9,019,769 statements.
9. Import `yagoSimpleTaxonomy.tsv` (499 KiB, 6,576 statements, ~1 sec), `yagoTaxonomy.tsv` (49 Mib, 451,708 statements)
10. Import `yagoGeonamesClassIds.tsv` (36 KiB), `yagoGeonamesClasses.tsv` (52 KiB), `yagoGeonamesGlosses.tsv` (65 KiB), `yagoGeonamesEntityIds.tsv` (7 MiB)
    ~2 mins SSD.
11. Import `yagoWordnetIds.tsv` (4 MiB, 68,862 statements), `yagoWordnetDomains.tsv` (9 MiB, 87,573 statements).
12. Index the labels (these can be dropped first if later imports are needed):

        CREATE INDEX ON :Literal(v)

Excluded Yago files are: (note: even if excluded, these can always be queried online through official Yago website)

1. `yagoWikipediaInfo.tsv` (2.3 GiB): just `linksTo` stuff, not useful
2. `yagoSources.tsv` (6.9 Gib): relates factIds to Wikipedia URIs
3. `yagoTransitiveType.tsv` (2.5 GiB): just the same as `yago(|Important|Simple)Types.csv` inferred using `yagoTaxonomy.tsv`
4. `yagoStatistics.tsv`: just meta about Yago dataset.
5. `yagoSchema.tsv`: just meta about Yago properties.
6. `yagoMultilingualInstanceLabels.tsv`: 443 MiB, 8,164,317 statements. English ones are OK (at least for now).
7. `yagoMultilingualClassLabels.tsv`: 46 MiB, 787,650 statements. English ones are OK (at least for now, but we do need to say that "car" is "mobil" soon).
8. `yagoGeonamesData.tsv`: 1.7 GiB, 32,216,600 statements. If you want exact lat-long for `yagoGeoEntity`s,
    you can already get the `geonamesEntityId` and look them up on Geonames DB.
