import { ChangeCase, defineConfig } from 'yapi-to-typescript';
import { promises as fs } from 'fs';
const apiDirPath = 'src/api/modules';
const apiExportPath = 'src/api/index.ts';
const openApiUrl = process.env.OPENAPI_URL || 'http://127.0.0.1:4523/export/openapi/2?version=3.0';
// 用于记录本次生成的文件路径
const generatedFilePaths = new Set<string>();
const getName = (dir: string, name: string, changeCase: ChangeCase) => {
	const dirName = dir
		.split('/')
		.filter(Boolean)
		.map((v) => (v.includes('{') ? 'by ' + v : v))
		.join(' ');
	name = (name || '').includes('{') ? 'by ' + name : name;
	return changeCase.camelCase([dirName, name].join(' '));
};
const getProductConfig = (token?: string) => {
	return {
		token: token || '',
		categories: [
			{
				id: 0
			}
		]
	};
};

// 生成基于接口路径的文件名，同时记录生成的文件路径
function getOutputFilePath(interfaceInfo: any) {
	const dirName = interfaceInfo.path.split('/')[1] || 'root';

	const targetFilePath = `${apiDirPath}/${dirName}.ts`;

	// 记录本次生成的文件路径
	generatedFilePaths.add(targetFilePath);

	return targetFilePath;
}
export default defineConfig(
	[
		{
			serverUrl: openApiUrl,
			typesOnly: false,
			serverType: 'swagger',
			target: 'typescript',
			reactHooks: {
				enabled: false
			},
			prodEnvName: 'production',
			outputFilePath: (interfaceInfo) => getOutputFilePath(interfaceInfo),
			requestFunctionFilePath: 'src/utils/request.ts',
			dataKey: 'data',
			comment: {
				requestHeader: true
			},
			getResponseDataTypeName(interfaceInfo, changeCase) {
				const { name, dir } = interfaceInfo.parsedPath;
				return 'I' + changeCase.pascalCase(interfaceInfo.method) + changeCase.pascalCase(getName(dir, name, changeCase) + 'Res');
			},
			getRequestFunctionName(interfaceInfo, changeCase) {
				let { dir, name } = interfaceInfo.parsedPath;
				return changeCase.camelCase([interfaceInfo.method, getName(dir, name, changeCase)].join(' '));
			},
			getRequestDataTypeName(interfaceInfo: any, changeCase: ChangeCase) {
				const { name, dir } = interfaceInfo.parsedPath;
				console.log(name, dir);
				return 'I' + changeCase.pascalCase(interfaceInfo.method) + changeCase.pascalCase(getName(dir, name, changeCase) + 'Req');
			},
			projects: [getProductConfig()]
		}
	],
	{
		async success() {
			try {
				// 生成统一导出文件 index.ts
				try {
					// 如果 index.ts 不存在，则先创建一个空文件
					try {
						await fs.access(apiExportPath);
					} catch {
						await fs.writeFile(apiExportPath, '', 'utf8');
					}
					const fileHeaderContent = `/* eslint-disable */\n// @ts-nocheck\n`;
					const generatedFiles = [...generatedFilePaths].sort();
					const existingFiles = await fs.readdir(apiDirPath);
					for (const file of existingFiles) {
						if (file.endsWith('.ts') && !generatedFilePaths.has(`${apiDirPath}/${file}`)) {
							await fs.unlink(`${apiDirPath}/${file}`);
						}
					}
					const exports = generatedFiles
						.map((file) => `export * from './modules/${file.slice(apiDirPath.length + 1, -3)}';`)
						.join('\n');

					await fs.writeFile(apiExportPath, fileHeaderContent + exports, 'utf8');
					console.log(`✅ API 统一导出文件已生成: ${apiExportPath}`);
				} catch (indexErr) {
					console.error('❌ 生成 API 导出文件失败:', indexErr);
				}
			} catch (error) {
				console.error('❌ 处理 API 文件过程中出现异常:', error);
			}
		}
	}
);
