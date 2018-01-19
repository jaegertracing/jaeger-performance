pipeline {
    agent any
    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }
    parameters {
        choice(choices: 'COLLECTOR\nAGENT', description: 'Write spans to the agent or the collector', name: 'USE_AGENT_OR_COLLECTOR')
        choice(choices: 'elasticSearch\ncassandra', description: 'Span Storage', name: 'SPAN_STORAGE_TYPE')
        string(name: 'JAEGER_AGENT_HOST', defaultValue: 'localhost', description: 'Host where the agent is running')
        string(name: 'JAEGER_COLLECTOR_HOST', defaultValue: 'jaeger-collector.${PROJECT_NAME}.svc', description: 'Host where the collector is running')   // FIXME
        string(name: 'JAEGER_COLLECTOR_PORT', defaultValue: '14268', description: 'Collector port')
        string(name: 'JAEGER_SAMPLING_RATE', defaultValue: '1.0', description: '0.0 to 1.0 percent of spans to record')
        string(name: 'KEYSPACE_NAME', defaultValue: 'jaeger_v1_dc1', description: 'Name of the Jaeger keyspace in Cassandra')
        string(name: 'ELASTICSEARCH_HOST', defaultValue: 'elasticsearch', description: 'ElasticShift host')
        string(name: 'ELASTICSEARCH_PORT', defaultValue: '9200', description: 'ElasticShift port')
        string(name: 'ES_BULK_SIZE', defaultValue: '10000000', description: '--es.bulk.size')
        string(name: 'ES_BULK_WORKERS', defaultValue: '10', description: '--es.bulk.workers')
        string(name: 'ES_BULK_FLUSH_INTERVAL', defaultValue: '1s', description: '--es.bulk.flush-interval')
        string(name: 'THREAD_COUNT', defaultValue: '100', description: 'The number of client threads to run')
        string(name: 'ITERATIONS', defaultValue: '1000', description: 'The number of iterations each client should execute')
        string(name: 'COLLECTOR_PODS', defaultValue: '1', description: 'The number of pods to deploy for the Jaeger Collector')
        string(name: 'DELAY', defaultValue: '1', description: 'delay in milliseconds between each span creation')
        booleanParam(name: 'DELETE_JAEGER_AT_END', defaultValue: true, description: 'Delete Jaeger instance at end of the test')
        booleanParam(name: 'DELETE_EXAMPLE_AT_END', defaultValue: true, description: 'Delete the target application at end of the test')
    }

    stages {
        stage('Set name and description') {
            steps {
                script {
                    currentBuild.displayName =params.USE_AGENT_OR_COLLECTOR +" " + params.SPAN_STORAGE_TYPE + " " + params.THREAD_COUNT + " " + params.ITERATIONS + " " + params.JAEGER_SAMPLING_RATE
                    currentBuild.description = currentBuild.displayName
                }
            }
        }
        stage('Delete Jaeger') {
            steps {
                sh 'oc delete all,template,daemonset,configmap -l jaeger-infra'
                sh 'env | sort'
            }
        }
        stage('Cleanup, checkout, build') {
            steps {
                deleteDir()
                script {
                        git 'https://github.com/Hawkular-QE/jaeger-standalone-performance-tests.git'
                }
            }
        }
        stage('deploy Cassandra') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'cassandra'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/cassandra.yml --output cassandra.yml
                    oc create --filename cassandra.yml
                '''
            }
        }
        stage('deploy ElasticSearch') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'elasticSearch'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/elasticsearch.yml --output elasticsearch.yml
                    oc create --filename elasticsearch.yml
                '''
            }
        }
        stage('deploy Jaeger') {
            steps {
               /* Before using the template we need to add '--collector.queue-size=${COLLECTOR_QUEUE_SIZE}' to the collector startup,
                  as well as defining the 'COLLECTOR_QUEUE_SIZE' parameter         TODO only add ES options if we're using ElasticSearch?         */
                sh '''
                    export JAEGER_MAX_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))
                    export COLLECTOR_QUEUE_SIZE=$(($ITERATIONS * $THREAD_COUNT))
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/jaeger-production-template.yml --output jaeger-production-template.yml
                    ./updateTemplate.sh
                    oc process -pCOLLECTOR_QUEUE_SIZE="${COLLECTOR_QUEUE_SIZE}" -pCOLLECTOR_PODS=${COLLECTOR_PODS} -pES_BULK_SIZE=${ES_BULK_SIZE} -pES_BULK_WORKERS=${ES_BULK_WORKERS} -pES_BULK_FLUSH_INTERVAL=${ES_BULK_FLUSH_INTERVAL} -f jaeger-production-template.yml  | oc create -n ${PROJECT_NAME} -f -
                '''
               openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-query', verbose: 'false'
               openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-collector', verbose: 'false'
            }
        }
        stage('Run tests'){
            steps{
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    sh 'git status'
                    sh 'mvn clean test'
                }
            }
        }
        stage('Delete Jaeger at end') {
            when {
                expression { params.DELETE_JAEGER_AT_END  }
            }
            steps {
                script {
                    sh 'oc delete all,template,daemonset,configmap -l jaeger-infra'
                }
            }
        }
        stage('Cleanup pods') {
            steps {
                script {
                    sh 'oc get pods | grep Completed | awk {"print \\$1"} | xargs oc delete pod || true'
                }
            }
        }
    }
}