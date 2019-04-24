#!/bin/bash
#
# Copyright 2018-2019 The Jaeger Authors
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
# NAMESPACE ACTION MAX_WAIT_TIME

# this script delete or create and keep on monitoring project status, until it gets deleted or created

# update namespace name
NAMESPACE=$1

# delete or create?
ACTION=$2

# update maximum wait time in seconds, ie: 10
MAX_WAIT_TIME=$3  # in seconds

# starting timestamp
TIMESTAMP_START=$(date +%s)

# convert maximum wait time to maximum timstamp
MAX_TIMESTAMP=`expr ${TIMESTAMP_START} + ${MAX_WAIT_TIME}`

# run the specified action
if [ ${ACTION} == 'delete' ]; then
  oc delete project ${NAMESPACE}
elif [ ${ACTION} == 'create' ]; then
  oc new-project ${NAMESPACE}
else
  echo "WARN: Unkown action[${ACTION}] specified!"
  # TODO: return from here
fi

ACTION_STATUS=""

while [ $(date +%s) -lt ${MAX_TIMESTAMP} ]
do
  NAMESPACES=$(oc get namespaces -o=jsonpath='{.items[*].metadata.name}')
  if [ ${ACTION} == 'delete' ]; then
    if [ "${NAMESPACES/$NAMESPACE}" == "$NAMESPACES" ]; then
      ACTION_STATUS="done"
      break
    fi
  elif [ ${ACTION} == 'create' ]; then
    if [  "${NAMESPACES/$NAMESPACE}" != "$NAMESPACES" ]; then
      ACTION_STATUS="done"
      break
    fi
  else
    echo "WARN: Unkown action[${ACTION}] specified!"
    break
  fi
  # sleep for a while
  sleep 5
  ACTION_STATUS="max"
done

# check we reached maximum timeout [or] completed in time.
if [ ${ACTION_STATUS} == "max" ]; then
  echo "WARNING: Reached maximum wait time! Namespace: ${NAMESPACE}, Action: ${ACTION}"
fi
