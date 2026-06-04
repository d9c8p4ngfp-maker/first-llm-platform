from app.models.profile import ProfileResult, ProfileSynthesizeRequest, ProfileSynthesizeResponse
from app.prompts.synthesis import synthesis_prompt
from app.services.llm_client import invoke_chat


def _format_memories(req: ProfileSynthesizeRequest) -> str:
    lines = []
    for m in req.memories[:50]:
        lines.append(f"- [{m.category}] (importance={m.importance}) {m.content}")
    return "\n".join(lines) if lines else "No memories"


def _parse_profile(raw: str, current) -> dict:
    """Parse profile using LangChain JsonOutputParser with fallback."""
    try:
        from langchain_core.output_parsers import JsonOutputParser
        parser = JsonOutputParser()
        return parser.invoke(raw) or {}
    except Exception:
        import json
        import re
        trimmed = (raw or "").strip()
        fence = re.compile(r"```(?:json)?\s*([\s\S]*?)\s*```", re.IGNORECASE)
        m = fence.search(trimmed)
        if m:
            trimmed = m.group(1).strip()
        start = trimmed.find("{")
        end = trimmed.rfind("}")
        if start >= 0 and end > start:
            trimmed = trimmed[start : end + 1]
        try:
            return json.loads(trimmed)
        except json.JSONDecodeError:
            return {}


def synthesize_profile(req: ProfileSynthesizeRequest) -> ProfileSynthesizeResponse:
    system_prompt = req.config.prompt_override or synthesis_prompt()
    current = req.current_profile
    user_content = (
        "## Current profile\n"
        f"summary: {current.ai_summary or ''}\n"
        f"tags: {current.ai_tags}\n"
        f"personality: {current.ai_personality}\n\n"
        "## Memories\n"
        + _format_memories(req)
    )
    params = req.config.model_params or {}
    temperature = float(params.get("temperature", 0.3))
    max_tokens = int(params.get("max_tokens", 3000))
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

    parsed = _parse_profile(content, current)
    profile = ProfileResult(
        ai_summary=str(parsed.get("ai_summary") or current.ai_summary or ""),
        ai_tags=parsed.get("ai_tags") or current.ai_tags or [],
        ai_personality=parsed.get("ai_personality") or current.ai_personality,
        ai_system_prompt=str(parsed.get("ai_system_prompt") or ""),
    )
    return ProfileSynthesizeResponse(profile=profile, usage=usage)
