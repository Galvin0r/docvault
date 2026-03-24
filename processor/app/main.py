import logging
from urllib.parse import urlparse

from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
import requests

from .config import get_settings
from .extraction import EmptyDocumentError, UnsupportedMimeTypeError
from .schemas import HealthResponse, ProcessRequest
from .service import extract_document_text, stream_processed_fragments

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
logger = logging.getLogger(__name__)

app = FastAPI(title="DocVault Processor", version="0.1.0")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    settings = get_settings()
    return HealthResponse(status="ok", modelName=settings.model_name)


@app.post("/process")
def process_document(request: ProcessRequest) -> StreamingResponse:
    signed_url = str(request.signedUrl)
    url_host = urlparse(signed_url).hostname or "unknown"
    logger.info("Processing request: mimeType=%s, host=%s", request.mimeType, url_host)

    try:
        text = extract_document_text(signed_url, request.mimeType)
    except UnsupportedMimeTypeError as exc:
        logger.warning("Unsupported mime type: %s", request.mimeType)
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except EmptyDocumentError as exc:
        logger.warning("Empty document: %s", exc)
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except requests.HTTPError as exc:
        logger.error("Failed to download source document: %s", exc)
        detail = f"Failed to download source document: {exc}"
        raise HTTPException(status_code=502, detail=detail) from exc
    except requests.RequestException as exc:
        logger.error("Could not fetch source document: %s", exc)
        detail = f"Processor could not fetch the source document: {exc}"
        raise HTTPException(status_code=502, detail=detail) from exc

    logger.info("Extracted %d characters, starting fragment streaming", len(text))

    return StreamingResponse(
        stream_processed_fragments(text),
        media_type="application/x-ndjson",
    )
