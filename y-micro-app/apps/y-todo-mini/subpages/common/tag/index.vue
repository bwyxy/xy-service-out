<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import { createTag, listTags, removeTag, updateTag } from '@/api/tag'

	/**
	 * 通用 · 清单管理
	 *
	 * 路由：subpages/common/tag/index（分包 subpages/common，按需加载）
	 * 职责：清单管理页，给每件事找个合适的去处——清单列表（图标/名称/待办数）、新建、改名换图标、删除。
	 * 数据：清单来自 GET /tag/list，新建 POST /tag/create，保存 PUT /tag/{tagId}，删除 DELETE /tag/{tagId}。
	 * 目录：状态（tags、editorVisible、editingTagId、draftName、draftIcon、saving）→ 方法（loadTags、openEditor、closeEditor、save、removeEditing）→ 样式（.group、.row、.footer、.sheet）
	 */

/** 清单名称长度上限，与后端 ContentCheckUtils 的校验保持一致 */
const TAG_NAME_MAX = 24

/** 可选图标（取自 y-icon 图标表） */
const ICON_OPTIONS = ['folder', 'briefcase', 'book', 'sprout', 'coffee', 'star', 'flame', 'list']

/** 清单列表 */
const tags = ref([])

/** 编辑弹框是否可见 */
const editorVisible = ref(false)

/** 正在编辑的清单 Id，null 表示新建 */
const editingTagId = ref(null)

/** 弹框里的清单名称 */
const draftName = ref('')

/** 弹框里选中的图标 */
const draftIcon = ref(ICON_OPTIONS[0])

/** 保存中标记，避免连点建出多条 */
const saving = ref(false)

/** 弹框标题 */
const editorTitle = computed(() => (editingTagId.value == null ? '新建清单' : '编辑清单'))

/** 拉取清单列表（未登录时请求层会先拉起登录半弹框） */
async function loadTags() {
	const res = await listTags()

	if (res.code !== 200 || !Array.isArray(res.data)) {
		return
	}

	tags.value = res.data.map((item) => ({
		tagId: item.tagId,
		tagName: item.tagName,
		icon: item.icon || 'folder',
		isDefault: item.isDefault,
		pendingCount: item.pendingCount || 0
	}))
}

/** 打开编辑弹框，tag 为空表示新建 */
function openEditor(tag) {
	editingTagId.value = tag ? tag.tagId : null
	draftName.value = tag ? tag.tagName : ''
	draftIcon.value = tag && tag.icon ? tag.icon : ICON_OPTIONS[0]
	editorVisible.value = true
}

/** 关闭编辑弹框 */
function closeEditor() {
	editorVisible.value = false
}

/** 保存（新建或改名换图标） */
async function save() {
	const tagName = draftName.value.trim()

	if (!tagName) {
		uni.showToast({ title: '给清单起个名字吧', icon: 'none' })

		return
	}

	if (saving.value) {
		return
	}

	saving.value = true

	try {
		const payload = { tagName, icon: draftIcon.value }
		const isCreate = editingTagId.value == null
		// 重名由后端拦截，失败提示已由 request 统一 toast
		const res = isCreate ? await createTag(payload) : await updateTag(editingTagId.value, payload)

		if (res.code !== 200) {
			return
		}

		closeEditor()
		uni.showToast({ title: isCreate ? '已新建清单' : '已保存', icon: 'none' })
		await loadTags()
	} catch (error) {
		console.warn('[tag] 保存清单失败', error)
	} finally {
		saving.value = false
	}
}

/** 删除当前编辑的清单（任务留着，变成未归类） */
async function removeEditing() {
	const tag = tags.value.find((item) => item.tagId === editingTagId.value)

	if (!tag) {
		return
	}

	const confirmed = await new Promise((resolve) => {
		uni.showModal({
			title: '删除清单',
			content: `「${tag.tagName}」下的任务会变为未归类，任务本身不会删除。`,
			confirmText: '删除',
			confirmColor: '#b5543f',
			success: (res) => resolve(!!res.confirm),
			fail: () => resolve(false)
		})
	})

	if (!confirmed) {
		return
	}

	const res = await removeTag(tag.tagId)

	if (res.code !== 200) {
		return
	}

	closeEditor()
	uni.showToast({ title: '已删除', icon: 'none' })
	await loadTags()
}

onShow(() => {
	loadTags()
})
</script>

