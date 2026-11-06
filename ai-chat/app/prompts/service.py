from app.llm.schemas import LLMMessage
from app.prompts.base import PromptScenario, PromptTemplate
from app.prompts.registry import prompt_registry
from app.prompts.renderer import render_prompt


class PromptService:
    def get_prompt(
        self,
        prompt_id: str,
        version: str = "v1",
    ) -> PromptTemplate:
        return prompt_registry.get(prompt_id=prompt_id, version=version)

    def list_prompts(self) -> list[PromptTemplate]:
        return prompt_registry.list()

    def render_by_prompt_id(
        self,
        prompt_id: str,
        variables: dict,
        version: str = "v1",
    ) -> list[LLMMessage]:
        prompt = prompt_registry.get(
            prompt_id=prompt_id,
            version=version,
        )

        return render_prompt(
            prompt=prompt,
            variables=variables,
        )

    def render_by_scenario(
        self,
        scenario: PromptScenario,
        variables: dict,
        version: str = "v1",
    ) -> list[LLMMessage]:
        prompt = prompt_registry.find_by_scenario(
            scenario=scenario,
            version=version,
        )

        return render_prompt(
            prompt=prompt,
            variables=variables,
        )


prompt_service = PromptService()