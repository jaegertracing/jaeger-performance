# Jaeger Performance Test

This project is designed to do basic smoke and performance testing of [JaegerTracing](https://www.jaegertracing.io/). The primary target for these tests is OpenShift, where they can be run using the Jenkinsfile in this directory.


### Overview of tests
This test can be executed locally with this source code or can be executed on OpenShift with Jenkins pipeline file.

### Running on the Local environment
To run this tests on your local environment, jaeger services should be installed manually on your local machine.

* Follow [these steps](https://www.jaegertracing.io/docs/1.7/getting-started/) to install locally on your machine.
* Clone this repository `git clone https://github.com/jkandasa/jaeger-performance-new`
* Test configurations can be set via the environment variable [or] pass it via YAML file.


#### Run test by passing YAML configuration file
This is the easy way to running the test. We can keep all our test configurations on a YAML file and pass this YAML file to the test.

Sample yaml file is [available here](/test_config.yaml)

```yaml
# what are all the tests need to be executed
testsToRun: 'performance,smoke'

# performance test data, two type of performance test can be executed
# data format: [type,data]
# 1. quick run example, 'quick,50', data: delay between spans in milliseconds
# 2. long run example, 'long,600', data: test duration in seconds
performanceTestData: 'quick,10'

# tracers count, number of tracers should send spans
tracersCount: 10

# spans count for per tracer.
spansCount: 50000

# maximum spans/data limit on Jaeger query test
queryLimit: 20000

# Number of times the query to be executed repetitively.
# To get the average time
querySamples: 5

# How long once repeat the query test? '-1' indicates only at final
# values in seconds
queryInterval: -1

# where to send spans, options: http, udp
# 'http' will send spans to 'collector'
# 'udp' will send spans to 'agent'
sender: http

# storage type, options: elasticsearch, cassandra
storageType: elasticsearch

# Get a number of spans count from storage at the end of the test
# options: storage, jaeger-query
# Type 'storage' will be used direct storage query API
# Type 'jaeger-query' will be used jaeger query API
spansCountFrom: jaeger-query

# storage hostname and port number
storageHost: localhost
storagePort: 9200

# storage keyspace, used in Cassandra storage
storageKeyspace: keyspace

# jaeger query hostname and port number
jaegerQueryHost: localhost
jaegerQueryPort: 16686

# jaeger collector hostname and port number
jaegerCollectorHost: localhost
jaegerCollectorPort: 14268

# jaeger agent hostname and port number
jaegerAgentHost: localhost
jaegerAgentPort: 6831

# jaeger client java library configurations
jaegerFlushInterval: 100
jaegerMaxPocketSize: 0
jaegerMaxQueueSize: 10000
```

Running test
```bash
# pass configuration file as system properties and running tests
mvn clean test -DTEST_CONFIG_FILE=test_config.yaml

# [OR] 

# set test configuration file in the environment and running tests
export TEST_CONFIG_FILE=test_config.yaml
mvn clean test
```

#### Run test by Setting environment variables
Here environment variables are listed with default values on `[]`

* `TESTS_TO_RUN` ["performance,smoke"] - what are all the tests need to be executed
* `PERFORMANCE_TEST_DATA` ["quick,10"] - performance test data, two type of performance test can be executed. data format: [type,data]

    1. quick run example, 'quick,10', data: delay between spans in milliseconds
    2. long run example, 'long,600', data: test duration in seconds
* `NUMBER_OF_TRACERS` [5] - Number of tracers should be created
* `NUMBER_OF_SPANS` [10] - Number of spans per tracer. *Spans per second = NUMBER_OF_TRACERS * NUMBER_OF_SPANS*
* `QUERY_LIMIT` [20000] - maximum records limit, when run query test
* `QUERY_SAMPLES` [5] - number of times the same query should be repeated (we will get average query execution time)
* `QUERY_INTERVAL` [-1] - if you want to run query tests on multiple intervals, specify the values in seconds. `-1` indicates run only at the end of the test
* `SENDER` [http] - Where program wants to send spans? `http` - to collector. `udp` - to agent
* `STORAGE_TYPE` [elasticsearch] - Storage type will be used, options: elasticsearch, cassandra
* `SPANS_COUNT_FROM` [storage] - how to get spans count from jaeger tracing? via storage api or jaeger query api? options: storage, jaeger-query
* `STORAGE_HOST` [localhost] - storage hostname
* `STORAGE_PORT` [9200] - storage host port number
* `STORAGE_KEYSPACE` [keyspace] - keyspace name, used in cassandra storage type
* `JAEGER_QUERY_HOST` [localhost] - jaeger query service hostname
* `JAEGER_QUERY_PORT` [16686] - jaeger query service port number
* `JAEGER_COLLECTOR_HOST` [localhost] - jaeger collector service hostname
* `JAEGER_COLLECTOR_PORT` [14268] - jaeger collector service port number
* `JAEGER_AGENT_HOST` [localhost] - jaeger agent service hostname
* `JAEGER_AGENT_PORT` [6831] - jaeger agent service port number
* `JAEGER_FLUSH_INTERVAL` [100] - flush interval will be used in jaeger java client library
* `JAEGER_MAX_POCKET_SIZE` [0] - maximum udp pocket size used in jaeger client library(when `SENDER` == `udp`)
* `JAEGER_MAX_QUEUE_SIZE` [10000] - queue size in jaeger java client library

Some of the environment variables not listed here, which is used to setup storage and jaeger services on OpenShift environment via Jenkins.

All environment variables are [listed here](/src/main/java/io/jaegertracing/tests/model/TestConfig.java)

##### Running test
```bash
mvn clean test
```



### Running on OpenShift
The [Jenkinsfile pipeline file](/openshift/Jenkinsfile) is designed to be running tests on OpenShift environment. This test will deploy performance test as a container on OpenShift on the same namespace where jaeger services are running. This is required to be able to connect to Jaeger Agent or Connector ports, which are not accessible outside of the cluster.

The following dependencies need to run this job on Jenkins,
* maven
* java
* git
* oc (OpenShift client)

Jenkins pipeline will be looking `jaeger-qe-java` labeled slave. This slave should be running with the dependencies.
```
agent { label 'jaeger-qe-java' }
```