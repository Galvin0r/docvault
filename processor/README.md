# Processor Service

Standalone Python document processor for DocVault.

It exposes:
- `GET /health`
- `POST /process`
- `POST /embed`

`POST /process` accepts:

```json
{
  "signedUrl": "https://...",
  "mimeType": "application/pdf"
}
```

The response is streamed as `application/x-ndjson`, one fragment per line:

```json
{"fragmentOrder":0,"pageNumber":1,"content":"...","embedding":[0.1,0.2]}
```

`pageNumber` is included when the source format exposes reliable page boundaries, such as PDF.

`POST /embed` accepts query text and returns one embedding from the same model used for document fragments:

```json
{"text":"semantic search query"}
```

```json
{"embedding":[0.1,0.2]}
```

Run standalone:

```bash
docker build -t processor docvault/processor
docker run --rm -p 8001:8000 processor
```