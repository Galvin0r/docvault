from collections.abc import Iterable
import re
from typing import Iterator

_SENTENCE_BOUNDARY_RE = re.compile(r'[.!?]["\')\]]*(?=\s|$)')


def normalize_text(text: str) -> str:
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    normalized = re.sub(r"[ \t]+", " ", normalized)
    normalized = re.sub(r"\n{3,}", "\n\n", normalized)
    return normalized.strip()


def iter_text_units(text: str) -> Iterator[str]:
    normalized = normalize_text(text)
    if not normalized:
        return

    for paragraph in normalized.split("\n\n"):
        stripped_paragraph = paragraph.strip()
        if not stripped_paragraph:
            continue

        sentence_start = 0
        for match in _SENTENCE_BOUNDARY_RE.finditer(stripped_paragraph):
            sentence = stripped_paragraph[sentence_start:match.end()].strip()
            if sentence:
                yield sentence
            sentence_start = match.end()

        tail = stripped_paragraph[sentence_start:].strip()
        if tail:
            yield tail


def iter_text_chunks(text: str, chunk_size_chars: int, chunk_overlap_chars: int) -> Iterator[str]:
    yield from iter_text_chunks_from_units(
        iter_text_units(text),
        chunk_size_chars,
        chunk_overlap_chars,
    )


def iter_text_chunks_from_units(
        units: Iterable[str],
        chunk_size_chars: int,
        chunk_overlap_chars: int,
        min_chunk_size_chars: int = 0,
) -> Iterator[str]:
    if chunk_size_chars <= 0:
        raise ValueError("chunk_size_chars must be positive")
    if chunk_overlap_chars < 0 or chunk_overlap_chars >= chunk_size_chars:
        raise ValueError("chunk_overlap_chars must be between 0 and chunk_size_chars - 1")
    if min_chunk_size_chars < 0:
        raise ValueError("min_chunk_size_chars must be non-negative")

    yield from _merge_small_chunks(
        _iter_raw_chunks_from_units(units, chunk_size_chars, chunk_overlap_chars),
        chunk_size_chars,
        min_chunk_size_chars,
    )


def _iter_raw_chunks_from_units(
        units: Iterable[str],
        chunk_size_chars: int,
        chunk_overlap_chars: int,
) -> Iterator[str]:
    current_units: list[str] = []
    current_length = 0

    for raw_unit in units:
        for unit in iter_text_units(raw_unit):
            if len(unit) > chunk_size_chars:
                if current_units:
                    yield " ".join(current_units)
                    current_units = []
                    current_length = 0

                yield from _split_long_unit(unit, chunk_size_chars, chunk_overlap_chars)
                continue

            candidate_length = _candidate_length(current_length, len(unit), bool(current_units))
            if current_units and candidate_length > chunk_size_chars:
                yield " ".join(current_units)
                current_units = _overlap_tail(current_units, chunk_overlap_chars)
                current_length = _joined_length(current_units)

                while current_units and _candidate_length(current_length, len(unit), True) > chunk_size_chars:
                    current_units.pop(0)
                    current_length = _joined_length(current_units)

            if not current_units:
                current_units = [unit]
                current_length = len(unit)
                continue

            current_units.append(unit)
            current_length = _candidate_length(current_length, len(unit), True)

    if current_units:
        yield " ".join(current_units)


def _merge_small_chunks(
        chunks: Iterable[str],
        chunk_size_chars: int,
        min_chunk_size_chars: int,
) -> Iterator[str]:
    pending: str | None = None

    for chunk in chunks:
        if pending is None:
            pending = chunk
            continue

        if len(pending) < min_chunk_size_chars:
            pending = _merge_chunk_pair(pending, chunk, chunk_size_chars)
            continue

        yield pending
        pending = chunk

    if pending:
        yield pending


def _merge_chunk_pair(first: str, second: str, chunk_size_chars: int) -> str:
    merged = f"{first} {second}".strip()
    if len(merged) <= chunk_size_chars:
        return merged
    return merged


def _candidate_length(current_length: int, unit_length: int, has_units: bool) -> int:
    return current_length + unit_length + (1 if has_units else 0)


def _joined_length(units: list[str]) -> int:
    if not units:
        return 0
    return sum(len(unit) for unit in units) + len(units) - 1


def _overlap_tail(units: list[str], chunk_overlap_chars: int) -> list[str]:
    if chunk_overlap_chars == 0 or not units:
        return []

    overlap_units: list[str] = []
    overlap_length = 0

    for unit in reversed(units):
        overlap_units.append(unit)
        overlap_length += len(unit) + (1 if len(overlap_units) > 1 else 0)
        if overlap_length >= chunk_overlap_chars:
            break

    overlap_units.reverse()
    return overlap_units


def _split_long_unit(unit: str, chunk_size_chars: int, chunk_overlap_chars: int) -> Iterator[str]:
    start = 0
    text_length = len(unit)

    while start < text_length:
        max_end = min(start + chunk_size_chars, text_length)
        end = _find_split_end(unit, start, max_end)
        chunk = unit[start:end].strip()
        if chunk:
            yield chunk

        if end >= text_length:
            break

        raw_start = max(end - chunk_overlap_chars, start + 1)
        start = _adjust_next_start(unit, raw_start, start + 1)


def _find_split_end(text: str, start: int, max_end: int) -> int:
    if max_end >= len(text):
        return len(text)

    for index in range(max_end - 1, start, -1):
        if text[index].isspace():
            return index

    return max_end


def _adjust_next_start(text: str, start: int, minimum_start: int) -> int:
    start = max(start, minimum_start)
    if start >= len(text):
        return len(text)

    if not _is_mid_word_boundary(text, start):
        return _skip_whitespace(text, start)

    candidate = start
    while candidate > minimum_start and _is_word_char(text[candidate - 1]):
        candidate -= 1
    if candidate > minimum_start and not _is_word_char(text[candidate - 1]):
        return candidate

    candidate = start
    while candidate < len(text) and _is_word_char(text[candidate]):
        candidate += 1
    return _skip_whitespace(text, candidate)


def _skip_whitespace(text: str, start: int) -> int:
    while start < len(text) and text[start].isspace():
        start += 1
    return start


def _is_mid_word_boundary(text: str, index: int) -> bool:
    return (
        0 < index < len(text)
        and _is_word_char(text[index - 1])
        and _is_word_char(text[index])
    )


def _is_word_char(char: str) -> bool:
    return char.isalnum()