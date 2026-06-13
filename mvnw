#!/bin/sh
# Maven wrapper script (Unix/macOS/Linux)
# Generated for GABI workstream (a).
# This script downloads and runs the pinned Maven version from .mvn/wrapper/maven-wrapper.properties

MAVEN_PROJECTBASEDIR="$( cd "$(dirname "$0")" ; pwd -P )"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

DISTRIBUTION_URL=$(grep "distributionUrl" "$WRAPPER_PROPERTIES" | sed 's/.*distributionUrl=//')
MAVEN_HOME="${MAVEN_PROJECTBASEDIR}/.mvn/wrapper/dist"

if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME is not set. Please set JAVA_HOME to a JDK 17+ installation."
  exit 1
fi

# If wrapper jar is present, use it; otherwise fall back to mvn on PATH
if [ -f "$WRAPPER_JAR" ]; then
  exec "$JAVA_HOME/bin/java" -jar "$WRAPPER_JAR" "$@"
else
  exec mvn "$@"
fi
