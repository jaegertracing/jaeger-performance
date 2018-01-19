# January 18 2018 Test Results of testing with ElasticSearch
The results below are for tests run on January 18 2018.  This set of tests in includes multiple runs, first using 
the mainline Jaeger code, then using that code with Pavol's ES BUlk PR (https://github.com/jaegertracing/jaeger/pull/656)

## Environment
### Host
+ `Lenovo T540`
+ `Memory 16G`
+ `Intel Core i7-4900MQ CPU @ 2.80Ghz x 8`
+ `Disk 250gb SSD`
+ `Ubuntu 17.10`

### Software Versions
+ ElasticSearch Version `6.1.0`
+ Jaeger Version `1.0`

## Test Summary
This is a standalone Java junit test which models our Jaeger Instrumentation Performance 
Test (See https://github.com/Hawkular-QE/jaeger-instrumentation-performance-tests) but
is simpler to setup and run.   (See the top-level README.md for instructions)

NOTE: For this set of tests I started the collector with the following command as with ElasticSearch as I was getting 
many instances of the errors shown in the **Errors** section below
+ `export SPAN_STORAGE_TYPE=elasticsearch`
+ `nohup go run ./cmd/collector/main.go --collector.queue-size=${COLLECTOR_QUEUE_SIZE} > log.txt`

### Parameters for these test runs
Tests were run with the following values
+ THREAD_COUNT=100
+ ITERATIONS=30000
+ DELAY=1
+ JAEGER_MAX_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))  
+ COLLECTOR_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))

### Reading test results
+ `Write Time ` is the amount of time it takes the test to create all the spans in seconds
+ `Read Time` is the amount of additional time in seconds it takes before all of the traces appear in storage.  This time is not exact
as the test will sleep for up to 30 seconds between queries to reduce load on the backend storage.
+ `Total test time` is the time reported by JUnit for the entire test to run
 + The expected number of traces for all test runs is 3000000
 
## Results from Jaeger Mainline 
 
### Results with Standalone ElasticSearch, Jaeger Collector run from source using Jaeger Master
|Run Number | Write Time | Read Time |  Total Test Time |Traces Found |
| ------------- | -----:|-----:|-----:|-----:|
| 1 | 36.010 | 2,794.600 | 2,831.069 | 2960472 | // RETEST with tracer.close
| 2 | 35.024 | 2,783.665 | 2,819.153 | 2959891 | 
| 3 | 35.870 | 2,833.766 | 2,869.745 | 2962935 | 

### Results with Standalone ElasticSearch, Jaeger Collector and Agent run from source using Jaeger Master
I started the agent with following command, where COLLECTOR_QUEUE_SIZE was 3000000

` go run ./cmd/agent/main.go --collector.host-port=localhost:14267 --processor.jaeger-binary.server-queue-size=${COLLECTOR_QUEUE_SIZE} --processor.jaeger-compact.server-queue-size=${COLLECTOR_QUEUE_SIZE} --processor.zipkin-compact.server-queue-size=${COLLECTOR_QUEUE_SIZE}`

|Run Number | Write Time | Read Time | Total Test Time |Traces Found |
| ------------- | -----:|-----:|-----:|-----:|
| 1 | 32.866 | 2,693.536 | 2,728.668 | 2827819 | 
| 2 | 33.079 | 2,283.75 | 2,318.052 | 2398703 | ** Agent got some TChannel errors
| 3 | 35.310 | 2,674.430 | 2,709.987 | 2813756 | 

## Results with ES Bulk PR
I ran with the following collector command, where COLLECTOR_QUEUE_SIZE was 3000000

`nohup go run ./cmd/collector/main.go --collector.queue-size=${COLLECTOR_QUEUE_SIZE}  --es.bulk.size=10000000 --es.bulk.workers=10 --es.bulk.flush-interval=1s   > log.txt &`

## Results with Standalone ElasticSearch, Jaeger Collector run from source with PR applied
|Run Number | Write Time | Read Time | Total Test Time | Traces Found |
| ------------- | -----:|-----:|-----:|-----:|
| 1 | 41.430 | 99.667 | 141.528 | 3000000 |  
| 2 | 40.893 | 91.694 | 133.019 | 3000000 |
| 3 | 41.541 | 89.209 | 131.213 | 3000000 |

