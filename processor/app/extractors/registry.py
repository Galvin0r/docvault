from .base import ExtractorDefinition
from .docx import EXTRACTOR as DOCX_EXTRACTOR
from .epub import EXTRACTOR as EPUB_EXTRACTOR
from .pdf import EXTRACTOR as PDF_EXTRACTOR
from .plain_text import EXTRACTOR as PLAIN_TEXT_EXTRACTOR

EXTRACTORS: tuple[ExtractorDefinition, ...] = (
    PDF_EXTRACTOR,
    DOCX_EXTRACTOR,
    PLAIN_TEXT_EXTRACTOR,
    EPUB_EXTRACTOR,
)

_EXTRACTORS_BY_MIME_TYPE: dict[str, ExtractorDefinition] = {
    mime_type: extractor
    for extractor in EXTRACTORS
    for mime_type in extractor.mime_types
}


def normalize_mime_type(mime_type: str) -> str:
    return mime_type.split(";", 1)[0].strip().lower()


def get_extractor(mime_type: str) -> ExtractorDefinition | None:
    return _EXTRACTORS_BY_MIME_TYPE.get(normalize_mime_type(mime_type))


def suffix_for_mime_type(mime_type: str) -> str:
    extractor = get_extractor(mime_type)
    if extractor is None:
        return ""
    return extractor.suffix