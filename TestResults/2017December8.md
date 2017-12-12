# December 8 2017 Test Results

**Host** 
+ `Lenovo T540`
+ `Memory 16G`
+ `Intel Core i7-4900MQ CPU @ 2.80Ghz x 8`
+ `Disk 250gb SSD`
+ `Ubuntu 17.10`

** Cassandra Version `Standalone 3.11.1`
** Jaeger Version `1.0?`

## Test Summary
This is a standalone Java junit test which models our Jaeger Instrumentation Performance 
Test (See https://github.com/Hawkular-QE/jaeger-instrumentation-performance-tests) but
is simpler to setup and run.   (See the top-level README.md for instructions)

### Parameters for this test run
Tests were run with the following values
+ THREAD_COUNT=100
+ ITERATIONS=30000
+ DELAY=1
+ USE_AGENT_OR_COLLECTOR=collector
+ JAEGER_MAX_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))  
+ export COLLECTOR_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))
+ CASSANDRA_KEYSPACE_NAME=jaeger_v1_test
 
## Results with Standalone Cassandra, Jaeger Collector run from source
+ `Write Time ` is the amount of time it takes the test to create all the spans in seconds
+ `Read Time` is the amount of additional time in seconds it takes before all of the traces appear in Cassandra

|Run Number | Write Time | Read Time | Traces Found |
| ------------- | -----:|-----:|-----:|
| 1 | 33.854 | 690.953 | **2540275** | 
| 2 | 33.759 | 676.482 | **2538751** |
| 3 | 33.699 | 674.683 | **2531677** |

## Results with Standalone Cassandra, Jaeger Collector and Agent run from source
+ `Write Time ` is the amount of time it takes the test to create all the spans in seconds
+ `Read Time` is the amount of additional time in seconds it takes before all of the traces appear in Cassandra

Run on 11 December 2017

|Run Number | Write Time | Read Time | Traces Found |
| ------------- | -----:|-----:|-----:|
| 1 | 35.133 | 819.832 | **2906388** | 
| 2 | 34.365 | 794.285 | **2925826** | 
| 3 | 34.636 | 784.96 | **2942341** | 


## Results with CCM single node Cassandra Cluster, Jaeger Collector run from source
+ `Write Time ` is the amount of time it takes the test to create all the spans in seconds
+ `Read Time` is the amount of additional time in seconds it takes before all of the traces appear in Cassandra

Run on 12 December 2017

|Run Number | Write Time | Read Time | Traces Found |
| ------------- | -----:|-----:|-----:|
| 1 | 36.991 | 1,088.723 | 3000000 | 
| 2 | 35.772 | 1040.100 | 3000000 | 
| 3 | 35175 | 1017.729 | 3000000 | 

## Results with CCM single node Cassandra Cluster, Jaeger Collector and Agent run from source
+ `Write Time ` is the amount of time it takes the test to create all the spans in seconds
+ `Read Time` is the amount of additional time in seconds it takes before all of the traces appear in Cassandra

Run on 12 December 2017

|Run Number | Write Time | Read Time | Traces Found |
| ------------- | -----:|-----:|-----:|
| 1 | 35.685 | 1,081.882 | 2910235 | 
| 2 | 35.310 | 1,123.587 | 2968483 | 
| 3 | 35.888 | 1,118.428 | 2927866 | 

**NOTE!!!!!**
We never retrieve all 3,000,000 traces when using a single instance of Cassandra

## Results with 3 node Cassandra Cluster, Jaeger Collector run from Source
Using `ccm create test --version=3.11.1 --nodes=3 --start`
|Run Number | Write Time | Read Time | Traces Found |
| ------------- | -----:|-----:|-----:|
| 1 | 38.220 | 1497.738 | 3000000 | 
| 2 | 36.001 | 1,560.044  | 3000000 | 
| 3 | 36.270 | 1497.207  | 3000000 | 

## Results with 3 node Cassandra Cluster, Jaeger Collector and Agent run from Sources
|Run Number | Write Time | Read Time | Traces Found |
| ------------- | -----:|-----:|-----:|
| 1 | 33.988 | 1,507.507 | 2868983 | 
| 2 | 33.869 | 1,454.852 | 2871953 | 
| 3 | 34.014 | 1472.324 | 2832293 | 











