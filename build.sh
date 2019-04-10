#!/usr/bin/env bash
echo 'building is starting...'
cd session-api/ && mvn clean install deploy
cd ../session-redis/ && mvn clean install deploy