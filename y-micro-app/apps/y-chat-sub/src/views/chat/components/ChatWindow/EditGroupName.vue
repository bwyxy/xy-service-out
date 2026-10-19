<script setup lang="ts">

import {EditPen} from "@element-plus/icons-vue";
import {ref} from "vue";
import {ElMessage} from "element-plus";
import {editGroup} from "@/api/group.ts";

const props = defineProps<{
  groupId: string | number
  name: string
}>()

const emit = defineEmits<{
  (e: 'success'): void
}>()

const isShow = ref<boolean>(false);
const groupName = ref<string>('');
const submitting = ref<boolean>(false);

/** 打开弹窗并回填当前群名称 */
const handleOpen = () => {
  groupName.value = props.name || '';
  isShow.value = true;
}

/** 提交修改群名称 */
const handleConfirm = async () => {
  const name = groupName.value.trim();

  if (name.length < 2) {
    ElMessage.warning('群名称不能低于两个字符');
    return;
  }

  submitting.value = true;
  try {
    const result = await editGroup({ groupId: props.groupId, name });

    if (result.code === 200) {
      ElMessage.success('修改成功');
      isShow.value = false;
      emit('success');
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="edit-group-name">
    <el-icon
        style="cursor: pointer"
        color="#2f54eb"
        @click="handleOpen"
    >
      <EditPen />
    </el-icon>

    <el-dialog
        v-model="isShow"
        :align-center="true"
        width="320px"
    >
      <template #header>
        <div class="title-box">
          <span>编辑名称</span>
        </div>
      </template>

      <el-input v-model="groupName" maxlength="64" placeholder="请输入群名称" />

      <template #footer>
        <el-button @click="isShow = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleConfirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.title-box {
  font-size: size(20);
  letter-spacing: 1px;
  color: #1f1f1f;
  text-align: left;
}
</style>
