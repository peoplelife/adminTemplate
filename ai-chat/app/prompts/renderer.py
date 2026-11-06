import re
from string import Formatter

from app.llm.schemas import LLMMessage, LLMRole
from app.prompts.base import PromptTemplate


class PromptRenderError(Exception):
    pass


def extract_variables(template: str) -> set[str]:
    variables = set()

    for _, field_name, _, _ in Formatter().parse(template):
        if field_name:
            variables.add(field_name)

    return variables


def validate_prompt_variables(
    prompt: PromptTemplate,
    variables: dict,
):
    missing = []

    for name in prompt.required_variables:
        if name not in variables or variables[name] is None:
            missing.append(name)

    if missing:
        raise PromptRenderError(
            f"Prompt 缺少必填变量：{', '.join(missing)}"
        )


def render_prompt(
    prompt: PromptTemplate,
    variables: dict,
) -> list[LLMMessage]:
    validate_prompt_variables(prompt, variables)

    rendered_messages: list[LLMMessage] = []

    for message_template in prompt.messages:
        try:
            content = message_template.template.format(**variables)
        except KeyError as exc:
            raise PromptRenderError(
                f"Prompt 模板变量未提供：{exc}"
            ) from exc

        rendered_messages.append(
            LLMMessage(
                role=LLMRole(message_template.role.value),
                content=content.strip(),
            )
        )

    return rendered_messages


def mask_prompt_for_log(content: str, max_length: int = 120) -> str:
    content = re.sub(r"\s+", " ", content).strip()

    if len(content) <= max_length:
        return content

    return content[:max_length] + "..."