#!/usr/bin/env bash

set -e
set -x

readonly GROUP_ID=deskdrop
readonly ARTIFACT_ID=people-api
readonly PROJECT_ID=sc-core-prd
readonly TAG=gcr.io/${PROJECT_ID}/${GROUP_ID}/${ARTIFACT_ID}
readonly BASE_PATH=$(cd $(dirname "$0") && pwd)

cd $BASE_PATH

SEMVER=${GIT_COMMIT:0:6}

if [[ -z "$SEMVER" ]]; then
	SEMVER=$(git rev-parse --short HEAD)
else
	GCLOUD_ACCOUNT="--account=903296551590-b2ggujrlcv15fbqds73kfh6nfeppvofd@developer.gserviceaccount.com"
	echo "GIT_COMMIT_REVISION=$GIT_COMMIT" > nextJob.properties
	echo "REGISTRY_DOCKER_IMAGE=${TAG}:${SEMVER}" >> nextJob.properties
fi

../gradlew installShadowApp -x test

docker build -t ${TAG}:${SEMVER} .

gcloud docker --project=$PROJECT_ID $GCLOUD_ACCOUNT push ${TAG}:${SEMVER}

docker tag -f ${TAG}:${SEMVER} ${TAG}:latest

gcloud docker --project=$PROJECT_ID $GCLOUD_ACCOUNT push ${TAG}:latest