<script setup lang="ts">
import { computed } from 'vue'
import useAppVersionStore from '@/store/appVersion.ts'

/**
 * 检查更新弹窗
 *
 * 全局常驻于 layout，与 ForceLogoutDialog / CallOverlay 同级。
 * 强制更新时不可关闭，只能点「立即更新」；可选更新可稍后再说（本次会话内不再提示）。
 * */
const appVersionStore = useAppVersionStore()

const info = computed(() => appVersionStore.updateInfo)
const forceUpdate = computed(() => !!info.value?.forceUpdate)

/** 更新说明按行拆成列表 */
const changelogLines = computed(() =>
  (info.value?.changelog || '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
)

/** 字节数转可读文本 */
function formatSize(size?: number): string {
  if (!size || size <= 0) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}
</script>

<template>
  <el-dialog
      :model-value="appVersionStore.visible"
      :show-close="!forceUpdate"
      :close-on-click-modal="false"
      :close-on-press-escape="!forceUpdate"
      width="380px"
      align-center
      class="check-update-dialog"
      append-to-body
      @close="appVersionStore.close"
  >
    <div class="update-body">
      <span class="update-title">{{ forceUpdate ? '发现新版本，需立即更新' : '发现新版本' }}</span>
      <span class="update-version">
        v{{ info?.latestVersion }}
        <span v-if="info?.fileSize" class="update-size">（{{ formatSize(info.fileSize) }}）</span>
      </span>

      <div v-if="changelogLines.length > 0" class="update-changelog">
        <span class="update-changelog-title">更新内容</span>
        <ul>
          <li v-for="(line, index) in changelogLines" :key="index">{{ line }}</li>
        </ul>
      </div>
      <span v-else class="update-empty">本次更新暂无说明</span>

      <div class="update-actions">
        <el-button v-if="!forceUpdate" class="update-btn" @click="appVersionStore.skipUpdate()">稍后再说</el-button>
        <el-button type="primary" class="update-btn" @click="appVersionStore.applyUpdate()">立即更新</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.update-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: size(8);
  padding: size(4) 0;
}

.update-title {
  font-size: size(17);
  color: #333;
  font-weight: 500;
}

.update-version {
  font-size: size(14);
  color: #409eff;
}

.update-size {
  color: #999;
}

.update-changelog {
  width: 100%;
  max-height: size(180);
  overflow-y: auto;
  margin-top: size(6);
  padding: size(10) size(12);
  background: #f7f8fa;
  border-radius: size(6);
  box-sizing: border-box;

  .update-changelog-title {
    font-size: size(13);
    color: #999;
  }

  ul {
    margin: size(6) 0 0;
    padding-left: size(16);

    li {
      font-size: size(13);
      color: #555;
      line-height: 1.8;
      word-break: break-all;
    }
  }
}

.update-empty {
  font-size: size(13);
  color: #999;
}

.update-actions {
  display: flex;
  align-items: center;
  gap: size(12);
  margin-top: size(10);
}

.update-btn {
  width: size(120);
  margin: 0;
}
</style>
