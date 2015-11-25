@rem we don't build the WAR because deploying a compressed WAR takes too much time
@rem better to build a standard stuff
mvn -DskipTests -DincludeScope=runtime clean dependency:copy-dependencies install
