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
#
set -x
sed -i 's;parameters:.*$;\0\n- description: Queue size parameter for the collector\n  displayName: Jaeger Collector Queue Size\n  name: COLLECTOR_QUEUE_SIZE\n  required: false\n  value: "300000";g' jaeger-production-template.yml
sed -i 's;parameters:.*$;\0\n- description: Number of collector pods\n  displayName: Jaeger Collector Pods\n  name: COLLECTOR_PODS\n  required: false\n  value: "1";g' jaeger-production-template.yml
sed -i 's;.*- "--config-file=/conf/collector.yaml".*$;\0\n            - "--collector.queue-size=${COLLECTOR_QUEUE_SIZE}";g' jaeger-production-template.yml

#
# HACK alert.  Change the number of replicas for the collector here if desired.  This script currently depends
# on the entry for the jaeger-collector occuring in the template (and specifically the line 'replicas: 1'
# occuring before the 'replicas: 1' entry for anything else, such as the jaeger-ui
sed -i '0,/replicas: 1/s/replicas: 1/replicas: ${COLLECTOR_PODS}/' jaeger-production-template.yml

