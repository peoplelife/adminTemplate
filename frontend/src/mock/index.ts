import Mock from 'mockjs';
Mock.mock('/v1/me', 'get', () => {
	return Mock.mock({
		code: 200,
		success: true,
		message: '请求成功。',
		data: {
			id: 1,
			username: '@cname',
			nickname: '@cname',
			email: '@email',
			phone: /1[3456789]\d{9}/,
			avatar: '',
			roleCodes: ['admin'],
			status: 1,
			lastLogin: Mock.Random.datetime('yyyy-MM-dd HH:mm:ss')
		}
	});
});
