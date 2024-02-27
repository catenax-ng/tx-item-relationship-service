# Dataprovider Helm Chart

This chart includes EDC, Digital Twin Registry and a Submodel Server.  
The Submodel Server images is based on an older Catena-X demo (catenax at home) since this is what we were using in our
testing environments.

## Prerequisites

- Running Kubernetes cluster
- Helm is installed

Example for local usage:

- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [Minikube ingress addon](https://minikube.sigs.k8s.io/docs/handbook/addons/ingress-dns/)

## Installing

Run the Chart with

```shell
helm dependency update
helm install dataprovider . --timeout 10m0s
```

Remove the chart by running

```shell
helm uninstall dataprovider
```
## Testdata seeding

After the installation, a Post-Install Helm Hook will be started which initiates the seeding of testdata. The Hook executes a python script which uploads a provided test dataset to the dataprovider.

Test data set and upload script are stored in [resources/](resources) and provided to the hook as config map.  
A custom config map can be used to provided e.g. `testdataConfigMap: my-custom-testdata-configmap`.