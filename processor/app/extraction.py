from collections.abc import Iterator
import logging
from pathlib import Path

from .chunking import normalize_text
from .extractors import get_extractor, normalize_mime_type
from .extractors.base import ExtractedTextUnit

logger = logging.getLogger(__name__)


class UnsupportedMimeTypeError(ValueError):
    pass


class EmptyDocumentError(ValueError):
    pass


def iter_extracted_text_units(file_path: Path, mime_type: str) -> Iterator[ExtractedTextUnit]:
    normalized_mime_type = normalize_mime_type(mime_type)
    extractor = get_extractor(normalized_mime_type)
    if extractor is None:
        raise UnsupportedMimeTypeError(f"Unsupported mime type: {mime_type}")

    yielded = 0
    total_characters = 0

    for unit in extractor.iter_text_units(file_path):
        content = unit.content if isinstance(unit, ExtractedTextUnit) else unit
        page_number = unit.page_number if isinstance(unit, ExtractedTextUnit) else None
        normalized_unit = normalize_text(content)
        if not normalized_unit:
            continue
        yielded += 1
        total_characters += len(normalized_unit)
        yield ExtractedTextUnit(content=normalized_unit, page_number=page_number)

    if yielded == 0:
        raise EmptyDocumentError("Document did not contain any extractable text")

    logger.info("Extracted %d characters from %s document", total_characters, normalized_mime_type)
