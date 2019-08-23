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
cp openshift/templates/performance-test.yaml performance-test.yaml

# update tchannel, grpc port. http:14268, tchannel:14267, grpc:14250
if [ ${REPORTER_TYPE} == 'grpc' ]; then
    export JAEGER_COLLECTOR_PORT_POD="14250"
    export JCH_PREFIX="dns:///" # Jaeger collector host prefix
else
    export JAEGER_COLLECTOR_PORT_POD="14267"
    export JCH_PREFIX=""
fi

# update references
RUNNING_ON_OPENSHIFT=true
LOGS_DIRECTORY=${PWD}/logs
JAEGER_AGENT_HOST="localhost"
JAEGER_AGENT_PORT=6831
JAEGER_COLLECTOR_HOST=${JAEGER_SERVICE_NAME}-collector
JAEGER_COLLECTOR_PORT=14268
JAEGER_QUERY_HOST=${JAEGER_SERVICE_NAME}-query
JAEGER_QUERY_PORT=16686

# update environment variables
sed -i 's;${JOB_REFERENCE};'${JOB_REFERENCE}';g' performance-test.yaml
sed -i 's;${JAEGER_SERVICE_NAME};'${JAEGER_SERVICE_NAME}';g' performance-test.yaml
sed -i 's;${OS_URL};'${OS_URL}';g' performance-test.yaml
sed -i 's;${OS_USERNAME};'${OS_USERNAME}';g' performance-test.yaml
sed -i 's;${OS_NAMESPACE};'${OS_NAMESPACE}';g' performance-test.yaml
sed -i 's;${TESTS_TO_RUN};'${TESTS_TO_RUN}';g' performance-test.yaml
sed -i 's;${ELASTICSEARCH_PROVIDER};'${ELASTICSEARCH_PROVIDER}';g' performance-test.yaml
sed -i 's;${STORAGE_HOST};'${STORAGE_HOST}';g' performance-test.yaml
sed -i 's;${STORAGE_PORT};'${STORAGE_PORT}';g' performance-test.yaml
sed -i 's;${PRE_INSTALL_JAEGER_OPERATOR};'${PRE_INSTALL_JAEGER_OPERATOR}';g' performance-test.yaml
sed -i 's;${PRE_INSTALL_JAEGER_SERVICES};'${PRE_INSTALL_JAEGER_SERVICES}';g' performance-test.yaml
sed -i 's;${PRE_INSTALL_REPORTER_NODES};'${PRE_INSTALL_REPORTER_NODES}';g' performance-test.yaml
sed -i 's;${REPORTER_NODE_REPLICA_COUNT};'${REPORTER_NODE_REPLICA_COUNT}';g' performance-test.yaml
sed -i 's;${REPORTER_REFERENCE};'${REPORTER_REFERENCE}';g' performance-test.yaml
sed -i 's;${POST_DELETE_JAEGER_SERVICES};'${POST_DELETE_JAEGER_SERVICES}';g' performance-test.yaml
sed -i 's;${POST_DELETE_TEST_JOBS};'${POST_DELETE_TEST_JOBS}';g' performance-test.yaml
sed -i 's;${POST_DELETE_REPORTER_NODES};'${POST_DELETE_REPORTER_NODES}';g' performance-test.yaml
sed -i 's;${TEST_HA_SETUP};'${TEST_HA_SETUP}';g' performance-test.yaml
sed -i 's;${JAEGERQE_CONTROLLER_URL};'${JAEGERQE_CONTROLLER_URL}';g' performance-test.yaml
sed -i 's;${REPORT_ENGINE_URL};'${REPORT_ENGINE_URL}';g' performance-test.yaml
sed -i 's;${REPORT_ENGINE_LABELS};'${REPORT_ENGINE_LABELS}';g' performance-test.yaml
sed -i 's;${REPORT_ENGINE_AGENT_REFERENCE};'${REPORT_ENGINE_AGENT_REFERENCE}';g' performance-test.yaml
sed -i 's;${IMAGE_PERFORMANCE_TEST};'${IMAGE_PERFORMANCE_TEST}';g' performance-test.yaml
sed -i 's;${IMAGE_ELASTICSEARCH_OPERATOR};'${IMAGE_ELASTICSEARCH_OPERATOR}';g' performance-test.yaml
sed -i 's;${IMAGE_ELASTICSEARCH};'${IMAGE_ELASTICSEARCH}';g' performance-test.yaml
sed -i 's;${IMAGE_JAEGER_OPERATOR};'${IMAGE_JAEGER_OPERATOR}';g' performance-test.yaml
sed -i 's;${IMAGE_JAEGER_ALL_IN_ONE};'${IMAGE_JAEGER_ALL_IN_ONE}';g' performance-test.yaml
sed -i 's;${IMAGE_JAEGER_AGENT};'${IMAGE_JAEGER_AGENT}';g' performance-test.yaml
sed -i 's;${IMAGE_JAEGER_COLLECTOR};'${IMAGE_JAEGER_COLLECTOR}';g' performance-test.yaml
sed -i 's;${IMAGE_JAEGER_QUERY};'${IMAGE_JAEGER_QUERY}';g' performance-test.yaml
sed -i 's;${IMAGE_JAEGER_ES_INDEX_CLEANER};'${IMAGE_JAEGER_ES_INDEX_CLEANER}';g' performance-test.yaml
sed -i 's;${USE_INTERNAL_REPORTER};'${USE_INTERNAL_REPORTER}';g' performance-test.yaml
sed -i 's;${NODE_COUNT_SPANS_REPORTER};'${NODE_COUNT_SPANS_REPORTER}';g' performance-test.yaml
sed -i 's;${NODE_COUNT_QUERY_RUNNER};'${NODE_COUNT_QUERY_RUNNER}';g' performance-test.yaml
sed -i 's;${MSG_BROKER_HOST};'${MSG_BROKER_HOST}';g' performance-test.yaml
sed -i 's;${MSG_BROKER_PORT};'${MSG_BROKER_PORT}';g' performance-test.yaml
sed -i 's;${MSG_BROKER_USER};'${MSG_BROKER_USER}';g' performance-test.yaml
sed -i 's;${NUMBER_OF_TRACERS};'${NUMBER_OF_TRACERS}';g' performance-test.yaml
sed -i 's;${NUMBER_OF_SPANS};'${NUMBER_OF_SPANS}';g' performance-test.yaml
sed -i 's;${REPORT_SPANS_DURATION};'${REPORT_SPANS_DURATION}';g' performance-test.yaml
sed -i 's;${SPANS_COUNT_FROM};'${SPANS_COUNT_FROM}';g' performance-test.yaml
sed -i 's;${QUERY_LIMIT};'${QUERY_LIMIT}';g' performance-test.yaml
sed -i 's;${QUERY_SAMPLES};'${QUERY_SAMPLES}';g' performance-test.yaml
sed -i 's;${QUERY_INTERVAL};'${QUERY_INTERVAL}';g' performance-test.yaml
sed -i 's;${SENDER};'${SENDER}';g' performance-test.yaml
sed -i 's;${REPORTER_TYPE};'${REPORTER_TYPE}';g' performance-test.yaml
sed -i 's;${METRICS_BACKEND};'${METRICS_BACKEND}';g' performance-test.yaml
sed -i 's;${RESOURCE_MONITOR_ENABLED};'${RESOURCE_MONITOR_ENABLED}';g' performance-test.yaml
sed -i 's;${JAEGER_AGENT_QUEUE_SIZE};'${JAEGER_AGENT_QUEUE_SIZE}';g' performance-test.yaml
sed -i 's;${JAEGER_AGENT_WORKERS};'${JAEGER_AGENT_WORKERS}';g' performance-test.yaml
sed -i 's;${JAEGER_CLIENT_FLUSH_INTERVAL};'${JAEGER_CLIENT_FLUSH_INTERVAL}';g' performance-test.yaml
sed -i 's;${JAEGER_CLIENT_MAX_POCKET_SIZE};'${JAEGER_CLIENT_MAX_POCKET_SIZE}';g' performance-test.yaml
sed -i 's;${JAEGER_CLIENT_MAX_QUEUE_SIZE};'${JAEGER_CLIENT_MAX_QUEUE_SIZE}';g' performance-test.yaml
sed -i 's;${COLLECTOR_REPLICA_COUNT};'${COLLECTOR_REPLICA_COUNT}';g' performance-test.yaml
sed -i 's;${COLLECTOR_QUEUE_SIZE};'${COLLECTOR_QUEUE_SIZE}';g' performance-test.yaml
sed -i 's;${COLLECTOR_NUM_WORKERS};'${COLLECTOR_NUM_WORKERS}';g' performance-test.yaml
sed -i 's;${COLLECTOR_ES_BULK_SIZE};'${COLLECTOR_ES_BULK_SIZE}';g' performance-test.yaml
sed -i 's;${COLLECTOR_ES_BULK_WORKERS};'${COLLECTOR_ES_BULK_WORKERS}';g' performance-test.yaml
sed -i 's;${COLLECTOR_ES_BULK_FLUSH_INTERVAL};'${COLLECTOR_ES_BULK_FLUSH_INTERVAL}';g' performance-test.yaml
sed -i 's;${COLLECTOR_ES_TAGS_AS_FIELDS};'${COLLECTOR_ES_TAGS_AS_FIELDS}';g' performance-test.yaml
sed -i 's;${JAEGER_QUERY_STATIC_FILES};'${JAEGER_QUERY_STATIC_FILES}';g' performance-test.yaml
sed -i 's;${ES_MEMORY};'${ES_MEMORY}';g' performance-test.yaml
sed -i 's;${LOG_LEVEL_JAEGER_AGENT};'${LOG_LEVEL_JAEGER_AGENT}';g' performance-test.yaml
sed -i 's;${LOG_LEVEL_JAEGER_COLLECTOR};'${LOG_LEVEL_JAEGER_COLLECTOR}';g' performance-test.yaml
sed -i 's;${LOG_LEVEL_JAEGER_OPERATOR};'${LOG_LEVEL_JAEGER_OPERATOR}';g' performance-test.yaml
sed -i 's;${LOG_LEVEL_JAEGER_QUERY};'${LOG_LEVEL_JAEGER_QUERY}';g' performance-test.yaml
sed -i 's;${RUNNING_ON_OPENSHIFT};'${RUNNING_ON_OPENSHIFT}';g' performance-test.yaml
sed -i 's;${LOGS_DIRECTORY};'${LOGS_DIRECTORY}';g' performance-test.yaml
sed -i 's;${JAEGER_AGENT_HOST};'${JAEGER_AGENT_HOST}';g' performance-test.yaml
sed -i 's;${JAEGER_AGENT_PORT};'${JAEGER_AGENT_PORT}';g' performance-test.yaml
sed -i 's;${JAEGER_AGENT_COLLECTOR_PORT};'${JAEGER_COLLECTOR_PORT_POD}';g' performance-test.yaml
sed -i 's;${JAEGER_COLLECTOR_HOST_PREFIX};'${JCH_PREFIX}';g' performance-test.yaml
sed -i 's;${JAEGER_COLLECTOR_HOST};'${JAEGER_COLLECTOR_HOST}';g' performance-test.yaml
sed -i 's;${JAEGER_COLLECTOR_PORT};'${JAEGER_COLLECTOR_PORT}';g' performance-test.yaml
sed -i 's;${JAEGER_QUERY_HOST};'${JAEGER_QUERY_HOST}';g' performance-test.yaml
sed -i 's;${JAEGER_QUERY_PORT};'${JAEGER_QUERY_PORT}';g' performance-test.yaml
sed -i 's;${RESO_LMT_AGENT_CPU};'${RESO_LMT_AGENT_CPU}';g' performance-test.yaml
sed -i 's;${RESO_LMT_AGENT_MEM};'${RESO_LMT_AGENT_MEM}';g' performance-test.yaml
sed -i 's;${RESO_LMT_COLLECTOR_CPU};'${RESO_LMT_COLLECTOR_CPU}';g' performance-test.yaml
sed -i 's;${RESO_LMT_COLLECTOR_MEM};'${RESO_LMT_COLLECTOR_MEM}';g' performance-test.yaml
sed -i 's;${RESO_LMT_QUERY_CPU};'${RESO_LMT_QUERY_CPU}';g' performance-test.yaml
sed -i 's;${RESO_LMT_QUERY_MEM};'${RESO_LMT_QUERY_MEM}';g' performance-test.yaml

# deploy jaeger performance test
oc create -n ${OS_NAMESPACE} -f performance-test.yaml

# move configmap file to logs directory
mkdir -p logs
mv performance-test.yaml logs/