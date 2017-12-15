## A Scala library to load STIX-2 objects to a Neo4j graph database

This library **stixtoneolib** provides for loading [STIX-2](https://oasis-open.github.io/cti-documentation/) 
objects and relations into a [Neo4j](https://neo4j.com/) graph database. 

The [OASIS](https://www.oasis-open.org/) open standard Structured Threat Information Expression [STIX-2](https://oasis-open.github.io/cti-documentation/) 
is a language for expressing cyber threat and observable information.

[Neo4j](https://neo4j.com/) "is a highly scalable native graph database that leverages data 
relationships as first-class entities, helping enterprises build intelligent applications 
to meet today’s evolving data challenges."
In essence, a graph database and processing engine that is used here for storing Stix objects 
and their relationships.
 
**stixtoneolib** provides a set of classes and methods to convert [STIX-2](https://oasis-open.github.io/cti-documentation/) 
domain objects (SDO) and relationships (SRO) to [Neo4j](https://neo4j.com/) nodes and relations 
using the [Java Neo4j API](https://neo4j.com/docs/java-reference/current/javadocs/). 
The library adds the converted STIX-2 objects and bundles to an existing Neo4j graph database 
or creates a new one. The library includes methods for processing text files containing 
STIX-2 bundles in JSON format and zip files.
    
### Installation and packaging

**stixtoneolib** is a library for use in Scala applications to load STIX-2 objects 
into a Neo4j graph database. To use the latest release add the following dependency to your *build.sbt*:

    libraryDependencies += "com.github.workingDog" %% "stixtoneolib" % "0.1"

To compile and package from source, type:

    sbt package

This will produce "stixtoneolib-0.2.jar" in the "./target/scala-2.12" directory. 
See the *build.sbt* file for the required dependencies.
      
### Usage

The main class for loading STIX-2 objects into a Neo4j database is **Neo4jLoader**.
This class constructor requires a Neo4j database directory name, which is used to connect to an 
existing database or create a new database.
**Neo4jLoader** has two main methods: 

    loadIntoNeo4j(bundle)  to load a bundle of STIX-2 objects into a Neo4j database
    loadIntoNeo4j(stix)    to load a STIX-2 object into a Neo4j database

**Neo4jLoader** delegates the creation of Neo4j nodes and relations 
to **NodesMaker** and **RelationsMaker** classes respectively. Typically, nodes are 
created first followed by the relations.

In addition the **Neo4jFileLoader** helper class has four methods for processing files of STIX-2 bundles:

    loadBundleFile(infile)        to load a file containing a bundle of STIX-2 objects
    loadBundleZipFile(infile)     to load a zip file containing files each having a bundle of STIX-2 objects
    
    loadLargeTextFile(infile)     to load one line at a time a file containing a bundle of STIX-2 objects
    loadLargeZipTextFile(infile)  to load one line at a time a zip file containing files each having a bundle of STIX-2 objects

An example use of **"stixtoneolib"** is [StixToNeoDB](https://github.com/workingDog/StixToNeoDB) 
which loads files of STIX-2 bundles into a Neo4j database.

                    
### References
 
1) [Neo4j](https://neo4j.com/)

2) [Java Neo4j API](https://neo4j.com/docs/java-reference/current/javadocs/)

3) [ScalaStix](https://github.com/workingDog/scalastix)

4) [STIX-2](https://oasis-open.github.io/cti-documentation/)


### Dependencies and requirements

Depends on the [ScalaStix](https://github.com/workingDog/scalastix) and 
the [Neo4j Community](https://mvnrepository.com/artifact/org.neo4j/neo4j) jar file.

See also the *build.sbt* file.

Java 8 is required and Neo4j Community Edition should be installed to process the results.

### Status

work in progress.