## Results with Standalone ElasticSearch, Jaeger Collector and Agent run with PR applied
|Run Number | Write Time | Read Time | Total Test Time | Traces Found |
| ------------- | -----:|-----:|-----:| -----:|
| 1 | 36.139 | 91.227 | 127.478 | 2733545 |  ** Got some TChannel errors
| 2 | 36.018 | 0.0 | 0.0 | 0 |   ** Test got a connection timeout to ES instance; agent got multiple TChannel errors;
| 3 | 36.575 | 82.150 | 118.839 | 2611344 | 
| 4 | 242.760 | 176.434 | 419.947 | 2130834 | ** Lots of TChannel errors
| 5 | 116.157 | 240.141 | 356.75 | 2274084 |

## Errors
In second run using Pavol's PR and the Agent I got the following on the ElasticSearch console, and the test failed with
a timeout waiting for a response from the ES rest api.  I also got multiple TChannel errors on the agent, one of which is shown below.


[2018-01-19T10:29:57,642][DEBUG][o.e.a.b.TransportShardBulkAction] [jaeger-span-586524-01-19][1] failed to execute bulk item (index) BulkShardRequest [[jaeger-span-586524-01-19][1]] containing [index {[jaeger-span-586524-01-19][span][viDADWEBluxXZbOP1vzV], source[{"traceID":"a0d86453665de159","spanID":"a0d86453665de159","parentSpanID":"0","flags":1,"operationName":"Thread 58","references":[],"startTime":18446744073709507484,"duration":4942,"tags":[{"key":"sampler.type","type":"string","value":"probabilistic"},{"key":"sampler.param","type":"float64","value":"1"},{"key":"iteration","type":"int64","value":"11613"}],"logs":[],"processID":"","process":{"serviceName":"standalone","tags":[{"key":"hostname","type":"string","value":"planet-express"},{"key":"jaeger.version","type":"string","value":"Java-0.23.0"},{"key":"ip","type":"string","value":"127.0.1.1"}]},"warnings":null,"startTimeMillis":18446744073709507}]}]
org.elasticsearch.index.mapper.MapperParsingException: failed to parse [startTime]
	at org.elasticsearch.index.mapper.FieldMapper.parse(FieldMapper.java:302) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.DocumentParser.parseObjectOrField(DocumentParser.java:485) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.DocumentParser.parseValue(DocumentParser.java:607) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.DocumentParser.innerParseObject(DocumentParser.java:407) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.DocumentParser.parseObjectOrNested(DocumentParser.java:384) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.DocumentParser.internalParseDocument(DocumentParser.java:93) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.DocumentParser.parseDocument(DocumentParser.java:67) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.DocumentMapper.parse(DocumentMapper.java:261) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.shard.IndexShard.prepareIndex(IndexShard.java:708) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.shard.IndexShard.applyIndexOperation(IndexShard.java:686) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.shard.IndexShard.applyIndexOperationOnPrimary(IndexShard.java:667) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.bulk.TransportShardBulkAction.executeIndexRequestOnPrimary(TransportShardBulkAction.java:548) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.bulk.TransportShardBulkAction.executeIndexRequest(TransportShardBulkAction.java:140) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.bulk.TransportShardBulkAction.executeBulkItemRequest(TransportShardBulkAction.java:236) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.bulk.TransportShardBulkAction.performOnPrimary(TransportShardBulkAction.java:123) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.bulk.TransportShardBulkAction.shardOperationOnPrimary(TransportShardBulkAction.java:110) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.bulk.TransportShardBulkAction.shardOperationOnPrimary(TransportShardBulkAction.java:72) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$PrimaryShardReference.perform(TransportReplicationAction.java:1033) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$PrimaryShardReference.perform(TransportReplicationAction.java:1011) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.ReplicationOperation.execute(ReplicationOperation.java:104) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$AsyncPrimaryAction.onResponse(TransportReplicationAction.java:358) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$AsyncPrimaryAction.onResponse(TransportReplicationAction.java:298) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$1.onResponse(TransportReplicationAction.java:974) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$1.onResponse(TransportReplicationAction.java:971) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.shard.IndexShardOperationPermits.acquire(IndexShardOperationPermits.java:238) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.shard.IndexShard.acquirePrimaryOperationPermit(IndexShard.java:2211) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction.acquirePrimaryShardReference(TransportReplicationAction.java:983) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction.access$500(TransportReplicationAction.java:97) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$AsyncPrimaryAction.doRun(TransportReplicationAction.java:319) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.common.util.concurrent.AbstractRunnable.run(AbstractRunnable.java:37) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$PrimaryOperationTransportHandler.messageReceived(TransportReplicationAction.java:294) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.action.support.replication.TransportReplicationAction$PrimaryOperationTransportHandler.messageReceived(TransportReplicationAction.java:281) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.transport.RequestHandlerRegistry.processMessageReceived(RequestHandlerRegistry.java:66) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.transport.TransportService$7.doRun(TransportService.java:652) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.common.util.concurrent.ThreadContext$ContextPreservingAbstractRunnable.doRun(ThreadContext.java:637) [elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.common.util.concurrent.AbstractRunnable.run(AbstractRunnable.java:37) [elasticsearch-6.1.0.jar:6.1.0]
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149) [?:1.8.0_141]
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624) [?:1.8.0_141]
	at java.lang.Thread.run(Thread.java:748) [?:1.8.0_141]
