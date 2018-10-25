#!/bin/bash
#
# Copyright 2018 The Jaeger Authors
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


# checks mandatory fields and do login

if [ -z "${OS_URL}" ]; then
    echo "Field OS_URL is missing!"
    exit 1
fi

if [ -z "${OS_USERNAME}" ]; then
    echo "Field OS_USERNAME is missing!"
    exit 1
fi

if [ -z "${OS_PASSWORD}" ]; then
    echo "Field OS_PASSWORD is missing!"
    exit 1
fi

if [ -z "${OS_NAMESPACE}" ]; then
    echo "Field OS_NAMESPACE is missing!"
    exit 1
fi

# do oc login for further actions
oc login ${OS_URL} --username=${OS_USERNAME} --password=${OS_PASSWORD} -n ${OS_NAMESPACE} --insecure-skip-tls-verify=true
