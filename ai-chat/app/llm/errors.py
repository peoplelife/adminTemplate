class LLMProviderError(Exception):
    def __init__(
        self,
        message: str,
        provider: str,
        status_code: int | None = None,
        raw_error: str | None = None,
    ):
        self.message = message
        self.provider = provider
        self.status_code = status_code
        self.raw_error = raw_error

        super().__init__(message)


class LLMProviderConfigError(LLMProviderError):
    pass


class LLMProviderTimeoutError(LLMProviderError):
    pass


class LLMProviderResponseParseError(LLMProviderError):
    pass