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

# enable set -x if you want to print commands on console
# set -x

echo -e "\n\n------------------------- Logs -------------------------"

echo -e "\n----------------------- Elasticsearch logs -----------------------"
oc get pods -l app=elasticsearch -o name | xargs  -n1 -I@ sh -c 'echo "---------------------- @ --------------------------"; oc logs @; echo -e "------------------------------------------------------------------\n"'

echo -e "\n---------------------- Jaeger logs --------------------------"
oc get pods -l app=jaeger -o name | xargs  -n1 -I@ sh -c 'echo "---------------------- @ --------------------------"; oc logs @; echo -e "------------------------------------------------------------------\n"'

echo -e "\n---------------------- oc describe pods,services,events,configmaps,deployments -l name!=jenkins--------------------------"
oc describe pods,services,events,configmaps,deployments -l name!=jenkins
echo -e "------------------------------------------------------------------\n\n"
