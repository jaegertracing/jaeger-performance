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
# NAMESPACE SERVICE_NAME MAX_WAIT_TIME

# wait for a service

# update namespace name
OS_NAMESPACE=$1

# service name
SERVICE_NAME=$2

# update maximum wait time in seconds, ie: 10
MAX_WAIT_TIME=$3  # in seconds

# starting timestamp
TIMESTAMP_START=$(date +%s)

# convert maximum wait time to maximum timstamp
MAX_TIMESTAMP=`expr ${TIMESTAMP_START} + ${MAX_WAIT_TIME}`

FINAL_STATUS="false"

while [ $(date +%s) -lt ${MAX_TIMESTAMP} ]
do
  # read service status from OpenShift
  if SERVICE_STATUS=$(oc get service ${SERVICE_NAME} -n ${OS_NAMESPACE} --no-headers 2>&1); then
    echo "INFO: ${SERVICE_STATUS}"
    echo "INFO: Overall time taken: `expr $(date +%s) - ${TIMESTAMP_START}` seconds"
    FINAL_STATUS="true"
    break
  else
    sleep 2
  fi
done

if [ "false" == ${FINAL_STATUS} ];then
  echo "WARNING: Reached maximum wait time and still service not up! status: ${SERVICE_STATUS}"
fi
