export const GITHUB_OAUTH_FLOW_KEY = 'admin-template-github-oauth';

export type GitHubOAuthFlow = {
	state: string;
	mode: 'login' | 'bind';
	userId?: number;
};

export const startGitHubOAuth = (
	authorization: { authorizationUrl?: string; state?: string },
	flow: Omit<GitHubOAuthFlow, 'state'>,
	storage: Storage = sessionStorage,
	navigate: (url: string) => void = (url) => window.location.assign(url)
) => {
	const url = new URL(authorization.authorizationUrl);
	if (url.origin !== 'https://github.com' || url.pathname !== '/login/oauth/authorize' || !authorization.state) {
		throw new Error('GitHub 授权信息无效，请重试');
	}
	const context: GitHubOAuthFlow = { state: authorization.state, mode: flow.mode };
	if (flow.mode === 'bind' && flow.userId !== undefined) context.userId = flow.userId;
	storage.setItem(GITHUB_OAUTH_FLOW_KEY, JSON.stringify(context));
	navigate(authorization.authorizationUrl);
};

export const consumeGitHubOAuth = (storage: Storage = sessionStorage): GitHubOAuthFlow | null => {
	const saved = storage.getItem(GITHUB_OAUTH_FLOW_KEY);
	storage.removeItem(GITHUB_OAUTH_FLOW_KEY);
	if (!saved) return null;
	try {
		const flow = JSON.parse(saved);
		if (
			!flow ||
			typeof flow.state !== 'string' ||
			!flow.state.trim() ||
			!['login', 'bind'].includes(flow.mode) ||
			(flow.userId !== undefined && (!Number.isSafeInteger(flow.userId) || flow.userId <= 0))
		) {
			return null;
		}
		return { state: flow.state, mode: flow.mode, ...(flow.userId !== undefined ? { userId: flow.userId } : {}) };
	} catch {
		return null;
	}
};
