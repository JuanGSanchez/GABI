#!/bin/sh
# Maven wrapper script (Unix/macOS/Linux)
# SPEC-R01: launches the pinned Maven via the wrapper jar on the CLASSPATH with the
# explicit main class. The wrapper jar (maven-wrapper-3.2.0.jar) has NO Main-Class in
# its manifest, so `java -jar` fails ("no main manifest attribute"); it must be run as
# `java -classpath <jar> org.apache.maven.wrapper.MavenWrapperMain`.

MAVEN_PROJECTBASEDIR="$( cd "$(dirname "$0")" ; pwd -P )"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if [ -z "$JAVA_HOME" ]; then
  JAVACMD=java
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

# If the wrapper jar is present, use it (downloads/uses the pinned Maven distribution);
# otherwise fall back to mvn on PATH.
if [ -f "$WRAPPER_JAR" ]; then
  exec "$JAVACMD" \
    -classpath "$WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    $WRAPPER_LAUNCHER "$@"
else
  exec mvn "$@"
fi
