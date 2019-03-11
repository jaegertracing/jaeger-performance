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

#
# Need to add // --collector.num-workers=50 --collector.queue-size=2000
#

# pass following arguments in the same order,
# NAMESPACE

# update namespace name
OS_NAMESPACE=$1

set -x

# copy jaeger services production template
if [ ${ELASTICSEARCH_PROVIDER} == 'es-operator' ]; then
    cp openshift/templates/jaeger-services-deploy-es.yaml jaeger-services-final.yaml
else
    cp openshift/templates/jaeger-services-es-url.yaml jaeger-services-final.yaml
fi

# update variables
sed -i 's;${JAEGER_SERVICE_NAME};'${JAEGER_SERVICE_NAME}';g' jaeger-services-final.yaml
sed -i 's;${METRICS_BACKEND};'${METRICS_BACKEND}';g' jaeger-services-final.yaml
sed -i 's;${COLLECTOR_REPLICA_COUNT};'${COLLECTOR_REPLICA_COUNT}';g' jaeger-services-final.yaml
sed -i 's;${COLLECTOR_NUM_WORKERS};'${COLLECTOR_NUM_WORKERS}';g' jaeger-services-final.yaml
sed -i 's;${COLLECTOR_QUEUE_SIZE};'${COLLECTOR_QUEUE_SIZE}';g' jaeger-services-final.yaml
sed -i 's;${COLLECTOR_ES_BULK_SIZE};'${COLLECTOR_ES_BULK_SIZE}';g' jaeger-services-final.yaml
sed -i 's;${COLLECTOR_ES_BULK_WORKERS};'${COLLECTOR_ES_BULK_WORKERS}';g' jaeger-services-final.yaml
sed -i 's;${COLLECTOR_ES_BULK_FLUSH_INTERVAL};'${COLLECTOR_ES_BULK_FLUSH_INTERVAL}';g' jaeger-services-final.yaml
sed -i 's;${STORAGE_HOST};'${STORAGE_HOST}';g' jaeger-services-final.yaml
sed -i 's;${STORAGE_PORT};'${STORAGE_PORT}';g' jaeger-services-final.yaml
sed -i 's;${IMAGE_ELASTICSEARCH};'${IMAGE_ELASTICSEARCH}';g' jaeger-services-final.yaml
sed -i 's;${IMAGE_JAEGER_AGENT};'${IMAGE_JAEGER_AGENT}';g' jaeger-services-final.yaml
sed -i 's;${IMAGE_JAEGER_COLLECTOR};'${IMAGE_JAEGER_COLLECTOR}';g' jaeger-services-final.yaml
sed -i 's;${IMAGE_JAEGER_QUERY};'${IMAGE_JAEGER_QUERY}';g' jaeger-services-final.yaml
sed -i 's;${LOG_LEVEL_JAEGER_AGENT};'${LOG_LEVEL_JAEGER_AGENT}';g' jaeger-services-final.yaml
sed -i 's;${LOG_LEVEL_JAEGER_COLLECTOR};'${LOG_LEVEL_JAEGER_COLLECTOR}';g' jaeger-services-final.yaml
sed -i 's;${LOG_LEVEL_JAEGER_QUERY};'${LOG_LEVEL_JAEGER_QUERY}';g' jaeger-services-final.yaml


if [ ${#QUERY_STATIC_FILES} -gt 0 ]
then
    sed -i 's;${QUERY_STATIC_FILES};'${QUERY_STATIC_FILES}';g' jaeger-services-final.yaml
else
    sed -i '/${QUERY_STATIC_FILES}/d' jaeger-services-final.yaml
fi

# update jaeger images
sed -i 's;jaegertracing/jaeger-agent.*;'${IMAGE_JAEGER_AGENT}';g' jaeger-services-final.yaml
sed -i 's;jaegertracing/jaeger-collector.*;'${IMAGE_JAEGER_COLLECTOR}';g' jaeger-services-final.yaml
sed -i 's;jaegertracing/jaeger-query.*;'${IMAGE_JAEGER_QUERY}';g' jaeger-services-final.yaml

# delete old deployments
oc delete -f jaeger-services-final.yaml -n ${OS_NAMESPACE}

# sleep for a while
sleep 20

# deploy jaeger services
oc create -f jaeger-services-final.yaml -n ${OS_NAMESPACE}

# wait for jaeger collector service to come up
./openshift/scripts/wait_for_service.sh ${OS_NAMESPACE} ${JAEGER_SERVICE_NAME}-collector 60
# ./openshift/scripts/wait_for_pods_status.sh ${OS_NAMESPACE} "app.kubernetes.io/name=${JAEGER_SERVICE_NAME}-collector" "Running" 60

# move jaeger services template file to logs directory
mkdir -p logs
mv jaeger-services-final.yaml logs/


# There is a blocking issue with jaeger operator
# For now as a workaround we have to remove jaeger operator service,
# once we deployed jaeger services.
# ISSUE: https://github.com/jaegertracing/jaeger-operator/issues/307
#        https://github.com/jaegertracing/jaeger-operator/issues/334
# TODO: once this issue fixed, remove this workaround.
#if [ -f "./logs/operator.yaml" ]; then
#    echo "WARN: Running workaround for the jaeger operator issue"
#    echo "GitHub issue: https://github.com/jaegertracing/jaeger-operator/issues/307"
#    echo "Jaeger Operator will be deleted now!"
#    # give some time to deploy jaeger services
#    sleep 20
#    oc delete -f ./logs/operator.yaml -n observability
#    # wait for a while
#    sleep 30
#fi