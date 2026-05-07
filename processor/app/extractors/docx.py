from collections.abc import Iterator
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET

from ..chunking import iter_text_units
from .base import ExtractorDefinition

MIME_TYPES = ("application/vnd.openxmlformats-officedocument.wordprocessingml.document",)
SUFFIX = ".docx"

_DOCX_NAMESPACE = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
_DOCX_PARAGRAPH_TAG = f"{{{_DOCX_NAMESPACE}}}p"
_DOCX_TEXT_TAG = f"{{{_DOCX_NAMESPACE}}}t"
_DOCX_TAB_TAG = f"{{{_DOCX_NAMESPACE}}}tab"
_DOCX_BREAK_TAGS = {
    f"{{{_DOCX_NAMESPACE}}}br",
    f"{{{_DOCX_NAMESPACE}}}cr",
}


def iter_docx_text_units(file_path: Path) -> Iterator[str]:
    with ZipFile(file_path) as archive, archive.open("word/document.xml") as document_xml:
        paragraph_fragments: list[str] = []

        for _, element in ET.iterparse(document_xml, events=("end",)):
            if element.tag == _DOCX_TEXT_TAG:
                paragraph_fragments.append(element.text or "")
            elif element.tag == _DOCX_TAB_TAG:
                paragraph_fragments.append("\t")
            elif element.tag in _DOCX_BREAK_TAGS:
                paragraph_fragments.append("\n")
            elif element.tag == _DOCX_PARAGRAPH_TAG:
                paragraph_text = "".join(paragraph_fragments).strip()
                paragraph_fragments.clear()
                if paragraph_text:
                    yield from iter_text_units(paragraph_text)
            element.clear()


EXTRACTOR = ExtractorDefinition(
    mime_types=MIME_TYPES,
    suffix=SUFFIX,
    iter_text_units=iter_docx_text_units,
)