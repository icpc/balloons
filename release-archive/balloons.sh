#!/bin/env bash

# Usage:
#   start: balloons.sh
#   commands:
#     ./balloons.sh volunteer create [--admin] login password
#     ./balloons.sh volunteer update login --manage=true
#     ./balloons.sh volunteer update login --new-password=password
#     ./balloons.sh h2shell

PORT=8081
CONFIG_DIR=config
CREDS_FILE=creds.json
SELF_REGISTRATION=true

if [[ "$#" -eq 0 ]]; then
  EXTRA_ARGS="--port=$PORT"
  if [[ "$SELF_REGISTRATION" != true ]]; then
    EXTRA_ARGS="$EXTRA_ARGS --disable-registration"
  fi
fi

java -jar balloons.jar \
    ${EXTRA_ARGS-} \
    --config-directory=${CONFIG_DIR} \
    --creds=${CREDS_FILE} \
    "$@"
