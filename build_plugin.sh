#!/usr/bin/env bash

rm -f prometheus/plugins/share/pluginPrometheus.jar

java -jar bob.jar --debug --platform=x86_64-macos --verbose --build-artifacts=plugins clean build

cp build/x86_64-osx/prometheus/pluginPrometheus.jar prometheus/plugins/share