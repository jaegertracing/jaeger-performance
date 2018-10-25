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
# NAMESPACE POD_FILTER WAIT_FOR_STATUS MAX_WAIT_TIME

# this script keep on monitoring pod status
# exit if there is no pods in WAIT_FOR_STATUS status [or] loop terminates on MAX_WAIT_TIME

# update namespace name
OS_NAMESPACE=$1
# update pod filters. ie: app=jaeger-performance-test-job
POD_FILTER=$2
# update wait for a pod state, ie: Running
WAIT_FOR_STATUS=$3
# update maximum wait time in seconds, ie: 10
MAX_WAIT_TIME=$4  # in seconds

# starting timestamp
TIMESTAMP_START=$(date +%s)

# convert maximum wait time to maximum timstamp
MAX_TIMESTAMP=`expr ${TIMESTAMP_START} + ${MAX_WAIT_TIME}`

while [ $(date +%s) -lt ${MAX_TIMESTAMP} ]
do
  # read pod status from OpenShift
  FINAL_STATUS="-"
  POD_STATUS=`oc get pods -n ${OS_NAMESPACE} --no-headers -l ${POD_FILTER} | awk '{print $1 "=" $3}'`
  NUMBER_OF_PODS=`echo ${POD_STATUS} | wc -w`
  PODS=$(echo ${POD_STATUS} | tr " " "\n")
  for _pod in ${PODS}
  do
    _status=(${_pod//=/ })
    if [ ${_status[1]} == ${WAIT_FOR_STATUS} ] || [ ${_status[1]} == 'ContainerCreating' ] || [ ${_status[1]} == 'Pending' ]; then
      FINAL_STATUS=${_status[1]}
    fi
  done
  if [ ${FINAL_STATUS} != ${WAIT_FOR_STATUS} ] && [ ${FINAL_STATUS} != 'ContainerCreating' ] || [ ${_status[1]} == 'Pending' ];then
    echo "INFO: There is no pods in '${WAIT_FOR_STATUS}' or 'ContainerCreating' or 'Pending' state."
    echo "INFO: Overall time taken: `expr $(date +%s) - ${TIMESTAMP_START}` seconds, Number of pod(s): ${NUMBER_OF_PODS}" 
    break
  else
    sleep 5
  fi
done

# display pods details
echo "INFO: Pods detail: ${POD_STATUS}"

# check we reached maximum timeout [or] completed in time.
if [ ${FINAL_STATUS} == ${WAIT_FOR_STATUS} ]; then
  echo "WARNING: Reached maximum wait time and still some of pod(s) are in '${WAIT_FOR_STATUS}' state."
fi
