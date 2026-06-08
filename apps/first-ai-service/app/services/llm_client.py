from typing import Any, Generator

import httpx
import logging
import os

logger = logging.getLogger(__name__)

# Shared connection pool - reused across all calls to avoid repeated TCP/TLS handshakes
_shared_client: httpx.Client | None = None


def _get_client() -> httpx.Client:
    global _shared_client
    if _shared_client is None:
        _shared_client = httpx.Client(
            timeout=httpx.Timeout(300.0, connect=10.0),
            limits=httpx.Limits(max_keepalive_connections=10, max_connections=20),
            verify=False,  # Skip SSL verification for compatibility with various API providers
        )
    return _shared_client


_PROXY_URL = os.getenv("AI_SERVICE_HTTPS_PROXY", "http://host.docker.internal:8899")
_proxied_client: httpx.Client | None = None


def _get_proxied_client() -> httpx.Client:
    global _proxied_client
    if _proxied_client is None:
        _proxied_client = httpx.Client(
            timeout=httpx.Timeout(300.0, connect=10.0),
            limits=httpx.Limits(max_keepalive_connections=10, max_connections=20),
            verify=False,
            proxy=_PROXY_URL,
        )
    return _proxied_client


def _needs_proxy(base_url: str) -> bool:
    """Check if this host needs to be accessed via the Windows-side proxy."""
    return "dashscope.aliyuncs.com" in base_url


from langchain_openai import ChatOpenAI


def get_chat_model(
    *,
    base_url: str,
    api_key: str,
    model: str,
    temperature: float = 0.7,
    max_tokens: int = 4000,
    timeout: float = 120.0,
) -> ChatOpenAI:
    url = base_url.rstrip("/")
    if not url.endswith("/v1"):
        url += "/v1"
    return ChatOpenAI(
        model=model,
        openai_api_key=api_key,
        openai_api_base=url,
        temperature=temperature,
        max_tokens=max_tokens,
        request_timeout=timeout,
    )


def invoke_chat(
    *,
    base_url: str,
    api_key: str,
    model: str,
    messages: list[dict[str, Any]],
    temperature: float = 0.7,
    max_tokens: int = 4000,
    timeout: float = 120.0,
) -> dict[str, Any] | None:
    """Synchronous chat completion using LangChain ChatOpenAI."""
    llm = get_chat_model(
        base_url=base_url,
        api_key=api_key,
        model=model,
        temperature=temperature,
        max_tokens=max_tokens,
        timeout=timeout,
    )
    try:
        from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

        lc_messages = []
        for m in messages:
            role = m.get("role", "user")
            content = m.get("content", "")
            if role == "system":
                lc_messages.append(SystemMessage(content=content))
            elif role == "assistant":
                lc_messages.append(AIMessage(content=content))
            else:
                lc_messages.append(HumanMessage(content=content))

        result = llm.invoke(lc_messages)
        return {
            "choices": [{
                "message": {
                    "role": "assistant",
                    "content": result.content,
                },
                "finish_reason": "stop",
            }],
            "usage": {
                "prompt_tokens": result.usage_metadata.get("input_tokens", 0) if result.usage_metadata else 0,
                "completion_tokens": result.usage_metadata.get("output_tokens", 0) if result.usage_metadata else 0,
                "total_tokens": result.usage_metadata.get("total_tokens", 0) if result.usage_metadata else 0,
            },
        }
    except Exception:
        return _chat_completion_raw(
            base_url=base_url,
            api_key=api_key,
            model=model,
            messages=messages,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=timeout,
        )


def _chat_completion_raw(
    *,
    base_url: str,
    api_key: str,
    model: str,
    messages: list[dict[str, Any]],
    temperature: float = 0.7,
    max_tokens: int = 4000,
    timeout: float = 120.0,
) -> dict[str, Any] | None:
    url = base_url.rstrip("/")
    if not url.endswith("/v1"):
        url += "/v1"
    url += "/chat/completions"
    payload: dict[str, Any] = {
        "model": model,
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens,
        "stream": False,
    }
    headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
    client = _get_client()
    resp = client.post(url, json=payload, headers=headers, timeout=timeout)
    resp.raise_for_status()
    return resp.json()


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
    url = base_url.rstrip("/")
    if not url.endswith("/v1"):
        url += "/v1"
    url += "/chat/completions"
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
    client = _get_client()
    with client.stream("POST", url, json=payload, headers=headers, timeout=timeout) as resp:
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
    url = base_url.rstrip("/")
    if not url.endswith("/v1"):
        url += "/v1"
    url += "/embeddings"
    payload = {"model": model, "input": input_text}
    headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
    use_proxy = _needs_proxy(base_url)
    client = _get_proxied_client() if use_proxy else _get_client()
    logger.info("embeddings: url=%s model=%s api_key=%s... proxy=%s input_count=%s",
        url, model, api_key[:8], use_proxy, len(input_text) if isinstance(input_text, list) else 1)
    resp = client.post(url, json=payload, headers=headers, timeout=timeout)
    logger.info("embeddings: status=%s", resp.status_code)
    resp.raise_for_status()
    data = resp.json()
    items = data.get("data") or []
    vectors: list[list[float]] = []
    for item in sorted(items, key=lambda x: x.get("index", 0)):
        emb = item.get("embedding")
        if isinstance(emb, list):
            vectors.append([float(v) for v in emb])
    return vectors