#!/usr/bin/env bash

# ====================================================
# =        HMPPS Legal Status data retriever         =
# ====================================================

set -euo pipefail

for cmd in kubectl jq base64 grep; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "$cmd is required but not installed." >&2
    exit 1
  fi
done

b64_no_wrap() {
  if base64 --help 2>&1 | grep -q -- ' -w '; then
    base64 -w 0
  else
    base64 -b 0
  fi
}


echo "===================================================="
echo "=        HMPPS Legal Status data retriever         ="
echo "===================================================="

echo 
echo "Select the environment you want to retrieve data from"
echo "1) dev"
echo "2) preprod"
echo "3) prod"
echo
read -r -p "Enter choice [1-3]: " choice
case "$choice" in
  1)
    NAMESPACE="hmpps-legal-status-dev"
	API_HOST_SUFFIX="-dev"
    ;;
  2)
    NAMESPACE="hmpps-legal-status-preprod"
	API_HOST_SUFFIX="-preprod"
    ;;
  3)
    NAMESPACE="hmpps-legal-status-prod"
	API_HOST_SUFFIX=""
    ;;
  *)
    echo "Invalid selection. Exiting." >&2
    exit 1
    ;;
esac

echo 
echo "Select the API you want to retrieve data from"
echo "1) prisoner-search-api        -  get prisoner by PRN"
echo "2) manage-offences-api        -  get offence severity by offence code"
echo "3) remand-and-sentencing-api  -  get charge data by PRN and fromDate"
echo "4) remand-and-sentencing-api  -  get sentence details by sentence type ID"
echo
read -r -p "Enter choice [1-4]: " choice
case "$choice" in
  1)
    read -r -p "Enter the PRN of the prisoner whose data you want to retrieve: " PRISON_NUMBER
    read -r -p "Enter the response fields (comma delimited): " -i "prisonerNumber,receptionDate,imprisonmentStatus" -e RESPONSE_FIELDS
    API_URL="https://prisoner-search$API_HOST_SUFFIX.prison.service.justice.gov.uk/prisoner/$PRISON_NUMBER?responseFields=$RESPONSE_FIELDS"
	API="PRISONER_SEARCH_API"
    ;;
  2)
    read -r -p "Enter the offence codes (comma delimited): " -i "CL67011,RR84216,RT88136" -e OFFENCE_CODES
    API_URL="https://manage-offences-api$API_HOST_SUFFIX.hmpps.service.justice.gov.uk/offences/severity-ratings?offenceCodes=$OFFENCE_CODES"
	API="MANAGE_OFFENCES_API"
    ;;
  3)
    read -r -p "Enter the PRN of the prisoner whose data you want to retrieve: " PRISON_NUMBER
    read -r -p "Enter the fromDate (no value will not filter by date): " -i "2026-01-20" -e FROM_DATE
    API_URL="https://remand-and-sentencing-api$API_HOST_SUFFIX.hmpps.service.justice.gov.uk/person/$PRISON_NUMBER/charges?fromDate=$FROM_DATE"
	API="REMAND_AND_SENTENCING_API"
    ;;
  4)
    read -r -p "Enter the sentence type ID (a UUID): " SENTENCE_TYPE_ID
    API_URL="https://remand-and-sentencing-api$API_HOST_SUFFIX.hmpps.service.justice.gov.uk/sentence-type/$SENTENCE_TYPE_ID/details"
	API="REMAND_AND_SENTENCING_API"
    ;;
  *)
    echo "Invalid selection. Exiting." >&2
    exit 1
    ;;
esac



DEPLOYMENT="hmpps-legal-status-api"

echo "Fetching secret data from Kubernetes..."

HMPPS_AUTH_URL=$(kubectl -n $NAMESPACE get deployment $DEPLOYMENT -o json | jq -r '.spec.template.spec.containers[0].env[] | select(.name == "HMPPS_AUTH_URL") | .value')
CLIENT_CREDS_SECRET_NAME="hmpps-legal-status-api-client-creds"

AUTH_STRING=$(kubectl -n $NAMESPACE get secrets $CLIENT_CREDS_SECRET_NAME -o json | jq -r '.data | map_values(@base64d) | "\(.'"$API"'_CLIENT_ID):\(.'"$API"'_CLIENT_SECRET)"')
CLIENT_AUTH=$(printf %s "$AUTH_STRING" | b64_no_wrap)
# echo $AUTH_STRING
# echo $CLIENT_AUTH

echo "Finding service pod..."
POD=$(
  kubectl -n "$NAMESPACE" get pods \
    -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' \
    | grep "service-pod" \
    | head -n 1
)
echo "Using pod: $POD"

echo "Requesting token from HMPPS Auth via pod..."
AUTH_RESPONSE=$(
  kubectl -n "$NAMESPACE" exec "$POD" -- \
    curl -sS -X POST "$HMPPS_AUTH_URL/oauth/token?grant_type=client_credentials" \
      -H "Authorization: Basic $CLIENT_AUTH"
)
AUTH_TOKEN=$(printf '%s' "$AUTH_RESPONSE" | jq -r '.access_token // empty')
# echo $AUTH_TOKEN

echo "Requesting data from API via pod..."
API_RESPONSE=$(
  kubectl -n $NAMESPACE exec $POD -- \
    curl -s $API_URL \
      -H "Accept: application/json" \
      -H "Authorization: Bearer $AUTH_TOKEN"
)
echo $API_RESPONSE | jq .