<template>
	<sub-page title="清单管理" subtitle="给每件事，找个合适的去处。" :fixed-footer="true">
		<view class="group">
			<view v-for="tag in tags" :key="tag.tagId" class="row" @click="openEditor(tag)">
				<view class="row__icon">
					<y-icon :name="tag.icon" size="40rpx" />
				</view>

				<view class="row__text">
					<text class="row__label">{{ tag.tagName }}</text>
					<text class="row__desc">{{ tag.pendingCount }} 件待办</text>
				</view>

				<text v-if="tag.isDefault" class="row__badge">默认</text>
				<y-icon name="chevron-right" size="30rpx" color="#c2c2b9" />
			</view>

			<view v-if="!tags.length" class="empty">
				<text class="empty__title">这里还没有清单</text>
				<text class="empty__desc">新建一个清单，把同类的事放在一起。</text>
			</view>
		</view>

		<view class="tip">
			<y-icon name="bulb" size="30rpx" />
			<text class="tip__text">删除清单不会删掉里面的任务，它们会回到「未归类」。</text>
		</view>

		<template #footer>
			<view class="footer">
				<view class="footer__button" @click="openEditor(null)">
					<y-icon name="plus" size="32rpx" color="#ffffff" />
					<text class="footer__text">新建清单</text>
				</view>
			</view>
		</template>

		<model-popup v-model="editorVisible" @beforeClose="closeEditor">
			<view class="sheet">
				<view class="sheet__bar" />

				<view class="sheet__head">
					<text class="sheet__title">{{ editorTitle }}</text>
					<text class="sheet__desc">清单名在同一账号下不能重复。</text>
				</view>

				<view class="field">
					<text class="field__label">清单名称</text>
					<input
						v-model="draftName"
						class="field__input"
						type="text"
						:maxlength="TAG_NAME_MAX"
						placeholder="比如：工作、读书"
						placeholder-style="color:#a9a9a0"
					/>
				</view>

				<view class="field">
					<text class="field__label">图标</text>

					<view class="icons">
						<view
							v-for="icon in ICON_OPTIONS"
							:key="icon"
							class="icons__item"
							:class="{ 'icons__item--active': draftIcon === icon }"
							@click="draftIcon = icon"
						>
							<y-icon :name="icon" size="40rpx" :color="draftIcon === icon ? '#4a6b3e' : '#8e8e85'" />
						</view>
					</view>
				</view>

				<view class="sheet__button" @click="save">
					<text class="sheet__button-text">{{ saving ? '保存中…' : '保存' }}</text>
				</view>

				<view v-if="editingTagId != null" class="sheet__danger" @click="removeEditing">
					<text class="sheet__danger-text">删除这个清单</text>
				</view>
			</view>
		</model-popup>
	</sub-page>
</template>

<style scoped lang="scss">
/* 清单列表 */
.group {
	margin-top: 32rpx;
	padding: 4rpx 32rpx;
	border-radius: 28rpx;
	background-color: $y-card-bg;
}

.row {
	display: flex;
	align-items: center;
	padding: 30rpx 0;

	& + .row {
		border-top: 2rpx solid $y-divider;
	}
}

.row__icon {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	width: 68rpx;
	height: 68rpx;
	border-radius: 20rpx;
	background-color: $y-green-soft;
}

.row__text {
	flex: 1;
	display: flex;
	flex-direction: column;
	margin-left: 24rpx;
	min-width: 0;
}

.row__label {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.row__desc {
	margin-top: 8rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.row__badge {
	flex-shrink: 0;
	margin-right: 12rpx;
	padding: 4rpx 14rpx;
	border-radius: 8rpx;
	background-color: $y-green-tag-bg;
	font-size: calc(20rpx * var(--y-font-scale, 1));
	color: $y-green-tag-text;
}

/* 空状态 */
.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 60rpx 0;
}

.empty__title {
	font-size: calc(30rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.empty__desc {
	margin-top: 12rpx;
	font-size: calc(25rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

/* 说明 */
.tip {
	display: flex;
	align-items: flex-start;
	margin-top: 28rpx;
	padding: 24rpx 28rpx;
	border-radius: 20rpx;
	background-color: $y-green-soft;
}

.tip__text {
	flex: 1;
	margin-left: 14rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	line-height: 1.6;
	color: $y-green-text;
}

/* 底部主按钮 */
.footer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 24rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
	background-color: $y-page-bg;
}

.footer__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	border-radius: 24rpx;
	background-color: $y-green-primary;
}

.footer__text {
	margin-left: 10rpx;
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 500;
	color: #ffffff;
}

/* 编辑弹框 */
.sheet {
	display: flex;
	flex-direction: column;
	padding: 20rpx 48rpx calc(40rpx + env(safe-area-inset-bottom));
	border-radius: 40rpx 40rpx 0 0;
	background-color: $y-card-bg;
}

.sheet__bar {
	width: 72rpx;
	height: 8rpx;
	margin: 8rpx auto 0;
	border-radius: 4rpx;
	background-color: $y-divider;
}

.sheet__head {
	display: flex;
	flex-direction: column;
	margin-top: 36rpx;
}

.sheet__title {
	font-size: calc(38rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: $y-text-main;
}

.sheet__desc {
	margin-top: 12rpx;
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-muted;
}

.field {
	margin-top: 36rpx;
}

.field__label {
	font-size: calc(24rpx * var(--y-font-scale, 1));
	color: $y-text-sub;
}

.field__input {
	margin-top: 12rpx;
	padding: 0 24rpx;
	height: 88rpx;
	box-sizing: border-box;
	border-radius: 20rpx;
	border: 2rpx solid $y-divider;
	font-size: calc(30rpx * var(--y-font-scale, 1));
	color: $y-text-main;
}

.icons {
	display: flex;
	flex-wrap: wrap;
	margin-top: 16rpx;
}

.icons__item {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 96rpx;
	height: 96rpx;
	margin: 0 20rpx 20rpx 0;
	border-radius: 24rpx;
	border: 2rpx solid $y-divider;
	background-color: $y-card-bg;
}

.icons__item--active {
	border-color: $y-green-primary;
	background-color: $y-green-soft;
}

.sheet__button {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	margin-top: 20rpx;
	border-radius: 48rpx;
	background-color: $y-green-primary;
}

.sheet__button-text {
	font-size: calc(32rpx * var(--y-font-scale, 1));
	font-weight: 600;
	color: #ffffff;
}

.sheet__danger {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	margin-top: 16rpx;
}

.sheet__danger-text {
	font-size: calc(28rpx * var(--y-font-scale, 1));
	color: $y-danger;
}
</style>
