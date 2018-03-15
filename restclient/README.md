# Jaeger Performance Testing Rest Client
This module creates a Java rest client which can be used to test the Jaeger REST API.

## Model generation
The data model is generated using the `jsonschema2pojo` maven plugin.  (See http://www.jsonschema2pojo.org/). 
The model is generated from the file `src/main/resources/schema/Traces.json`  The contents of this file are based on 
https://github.com/jaegertracing/jaeger/blob/master/model/json/fixture.json  However, in order to work properly
they need to be wrapped as shown below:

`{ data: [ 
      <contents of fixture.json> 
    ], 
    "total": 0,
   "limit": 0,
   "offset": 0,
   "errors": null
 }`
 
 Note that on account of traces being returned as an array of `data`, the class generated for them is called `Datum`.  Obviously
 `Trace` would be better, but I'm not sure how to make the plugin do this.
 
 
 
