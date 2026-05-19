import pytest
from pathlib import Path
from app.extraction import iter_extracted_text_units, UnsupportedMimeTypeError, EmptyDocumentError

def test_extract_plain_text_utf8(tmp_path: Path):
    doc_path = tmp_path / "test.txt"
    doc_path.write_text("Hello World", encoding="utf-8")
    
    result = list(iter_extracted_text_units(doc_path, "text/plain"))
    assert [unit.content for unit in result] == ["Hello World"]
    assert [unit.page_number for unit in result] == [None]

def test_extract_plain_text_latin1(tmp_path: Path):
    doc_path = tmp_path / "test.txt"
    doc_path.write_bytes(b"Hello \xe9 World")
    
    result = list(iter_extracted_text_units(doc_path, "text/plain"))
    assert [unit.content for unit in result] == ["Hello é World"]
    assert [unit.page_number for unit in result] == [None]

def test_unsupported_mime():
    with pytest.raises(UnsupportedMimeTypeError):
        list(iter_extracted_text_units(Path("dummy.xyz"), "application/xyz"))

def test_empty_document(tmp_path: Path):
    doc_path = tmp_path / "empty.txt"
    doc_path.write_text("   \n  \t ")
    
    with pytest.raises(EmptyDocumentError):
        list(iter_extracted_text_units(doc_path, "text/plain"))
