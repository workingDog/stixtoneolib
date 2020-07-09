Change Log
==========

### changes in 0.7-SNAPSHOT


### changes in 0.6

* updated scala to 2.13.3, sbt to 1.3.13 and associated plugins and dependencies 
* updated to "scalastix" % "1.1"

### changes in 0.5

* fix a bug when running on Windows, see reading source file in Neo4jFileLoader
(basically changed Source.fromFile(inFile) to Source.fromFile(inFile, "UTF-8"))
* include the dependency: "org.slf4j" % "slf4j-nop" % "1.7.26"
* updated to neo4j 3.3.9
* updated to "scalastix" % "1.0"


### changes in 0.4

* in Neo4jFileLoader restrict processing zip files that contain only .json and .stix entry files.
* updated scalastix to ver: 0.9
* modified createAltDataStream
* changed Int to Long where appropriate
* updated sbt to ver: 1.2.8
* made Counter a separate class
* removed dependency, "ch.qos.logback" % "logback-classic" % "1.3.0-alpha4"

### changes in 0.3

* make val dbService = Neo4jDbService in Neo4jLoader to have access to its Neo4jDbService.
* make registerShutdownHook in Neo4jDbService public
* make val dbDir in the constructor of Neo4jLoader
* added the BoltConnector configuration in Neo4jDbService
* added  hostAddress: String = "localhost:7687" to Neo4jDbService, Neo4jLoader and Neo4jFileLoader
* make Neo4jDbService and all the other "objects" into classes

### changes in 0.2

* changed loadBundle() from MarkerSupport to be readBundle() in Neo4jFileLoader
* added an Option[Logger] to readBundle()
* pass an implicit Logger to all that need it
* removed the requirement for .json file extension when processing zip files
* added a Counter class to count the SDO, SRO and StixObj processed

### changes in 0.1

* initial commit