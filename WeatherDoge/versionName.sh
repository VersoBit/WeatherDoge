#!/bin/sh

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
printf `sed -n '2p' < "$DIR/versionDef.txt"`-
git --git-dir="$DIR/../.git" --work-tree="$DIR/.." rev-parse --short HEAD 2> /dev/null
