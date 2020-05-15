#!/usr/bin/env sh
# Use this script to run oc commands to create resources in the selected namespace. Files are ordered
# in proper order. 'oc process' processes the template as resources which is again piped to
# 'oc apply' to create those resources in OpenShift namespace
oc process -f ../ocp/registry/scc-config.yml | oc apply -f -
oc process -f ../ocp/registry/application-configmap.yml | oc apply -f -
oc process -f ../ocp/registry/jhipster-registry.yml | oc apply -f -
oc process -f ../ocp/monitoring/jhipster-monitoring.yml | oc apply -f -
oc process -f ../ocp/gateway/gateway-mysql.yml | oc apply -f -
oc process -f ../ocp/gateway/gateway-deployment.yml | oc apply -f -
oc process -f ../ocp/invoice/invoice-mysql.yml | oc apply -f -
oc process -f ../ocp/invoice/invoice-deployment.yml | oc apply -f -
oc process -f ../ocp/review/review-mysql.yml | oc apply -f -
oc process -f ../ocp/review/review-deployment.yml | oc apply -f -
