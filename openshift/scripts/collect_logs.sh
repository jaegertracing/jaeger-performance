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
# NAMESPACE

# update namespace name
OS_NAMESPACE=$1

# metric port details
JAGENT=14271
JCOLLECTOR=14269
JQUERY=16687

# enable set -x if you want to print commands on console
#set -x

# set metrics file extension
METRICS_EXTENSION="txt"
if [ ${METRICS_BACKEND} = "prometheus" ]; then
  METRICS_EXTENSION="prom"
  # download prometheus-scraper
  curl https://search.maven.org/remotecontent?filepath=org/hawkular/agent/prometheus-scraper/0.23.0.Final/prometheus-scraper-0.23.0.Final-cli.jar -o prometheus-scraper-0.23.0.Final-cli.jar
elif [ ${METRICS_BACKEND} = "expvar" ]; then
  METRICS_EXTENSION="json"
fi
echo "DEBUG: Metric backend: ${METRICS_BACKEND}, selected file extension:${METRICS_EXTENSION}"

# copy log files

# collect elasticsearch logs
./openshift/scripts/copy-log-file.sh  ${OS_NAMESPACE} "app=elasticsearch"

# collect jaeger services logs
# PODS=`oc get pods -n ${OS_NAMESPACE} --no-headers -l app=jaeger | awk '{print $1}'`
# disabline to get spans reporter pods
PODS=`oc get pods -n ${OS_NAMESPACE} --no-headers | awk '{print $1}'`

PODS_LIST=$(echo ${PODS} | tr " " "\n")
for _pod in ${PODS_LIST}; do
  _pod_ip=$(oc get pod ${_pod} --template={{.status.podIP}} -n ${OS_NAMESPACE})
  if [[ ${_pod_ip} == "" ]];then
    echo "DEBUG: Trying IP with jsonpath option"
    _pod_ip=$(oc get pod ${_pod} -o=jsonpath='{.status.podIP}' -n ${OS_NAMESPACE})
  fi
  
  echo "INFO: Copying log file from ${_pod}, IP:${_pod_ip}"
  if [[ ${_pod} = *"query"* ]]; then
    oc logs ${_pod} -c "jaeger-query" -n ${OS_NAMESPACE} > logs/${OS_NAMESPACE}_${_pod}_jaeger-query.log
    oc logs ${_pod} -c "jaeger-agent" -n ${OS_NAMESPACE} > logs/${OS_NAMESPACE}_${_pod}_jaeger-agent.log
    # metrics - query and agent
    if [[ ${METRICS_BACKEND} == "DONOTRUN" ]]; then
      curl http://${_pod_ip}:${JQUERY}/metrics --output logs/${OS_NAMESPACE}_${_pod}_metrics-query.${METRICS_EXTENSION}
      curl http://${_pod_ip}:${JAGENT}/metrics --output logs/${OS_NAMESPACE}_${_pod}_metrics-agent.${METRICS_EXTENSION}
      # convert prometheus logs to json
      if [ ${METRICS_BACKEND} = "prometheus" ]; then
        java -jar prometheus-scraper*-cli.jar --json http://${_pod_ip}:${JQUERY}/metrics > logs/${OS_NAMESPACE}_${_pod}_metrics-query.json
        java -jar prometheus-scraper*-cli.jar --json http://${_pod_ip}:${JAGENT}/metrics > logs/${OS_NAMESPACE}_${_pod}_metrics-agent.json
      fi
    fi
  elif [[ ${_pod} = *"collector"* ]]; then
    oc logs ${_pod} -n ${OS_NAMESPACE} > logs/${OS_NAMESPACE}_${_pod}.log
    # metrics - collector
    if [[ ${METRICS_BACKEND} == "DONOTRUN" ]]; then
      curl http://${_pod_ip}:${JCOLLECTOR}/metrics --output logs/${OS_NAMESPACE}_${_pod}_metrics-collector.${METRICS_EXTENSION}
      # convert prometheus logs to json
      if [ ${METRICS_BACKEND} = "prometheus" ]; then
        java -jar prometheus-scraper*-cli.jar --json http://${_pod_ip}:${JCOLLECTOR}/metrics > logs/${OS_NAMESPACE}_${_pod}_metrics-collector.json
      fi
    fi
  elif [[ ${_pod} = *"spans-reporter"* ]]; then
    # metrics - spans-reporter, jaeger agent
    if [[ ${METRICS_BACKEND} == "DONOTRUN" ]]; then
      curl http://${_pod_ip}:${JAGENT}/metrics --output logs/${OS_NAMESPACE}_${_pod}_metrics-agent.${METRICS_EXTENSION}
      # convert prometheus logs to json
      if [ ${METRICS_BACKEND} = "prometheus" ]; then
        java -jar prometheus-scraper*-cli.jar --json http://${_pod_ip}:${JAGENT}/metrics > logs/${OS_NAMESPACE}_${_pod}_metrics-agent.json
      fi
    fi
  fi
done

# collect describe logs
oc describe pods,services,events,configmaps,deployments -l name!=jenkins -n ${OS_NAMESPACE} >> logs/describe.log
oc get pods -n ${OS_NAMESPACE} >> logs/pods.log

# collect performance test xml logs
ls -alh
cp target/surefire-reports/* logs/ -R | true