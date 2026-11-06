import unittest

from app.prompts import rag
from app.prompts.renderer import PromptRenderError
from app.prompts.service import prompt_service


class RagPromptTests(unittest.TestCase):
    def test_prompt_exposes_stable_identity(self):
        self.assertEqual(getattr(rag, "RAG_QA_PROMPT_ID", None), "rag_qa")
        self.assertEqual(getattr(rag, "RAG_QA_PROMPT_VERSION", None), "v1")

    def test_prompt_requires_question_and_retrieved_context(self):
        with self.assertRaises(PromptRenderError):
            prompt_service.render_by_prompt_id(
                prompt_id="rag_qa",
                version="v1",
                variables={"user_question": "question"},
            )

    def test_registered_prompt_renders_evidence_rules_and_numbered_context(self):
        messages = prompt_service.render_by_prompt_id(
            prompt_id="rag_qa",
            version="v1",
            variables={
                "user_question": "Who approved the policy?",
                "retrieved_context": "[1] HR approved the policy.",
            },
        )

        self.assertEqual([message.role.value for message in messages], ["system", "user"])
        self.assertIn("非可信", messages[0].content)
        self.assertIn("指令", messages[0].content)
        self.assertIn("根据当前知识库无法确认", messages[0].content)
        self.assertIn("[1]", messages[0].content)
        self.assertIn("Who approved the policy?", messages[1].content)
        self.assertIn("[1] HR approved the policy.", messages[1].content)


if __name__ == "__main__":
    unittest.main()
