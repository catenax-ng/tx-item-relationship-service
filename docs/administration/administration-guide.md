Administration Guide
====================

Table of Contents

-   [System Overview](#system-overview)
-   [Installation](#installation)
    -   [Deployment using Helm](#deployment-using-helm)
    -   [Deployment using ArgoCD](#deployment-using-argocd)
-   [Configuration](#configuration)
    -   [Helm configuration IRS (values.yaml)](#helm-configuration-irs-values-yaml)
    -   [EDC consumer configuration](#edc-consumer-configuration)
    -   [Secrets](#secrets)
-   [Troubleshooting](#troubleshooting)

System Overview
---------------

The deployment contains the components required to connect the IRS to an existing Catena-X network. This includes:

-   IRS with Minio - part of the "irs-helm" Helm chart

-   EDC Consumer (controlplane & dataplane) - part of the "irs-edc-consumer" Helm chart

Everything else needs to be provided externally.

<img src="integrated-overview.svg" width="852" height="248" alt="integrated overview" />

Installation
------------

The IRS Helm repository can be found here:
<a href="https://eclipse-tractusx.github.io/item-relationship-service/index.yaml" class="bare">https://eclipse-tractusx.github.io/item-relationship-service/index.yaml</a>

Use the latest release of the "irs-helm" chart.
It contains all required dependencies.

If you also want to set up your own EDC consumer, use the "irs-edc-consumer" chart.

Supply the required configuration properties (see chapter [Configuration](#configuration)) in a values.yaml file or override the settings directly.

### Deployment using Helm

Add the IRS Helm repository:

    $ helm repo add irs https://eclipse-tractusx.github.io/item-relationship-service

Then install the Helm chart into your cluster:

    $ helm install -f your-values.yaml irs-app irs/irs-helm

### Deployment using ArgoCD

Create a new Helm chart and use the IRS as a dependency.

    dependencies:
      - name: irs-helm
        repository: https://eclipse-tractusx.github.io/item-relationship-service
        version: 3.x.x
      - name: irs-edc-consumer # optional
        repository: https://eclipse-tractusx.github.io/item-relationship-service
        version: 1.x.x

Then provide your configuration as the values.yaml of that chart.

Create a new application in ArgoCD and point it to your repository / Helm chart folder.

Configuration
-------------

Take the following template and adjust the configuration parameters (&lt;placeholders&gt; mark the relevant spots).
You can define the URLs as well as most of the secrets yourself.

The Keycloak, DAPS and Vault configuration / secrets depend on your setup and might need to be provided externally.

### Helm configuration IRS (values.yaml)

    #####################
    # IRS Configuration #
    #####################
    irsUrl: "https://<irs-url>"
    ingress:
      enabled: false
      className: "nginx"
      annotations:
        nginx.ingress.kubernetes.io/ssl-passthrough: "false"
        nginx.ingress.kubernetes.io/backend-protocol: "HTTP"
        nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
      hosts:
        - host: "<irs-url>"
          paths:
            - path: /
              pathType: ImplementationSpecific
      tls:
        - hosts:
            - "<irs-url>"
          secretName: tls-secret
    digitalTwinRegistry:
      url: https://<digital-twin-registry-url>
    semanticsHub:
      url: https://<semantics-hub-url>
      defaultUrns: >
        urn:bamm:io.catenax.serial_part_typization:1.0.0#SerialPartTypization
    #    ,urn:bamm:com.catenax.assembly_part_relationship:1.0.0#AssemblyPartRelationship
    bpdm:
      url: https://<bpdm-url>
    minioUser: <minio-username>
    minioPassword: <minio-password>
    minioUrl: "http://{{ .Release.Name }}-minio:9000"
    keycloak:
      oauth2:
        clientId: <keycloak-client-id>
        clientSecret: <keycloak-client-secret>
        clientTokenUri: <keycloak-token-uri>
        jwkSetUri: <keycloak-jwkset-uri>
    edc:
      controlplane:
        endpoint:
          data: "" #<edc-controlplane-endpoint-data>
        request:
          ttl: PT10M # Requests to controlplane will time out after this duration (see https://en.wikipedia.org/wiki/ISO_8601#Durations)
        provider:
          suffix: /api/v1/ids/data
        catalog:
          limit: 1000 # Max number of catalog items to retrieve from the controlplane
        apikey:
          header: "X-Api-Key" # Name of the EDC api key header field
          secret: "" #<edc-api-key>
      submodel:
        request:
          ttl: PT10M # Requests to dataplane will time out after this duration (see https://en.wikipedia.org/wiki/ISO_8601#Durations)
        path: /submodel
        urnprefix: /urn


    #######################
    # Minio Configuration #
    #######################
    minio:
      enabled: true
      mode: standalone
      resources:
        requests:
          memory: 4Gi
      rootUser: <minio-username>
      rootPassword: <minio-password>

      environment:
        MINIO_PROMETHEUS_JOB_ID: minio-actuator
        MINIO_PROMETHEUS_URL: http://prometheus:9090

    #########################
    # Grafana Configuration #
    #########################
    grafana:
      enabled: false (1)
      rbac:
        create: false
      persistence:
        enabled: false

      user: <grafana-username>
      password: <grafana-password>

      admin:
        existingSecret: "{{ .Release.Name }}-irs-helm"
        userKey: grafanaUser
        passwordKey: grafanaPassword

      datasources:
        datasources.yaml:
          apiVersion: 1
          datasources:
            - name: Prometheus
              type: prometheus
              url: "http://{{ .Release.Name }}-prometheus-server"
              isDefault: true
      sidecar:
        dashboards:
          enabled: true

      importDashboards:
        minio: dashboards/minio-dashboard.json
        outbound: dashboards/irs-outbound-requests.json
        irsmonitoring: dashboards/resource-monitoring-dashboard.json
        irsjobs: dashboards/irs-jobs-dashboard.json
        irsapi: dashboards/irs-api-dashboard.json

    ############################
    # Prometheus Configuration #
    ############################
    prometheus:
      enabled: false (1)
      rbac:
        create: false
      alertmanager:
        enabled: false
      prometheus-node-exporter:
        enabled: false
      kubeStateMetrics:
        enabled: false
      prometheus-pushgateway:
        enabled: false
      configmapReload:
        prometheus:
          enabled: false

      extraScrapeConfigs: |
        - job_name: 'spring-actuator'
          metrics_path: '/actuator/prometheus'
          scrape_interval: 5s
          static_configs:
            - targets: [ '{{ .Release.Name }}-irs-helm:4004' ]

        - job_name: 'minio-actuator'
          metrics_path: /minio/v2/metrics/cluster
          static_configs:
            - targets: [ '{{ .Release.Name }}-minio:9000' ]

<table>
<tbody>
<tr class="odd">
<td><em></em><strong>1</strong></td>
<td>Use this to enable or disable the monitoring components</td>
</tr>
</tbody>
</table>

#### Values explained

##### &lt;irs-url&gt;

The hostname where the IRS will be made available.

##### &lt;digital-twin-registry-url&gt;

The URL of the Digital Twin Registry. The IRS uses this service to fetch AAS shells.

##### &lt;semantics-hub-url&gt;

The URL of the SemanticsHub. The IRS uses this service to fetch aspect schemas for payload validation.

##### &lt;bpdm-url&gt;

The URL of the BPDM service. The IRS uses this service to fetch business partner information based on BPNs.

##### &lt;keycloak-token-uri&gt;

The URL of the Keycloak token API. Used by the IRS for token creation to authenticate with other services.

##### &lt;keycloak-jwkset-uri&gt;

The URL of the Keycloak JWK Set. Used by the IRS to validate tokens when the IRS API is called.

##### &lt;grafana-url&gt;

The hostname where Grafana will be made available.

##### &lt;edc-controlplane-endpoint-data&gt;

The EDC consumer controlplane endpoint URL for data management, including the protocol.
If left empty, this defaults to the internal endpoint of the controlplane provided by the irs-edc-consumer Helm chart.

### EDC consumer configuration

If you want to provide your own EDC consumer, add the following entries to your values.yaml:

    ##############################
    # EDC Postgres Configuration #
    ##############################
    postgresql:
      auth:
        username: edc
        database: edc
        postgresPassword: <postgres-admin-password>
        password: <postgres-password>

    ##################################
    # EDC Controlplane Configuration #
    ##################################
    edc-controlplane:
      ingresses:
        - enabled: true
          hostname: "<controlplane-url>"
          annotations:
            nginx.ingress.kubernetes.io/ssl-passthrough: "false"
            nginx.ingress.kubernetes.io/backend-protocol: "HTTP"
            nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
          endpoints:
            - ids
            - data
          className: ""
          tls:
            - hosts:
                - "<controlplane-url>"
              secretName: tls-secret
          certManager:
            issuer: ""
            clusterIssuer: ""

      edc:
        receiver:
          callback:
            url: "http://{{ .Release.Name }}-irs-helm:8181/internal/endpoint-data-reference" # IRS EDC callback URL, e.g. http://app-irs-helm:8181/internal/endpoint-data-reference
        postgresql:
          user: edc
          password: <postgres-password>
        transfer:
          proxy:
            token:
              verifier:
                publickey:
                  alias: <daps-certificate-name>
              signer:
                privatekey:
                  alias: <daps-privatekey-name>
        api:
          auth:
            key: "<edc-api-key>"
        controlplane:
          url: "https://<controlplane-url>"
        dataplane:
          url: "https://<dataplane-url>"
      configuration:
        properties: |-
          edc.oauth.client.id=<daps-client-id>
          edc.oauth.private.key.alias=<daps-privatekey-name>
          edc.oauth.provider.jwks.url=<daps-jwks-url>
          edc.oauth.public.key.alias=<daps-certificate-name>
          edc.oauth.token.url=<daps-token-url>
          edc.vault.hashicorp.url=<vault-url>
          edc.vault.hashicorp.token=<vault-token>
          edc.vault.hashicorp.api.secret.path=<vault-secret-store-path>
          edc.data.encryption.keys.alias=<daps-privatekey-name>
          edc.data.encryption.algorithm=NONE

    ###############################
    # EDC Dataplane Configuration #
    ###############################
    edc-dataplane:
      edc:
        api:
          auth:
            key: "<edc-api-key>"
      ## Ingress declaration to expose the network service.
      ingresses:
        - enabled: true
          hostname: "<dataplane-url>"
          annotations:
            nginx.ingress.kubernetes.io/ssl-passthrough: "false"
            nginx.ingress.kubernetes.io/backend-protocol: "HTTP"
            nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
          endpoints:
            - public
          className: "nginx"
          tls:
            - hosts:
                - "<dataplane-url>"
              secretName: tls-secret
          certManager:
            issuer: ""
            clusterIssuer: ""

      configuration:
        properties: |-
          edc.oauth.client.id=<daps-client-id>
          edc.oauth.private.key.alias=<daps-privatekey-name>
          edc.oauth.provider.audience=idsc:IDS_CONNECTORS_ALL
          edc.oauth.provider.jwks.url=<daps-jwks-url>
          edc.oauth.public.key.alias=<daps-certificate-name>
          edc.oauth.token.url=<daps-token-url>
          edc.vault.hashicorp.url=<vault-url>
          edc.vault.hashicorp.token=<vault-token>
          edc.vault.hashicorp.api.secret.path=<vault-secret-store-path>

#### Values explained

EDC requires a DAPS instance to function correctly. For more information on this, please refer to the [DAPS](https://github.com/catenax-ng/product-DAPS) or the [EDC](https://github.com/catenax-ng/product-edc) documentation.

##### &lt;controlplane-url&gt;

The hostname where the EDC consumer controlplane will be made available.

##### &lt;dataplane-url&gt;

The hostname where the EDC consumer dataplane will be made available.

##### &lt;vault-url&gt;

The base URL of the Vault instance.
EDC requires a running instance of HashiCorp Vault to store the DAPS certificate and private key.

##### &lt;vault-secret-store-path&gt;

The path to the secret store in Vault where the DAPS certificate and key can be found.

*Example: /v1/team-name*

##### &lt;daps-certificate-name&gt;

The name of the DAPS certificate in the Vault.

*Example: irs-daps-certificate*

##### &lt;daps-privatekey-name&gt;

The name of the DAPS private key in the Vault.

*Example: irs-daps-private-key*

##### &lt;daps-client-id&gt;

The DAPS client ID.

##### &lt;daps-jwks-url&gt;

The URL of the DAPS JWK Set.

*Example: <a href="https://daps-hostname/.well-known/jwks.json" class="bare">https://daps-hostname/.well-known/jwks.json</a>*

##### &lt;daps-token-url&gt;

The URL of the DAPS token API.

*Example: <a href="https://daps-hostname/token" class="bare">https://daps-hostname/token</a>*

### Secrets

This is a list of all secrets used in the deployment.

<table>
<tbody>
<tr class="odd">
<td><em></em></td>
<td>Keep the values for these settings safe and do not publish them!</td>
</tr>
</tbody>
</table>

#### &lt;postgres-admin-password&gt;

Database password for the **postgres** user. To be defined by you.

#### &lt;postgres-password&gt;

Database password for the application user (default username: **edc**). To be defined by you.

#### &lt;keycloak-client-id&gt;

Client ID for Keycloak. Request this from your Keycloak operator.

#### &lt;keycloak-client-secret&gt;

Client secret for Keycloak. Request this from your Keycloak operator.

#### &lt;minio-username&gt;

Login username for Minio. To be defined by you.

#### &lt;minio-password&gt;

Login password for Minio. To be defined by you.

#### &lt;edc-api-key&gt;

An API key for the EDC API. To be defined by you.

#### &lt;vault-token&gt;

The access token for the HashiCorp Vault API.

#### &lt;grafana-username&gt;

Login username for Grafana. To be defined by you.

#### &lt;grafana-password&gt;

Login password for Grafana. To be defined by you.

Troubleshooting
---------------

Coming soon…​

Last updated 2022-12-21 13:41:46 UTC
