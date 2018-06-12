# Jaeger Standalone Performance Tests
This project is designed to do basic performance testing of Jaeger. The primary target for these tests is OpenShift, 
where they can be run using the Jenkinsfile in this directory.  Instructions for running Jaeger from source or in 
Docker  are provided to help with test development.

## Overview of tests
The following steps are required for a test run:

+ Set appropriate environment variables such as DURATION_IN_MINUTES and SPAN_STORAGE_TYPE  (see below for details)
+ Start Jaeger
+ Run the `CreateTraces` application which will create traces based on environment variable settings.  When running on 
OpenShift `CreateTraces` is deployed as an OpenShift Job type using the **fabric8 maven plugin**.  It is deployed with
the _jaeger-agent_ as a sidecar.
+ Run the ValidateTraces test to confirm the expected number of traces were created.  This count is obtained using either the ElasticSearch
or Cassandra Java APIs.
+ Run the TimeQueries test to time some queries using the Jaeger UI.

## Running on OpenShift
The Jenkinsfile is designed to be run on a Jenkins instance deployed within OpenShift.  This is required to be able to 
connect to Jaeger Agent or Connector ports, which are not accessible outside of the cluster.

### Updates for ElasticSearch
In order to run ElasticSearch on your OpenShift cluster, you may have to have your cluster admin execute the command `sudo sysctl -w vm.max_map_count=262144`

If you are running on minishift you can do: `minishift ssh 'echo "sysctl -w vm.max_map_count=262144" | sudo tee -a /var/lib/boot2docker/bootlocal.sh'`

### Creating the Jenkins job
+ In Jenkins, create a new item.  Give it a name, select the `Pipeline` type, and click on OK
+ Select `Do not allow concurrent builds`
+ In the Pipeline section select `Pipeline script from SCM`.  Select `Git` as SCM, and enter the URL of this repo next to `Repository URL`
+ Change `Script Path ` to `standalone/Jenkinsfile`
+ Click on `SAVE`

### Running the test
NOTE: Because of this bug https://issues.jenkins-ci.org/browse/JENKINS-41929 you will only see a `Build Now` link instead of
a `Build With parameters` link.  Click on this, and wait for the job to fail and refresh the page.

After the first run you should see the `Build with Parameters` link.  Click on this.

NOTE: be sure to check the ES_MEMORY setting.  It currently defaults to **2Gi** but as 3 elasticsearch replicas are created 
that may cause problems on minishift or smaller OpenShift installations, so you may want to reduce this to **1Gi** or **512Mi**

#### Test parameters
+ `USE_AGENT_OR_COLLECTOR` Determines how traces will be created
+ `SPAN_STORAGE_TYPE` Which back end to use.  Typically we test with ElasticSearch
+ `DURATION_IN_MINUTES` Amount of time trace creation will run for
+ `THREAD_COUNT` Number of _TraceWriter_ threads to create per `WORKER_PODS`
+ `DELAY` Delay in milliseconds between trace creation
+ `WORKER_PODS` The number of pods to run _TraceWriter_ threads in
+ `TRACERS_PER_POD` The number of distinct Jaeger Tracers to create per `WORKER_POD`
+ `COLLECTOR_PODS` Number of `jaeger-collector` replicas to create
+ `COLLECTOR_QUEUE_SIZE` Startup option for jaeger-collector
+ `ES_MEMORY` Amount of memory to allocate to each ElasticSearch pod
+ `ES_BULK_SIZE, ES_BULK_WORKERS, ES_BULK_FLUSH_INTERVAL` Startup options for `jaeger-collector` when using ElasticSearch
+ `JAEGER_SAMPLING_RATE` Percentage of traces to store to back end.

Less typical options
+ `DELETE_JAEGER_AT_END, DELETE_JOB_AT_END` Permits leaving Jaeger or _TracerWriter_ running on OpenShift if desired.  This is sometimes useful for debugging.
+ `RUN_SMOKE_TESTS` Run the smoke tests, usually only used when testing specific images
+ `JAEGER_AGENT_IMAGE, JAEGER_COLLECTOR_IMAGE, JAEGER_QUERY_IMAGE` Override default `latest` docker hub images if desired


