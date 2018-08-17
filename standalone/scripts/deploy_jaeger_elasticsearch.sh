#!/usr/bin/env bash
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

#
# Need to add // --collector.num-workers=50 --collector.queue-size=2000
#
set -x

# download config map from master branch
cp standalone/templates/configmap-elasticsearch.yml configmap-elasticsearch.yml
# update configmap
sed -i 's;${COLLECTOR_NUM_WORKERS};'${COLLECTOR_NUM_WORKERS}';g' configmap-elasticsearch.yml
sed -i 's;${COLLECTOR_QUEUE_SIZE};'${COLLECTOR_QUEUE_SIZE}';g' configmap-elasticsearch.yml
sed -i 's;${ES_BULK_SIZE};'${ES_BULK_SIZE}';g' configmap-elasticsearch.yml
sed -i 's;${ES_BULK_WORKERS};'${ES_BULK_WORKERS}';g' configmap-elasticsearch.yml
sed -i 's;${ES_BULK_FLUSH_INTERVAL};'${ES_BULK_FLUSH_INTERVAL}';g' configmap-elasticsearch.yml
if [ ${#QUERY_STATIC_FILES} -gt 0 ]
then
    sed -i 's;${QUERY_STATIC_FILES};'${QUERY_STATIC_FILES}';g' configmap-elasticsearch.yml
else
    sed -i '/${QUERY_STATIC_FILES}/d' configmap-elasticsearch.yml
fi
# create configmap
oc create -f configmap-elasticsearch.yml
rm configmap-elasticsearch.yml

# download jaeger production template from master branch
curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/jaeger-production-template.yml --output jaeger-production-template.yml
# update jaeger collector image
sed -i 's;jaegertracing/jaeger-collector:${IMAGE_VERSION};'${JAEGER_COLLECTOR_IMAGE}';g' jaeger-production-template.yml
# update jaeger query image
sed -i 's;jaegertracing/jaeger-query:${IMAGE_VERSION};'${JAEGER_QUERY_IMAGE}';g' jaeger-production-template.yml
# print selected images on the console
grep "image:" jaeger-production-template.yml

#
# HACK alert.  Change the number of replicas for the collector here if desired.  This script currently depends
# on the entry for the jaeger-collector occuring in the template (and specifically the line 'replicas: 1'
# occuring before the 'replicas: 1' entry for anything else, such as the jaeger-ui
# update parameters
sed -i 's;parameters:.*$;\0\n- description: Number of collector pods\n  displayName: Jaeger Collector Pods\n  name: COLLECTOR_PODS\n  required: false\n  value: "1";g' jaeger-production-template.yml
sed -i '0,/replicas: 1/s/replicas: 1/replicas: ${COLLECTOR_PODS}/' jaeger-production-template.yml

# deploy jaeger template
oc process ${DEPLOYMENT_PARAMETERS} -f jaeger-production-template.yml  | oc create -n ${PROJECT_NAME} -f -
