#!/bin/bash

SEMTAG='./tools/semtag'
ACTION=${1:-patch}

git fetch origin --tags

RELEASE_VERSION="$($SEMTAG final -s $ACTION -o)"

echo "Next release version: $RELEASE_VERSION"

if test -f "manifest.json"; then
  NO_PREFIX_VERSION="${RELEASE_VERSION//v/}"
  echo "Update version in manifest.json to $NO_PREFIX_VERSION"

  jq --arg version "$NO_PREFIX_VERSION" '.version |= $version' manifest.json > /tmp/manifest.json
  mv /tmp/manifest.json manifest.json
  cat manifest.json

  git config --global user.name 'github-actions'
  git config --global user.email 'github-actions@users.noreply.github.com'

  git add manifest.json
  git commit -m "Bump version to $RELEASE_VERSION"
  git push origin HEAD
fi

$SEMTAG final -s $ACTION -v "$RELEASE_VERSION"
