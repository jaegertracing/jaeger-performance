pipeline {
    agent any
    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }
    environment {
        JAEGER_AGENT_HOST = "localhost"
        JAEGER_COLLECTOR_HOST = "jaeger-collector"
        JAEGER_COLLECTOR_PORT = 14268
        ELASTICSEARCH_HOST = "elasticsearch"
        ELASTICSEARCH_PORT = "9200"
        CASSANDRA_CLUSTER_IP = "cassandra"
        CASSANDRA_KEYSPACE_NAME="jaeger_v1_dc1"
        DEPLOYMENT_PARAMETERS="-pIMAGE_VERSION=latest -pCOLLECTOR_QUEUE_SIZE=${COLLECTOR_QUEUE_SIZE} -pCOLLECTOR_PODS=${COLLECTOR_PODS}"
    }
    parameters {
        choice(choices: 'COLLECTOR\nAGENT', name: 'USE_AGENT_OR_COLLECTOR')
        choice(choices: 'elasticsearch\ncassandra',  name: 'SPAN_STORAGE_TYPE')
        string(name: 'ES_BULK_SIZE', defaultValue: '10000000', description: '--es.bulk.size')
        string(name: 'ES_BULK_WORKERS', defaultValue: '10', description: '--es.bulk.workers')
        string(name: 'ES_BULK_FLUSH_INTERVAL', defaultValue: '1s', description: '--es.bulk.flush-interval')
        string(name: 'THREAD_COUNT', defaultValue: '100', description: 'The number of client threads to run')
        string(name: 'ITERATIONS', defaultValue: '30000', description: 'The number of iterations each client should execute')
        string(name: 'DELAY', defaultValue: '1', description: 'delay in milliseconds between each span creation')
        string(name: 'COLLECTOR_PODS', defaultValue: '1')
        string(name: 'COLLECTOR_QUEUE_SIZE', defaultValue: '3000000')

        booleanParam(name: 'DELETE_JAEGER_AT_END', defaultValue: true, description: 'Delete Jaeger instance at end of the test')
        booleanParam(name: 'DELETE_EXAMPLE_AT_END', defaultValue: true, description: 'Delete the target application at end of the test')
        string(name: 'JAEGER_SAMPLING_RATE', defaultValue: '1.0', description: '0.0 to 1.0 percent of spans to record')
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
                checkout scm
                sh 'ls -alF'
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
                expression { params.SPAN_STORAGE_TYPE == 'elasticsearch'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/elasticsearch.yml --output elasticsearch.yml
                    oc create --filename elasticsearch.yml
                '''
            }
        }
        stage('deploy Jaeger with Cassandra') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'cassandra'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/jaeger-production-template.yml --output jaeger-production-template.yml
                    ./updateTemplateForCassandra.sh
                    oc process  ${DEPLOYMENT_PARAMETERS} -f jaeger-production-template.yml  | oc create -n ${PROJECT_NAME} -f -
                '''
            }
        }
        stage('deploy Jaeger with ElasticSearch') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'elasticsearch'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/jaeger-production-template.yml --output jaeger-production-template.yml
                    ./updateTemplateForElasticSearch.sh
                    oc process ${DEPLOYMENT_PARAMETERS} -pES_BULK_SIZE=${ES_BULK_SIZE} -pES_BULK_WORKERS=${ES_BULK_WORKERS} -pES_BULK_FLUSH_INTERVAL=${ES_BULK_FLUSH_INTERVAL} -f jaeger-production-template.yml  | oc create -n ${PROJECT_NAME} -f -
                '''
            }
        }
        stage('Wait for Jaeger Deployment') {
            steps {
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-query', verbose: 'false'
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-collector', verbose: 'false'
            }
        }
        stage('Run tests'){
            steps{
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    sh 'git status'
                    sh 'mvn clean -Dtest=SimpleTest#createTracesTest test'
                }
                 script {
                    env.TRACE_COUNT=readFile 'traceCount.txt'
                    currentBuild.description = currentBuild.description + " Trace count " + env.TRACE_COUNT
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