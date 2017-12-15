Change Log
==========

### changes in 0.2

* changed loadBundle() from MarkerSupport to be readBundle() in Neo4jFileLoader
* added an Option[Logger] to readBundle()
* pass an implicit Logger to all that need it
* removed the requirement for .json file extension when processing zip files

### changes in 0.1

* initial commit