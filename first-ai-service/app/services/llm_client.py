from typing import Any, Generator

import httpx


def chat_completion(
    *,
    base_url: str,
    api_key: str,
    model: str,
    messages: list[dict[str, Any]],
    temperature: float = 0.7,
    max_tokens: int = 4000,
    tools: list[dict[str, Any]] | None = None,
    timeout: float = 120.0,
) -> tuple[dict[str, Any] | None, dict[str, Any] | None]:
    url = base_url.rstrip("/") + "/v1/chat/completions"
    payload: dict[str, Any] = {
        "model": model,
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens,
        "stream": False,
    }
    if tools:
        payload["tools"] = tools
    headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
    with httpx.Client(timeout=timeout) as client:
        resp = client.post(url, json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
    return data, data.get("usage")


def chat_completion_stream(
    *,
    base_url: str,
    api_key: str,
    model: str,
    messages: list[dict[str, Any]],
    temperature: float = 0.7,
    max_tokens: int = 4000,
    tools: list[dict[str, Any]] | None = None,
    timeout: float = 300.0,
) -> Generator[str, None, None]:
    url = base_url.rstrip("/") + "/v1/chat/completions"
    payload: dict[str, Any] = {
        "model": model,
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens,
        "stream": True,
        "stream_options": {"include_usage": True},
    }
    if tools:
        payload["tools"] = tools
    headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
    with httpx.Client(timeout=timeout) as client:
        with client.stream("POST", url, json=payload, headers=headers) as resp:
            resp.raise_for_status()
            for line in resp.iter_lines():
                if line is None:
                    continue
                text = line.decode("utf-8") if isinstance(line, bytes) else str(line)
                if not text:
                    continue
                yield text + "\n"


def embeddings(
    *,
    base_url: str,
    api_key: str,
    model: str,
    input_text: str | list[str],
    timeout: float = 60.0,
) -> list[list[float]]:
    url = base_url.rstrip("/") + "/v1/embeddings"
    payload = {"model": model, "input": input_text}
    headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
    with httpx.Client(timeout=timeout) as client:
        resp = client.post(url, json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
    items = data.get("data") or []
    vectors: list[list[float]] = []
    for item in sorted(items, key=lambda x: x.get("index", 0)):
        emb = item.get("embedding")
        if isinstance(emb, list):
            vectors.append([float(v) for v in emb])
    return vectors