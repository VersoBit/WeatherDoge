#!/bin/sh

printf `sed -n '2p' < versionDef.txt`-
git rev-parse --short HEAD 2> /dev/null
