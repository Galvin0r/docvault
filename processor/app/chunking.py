import re
from typing import Iterator


def normalize_text(text: str) -> str:
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    normalized = re.sub(r"[ \t]+", " ", normalized)
    normalized = re.sub(r"\n{3,}", "\n\n", normalized)
    return normalized.strip()


def iter_text_chunks(text: str, chunk_size_chars: int, chunk_overlap_chars: int) -> Iterator[str]:
    if chunk_size_chars <= 0:
        raise ValueError("chunk_size_chars must be positive")
    if chunk_overlap_chars < 0 or chunk_overlap_chars >= chunk_size_chars:
        raise ValueError("chunk_overlap_chars must be between 0 and chunk_size_chars - 1")

    normalized = normalize_text(text)
    if not normalized:
        return

    start = 0
    text_length = len(normalized)
    while start < text_length:
        max_end = min(start + chunk_size_chars, text_length)
        end = max_end

        if max_end < text_length:
            split_at = normalized.rfind(" ", start, max_end)
            if split_at > start + (chunk_size_chars // 2):
                end = split_at

        chunk = normalized[start:end].strip()
        if chunk:
            yield chunk

        if end >= text_length:
            break

        start = max(end - chunk_overlap_chars, start + 1)
