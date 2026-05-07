from collections.abc import Iterator
from pathlib import Path

from ..chunking import iter_text_units
from .base import ExtractorDefinition

MIME_TYPES = ("text/plain",)
SUFFIX = ".txt"


def iter_plain_text_units(file_path: Path) -> Iterator[str]:
    try:
        yield from _iter_plain_text_units_with_encoding(file_path, "utf-8")
    except UnicodeDecodeError:
        yield from _iter_plain_text_units_with_encoding(file_path, "latin-1")


def _iter_plain_text_units_with_encoding(file_path: Path, encoding: str) -> Iterator[str]:
    paragraph_lines: list[str] = []

    with file_path.open("r", encoding=encoding) as source:
        for line in source:
            stripped_line = line.strip()
            if stripped_line:
                paragraph_lines.append(stripped_line)
                continue

            if paragraph_lines:
                yield from iter_text_units(" ".join(paragraph_lines))
                paragraph_lines.clear()

    if paragraph_lines:
        yield from iter_text_units(" ".join(paragraph_lines))


EXTRACTOR = ExtractorDefinition(
    mime_types=MIME_TYPES,
    suffix=SUFFIX,
    iter_text_units=iter_plain_text_units,
)