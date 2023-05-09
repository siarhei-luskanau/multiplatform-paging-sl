#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh test zipOwnersFiles \
    -Pandroidx.useMaxDepVersions \
    -Pandroidx.displayTestOutput=false \
    -Pandroidx.ignoreTestFailures "$@"

echo "Completing $0 at $(date)"
