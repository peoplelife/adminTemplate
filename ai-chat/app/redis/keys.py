def chat_response_cache_key(question_hash: str) -> str:
    return f"chat:response:{question_hash}"


def rate_limit_key(user_id: str, api_name: str) -> str:
    return f"rate_limit:{api_name}:{user_id}"


def api_key_cache_key(api_key_hash: str) -> str:
    return f"api_key:{api_key_hash}"


def agent_run_state_key(run_id: str) -> str:
    return f"agent:run:{run_id}:state"


def lock_key(resource: str, resource_id: str) -> str:
    return f"lock:{resource}:{resource_id}"
