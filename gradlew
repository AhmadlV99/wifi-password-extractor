#!/bin/sh
GRADLE_OPTS="-Xmx2048m"
APP_HOME=$(cd "$(dirname "$0")" && pwd)
JAVACMD=java
if [ -n "$JAVA_HOME" ]; then JAVACMD="$JAVA_HOME/bin/java"; fi
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec "$JAVACMD" $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
