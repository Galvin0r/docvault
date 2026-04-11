from collections.abc import Callable, Iterator
from dataclasses import dataclass
from pathlib import Path

ExtractorFunction = Callable[[Path], Iterator[str]]


@dataclass(frozen=True, slots=True)
class ExtractorDefinition:
    mime_types: tuple[str, ...]
    suffix: str
    iter_text_units: ExtractorFunction
