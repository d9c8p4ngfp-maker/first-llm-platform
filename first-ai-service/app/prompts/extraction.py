from datetime import date


def default_extraction_prompt(today: date | None = None) -> str:
    d = today or date.today()
    return (
        "You are an information extraction expert. Extract structured memories from user conversations.\n"
        f"Today is {d.isoformat()}. Resolve relative dates (today/tomorrow) against this date.\n"
        "Rules:\n"
        "1. Prioritize USER messages; ignore assistant jokes and generic advice\n"
        "2. Extract TODO or SCHEDULE for near-term plans even without exact time "
        "(schedule_date=today, schedule_time=null)\n"
        "3. Extract PREFERENCE for food/taste/habits\n"
        "4. Do not duplicate same date+time+event as existing memories\n"
        "5. Skip pure greetings\n"
        "Output strict JSON array only:\n"
        '[{"category":"FACT|PREFERENCE|EVENT|GOAL|TODO|SCHEDULE","content":"...",'
        '"importance":1-5,"schedule_date":"YYYY-MM-DD or null",'
        '"schedule_time":"HH:mm or null","numeric_value":null,"update_ref":null}]\n'
        "Return [] only when nothing extractable."
    )
