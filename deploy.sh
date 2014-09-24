#!/bin/bash

cd app
./gradlew distZip
cf api https://api.run.pivotal.io
cf auth $CF_USERNAME $CF_PASSWORD
cf target -o $CF_ORG -s $CF_SPACE
cf push -n $HOSTNAME
