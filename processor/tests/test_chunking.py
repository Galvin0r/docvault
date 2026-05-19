from app.chunking import (
    iter_text_chunks,
    iter_text_chunks_from_paged_units,
    iter_text_chunks_from_units,
    normalize_text,
)


def test_normalize_text_collapses_spacing():
    assert normalize_text("A\t\tB\r\n\r\n\r\nC") == "A B\n\nC"


def test_iter_text_chunks_prefers_sentence_boundaries():
    text = (
        "Alpha beta gamma. "
        "Delta epsilon zeta. "
        "Eta theta iota kappa lambda."
    )

    chunks = list(iter_text_chunks(text, chunk_size_chars=30, chunk_overlap_chars=8))

    assert chunks[0] == "Alpha beta gamma."
    assert chunks[1] == "Delta epsilon zeta."
    assert chunks[2] == "Eta theta iota kappa lambda."


def test_iter_text_chunks_overlap_does_not_start_mid_word():
    text = "Alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi."

    chunks = list(iter_text_chunks(text, chunk_size_chars=34, chunk_overlap_chars=12))

    assert len(chunks) >= 2
    assert chunks[0] == "Alpha beta gamma delta epsilon"
    assert chunks[1].startswith("delta")
    assert not chunks[1].startswith("ta")


def test_iter_text_chunks_merges_small_non_final_chunks():
    units = [
        "6.",
        "Alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi omicron pi rho sigma tau.",
        "Final short remainder.",
    ]

    chunks = list(iter_text_chunks_from_units(units, chunk_size_chars=80, chunk_overlap_chars=10, min_chunk_size_chars=20))

    assert chunks[0].startswith("6. Alpha beta gamma")
    assert chunks[-1] == "Final short remainder."


def test_iter_text_chunks_from_paged_units_keeps_starting_page_number():
    chunks = list(iter_text_chunks_from_paged_units(
        [
            ("Introduction", 1),
            (
                "Hallucinations LLM Hallucinations: A Deeper Examination of a Fundamental Limitation. "
                "Large Language Models have quickly become central to text generation and search.",
                4,
            ),
        ],
        chunk_size_chars=220,
        chunk_overlap_chars=20,
        min_chunk_size_chars=40,
    ))

    assert len(chunks) == 1
    assert chunks[0].content.startswith("Introduction Hallucinations")
    assert chunks[0].page_number == 1


def test_iter_text_chunks_from_paged_units_does_not_force_page_boundaries():
    chunks = list(iter_text_chunks_from_paged_units(
        [
            ("Short title", 1),
            ("Body text continues on a later page with enough words to form a useful fragment.", 2),
        ],
        chunk_size_chars=140,
        chunk_overlap_chars=10,
        min_chunk_size_chars=30,
    ))

    assert chunks[0].content.startswith("Short title Body text")
    assert chunks[0].page_number == 1
