from typing import Any

from app.models.chat import ChatRequest
from app.services.llm_client import chat_completion_stream, invoke_chat


def _memory_block(memories: list) -> str:
    if not memories:
        return ""
    lines = [f"- [{m.category or 'FACT'}] {m.content}" for m in memories[:30]]
    return "--- Relevant memories ---\n" + "\n".join(lines)


def _rag_block(chunks: list) -> str:
    if not chunks:
        return ""
    parts: list[str] = []
    for c in chunks[:10]:
        src = c.source or "knowledge"
        parts.append(f"[{src}] {c.content}")
    return "--- Reference materials ---\n" + "\n\n".join(parts)


def _profile_block(profile) -> str:
    if profile is None:
        return ""
    block = ""
    if profile.ai_system_prompt:
        block += profile.ai_system_prompt
    if profile.ai_tags:
        block += "\nTags: " + ", ".join(profile.ai_tags)
    if not block.strip():
        return ""
    return "--- User persona ---\n" + block.strip()


def assemble_messages(req: ChatRequest) -> list[dict[str, Any]]:
    extra = "\n\n".join(
        x
        for x in [
            _profile_block(req.user_profile),
            _rag_block(req.rag_context),
            _memory_block(req.user_memories),
        ]
        if x
    )
    messages: list[dict[str, Any]] = [m.model_dump() for m in req.messages]
    if not extra:
        return messages
    injected = False
    for msg in messages:
        if msg.get("role") == "system":
            msg["content"] = (msg.get("content") or "") + "\n\n" + extra
            injected = True
            break
    if not injected:
        messages.insert(0, {"role": "system", "content": extra})
    return messages


def run_chat(req: ChatRequest):
    messages = assemble_messages(req)
    model = req.model or req.upstream.model
    params = req.model_params
    if req.stream:
        return chat_completion_stream(
            base_url=req.upstream.base_url,
            api_key=req.upstream.api_key,
            model=model,
            messages=messages,
            temperature=params.temperature,
            max_tokens=params.max_tokens,
            tools=req.tools,
        )
    data = invoke_chat(
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
        model=model,
        messages=messages,
        temperature=params.temperature,
        max_tokens=params.max_tokens,
    )
    return data
