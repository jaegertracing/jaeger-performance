#!/bin/bash
#
# Copyright ${license.git.copyrightYears} The Jaeger Authors
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
# NAMESPACE

# update namespace name
OS_NAMESPACE=$1

# enable set -x if you want to print commands on console
#set -x


# copy log files

# collect elasticsearch logs
./openshift/scripts/copy-log-file.sh  ${OS_NAMESPACE} "app=elasticsearch"

# collect jaeger services logs
PODS=`oc get pods -n ${OS_NAMESPACE} --no-headers -l app=jaeger | awk '{print $1}'`

PODS_LIST=$(echo ${PODS} | tr " " "\n")
for _pod in ${PODS_LIST}; do
  echo "INFO: Copying log file from ${_pod}"
  if [[ ${_pod} = *"query"* ]]; then
    oc logs ${_pod} -c "jaeger-query" -n ${OS_NAMESPACE} > logs/${OS_NAMESPACE}_${_pod}_jaeger-query.log
    oc logs ${_pod} -c "jaeger-agent" -n ${OS_NAMESPACE} > logs/${OS_NAMESPACE}_${_pod}_jaeger-agent.log
  else
    oc logs ${_pod} -n ${OS_NAMESPACE} > logs/${OS_NAMESPACE}_${_pod}.log
  fi
done

# collect describe logs
oc describe pods,services,events,configmaps,deployments -l name!=jenkins >> logs/describe.log

