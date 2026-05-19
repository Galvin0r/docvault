from collections.abc import Callable, Iterator
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True, slots=True)
class ExtractedTextUnit:
    content: str
    page_number: int | None = None


ExtractorFunction = Callable[[Path], Iterator[str | ExtractedTextUnit]]


@dataclass(frozen=True, slots=True)
class ExtractorDefinition:
    mime_types: tuple[str, ...]
    suffix: str
    iter_text_units: ExtractorFunction
