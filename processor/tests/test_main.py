from unittest.mock import patch

from app.main import embed_query
from app.schemas import EmbedRequest


def test_embed_returns_query_embedding():
    with patch("app.main.embed_texts") as mock_embed:
        mock_embed.return_value = [[0.1, 0.2, 0.3]]

        response = embed_query(EmbedRequest(text="semantic query"))

    assert response.embedding == [0.1, 0.2, 0.3]
    mock_embed.assert_called_once_with(["semantic query"])
