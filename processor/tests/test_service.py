import json
from pathlib import Path
from unittest.mock import patch

from app.service import download_to_file, stream_processed_fragments, stream_processed_fragments_from_document, _suffix_for

def test_download_to_file(tmp_path: Path, mock_settings):
    dest = tmp_path / "download.tmp"
    with patch("app.service.requests.get") as mock_get:
        mock_response = mock_get.return_value.__enter__.return_value
        mock_response.iter_content.return_value = [b"chunk1", b"chunk2"]
        mock_response.raise_for_status.return_value = None
        
        download_to_file("http://example.com/file", dest)
        
        assert dest.read_bytes() == b"chunk1chunk2"

def test_stream_processed_fragments_from_document(mock_settings):
    with patch("app.service.download_to_file") as mock_download, \
         patch("app.service.iter_extracted_text_units") as mock_units, \
         patch("app.service.embed_texts") as mock_embed:
        mock_units.return_value = iter(["Hello world.", "Second chunk."])
        mock_embed.return_value = [[0.1, 0.2]]

        items = list(stream_processed_fragments_from_document("http://dummy", "text/plain"))

        assert len(items) == 1
        first = json.loads(items[0])
        assert first["content"] == "Hello world. Second chunk."

def test_stream_processed_fragments(mock_settings):
    text = "Hello world. This is a test string for fragmentation."
    
    with patch("app.service.embed_texts") as mock_embed:
        mock_embed.return_value = [[0.1, 0.2], [0.3, 0.4]]
        
        with patch("app.service.iter_text_chunks_from_units") as mock_chunk:
            mock_chunk.return_value = ["Hello", "world"]
            items = list(stream_processed_fragments(text))
            
            assert len(items) == 2
            data1 = json.loads(items[0])
            assert data1["content"] == "Hello"
            assert data1["embedding"] == [0.1, 0.2]
            assert data1["fragmentOrder"] == 0

def test_suffix_for():
    assert _suffix_for("application/pdf") == ".pdf"
    assert _suffix_for("APPlication/Pdf ; charset=utf8") == ".pdf"
    assert _suffix_for("text/plain") == ".txt"
    assert _suffix_for("unknown/type") == ""