#!/bin/bash

BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Updating helm charts${NC}"
helm dependency update

echo -e "${BLUE}Installing helm charts${NC}"
helm install dil-test .

uninstall() {
  echo -e "${BLUE}Uninstalling charts${NC}"
  helm uninstall dil-test
}

trap uninstall EXIT

echo -e "${BLUE}Waiting for deployments to be available...${NC}"
kubectl wait deployment --for condition=Available --timeout=180s dil-test-minio
kubectl wait deployment --for condition=Available --timeout=180s dil-test-edc-provider-dataplane
kubectl wait deployment --for condition=Available --timeout=180s dil-test-edc-consumer-dataplane
kubectl wait deployment --for condition=Available --timeout=180s dil-test-edc-provider2-dataplane
kubectl wait deployment --for condition=Available --timeout=180s dil-test-edc-consumer-controlplane
kubectl wait deployment --for condition=Available --timeout=180s dil-test-edc-provider2-controlplane
kubectl wait deployment --for condition=Available --timeout=180s dil-test-discovery
kubectl wait deployment --for condition=Available --timeout=180s dil-test-irs-helm
kubectl wait deployment --for condition=Available --timeout=180s cx-dil-test-registry
#kubectl wait deployment --for condition=Available --timeout=90s cx-dil-test-hub

echo -e "${BLUE}Port forwarding${NC}"
kubectl port-forward svc/dil-test-edc-provider-controlplane 6081:8081 &
kubectl port-forward svc/dil-test-edc-provider2-controlplane 6091:8081 &
kubectl port-forward svc/dil-test-edc-consumer-controlplane 7081:8081 &
kubectl port-forward svc/dil-test-discovery 8888:8080 &
kubectl port-forward svc/cx-dil-test-registry-svc 10200:8080 &
#kubectl port-forward svc/cx-dil-test-hub-svc 10201:8080 &
kubectl port-forward svc/dil-test-submodelservers 10199:8080 &
kubectl port-forward svc/dil-test-irs-helm 8080:8080
