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

# update namespace details
JO_NAMESPACE=observability
ESO_NAMESPACE=observability

set -x

# download yaml files
curl https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/operator.yaml --output operator.yaml
curl https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/role_binding.yaml --output role_binding.yaml
curl https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/role.yaml --output role.yaml
curl https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/service_account.yaml --output service_account.yaml
curl https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/crds/jaegertracing_v1_jaeger_crd.yaml --output jaegertracing_v1_jaeger_crd.yaml

# update jaeger operator image
sed -i 's;jaegertracing/jaeger-operator.*;'${IMAGE_JAEGER_OPERATOR}';g' operator.yaml
# enable debug for jaeger operator
sed -i 's;\["start"\];["start", "--log-level='${LOG_LEVEL_JAEGER_OPERATOR}'"];g' operator.yaml

# delete jaeger operator
oc delete -f operator.yaml -n ${JO_NAMESPACE}
oc delete -f role_binding.yaml -n ${JO_NAMESPACE}
oc delete -f role.yaml -n ${JO_NAMESPACE}
oc delete -f service_account.yaml -n ${JO_NAMESPACE}
oc delete -f jaegertracing_v1_jaeger_crd.yaml -n ${JO_NAMESPACE}

# elasticsearch operator
if [ ${ELASTICSEARCH_PROVIDER} == 'es-operator' ]; then
    curl https://raw.githubusercontent.com/openshift/elasticsearch-operator/master/manifests/01-service-account.yaml --output es_01-service-account.yaml
    curl https://raw.githubusercontent.com/openshift/elasticsearch-operator/master/manifests/02-role.yaml --output es_02-role.yaml
    curl https://raw.githubusercontent.com/openshift/elasticsearch-operator/master/manifests/03-role-bindings.yaml --output es_03-role-bindings.yaml
    curl https://raw.githubusercontent.com/openshift/elasticsearch-operator/master/manifests/04-crd.yaml --output es_04-crd.yaml
    curl https://raw.githubusercontent.com/openshift/elasticsearch-operator/master/manifests/05-deployment.yaml --output es_05-deployment.yaml

    # update ES operator image
    sed -i 's;quay.io/openshift/origin-elasticsearch-operator.*;'${IMAGE_ELASTICSEARCH_OPERATOR}';g' es_05-deployment.yaml
    sed -i 's;imagePullPolicy: IfNotPresent.*;imagePullPolicy: Always;g' es_05-deployment.yaml

    # delete ES operator
    oc delete -f es_05-deployment.yaml -n ${ESO_NAMESPACE}
    oc delete -f es_04-crd.yaml -n ${ESO_NAMESPACE}
    oc delete -f es_03-role-bindings.yaml -n ${ESO_NAMESPACE}
    oc delete -f es_02-role.yaml -n ${ESO_NAMESPACE}
    oc delete -f es_01-service-account.yaml -n ${ESO_NAMESPACE}
fi

# delete and wait to complete the process: NAMESPACE ACTION TIMEOUT
./openshift/scripts/wait_for_project.sh ${JO_NAMESPACE} delete 60

# create and wait to complete the process: NAMESPACE ACTION TIMEOUT
./openshift/scripts/wait_for_project.sh ${JO_NAMESPACE} create 60

# deploy jaeger operator
oc create -f jaegertracing_v1_jaeger_crd.yaml -n ${JO_NAMESPACE}
oc create -f service_account.yaml -n ${JO_NAMESPACE}
oc create -f role.yaml -n ${JO_NAMESPACE}
oc create -f role_binding.yaml -n ${JO_NAMESPACE}
oc create -f operator.yaml -n ${JO_NAMESPACE}

# deploy elasticsearch operator
if [ ${ELASTICSEARCH_PROVIDER} == 'es-operator' ]; then
    oc create -f es_01-service-account.yaml -n ${ESO_NAMESPACE}
    oc create -f es_02-role.yaml -n ${ESO_NAMESPACE}
    oc create -f es_03-role-bindings.yaml -n ${ESO_NAMESPACE}
    oc create -f es_04-crd.yaml -n ${ESO_NAMESPACE}
    oc create -f es_05-deployment.yaml -n ${ESO_NAMESPACE}
fi

# move files to logs directory
mkdir -p logs
mv jaegertracing_v1_jaeger_crd.yaml logs/
mv service_account.yaml logs/
mv role.yaml logs/
mv role_binding.yaml logs/
mv operator.yaml logs/
if [ ${ELASTICSEARCH_PROVIDER} == 'es-operator' ]; then
    mv es_01-service-account.yaml logs/
    mv es_02-role.yaml logs/
    mv es_03-role-bindings.yaml logs/
    mv es_04-crd.yaml logs/
    mv es_05-deployment.yaml logs/
fi

# wait for jaeger operator to come up
./openshift/scripts/wait_for_service.sh ${JO_NAMESPACE} jaeger-operator 60