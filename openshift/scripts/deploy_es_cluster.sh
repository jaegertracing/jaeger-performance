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

# Unable to add multiline sed replace with Jenkins pipeline file, hence created this script as a workaround,
# and moved all the commands to this file.

# pass following arguments in the same order,
# NAMESPACE

# update namespace name
OS_NAMESPACE=$1

set -x

STORAGE_IMAGE_INSECURE="true"

curl https://raw.githubusercontent.com/RHsyseng/docker-rhel-elasticsearch/5.x/es-cluster-deployment.yml --output es-cluster-deployment.yaml
sed -i 's/512Mi/'${ES_MEMORY}'/g' es-cluster-deployment.yaml
sed -i 's/registry.centos.org\/rhsyseng\/elasticsearch:5.6.10/'${IMAGE_ELASTICSEARCH//\//\\/}'  \
    importPolicy: \
      insecure: '${STORAGE_IMAGE_INSECURE}'/g' es-cluster-deployment.yaml

# remove old deployments
oc delete -f es-cluster-deployment.yaml --grace-period=1 -n ${OS_NAMESPACE} || true
# sleep for a while
sleep 10
# deploy ES cluster
oc create -f es-cluster-deployment.yaml -n ${OS_NAMESPACE}
while true; do
    replicas=$(oc get statefulset/elasticsearch -o=jsonpath='{.status.readyReplicas}' -n ${OS_NAMESPACE})
    ((replicas > 1)) && break
    sleep 5
 done

# move deployment file to logs directory
mkdir -p logs
mv es-cluster-deployment.yaml logs/
