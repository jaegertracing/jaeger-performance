#
# Copyright 2018-2019 The Jaeger Authors
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
# in compliance with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License
# is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
# or implied. See the License for the specific language governing permissions and limitations under
# the License.
#



# Import elasticsearch ca, key, cert files to java keystore
# if needed
if [ ${ELASTICSEARCH_PROVIDER} == 'es-operator' ]; then
    export RANDFILE=/tmp/.rnd
    openssl pkcs12 -export -in /certs/cert -inkey /certs/key -chain -CAfile /certs/ca -name "elasticsearch" -out /tmp/es.pkcs12 -passout pass:es
    keytool -importkeystore -deststorepass changeit -destkeystore /tmp/es.keystore -srckeystore /tmp/es.pkcs12 -srcstoretype pkcs12 -srcstorepass es
fi

# TODO: this fix not working with elasticsearch query
#java \
#    -Djavax.net.debug=all \
#    -Djavax.net.ssl.trustStore=/tmp/es.keystore \
#    -Djavax.net.ssl.trustStorePassword=changeit \
#    -jar /app/performance-tests-jar-with-dependencies.jar

java -jar /app/performance-tests-jar-with-dependencies.jar