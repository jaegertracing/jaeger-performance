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

set -x

# download config map from master branch
cp openshift/templates/performance-test-in-openshift.yml performance-test-in-openshift.yml

# update tchannel, grpc port. http:14268, tchannel:14267, grpc:14250
if [ ${REPORTER_TYPE} == 'grpc' ]; then
    export JAEGER_COLLECTOR_PORT_POD="14250"
else
    export JAEGER_COLLECTOR_PORT_POD="14267"
fi

# update environment variables
sed -i 's;${RUNNING_ON_OPENSHIFT};'${RUNNING_ON_OPENSHIFT}';g' performance-test-in-openshift.yml
sed -i 's;${TESTS_TO_RUN};'${TESTS_TO_RUN}';g' performance-test-in-openshift.yml
sed -i 's;${PERFORMANCE_TEST_DATA};'${PERFORMANCE_TEST_DATA}';g' performance-test-in-openshift.yml
sed -i 's;${NUMBER_OF_TRACERS};'${NUMBER_OF_TRACERS}';g' performance-test-in-openshift.yml
sed -i 's;${NUMBER_OF_SPANS};'${NUMBER_OF_SPANS}';g' performance-test-in-openshift.yml
sed -i 's;${QUERY_LIMIT};'${QUERY_LIMIT}';g' performance-test-in-openshift.yml
sed -i 's;${QUERY_SAMPLES};'${QUERY_SAMPLES}';g' performance-test-in-openshift.yml
sed -i 's;${QUERY_INTERVAL};'${QUERY_INTERVAL}';g' performance-test-in-openshift.yml
sed -i 's;${SENDER};'${SENDER}';g' performance-test-in-openshift.yml
sed -i 's;${STORAGE_TYPE};'${STORAGE_TYPE}';g' performance-test-in-openshift.yml
sed -i 's;${SPANS_COUNT_FROM};'${SPANS_COUNT_FROM}';g' performance-test-in-openshift.yml
sed -i 's;${STORAGE_HOST};'${STORAGE_HOST}';g' performance-test-in-openshift.yml
sed -i 's;${STORAGE_PORT};'${STORAGE_PORT}';g' performance-test-in-openshift.yml
sed -i 's;${STORAGE_KEYSPACE};'${STORAGE_KEYSPACE}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_QUERY_HOST};'${JAEGER_QUERY_HOST}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_QUERY_PORT};'${JAEGER_QUERY_PORT}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_COLLECTOR_HOST};'${JAEGER_COLLECTOR_HOST}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_COLLECTOR_PORT};'${JAEGER_COLLECTOR_PORT}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_AGENT_HOST};'${JAEGER_AGENT_HOST}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_AGENT_PORT};'${JAEGER_AGENT_PORT}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_FLUSH_INTERVAL};'${JAEGER_FLUSH_INTERVAL}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_MAX_POCKET_SIZE};'${JAEGER_MAX_POCKET_SIZE}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_SAMPLING_RATE};'${JAEGER_SAMPLING_RATE}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_MAX_QUEUE_SIZE};'${JAEGER_MAX_QUEUE_SIZE}';g' performance-test-in-openshift.yml
sed -i 's;${COLLECTOR_PODS};'${COLLECTOR_PODS}';g' performance-test-in-openshift.yml
sed -i 's;${COLLECTOR_QUEUE_SIZE};'${COLLECTOR_QUEUE_SIZE}';g' performance-test-in-openshift.yml
sed -i 's;${COLLECTOR_NUM_WORKERS};'${COLLECTOR_NUM_WORKERS}';g' performance-test-in-openshift.yml
sed -i 's;${QUERY_STATIC_FILES};'${QUERY_STATIC_FILES:=default}';g' performance-test-in-openshift.yml
sed -i 's;${ES_MEMORY};'${ES_MEMORY}';g' performance-test-in-openshift.yml
sed -i 's;${ES_BULK_SIZE};'${ES_BULK_SIZE}';g' performance-test-in-openshift.yml
sed -i 's;${ES_BULK_WORKERS};'${ES_BULK_WORKERS}';g' performance-test-in-openshift.yml
sed -i 's;${ES_BULK_FLUSH_INTERVAL};'${ES_BULK_FLUSH_INTERVAL}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_AGENT_IMAGE};'${JAEGER_AGENT_IMAGE}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_COLLECTOR_IMAGE};'${JAEGER_COLLECTOR_IMAGE}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_QUERY_IMAGE};'${JAEGER_QUERY_IMAGE}';g' performance-test-in-openshift.yml
sed -i 's;${STORAGE_IMAGE};'${STORAGE_IMAGE}';g' performance-test-in-openshift.yml
sed -i 's;${STORAGE_IMAGE_INSECURE};'${STORAGE_IMAGE_INSECURE}';g' performance-test-in-openshift.yml
sed -i 's;${PERFORMANCE_TEST_IMAGE};'${PERFORMANCE_TEST_IMAGE}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_AGENT_QUEUE_SIZE};'${JAEGER_AGENT_QUEUE_SIZE}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_AGENT_WORKERS};'${JAEGER_AGENT_WORKERS}';g' performance-test-in-openshift.yml
sed -i 's;${REPORTER_TYPE};'${REPORTER_TYPE}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGER_COLLECTOR_PORT_POD};'${JAEGER_COLLECTOR_PORT_POD}';g' performance-test-in-openshift.yml
sed -i 's;${JAEGERQE_CONTROLLER_URL};'${JAEGERQE_CONTROLLER_URL}';g' performance-test-in-openshift.yml
sed -i 's;${USE_INTERNAL_REPORTER};'${USE_INTERNAL_REPORTER}';g' performance-test-in-openshift.yml
sed -i 's;${REPORT_SPANS_DURATION};'${REPORT_SPANS_DURATION}';g' performance-test-in-openshift.yml
sed -i 's;${REPORTER_REPLICA_COUNT};'${REPORTER_REPLICA_COUNT}';g' performance-test-in-openshift.yml
sed -i 's;${METRICS_BACKEND};'${METRICS_BACKEND}';g' performance-test-in-openshift.yml
sed -i 's;${REPORT_ENGINE_URL};'${REPORT_ENGINE_URL}';g' performance-test-in-openshift.yml
sed -i 's;${HOST_COUNT_REPORTER};'${HOST_COUNT_REPORTER}';g' performance-test-in-openshift.yml
sed -i 's;${HOST_COUNT_QUERY};'${HOST_COUNT_QUERY}';g' performance-test-in-openshift.yml
sed -i 's;${REPORTER_REFERENCE};'${REPORTER_REFERENCE}';g' performance-test-in-openshift.yml

# deploy jaeger performance test
oc create -n ${OS_NAMESPACE} -f performance-test-in-openshift.yml

# move configmap file to logs directory
mkdir -p logs
mv performance-test-in-openshift.yml logs/