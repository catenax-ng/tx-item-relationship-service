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
    {{- with (first (index .Values "edc-provider" "tractusx-connector" "controlplane" "ingresses")) }}
        {{- printf "https://%s" .hostname }}
    {{- end }}
{{- end }}
{{- define "edc.dataplane.host" -}}
    {{- with (first (index .Values "edc-provider" "tractusx-connector" "dataplane" "ingresses")) }}
        {{- printf "https://%s" .hostname }}
    {{- end }}
{{- end }}
{{- define "edc.key" -}}
    {{- index .Values "edc-provider" "tractusx-connector" "controlplane" "endpoints" "management" "authKey" }}
{{- end }}
{{- define "edc.bpn" -}}
    {{- index .Values "edc-provider" "tractusx-connector" "participant" "id" }}
{{- end }}

