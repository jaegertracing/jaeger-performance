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
# NAMESPACE POD_FILTER MAX_WAIT_TIME

# this script keep on monitoring performance test pod status
# exit if the performence completes [or] loop terminates on MAX_WAIT_TIME

# update namespace name
OS_NAMESPACE=$1
# update pod filters. ie: app=jaeger-performance-test-job
POD_FILTER=$2
# update maximum wait time in seconds, ie: 10
MAX_WAIT_TIME=$3  # in seconds

# starting timestamp
TIMESTAMP_START=$(date +%s)

# convert maximum wait time to maximum timstamp
MAX_TIMESTAMP=`expr ${TIMESTAMP_START} + ${MAX_WAIT_TIME}`

while [ $(date +%s) -lt ${MAX_TIMESTAMP} ]
do
  # read pod status from OpenShift
  POD_STATUS=`oc get pods -n ${OS_NAMESPACE} -l ${POD_FILTER} -o jsonpath="{.items[*].status.containerStatuses[*].state.terminated.reason}"`
  if [ -z "${POD_STATUS}" ]; then
    sleep 5
  else
    echo "INFO: Performencae test pod status: ${POD_STATUS}"
    echo "INFO: Overall time taken: `expr $(date +%s) - ${TIMESTAMP_START}` seconds."
    break
  fi
done

# display pods details
echo "INFO: Pods detail: ${POD_STATUS}"

# check we reached maximum timeout [or] completed in time.
if [ ${POD_STATUS} == "" ]; then
  echo "WARNING: Reached maximum wait time and still performance test not completed"
fi
