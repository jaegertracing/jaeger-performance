# December 14 2017 Test Results of testing with ElasticSearch
The results below are for tests run on December 14 and 15 2017

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
`nohup go run ./cmd/collector/main.go --collector.queue-size=${COLLECTOR_QUEUE_SIZE} --span-storage.type=elasticsearch > log.txt`

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
 + The expected number of traces for all test runs is 3000000
 
## Results with Standalone ElasticSearch, Jaeger Collector run from source
|Run Number | Write Time | Read Time |  Traces Found |
| ------------- | -----:|-----:|-----:|
| 1 | 33.720 | 2940.951 | 2966258 | 
| 2 | 33.424 | 2880.824 | 2960835 | 
| 3 | 33.595 | 2881.544 | 2963357 | 

## Results with Standalone ElasticSearch, Jaeger Collector and Agent run from source
|Run Number | Write Time | Read Time | Traces Found |
| ------------- | -----:|-----:|-----:|
| 1 | 34.725 | 2910.935 | 2914632 | 
| 2 | 34.496 | 2852.706 | 2886423 | 
| 3 | 34.552 | 2882.13 | 2905762 | 

## Errors
+ On most runs I got multiple pairs of these two errors on the collector
`{"level":"error","ts":1513261802.224524,"caller":"spanstore/writer.go:190","msg":"Failed to create index","trace_id":"8cdd2d72571f3f47","span_id":"8cdd2d72571f3f47","error":"elastic: Error 400 (Bad Request): index [jaeger-span-2017-12-14/pd08v6mSSwqU4yFC7NGVSQ] already exists [type=resource_already_exists_exception]","stacktrace":"github.com/jaegertracing/jaeger/plugin/storage/es/spanstore.(*SpanWriter).logError\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/plugin/storage/es/spanstore/writer.go:190\ngithub.com/jaegertracing/jaeger/plugin/storage/es/spanstore.(*SpanWriter).createIndex\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/plugin/storage/es/spanstore/writer.go:148\ngithub.com/jaegertracing/jaeger/plugin/storage/es/spanstore.(*SpanWriter).WriteSpan\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/plugin/storage/es/spanstore/writer.go:126\ngithub.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).saveSpan\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:102\ngithub.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).(github.com/jaegertracing/jaeger/cmd/collector/app.saveSpan)-fm\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:89\ngithub.com/jaegertracing/jaeger/cmd/collector/app.ChainedProcessSpan.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/model_consumer.go:32\ngithub.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).processItemFromQueue\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:126\ngithub.com/jaegertracing/jaeger/cmd/collector/app.NewSpanProcessor.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:57\ngithub.com/jaegertracing/jaeger/pkg/queue.(*BoundedQueue).StartConsumers.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/pkg/queue/bounded_queue.go:65"}
 {"level":"error","ts":1513261802.227579,"caller":"app/span_processor.go:103","msg":"Failed to save span","error":"Failed to create index: elastic: Error 400 (Bad Request): index [jaeger-span-2017-12-14/pd08v6mSSwqU4yFC7NGVSQ] already exists [type=resource_already_exists_exception]","errorVerbose":"elastic: Error 400 (Bad Request): index [jaeger-span-2017-12-14/pd08v6mSSwqU4yFC7NGVSQ] already exists [type=resource_already_exists_exception]\nFailed to create index\ngithub.com/jaegertracing/jaeger/plugin/storage/es/spanstore.(*SpanWriter).logError\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/plugin/storage/es/spanstore/writer.go:191\ngithub.com/jaegertracing/jaeger/plugin/storage/es/spanstore.(*SpanWriter).createIndex\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/plugin/storage/es/spanstore/writer.go:148\ngithub.com/jaegertracing/jaeger/plugin/storage/es/spanstore.(*SpanWriter).WriteSpan\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/plugin/storage/es/spanstore/writer.go:126\ngithub.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).saveSpan\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:102\ngithub.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).(github.com/jaegertracing/jaeger/cmd/collector/app.saveSpan)-fm\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:89\ngithub.com/jaegertracing/jaeger/cmd/collector/app.ChainedProcessSpan.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/model_consumer.go:32\ngithub.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).processItemFromQueue\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:126\ngithub.com/jaegertracing/jaeger/cmd/collector/app.NewSpanProcessor.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:57\ngithub.com/jaegertracing/jaeger/pkg/queue.(*BoundedQueue).StartConsumers.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/pkg/queue/bounded_queue.go:65\nruntime.goexit\n\t/usr/lib/go-1.8/src/runtime/asm_amd64.s:2197","stacktrace":"github.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).saveSpan\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:103\ngithub.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).(github.com/jaegertracing/jaeger/cmd/collector/app.saveSpan)-fm\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:89\ngithub.com/jaegertracing/jaeger/cmd/collector/app.ChainedProcessSpan.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/model_consumer.go:32\ngithub.com/jaegertracing/jaeger/cmd/collector/app.(*spanProcessor).processItemFromQueue\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:126\ngithub.com/jaegertracing/jaeger/cmd/collector/app.NewSpanProcessor.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/cmd/collector/app/span_processor.go:57\ngithub.com/jaegertracing/jaeger/pkg/queue.(*BoundedQueue).StartConsumers.func1\n\t/home/kearls/go/src/github.com/jaegertracing/jaeger/pkg/queue/bounded_queue.go:65"}
`
