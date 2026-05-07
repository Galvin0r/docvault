from app.extractors import get_extractor, suffix_for_mime_type


def test_get_extractor_returns_registered_extractor():
    extractor = get_extractor("application/pdf")

    assert extractor is not None
    assert extractor.suffix == ".pdf"


def test_suffix_for_mime_type_uses_normalized_value():
    assert suffix_for_mime_type("text/plain; charset=utf-8") == ".txt"


def test_get_extractor_returns_none_for_unknown_mime_type():
    assert get_extractor("application/unknown") is None