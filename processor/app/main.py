import logging
from itertools import chain
from urllib.parse import urlparse

from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
import requests

from .config import get_settings
from .embedding import embed_texts
from .extraction import EmptyDocumentError, UnsupportedMimeTypeError
from .schemas import EmbedRequest, EmbedResponse, HealthResponse, ProcessRequest
from .service import stream_processed_fragments_from_document

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
        fragment_stream = stream_processed_fragments_from_document(signed_url, request.mimeType)
        first_fragment = next(fragment_stream)
    except UnsupportedMimeTypeError as exc:
        logger.warning("Unsupported mime type: %s", request.mimeType)
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except EmptyDocumentError as exc:
        logger.warning("Empty document: %s", exc)
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except StopIteration as exc:
        logger.warning("Empty document stream produced no fragments")
        raise HTTPException(status_code=422, detail="Document did not contain any extractable text") from exc
    except requests.HTTPError as exc:
        logger.error("Failed to download source document: %s", exc)
        detail = f"Failed to download source document: {exc}"
        raise HTTPException(status_code=502, detail=detail) from exc
    except requests.RequestException as exc:
        logger.error("Could not fetch source document: %s", exc)
        detail = f"Processor could not fetch the source document: {exc}"
        raise HTTPException(status_code=502, detail=detail) from exc

    logger.info("Starting fragment streaming without loading the full extracted document into memory")

    return StreamingResponse(
        chain([first_fragment], fragment_stream),
        media_type="application/x-ndjson",
    )


@app.post("/embed", response_model=EmbedResponse)
def embed_query(request: EmbedRequest) -> EmbedResponse:
    return EmbedResponse(embedding=embed_texts([request.text])[0])