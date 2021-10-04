#!/bin/bash

set -eu

tag_for_branch() {
  git rev-parse --abbrev-ref HEAD | sed -e 's:/:-:g' | sed -E 's/^(main|devel)$/latest/'
}

readonly IMAGE_NAME=${IMAGE_NAME:-"chrovis/duxhund:$(tag_for_branch)"}
readonly FUSIONFUSION_TAG="$(cd fusionfusion; tag_for_branch)"
readonly DIRNAME=$(dirname "$(dirname "$(realpath "$0")")")

docker build -t "chrovis-genomon/fusionfusion:$FUSIONFUSION_TAG" "$DIRNAME/fusionfusion"
docker build -t "$IMAGE_NAME" --build-arg "BASE_IMAGE_TAG=$FUSIONFUSION_TAG" "$DIRNAME"
