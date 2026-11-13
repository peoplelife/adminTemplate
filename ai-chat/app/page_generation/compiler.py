from typing import Any

from app.page_generation.schemas import PagePreview, PageSpec


def _value_for_field(field, row_number: int) -> Any:
    if field.kind in {"select", "radio"}:
        return field.options[(row_number - 1) % len(field.options)].value
    if field.kind == "number":
        return row_number * 10
    return f"{field.label} {row_number}"


def compile_preview(spec: PageSpec) -> PagePreview:
    columns = [
        {
            "key": field.key, "label": field.label, "type": field.kind,
            "options": [option.model_dump(mode="json") for option in field.options],
            "column": field.column.model_dump(mode="json", by_alias=True),
            "attrs": field.attrs.model_dump(mode="json", by_alias=True),
            "defaultValue": field.default_value,
        }
        for field in spec.fields
    ]
    filters = [
        column.copy()
        for field, column in zip(spec.fields, columns)
        if field.searchable
    ]
    form = [
        {**column, "required": field.required}
        for field, column in zip(spec.fields, columns)
    ]
    rows = [
        {"id": str(index), **{field.key: row.get(field.key, field.default_value) for field in spec.fields}}
        for index, row in enumerate(spec.sample_data, 1)
    ] if spec.sample_data is not None else [
        {"id": str(row_number), **{field.key: _value_for_field(field, row_number) for field in spec.fields}}
        for row_number in range(1, 4)
    ]
    return PagePreview(
        columns=columns,
        filters=filters,
        form=form,
        actions=spec.actions,
        form_columns=spec.form_columns,
        rows=rows,
        total=len(rows),
    )
