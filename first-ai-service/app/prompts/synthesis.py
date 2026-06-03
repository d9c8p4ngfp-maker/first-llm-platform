def synthesis_prompt() -> str:
    return (
        "You are a user persona synthesis expert. Given memory items, produce an updated user profile.\n"
        "Output strict JSON only with keys: ai_summary, ai_tags (array of hashtag strings), "
        "ai_personality (object with optional mbti, mbti_label, mbti_desc, mbti_confidence, zodiac, zodiac_desc), "
        "ai_system_prompt (second-person instructions for the assistant when chatting with this user).\n"
        "Be concise, factual, in Chinese when memories are Chinese."
    )