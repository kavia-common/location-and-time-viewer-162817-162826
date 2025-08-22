#!/bin/bash
cd /home/kavia/workspace/code-generation/location-and-time-viewer-162817-162826/android_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

