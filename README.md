# Simple standalone example of writing spans to Jaeger

This project can be used to test performance of Jaeger.  There are 4 setup options.  First, either Cassandra or ElasticSearch
can be used for storage.  Then we can run everything either from source or using docker

## Environment setup
For a basic test run, set at least these environment variables
+ export THREAD_COUNT=100
+ export ITERATIONS=3000
+ export DELAY=1  // millisecond delay between span creation
+ export USE_AGENT_OR_COLLECTOR=collector /// optional defaults to agent
+ export JAEGER_MAX_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))  
+ export COLLECTOR_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))

To select storage backend `export JAEGER_STORAGE=cassandra` or `export JAEGER_STORAGE=elastic`

If using Cassandra
+ export CASSANDRA_KEYSPACE_NAME=jaeger_v1_test

If using Cassandra with Docker
+ `export CASSANDRA_CLUSTER_IP=<ctual ip of cassandra is running on, not localhost`

## Jaeger Setup
+ Clone Jaeger source from git@github.com:jaegertracing/jaeger.git

## Cassandra Setup

### Running with Docker

#### Start Cassandra and create the keyspace
+ `docker run --name=cassandra --rm -it -p 7000:7000 -p 9042:9042 cassandra:3.9 `
+ `MODE=test ./plugin/storage/cassandra/schema/create.sh | cqlsh `

#### Start the Collector and Agent
+ `docker run -it -e COLLECTER_QUEUE_SIZE=${COLLECTOR_QUEUE_SIZE} -e CASSANDRA_SERVERS=${CASSANDRA_CLUSTER_IP} -e CASSANDRA_KEYSPACE=${CASSANDRA_KEYSPACE_NAME} --rm -p14267:14267 -p14268:14268 jaegertracing/jaeger-collector:latest` 
+ `docker run -it -e PROCESSOR_JAEGER_BINARY_SERVER_QUEUE_SIZE=100000 -e PROCESSOR_JAEGER_COMPACT_SERVER_QUEUE_SIZE=100000 -e COLLECTOR_HOST_PORT=${CASSANDRA_CLUSTER_IP}:14267 -p5775:5775/udp -p6831:6831/udp -p6832:6832/udp -p5778:5778/tcp jaegertracing/jaeger-agent:latest
`
#### Optional: Start the UI
+ `docker run -it -e CASSANDRA_SERVERS=${CASSANDRA_CLUSTER_IP} -e CASSANDRA_KEYSPACE=${CASSANDRA_KEYSPACE_NAME} -p16686:16686  jaegertracing/jaeger-query:latest`
`
### Running from source 
+ Start cassandra (i.e. download from http://cassandra.apache.org/download/, extract, and run ./bin/cassandra -f)
+ cd to the Jaeger source directory and build.
+ `MODE=test ./plugin/storage/cassandra/schema/create.sh | cqlsh `
+ Collector: `go run ./cmd/collector/main.go  --cassandra.keyspace=${CASSANDRA_KEYSPACE_NAME} --collector.queue-size=${COLLECTOR_QUEUE_SIZE}`
+ Agent: `go run ./cmd/agent/main.go --collector.host-port=localhost:14267 --processor.jaeger-binary.server-queue-size=${COLLECTOR_QUEUE_SIZE} --processor.jaeger-compact.server-queue-size=${COLLECTOR_QUEUE_SIZE} --processor.zipkin-compact.server-queue-size=${COLLECTOR_QUEUE_SIZE} `
 (NOTE:  I'm not sure which of these we need...maybe compact for 6831, zipkin-compact for 5775?)

## ElasticSearch Setup

### Running with Docker

#### Starting ElasticSearch with Docker 
`docker run -it --rm -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "xpack.security.enabled=false"  docker.elastic.co/elasticsearch/elasticsearch:5.6.1

#### Starting the Collector and Agent
+ Add `--span-storage.type=elasticsearch ` to the commands from the Cassandra section

### Running from Sources

#### Starting ElasticSearch
+ Download from https://www.elastic.co/downloads/elasticsearch, extract, run `./bin/elasticsearch`

#### Starting the Collector and Agent
`go run ./cmd/collector/main.go --collector.queue-size=${COLLECTOR_QUEUE_SIZE} --span-storage.type=elasticsearch `


### Checkout and run the tests
Clone this repo and cd into it `git@github.com:Hawkular-QE/SimpleJaegerExample.git`

### Run the tests
+ `mvn clean -Dtest=SimpleTest#createTracesTest test`

### Between test runs do this to clear out the traces table
+ For Cassandra: `cqlsh --keyspace=jaeger_v1_test --execute="truncate traces;"`
+ For ElasticSearch `curl -X DELETE localhost:9200/jaeger-*`

### Just get a count of traces
+ For Cassandra `mvn clean -Dtest=SimpleTest#countTraces test`
+ For ElasticSearch `curl http://localhost:9200/jaeger-span-2017-11-03/_count` (after changing the date)









