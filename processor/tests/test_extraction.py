import pytest
from pathlib import Path
from app.extraction import extract_text, UnsupportedMimeTypeError, EmptyDocumentError

def test_extract_plain_text_utf8(tmp_path: Path):
    doc_path = tmp_path / "test.txt"
    doc_path.write_text("Hello World", encoding="utf-8")
    
    result = extract_text(doc_path, "text/plain")
    assert result == "Hello World"

def test_extract_plain_text_latin1(tmp_path: Path):
    doc_path = tmp_path / "test.txt"
    doc_path.write_bytes(b"Hello \xe9 World")
    
    result = extract_text(doc_path, "text/plain")
    assert result == "Hello é World"

def test_unsupported_mime():
    with pytest.raises(UnsupportedMimeTypeError):
        extract_text(Path("dummy.xyz"), "application/xyz")

def test_empty_document(tmp_path: Path):
    doc_path = tmp_path / "empty.txt"
    doc_path.write_text("   \n  \t ")
    
    with pytest.raises(EmptyDocumentError):
        extract_text(doc_path, "text/plain")
