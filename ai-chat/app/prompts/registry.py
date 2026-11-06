from app.prompts.base import PromptScenario, PromptTemplate
from app.prompts.rag import RAG_QA_PROMPT_V1
from app.prompts.rag_evaluation import RAG_EVALUATION_PROMPT


class PromptRegistry:
    def __init__(self):
        self._prompts: dict[str, PromptTemplate] = {}

    def register(self, prompt: PromptTemplate):
        key = self._build_key(prompt.prompt_id, prompt.version)
        self._prompts[key] = prompt

    def get(self, prompt_id: str, version: str = "v1") -> PromptTemplate:
        key = self._build_key(prompt_id, version)

        if key not in self._prompts:
            raise KeyError(f"Prompt 不存在：{prompt_id}@{version}")

        return self._prompts[key]

    def list(self) -> list[PromptTemplate]:
        return list(self._prompts.values())

    def find_by_scenario(
        self,
        scenario: PromptScenario,
        version: str = "v1",
    ) -> PromptTemplate:
        for prompt in self._prompts.values():
            if prompt.scenario == scenario and prompt.version == version:
                return prompt

        raise KeyError(f"场景 Prompt 不存在：{scenario}@{version}")

    @staticmethod
    def _build_key(prompt_id: str, version: str) -> str:
        return f"{prompt_id}@{version}"


prompt_registry = PromptRegistry()

prompt_registry.register(RAG_QA_PROMPT_V1)
prompt_registry.register(RAG_EVALUATION_PROMPT)
