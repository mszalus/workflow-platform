{{- define "chart.fullname" -}}
{{- .Release.Name }}-{{ .Chart.Name }}
{{- end }}
