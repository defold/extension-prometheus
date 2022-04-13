#!/usr/bin/env bash

java -jar bob.jar --debug --platform=x86_64-darwin --verbose --build-artifacts=plugins clean build

cp build/x86_64-osx/prometheus/pluginPrometheus.jar prometheus/plugins/share