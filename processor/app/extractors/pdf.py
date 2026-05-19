from collections.abc import Iterator
from pathlib import Path

from pypdf import PdfReader

from ..chunking import iter_text_units
from .base import ExtractedTextUnit, ExtractorDefinition

MIME_TYPES = ("application/pdf",)
SUFFIX = ".pdf"


def iter_pdf_text_units(file_path: Path) -> Iterator[ExtractedTextUnit]:
    reader = PdfReader(file_path)
    for page_number, page in enumerate(reader.pages, start=1):
        page_text = (page.extract_text() or "").strip()
        if page_text:
            for unit in iter_text_units(page_text):
                yield ExtractedTextUnit(content=unit, page_number=page_number)


EXTRACTOR = ExtractorDefinition(
    mime_types=MIME_TYPES,
    suffix=SUFFIX,
    iter_text_units=iter_pdf_text_units,
)
