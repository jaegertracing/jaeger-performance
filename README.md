# Simple standalone example of writing spans to Jaeger

This project can be used to test performance of Jaeger.  There are 4 setup options.  First, either Cassandra or ElasticSearch
can be used for storage.  Then we can run everything either from source or using docker

## Environment setup
For a basic test run, set at least these environment variables
+ export THREAD_COUNT=100
+ export ITERATIONS=30000
+ export USE_AGENT_OR_COLLECTOR=collector /// optional defaults to agent
+ export JAEGER_MAX_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))  
+ export COLLECTOR_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))
+ export `JAEGER_STORAGE=cassandra` or `export JAEGER_STORAGE=elastic`

## Download and build Jaeger
Download the Jaeger source and build it based on the instructions under **Running Individual Jaeger Components** from https://jaeger.readthedocs.io/en/latest/getting_started/

## Running with Cassandra

### Storage Setup
Set the keyspace name `export CASSANDRA_KEYSPACE_NAME=jaeger_v1_test`
Set `CASSANDRA_CLUSTER_IP` to the real IP of the machine if using docker.  Otherwise set it to `localhost`

#### Running Cassandra with Docker
+ `docker run --name=cassandra --rm -it -p 7000:7000 -p 9042:9042 cassandra:3.9 `
+ `export CASSANDRA_CLUSTER_IP=<ctual ip of cassandra is running on, not localhost`

#### Running a standalone Cassandra
+ Install Cassandra following the instructions here: `http://cassandra.apache.org/download/`
+ Start your Cassandra instance using `./bin/cassandra -f`

#### Running Cassandra with CCM
+ Follow the installation instructions here: `https://github.com/pcmanus/ccm`
+ `ccm create test --version=3.11.1 --nodes=1 --start`  Adjust the number of nodes as needed

#### Creating the keyspace
After starting your Cassandra instance, use the `cqlsh` client to create the keyspace for testing.  
+ cd to the Jaeger source directory
+ `MODE=test ./plugin/storage/cassandra/schema/create.sh | cqlsh `

### Running Jaeger
#### Run JAEGER using Docker

##### Start the Jaeger Collector
+ `docker run -it -e COLLECTER_QUEUE_SIZE=${COLLECTOR_QUEUE_SIZE} -e CASSANDRA_SERVERS=${CASSANDRA_CLUSTER_IP} -e CASSANDRA_KEYSPACE=${CASSANDRA_KEYSPACE_NAME} --rm -p14267:14267 -p14268:14268 jaegertracing/jaeger-collector:latest` 

##### Optional: Start the Jaeger Agent and UI
+ `docker run -it -e CASSANDRA_SERVERS=${CASSANDRA_CLUSTER_IP} -e CASSANDRA_KEYSPACE=${CASSANDRA_KEYSPACE_NAME} -p16686:16686  jaegertracing/jaeger-query:latest`
+ `docker run -it -e PROCESSOR_JAEGER_BINARY_SERVER_QUEUE_SIZE=100000 -e PROCESSOR_JAEGER_COMPACT_SERVER_QUEUE_SIZE=100000 -e COLLECTOR_HOST_PORT=${CASSANDRA_CLUSTER_IP}:14267 -p5775:5775/udp -p6831:6831/udp -p6832:6832/udp -p5778:5778/tcp jaegertracing/jaeger-agent:latest
`

#### Run Jaeger from Source
Start by downloading and building Jaeger based on the instructions under **Running Individual Jaeger Components** from https://jaeger.readthedocs.io/en/latest/getting_started/

##### Start the Jaeger Collector
+ cd to the Jaeger source directory and build.
+ Collector: `go run ./cmd/collector/main.go  --cassandra.keyspace=${CASSANDRA_KEYSPACE_NAME} --collector.queue-size=${COLLECTOR_QUEUE_SIZE}`

##### Optional: Start the Jaeger Agent and UI
+ Agent: `go run ./cmd/agent/main.go --collector.host-port=localhost:14267 --processor.jaeger-binary.server-queue-size=${COLLECTOR_QUEUE_SIZE} --processor.jaeger-compact.server-queue-size=${COLLECTOR_QUEUE_SIZE} --processor.zipkin-compact.server-queue-size=${COLLECTOR_QUEUE_SIZE} `
 (NOTE:  I'm not sure which of these we need...maybe compact for 6831, zipkin-compact for 5775?)
 + TODO Add instructions for the UI

## Running the tests
+ Clone this repo (`git@github.com:Hawkular-QE/SimpleJaegerExample.git`) and cd into it.
+ Optional: `export USE_AGENT_OR_COLLECTOR=agent` if you want to test using the agent\
+ `mvn clean -Dtest=SimpleTest#createTracesTest test`

### To get a count of traces 
`mvn clean -Dtest=SimpleTest#countTraces test`

### To empty the traces keyspace between runs
`cqlsh --keyspace=jaeger_v1_test --execute="truncate traces;"`

## Running with ElasticSearch
Note that unlike Cassandra with ElasticSearch there is no need to create a keyspace or database.

#### Running ElasticSearch with Docker 
`docker run -it --rm -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "xpack.security.enabled=false"  docker.elastic.co/elasticsearch/elasticsearch:5.6.1

#### Running a standalone ElasticSearch
+ Download from https://www.elastic.co/downloads/elasticsearch, extract, run `./bin/elasticsearch`

### Running Jaeger

#### Starting the Collector and Agent with ElasticSearch
`go run ./cmd/collector/main.go --collector.queue-size=${COLLECTOR_QUEUE_SIZE} --span-storage.type=elasticsearch `

#### TODO run Agent, UI.  Or just say they are not needed for these tests?

## Running the tests

### Checkout and run the tests
+ Clone this repo (`git@github.com:Hawkular-QE/SimpleJaegerExample.git`) and cd into it.
+ Optional: `export USE_AGENT_OR_COLLECTOR=agent` if you want to test using the agent
+ `export JAEGER_STORAGE=elastic`
+ `mvn clean -Dtest=SimpleTest#createTracesTest test`

### To empty the traces table between runs
+ `curl -X DELETE localhost:9200/jaeger-*`

### To get a count of traces 
+ `curl http://localhost:9200/jaeger-span-2017-12-12/_count` (after changing the date)









