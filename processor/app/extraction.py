from pathlib import Path
from io import BytesIO
import logging

from bs4 import BeautifulSoup
from docx import Document as DocxDocument
from ebooklib import ITEM_DOCUMENT, epub
from pypdf import PdfReader

from .chunking import normalize_text

logger = logging.getLogger(__name__)


class UnsupportedMimeTypeError(ValueError):
    pass


class EmptyDocumentError(ValueError):
    pass


PDF_MIME_TYPES = {"application/pdf"}
DOCX_MIME_TYPES = {
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
}
TEXT_MIME_TYPES = {"text/plain"}
EPUB_MIME_TYPES = {"application/epub+zip"}


def extract_text(file_path: Path, mime_type: str) -> str:
    normalized_mime_type = mime_type.split(";", 1)[0].strip().lower()

    if normalized_mime_type in PDF_MIME_TYPES:
        text = _extract_pdf_text(file_path)
    elif normalized_mime_type in DOCX_MIME_TYPES:
        text = _extract_docx_text(file_path)
    elif normalized_mime_type in TEXT_MIME_TYPES:
        text = _extract_plain_text(file_path)
    elif normalized_mime_type in EPUB_MIME_TYPES:
        text = _extract_epub_text(file_path)
    else:
        raise UnsupportedMimeTypeError(f"Unsupported mime type: {mime_type}")

    normalized_text = normalize_text(text)
    if not normalized_text:
        raise EmptyDocumentError("Document did not contain any extractable text")

    logger.info("Extracted %d characters from %s document", len(normalized_text), normalized_mime_type)
    return normalized_text


def _extract_pdf_text(file_path: Path) -> str:
    reader = PdfReader(file_path)
    return "\n\n".join(
        text for page in reader.pages
        if (text := (page.extract_text() or "").strip())
    )


def _extract_docx_text(file_path: Path) -> str:
    document = DocxDocument(str(file_path))
    paragraphs = [paragraph.text.strip() for paragraph in document.paragraphs if paragraph.text.strip()]
    return "\n\n".join(paragraphs)


def _extract_plain_text(file_path: Path) -> str:
    try:
        return file_path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return file_path.read_text(encoding="latin-1")


def _extract_epub_text(file_path: Path) -> str:
    book = epub.read_epub(str(file_path))

    parts: list[str] = []
    for item in book.get_items_of_type(ITEM_DOCUMENT):
        soup = BeautifulSoup(item.get_content(), "html.parser")
        text = soup.get_text("\n", strip=True)
        if text:
            parts.append(text)
    return "\n\n".join(parts)
