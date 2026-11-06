from typing import Generic, TypeVar

from pydantic import BaseModel, ConfigDict, Field

T = TypeVar("T")


class PageParams(BaseModel):
    page: int = Field(default=1, ge=1, description="当前页码，从 1 开始")
    page_size: int = Field(default=20, ge=1, le=100, description="每页数量")


class Page(BaseModel, Generic[T]):
    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    items: list[T] = Field(description="当前页数据")
    total: int = Field(description="符合查询条件的总条数")
    page: int = Field(description="当前页码")
    page_size: int = Field(alias="pageSize", description="每页数量")