## Running on a desktop

### Environment setup
For a basic test run, set at least these environment variables
+ export THREAD_COUNT=100
+ export DURATION_IN_MINUTES=5
+ export COLLECTOR_QUEUE_SIZE=3000000   
+ `export SPAN_STORAGE_TYPE=cassandra` or `export SPAN_STORAGE_TYPE=elasticsearch`

### Download and build Jaeger
Download and build it based on the instructions under **Running Individual Jaeger Components** from https://jaeger.readthedocs.io/en/latest/getting_started/
NOTE: Even if you don't want to run from source, you'll need this to create a Cassandra keyspace.  You can skip this step if you are using 
ElasticSearch and don't want to run from source.

### Running with Cassandra

#### Storage Setup
Set the keyspace name `export CASSANDRA_KEYSPACE_NAME=jaeger_v1_test`
Set `CASSANDRA_CLUSTER_IP` to the real IP of the machine if using docker.  Otherwise set it to `localhost`

##### Running Cassandra with Docker
+ `docker run --name=cassandra --rm -it -p 7000:7000 -p 9042:9042 cassandra:3.11.1 `

##### Running a standalone Cassandra
+ Install Cassandra following the instructions here: `http://cassandra.apache.org/download/`
+ Start your Cassandra instance using `./bin/cassandra -f`

##### Running Cassandra with CCM
+ Follow the installation instructions here: `https://github.com/pcmanus/ccm`
+ `ccm create test --version=3.11.1 --nodes=1 --start`  Adjust the number of nodes as needed

##### Creating the keyspace
After starting your Cassandra instance, use the `cqlsh` client to create the keyspace for testing.  
+ cd to the Jaeger source directory.  (NOTE if you have not already done so, install it from `http://cassandra.apache.org/download/`)
+ `MODE=test ./plugin/storage/cassandra/schema/create.sh | cqlsh `

#### Running Jaeger
##### Run JAEGER using Docker

###### Start the Jaeger Collector
+ `docker run -it -e COLLECTER_QUEUE_SIZE=${COLLECTOR_QUEUE_SIZE} -e CASSANDRA_SERVERS=${CASSANDRA_CLUSTER_IP} -e CASSANDRA_KEYSPACE=${CASSANDRA_KEYSPACE_NAME} --rm -p14267:14267 -p14268:14268 jaegertracing/jaeger-collector:latest` 

###### Optional: Start the Jaeger Agent and UI
+ `docker run -it -e CASSANDRA_SERVERS=${CASSANDRA_CLUSTER_IP} -e CASSANDRA_KEYSPACE=${CASSANDRA_KEYSPACE_NAME} -p16686:16686  jaegertracing/jaeger-query:latest`
+ `docker run -it -e PROCESSOR_JAEGER_BINARY_SERVER_QUEUE_SIZE=100000 -e PROCESSOR_JAEGER_COMPACT_SERVER_QUEUE_SIZE=100000 -e COLLECTOR_HOST_PORT=${CASSANDRA_CLUSTER_IP}:14267 -p5775:5775/udp -p6831:6831/udp -p6832:6832/udp -p5778:5778/tcp jaegertracing/jaeger-agent:latest
`
##### Run Jaeger from Source
Start by downloading and building Jaeger based on the instructions under **Running Individual Jaeger Components** from https://jaeger.readthedocs.io/en/latest/getting_started/

###### Start the Jaeger Collector
+ cd to the Jaeger source directory and build.
+ Collector: `go run ./cmd/collector/main.go  --cassandra.keyspace=${CASSANDRA_KEYSPACE_NAME} --collector.queue-size=${COLLECTOR_QUEUE_SIZE}`

###### Start the Jaeger UI
Execute the following command from the Jaeger source directory
+ `go run cmd/query/main.go ` 

