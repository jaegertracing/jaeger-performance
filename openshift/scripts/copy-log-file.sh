#!/bin/bash
#
# Copyright 2018 The Jaeger Authors
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
# in compliance with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License
# is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
# or implied. See the License for the specific language governing permissions and limitations under
# the License.
#


# pass following arguments in the same order,
# NAMESPACE POD_FILTER CONTAINER_NAME

# this script copy files from inside pod. We can filter pods with POD_FILTER option

# update namespace name
OS_NAMESPACE=$1

# update pod filters. ie: app=jaeger-performance-test-job
POD_FILTER=$2

# update container name
CONTAINER_NAME=$3

# create logs directory
mkdir -p logs

# get pods
PODS=`oc get pods -n ${OS_NAMESPACE} --no-headers -l ${POD_FILTER} | awk '{print $1}'`
NUMBER_OF_PODS=`echo ${PODS} | wc -w`
PODS_LIST=$(echo ${PODS} | tr " " "\n")
echo "INFO: Pods list: ${PODS}"
for _pod in ${PODS_LIST}; do
  echo "INFO: Copying log file from ${_pod}"
  if [ -n "$CONTAINER_NAME" ]; then
    oc logs ${_pod} -c ${CONTAINER_NAME} -n ${OS_NAMESPACE} > logs/${OS_NAMESPACE}_${_pod}_${CONTAINER_NAME}.log
  else
    oc logs ${_pod} -n ${OS_NAMESPACE} > logs/${OS_NAMESPACE}_${_pod}.log
  fi
done
