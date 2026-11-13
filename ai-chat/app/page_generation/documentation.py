"""Search the frontend's source documentation, bundled unchanged in the AI image."""

import re
from pathlib import Path


DOCUMENT_NAMES = ("base-table-usage.md", "form-usage.md")


def _document_directory() -> Path:
    repository_docs = Path(__file__).resolve().parents[3] / "frontend" / "docs"
    return repository_docs if repository_docs.is_dir() else Path(__file__).resolve().parents[2] / "template-docs"


def _sections(content: str):
    heading, lines, fenced = "", [], False
    for line in content.splitlines():
        if line.lstrip().startswith("```"):
            fenced = not fenced
        if not fenced and re.match(r"^#{1,6} ", line):
            if lines:
                yield heading, "\n".join(lines).strip()
            heading = line.lstrip("# ").strip()
            lines = []
        lines.append(line)
    if lines:
        yield heading, "\n".join(lines).strip()


def search_template_docs(query: str) -> list[dict[str, str]]:
    terms = set(re.findall(r"[a-zA-Z][a-zA-Z0-9_]*", query.lower()))
    for phrase in re.findall(r"[\u4e00-\u9fff]+", query):
        terms.update(phrase[index:index + 2] for index in range(len(phrase) - 1))
    aliases = {
        "边框": "border", "斑马": "stripe", "表头": "showheader height", "固定": "fixed height",
        "分页": "pagination pagesize pagesizes", "每页": "pagesize", "标签": "labelposition labelwidth",
        "列宽": "width minwidth", "对齐": "align", "排序": "sortable", "占位": "placeholder",
        "默认值": "defaultvalue", "示例": "sampledata", "数据": "sampledata", "禁用": "disabled",
    }
    for phrase, keywords in aliases.items():
        if phrase in query:
            terms.update(keywords.split())
    results = []
    for name in DOCUMENT_NAMES:
        content = (_document_directory() / name).read_text(encoding="utf-8-sig")
        for title, section in _sections(content):
            score = sum(4 * (term in title.lower()) + min(section.lower().count(term), 3) for term in terms)
            if score:
                results.append((score, {"source": f"frontend/docs/{name}", "title": title, "content": section[:5000]}))
    results.sort(key=lambda item: item[0], reverse=True)
    return [item for _, item in results[:4]]