###### Optional: Start the Jaeger Agent
+ Agent: `go run ./cmd/agent/main.go --collector.host-port=localhost:14267 --processor.jaeger-binary.server-queue-size=${COLLECTOR_QUEUE_SIZE} --processor.jaeger-compact.server-queue-size=${COLLECTOR_QUEUE_SIZE} --processor.zipkin-compact.server-queue-size=${COLLECTOR_QUEUE_SIZE} `
 (NOTE:  I'm not sure which of these we need...maybe compact for 6831, zipkin-compact for 5775?)
 
### Running the tests
+ Clone this repo (`https://github.com/jaegertracing/jaeger-performance.git`) 
+ cd jaeger-performance
+ cd standalone
+ `export RUNNING_IN_OPENSHIFT=false`
+ Optional: `export USE_AGENT_OR_COLLECTOR=agent` if you want to test using the agent
+ Optional: `export CASSANDRA_CLUSTER_IP=localhost` or wherever cassandra is running, default is cassandra
+ Optional: `export RUNNING_IN_OPENSHIFT=false` if not on OpenShift
+ Recommended `export DELAY=100`  NOTE: Cassandra is suceptible to dropping traces if they are created too quickly.
+ `mvn exec:java`
+ `mvn -Pvalidate -DexpectedTraceCount=nnnnnn clean verify`  where nnnnnn is the trace count output by the previous step

#### To empty the traces keyspace between runs
`cqlsh --keyspace=jaeger_v1_test --execute="truncate traces;"`

### Running with ElasticSearch
Note that unlike Cassandra with ElasticSearch there is no need to create a keyspace or database.
+ `export SPAN_STORAGE_TYPE=elasticsearch`

##### Running ElasticSearch with Docker 
`docker run -it --rm -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "xpack.security.enabled=false"  docker.elastic.co/elasticsearch/elasticsearch:5.6.2`

##### Running a standalone ElasticSearch
+ Download from https://www.elastic.co/downloads/elasticsearch, extract, run `./bin/elasticsearch`

#### Running Jaeger from Docker
##### Starting the Collector
+ `docker run -it -e SPAN_STORAGE_TYPE=elasticsearch -e ES_SERVER_URLS="http://192.168.0.nnn:9200" -e COLLECTER_QUEUE_SIZE=${COLLECTOR_QUEUE_SIZE} -e ES_BULK_SIZE=10000000 -e ES_BULK_WORKERS=10 -e ES_BULK_FLUSH_INTERVAL=1s --rm -p14267:14267 -p14268:14268 jaegertracing/jaeger-collector:latest`
NOTE: for `ES_SERVIER_URLS` use the actual ip address instead of `localhost` if you are running docker

###### Start the Jaeger UI
NOTE: I can't get this to work at the moment, as I don't know how to tell the UI where the collector is.  This may require crossdock
`docker run -it -p16686:16686 -e SPAN_STORAGE_TYPE=elasticsearch -e ES_SERVER_URLS="http://192.168.0.173:9200" jaegertracing/jaeger-query:latest `

#### Running Jaeger from Source

##### Starting the Collector
+ `export SPAN_STORAGE_TYPE=elasticsearch`
+ `go run ./cmd/collector/main.go --collector.queue-size=${COLLECTOR_QUEUE_SIZE} --es.bulk.size=10000000 --es.bulk.workers=10 --es.bulk.flush-interval=1s`
Note that the `-es.` arguments are optional but may be needed for long running tests

###### Start the Jaeger UI
Execute the following command from the Jaeger source directory
+ `go run cmd/query/main.go ` 

### Running the tests

#### Checkout and run the tests
TODO what are correct settings for a desktop test?  Need at least 100 threads, but probably should increase DELAY
+ Clone this repo (`https://github.com/jaegertracing/jaeger-performance.git`) 
+ Optional: `export USE_AGENT_OR_COLLECTOR=agent` if you want to test using the agent
+ Optional: `export RUNNING_IN_OPENSHIFT=false` if not on OpenShift
+ `export SPAN_STORAGE_TYPE=elasticsearch`
+ `export ELASTICSEARCH_HOST=localhost` or wherever elasticsearch is running, default is elasticsearch
+ `mvn exec:java`
+ `mvn -Pvalidate clean -DexpectedTraceCount=nnnnnn verify`  where nnnnnn is the trace count output by the previous step

#### To empty the traces table between runs
+ `curl -X DELETE localhost:9200/jaeger-*`

#### To get a count of traces 
+ `curl http://localhost:9200/jaeger-span-*/_count` 













