<script setup lang="ts">
//语言配置和层级配置
import { ElConfigProvider } from 'element-plus';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import { useTheme } from './hooks/useTheme';
import WaterMark from './components/waterMark';
const { initTheme } = useTheme();
initTheme();
</script>

<template>
	<el-config-provider :locale="zhCn" size="default" :z-index="200">
		<WaterMark>
			<router-view />
		</WaterMark>
	</el-config-provider>
</template>

<style scoped></style>