Caused by: com.fasterxml.jackson.core.JsonParseException: Numeric value (18446744073709507484) out of range of long (-9223372036854775808 - 9223372036854775807)
 at [Source: org.elasticsearch.common.bytes.BytesReference$MarkSupportingStreamInputWrapper@164537a1; line: 1, column: 164]
	at com.fasterxml.jackson.core.JsonParser._constructError(JsonParser.java:1702) ~[jackson-core-2.8.10.jar:2.8.10]
	at com.fasterxml.jackson.core.base.ParserMinimalBase._reportError(ParserMinimalBase.java:558) ~[jackson-core-2.8.10.jar:2.8.10]
	at com.fasterxml.jackson.core.base.ParserBase.reportOverflowLong(ParserBase.java:1071) ~[jackson-core-2.8.10.jar:2.8.10]
	at com.fasterxml.jackson.core.base.ParserBase.convertNumberToLong(ParserBase.java:962) ~[jackson-core-2.8.10.jar:2.8.10]
	at com.fasterxml.jackson.core.base.ParserBase.getLongValue(ParserBase.java:711) ~[jackson-core-2.8.10.jar:2.8.10]
	at org.elasticsearch.common.xcontent.json.JsonXContentParser.doLongValue(JsonXContentParser.java:167) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.common.xcontent.support.AbstractXContentParser.longValue(AbstractXContentParser.java:190) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.NumberFieldMapper$NumberType$7.parse(NumberFieldMapper.java:679) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.NumberFieldMapper$NumberType$7.parse(NumberFieldMapper.java:655) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.NumberFieldMapper.parseCreateField(NumberFieldMapper.java:996) ~[elasticsearch-6.1.0.jar:6.1.0]
	at org.elasticsearch.index.mapper.FieldMapper.parse(FieldMapper.java:297) ~[elasticsearch-6.1.0.jar:6.1.0]
	... 38 more
	
The agent had numerous errors which looked like this:

`{"level":"error","ts":1516354175.6653244,"caller":"tchannel/reporter.go:131","msg":"Could not submit jaeger batch","error":"tchannel error ErrCodeTimeout: timeout","stacktrace":"github.com/jaegertracing/jaeger/cmd/agent/app/reporter/tchannel.(*Reporter).submitAndReport\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/agent/app/reporter/tchannel/reporter.go:131\ngithub.com/jaegertracing/jaeger/cmd/agent/app/reporter/tchannel.(*Reporter).EmitBatch\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/agent/app/reporter/tchannel/reporter.go:121\ngithub.com/jaegertracing/jaeger/thrift-gen/agent.(*agentProcessorEmitBatch).Process\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/thrift-gen/agent/agent.go:196\ngithub.com/jaegertracing/jaeger/thrift-gen/agent.(*AgentProcessor).Process\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/thrift-gen/agent/agent.go:151\ngithub.com/jaegertracing/jaeger/cmd/agent/app/processors.(*ThriftProcessor).processBuffer\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/agent/app/processors/thrift_processor.go:110"}`




