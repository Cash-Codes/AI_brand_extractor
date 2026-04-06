#!/usr/bin/env bash
# deploy-cloudrun.sh
# One-shot deploy to Google Cloud Run.
#
# Prerequisites:
#   gcloud auth login && gcloud auth configure-docker
#   Set the variables below or export them before running.
#
# Usage:
#   ./deploy-cloudrun.sh

set -euo pipefail

# ── configuration ────────────────────────────────────────────────────
PROJECT_ID="${GCP_PROJECT_ID:?Set GCP_PROJECT_ID}"
REGION="${GCP_REGION:-us-central1}"
SERVICE="${CLOUD_RUN_SERVICE:-brand-extractor}"
IMAGE="gcr.io/${PROJECT_ID}/${SERVICE}"

VERTEXAI_PROJECT_ID="${VERTEXAI_PROJECT_ID:-}"
VERTEXAI_ENABLED="${VERTEXAI_ENABLED:-false}"

# ── build ────────────────────────────────────────────────────────────
echo "▶ Building image: ${IMAGE}"
docker build --platform linux/amd64 -t "${IMAGE}" .

echo "▶ Pushing to GCR"
docker push "${IMAGE}"

# ── deploy ───────────────────────────────────────────────────────────
echo "▶ Deploying to Cloud Run (${SERVICE} / ${REGION})"

ENV_VARS="SPRING_PROFILES_ACTIVE=prod"
ENV_VARS+=",VERTEXAI_ENABLED=${VERTEXAI_ENABLED}"
if [[ -n "${VERTEXAI_PROJECT_ID}" ]]; then
  ENV_VARS+=",VERTEXAI_PROJECT_ID=${VERTEXAI_PROJECT_ID}"
fi

gcloud run deploy "${SERVICE}" \
  --image           "${IMAGE}" \
  --region          "${REGION}" \
  --platform        managed \
  --allow-unauthenticated \
  --port            8080 \
  --memory          512Mi \
  --cpu             1 \
  --min-instances   0 \
  --max-instances   10 \
  --timeout         60 \
  --set-env-vars    "${ENV_VARS}" \
  --project         "${PROJECT_ID}"

URL=$(gcloud run services describe "${SERVICE}" \
  --region "${REGION}" --project "${PROJECT_ID}" \
  --format "value(status.url)")

echo ""
echo "✓ Deployed: ${URL}"
echo "  UI →  ${URL}/"
echo "  API → ${URL}/api/v1/extractions/url"
echo "  Docs → ${URL}/swagger-ui.html"
