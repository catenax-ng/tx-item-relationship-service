{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "dataprovider.fullname" -}}
    {{- if .Values.fullnameOverride }}
        {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
    {{- else }}
        {{- $name := default .Chart.Name .Values.nameOverride }}
        {{- if contains $name .Release.Name }}
            {{- .Release.Name | trunc 63 | trimSuffix "-" }}
        {{- else }}
            {{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
        {{- end }}
    {{- end }}
{{- end }}

{{/*
Submodel URL helpers
*/}}
{{- define "submodelservers.host" -}}
    {{- if .Values.submodelservers.ingress.enabled }}
        {{- with (first .Values.submodelservers.ingress.hosts) }}
            {{- printf "https://%s" .host }}
        {{- end }}
    {{- else }}
        {{- print "http://test" }}
    {{- end }}
{{- end }}

{{/*
Registry URL helpers
*/}}
{{- define "registry.host" -}}
    {{- printf "https://%s" (index .Values "digital-twin-registry" "registry" "host") }}
{{- end }}
{{- define "registry.path" -}}
    {{- index .Values "digital-twin-registry" "registry" "ingress" "urlPrefix" }}
{{- end }}
{{- define "registry.url" -}}
    {{- printf "%s%s%s" (include "registry.host" .) (include "registry.path" .) "/api/v3.0" }}
{{- end }}

{{/*
EDC URL helpers
*/}}
{{- define "edc.controlplane.host" -}}
    {{- with (first (index .Values "tractusx-connector" "controlplane" "ingresses")) }}
        {{- printf "https://%s" .hostname }}
    {{- end }}
{{- end }}
{{- define "edc.dataplane.host" -}}
    {{- with (first (index .Values "tractusx-connector" "dataplane" "ingresses")) }}
        {{- printf "https://%s" .hostname }}
    {{- end }}
{{- end }}
{{- define "edc.key" -}}
    {{- index .Values "tractusx-connector" "controlplane" "endpoints" "management" "authKey" }}
{{- end }}
{{- define "edc.bpn" -}}
    {{- index .Values "tractusx-connector" "participant" "id" }}
{{- end }}

