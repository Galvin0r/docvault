import pytest

@pytest.fixture
def mock_settings(monkeypatch):
    monkeypatch.setenv("DOCVAULT_PROCESSOR_CHUNK_SIZE_CHARS", "100")
    monkeypatch.setenv("DOCVAULT_PROCESSOR_CHUNK_OVERLAP_CHARS", "20")
    monkeypatch.setenv("DOCVAULT_PROCESSOR_EMBEDDING_BATCH_SIZE", "2")
    monkeypatch.setenv("DOCVAULT_PROCESSOR_REQUEST_TIMEOUT_SECONDS", "5")
    from app.config import get_settings
    get_settings.cache_clear()
