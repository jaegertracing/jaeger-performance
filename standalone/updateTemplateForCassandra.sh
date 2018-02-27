#!/usr/bin/env bash
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

