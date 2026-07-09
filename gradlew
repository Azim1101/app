#!/usr/bin/env sh
# VDub Gradle Wrapper
# Run: ./gradlew assembleDebug

APP_BASE_NAME=\${0##*/}
APP_HOME=\$( cd "\${0%/*}" && pwd )
exec "\$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "\$@"
