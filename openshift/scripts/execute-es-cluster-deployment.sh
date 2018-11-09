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

# Unable to add multiline sed replace with Jenkins pipeline file, hence created this script as a workaround,
# and moved all the commands to this file.

set -x
curl https://raw.githubusercontent.com/RHsyseng/docker-rhel-elasticsearch/5.x/es-cluster-deployment.yml --output es-cluster-deployment.yml
sed -i 's/512Mi/'${ES_MEMORY}'/g' es-cluster-deployment.yml
sed -i 's/registry.centos.org\/rhsyseng\/elasticsearch:5.6.10/'${STORAGE_IMAGE//\//\\/}'  \
    importPolicy: \
      insecure: '${STORAGE_IMAGE_INSECURE}'/g' es-cluster-deployment.yml
oc create -f es-cluster-deployment.yml -n ${OS_NAMESPACE}
while true; do
    replicas=$(oc get statefulset/elasticsearch -o=jsonpath='{.status.readyReplicas}' -n ${OS_NAMESPACE})
    ((replicas > 1)) && break
    sleep 5
 done

# move deployment file to logs directory
mkdir -p logs
mv es-cluster-deployment.yml logs/
