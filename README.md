## Simple standalone example of writing spans to Jaeger

# Start Cassandra and create the keyspace
+ docker run --name=cassandra --rm -it -p 7000:7000 -p 9042:9042 cassandra:3.9 
+ MODE=test ./plugin/storage/cassandra/schema/create.sh | cqlsh 

# Start the Collector and Agent
+ export CASSANDRA_KEYSPACE_NAME=jaeger_v1_test
+ export CASSANDRA_CLUSTER_IP=<actual ip of cassandra is running on>
+ docker run -it --rm -p14267:14267 -p14268:14268 jaegertracing/jaeger-collector /go/bin/collector-linux --cassandra.keyspace=${CASSANDRA_KEYSPACE_NAME} --cassandra.servers=${CASSANDRA_CLUSTER_IP} --collector.queue-size=100000 
+ docker run -it -p5775:5775/udp -p6831:6831/udp -p6832:6832/udp -p5778:5778/tcp jaegertracing/jaeger-agent:latest /go/bin/agent-linux --collector.host-port=${CASSANDRA_CLUSTER_IP}:14267 --processor.jaeger-binary.server-queue-size=100000 --processor.jaeger-compact.server-queue-size=100000

# Optional: Start the UI
`docker run -it -p16686:16686 jaegertracing/jaeger-query:latest /go/bin/query-linux --cassandra.servers=${CASSANDRA_CLUSTER_IP} --cassandra.keyspace=${CASSANDRA_KEYSPACE_NAME}
`
#Checkout and run the tests
Clone this repo and cd into it `git@github.com:Hawkular-QE/SimpleJaegerExample.git`

### Run the test

#### I configured this to create 300 000 spans in 30 seconds, 
+ export THREAD_COUNT=100
+ export ITERATIONS=3000
+ export DELAY=10
+ export USE_AGENT_OR_COLLECTOR=collector /// if you want,defaults to agent
+ export JAEGER_MAX_QUEUE_SIZE=100000  // This is the default

Optional for the test: CASSANDRA_CLUSTER_IP defaults to localhost CASSANDRA_KEYSPACE_NAME defaults to jaeger_v1_test
`mvn clean -Dtest=SimpleTest#createTracesTest test`

### Between test runs do this to clear out the traces table
`cqlsh --keyspace=jaeger_v1_test --execute="truncate traces;"
`
### Just get a count of traces in Cassandra
`mvn clean -Dtest=SimpleTest#countTraces test`



