import json
from datetime import date

from app.models.memory import (
    ExistingMemory,
    ExtractedMemory,
    MemoryExtractRequest,
    MemoryExtractResponse,
    MessageTurn,
)
from app.prompts.extraction import default_extraction_prompt
from app.services.llm_client import invoke_chat


def _format_existing(memories: list[ExistingMemory]) -> str:
    if not memories:
        return "No existing memories"
    lines: list[str] = []
    for m in memories:
        line = f"- [{m.category or 'FACT'}]"
        if m.id is not None:
            line += f" id={m.id}"
        line += f" {m.content}"
        lines.append(line)
    return "\n".join(lines)


def _format_conversation(segment: list[MessageTurn]) -> str:
    parts: list[str] = []
    for turn in segment:
        parts.append(f"{turn.role.upper()}: {turn.content}")
    return "\n\n".join(parts)


def _parse_memories(raw: str) -> list[ExtractedMemory]:
    """Parse extraction result using LangChain JsonOutputParser with fallback."""
    try:
        from langchain_core.output_parsers import JsonOutputParser
        parser = JsonOutputParser()
        items = parser.invoke(raw)
    except Exception:
        return _parse_memories_fallback(raw)

    if not isinstance(items, list):
        return []
    result: list[ExtractedMemory] = []
    for item in items:
        if not isinstance(item, dict):
            continue
        content = item.get("content")
        if not content or not str(content).strip():
            continue
        result.append(
            ExtractedMemory(
                category=str(item.get("category") or "FACT"),
                content=str(content).strip(),
                importance=int(item.get("importance") or 3),
                schedule_date=item.get("schedule_date"),
                schedule_time=item.get("schedule_time"),
                numeric_value=item.get("numeric_value"),
                update_ref=item.get("update_ref"),
            )
        )
    return result


def _parse_memories_fallback(raw: str) -> list[ExtractedMemory]:
    """Fallback JSON parsing with markdown fence handling."""
    import re
    trimmed = (raw or "").strip()
    if not trimmed:
        return []
    fence = re.compile(r"```(?:json)?\s*([\s\S]*?)\s*```", re.IGNORECASE)
    m = fence.search(trimmed)
    if m:
        trimmed = m.group(1).strip()
    start = trimmed.find("[")
    end = trimmed.rfind("]")
    if start >= 0 and end > start:
        trimmed = trimmed[start : end + 1]
    try:
        items = json.loads(trimmed)
    except json.JSONDecodeError:
        return []
    if not isinstance(items, list):
        return []
    result: list[ExtractedMemory] = []
    for item in items:
        if not isinstance(item, dict):
            continue
        content = item.get("content")
        if not content or not str(content).strip():
            continue
        result.append(
            ExtractedMemory(
                category=str(item.get("category") or "FACT"),
                content=str(content).strip(),
                importance=int(item.get("importance") or 3),
                schedule_date=item.get("schedule_date"),
                schedule_time=item.get("schedule_time"),
                numeric_value=item.get("numeric_value"),
                update_ref=item.get("update_ref"),
            )
        )
    return result


def extract_memories(req: MemoryExtractRequest) -> MemoryExtractResponse:
    system_prompt = req.config.prompt_override or default_extraction_prompt(date.today())
    user_content = (
        "## Existing memories:\n"
        + _format_existing(req.existing_memories)
        + "\n\n## Conversation:\n"
        + _format_conversation(req.conversation_segment)
    )
    params = req.config.model_params or {}
    temperature = float(params.get("temperature", 0.2))
    max_tokens = int(params.get("max_tokens", 2000))
    model = req.config.model or req.upstream.model

    data = invoke_chat(
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
        model=model,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content},
        ],
        temperature=temperature,
        max_tokens=max_tokens,
    )

    usage = data.get("usage") if data else None
    content = ""
    if data:
        choices = data.get("choices") or []
        if choices:
            msg = choices[0].get("message") or {}
            content = (msg.get("content") or "").strip()
    memories = _parse_memories(content)
    return MemoryExtractResponse(memories=memories, usage=usage)
