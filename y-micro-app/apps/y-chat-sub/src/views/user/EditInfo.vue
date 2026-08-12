<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useUserStore from "@/store/user.ts";
import {onMounted, reactive, ref} from "vue";
import { updateUserInfo } from '@/api/auth.ts'
import {ElMessage} from "element-plus";

const userStore = useUserStore();
const { userInfo } = storeToRefs(useUserStore())

const isLoading = ref<boolean>(false)
const ruleForm = reactive({
  nickname: "",
  sex: ""
})

const handleSubmit = async () => {
  isLoading.value = true
  const response = await updateUserInfo(ruleForm)

  isLoading.value = false
  if (response.code === 200) {
    ElMessage.success("更新成功")
    await userStore.setUserInfo()
  }
}

onMounted(() => {
  if (userInfo) {
    ruleForm.nickname = userInfo.value.nickname
    ruleForm.sex = String(userInfo.value.sex) || '1'
  }
})

</script>

<template>
  <el-form :model="ruleForm">
    <el-form-item label="昵称">
      <el-input v-model="ruleForm.nickname" />
    </el-form-item>
    <el-form-item label="性别">
      <el-radio-group v-model="ruleForm.sex">
        <el-radio label="1">男</el-radio>
        <el-radio label="2">女</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <div class="form-btm">
        <el-button v-loading="isLoading" type="primary" @click="handleSubmit">保存</el-button>
      </div>
    </el-form-item>
  </el-form>
</template>

<style scoped lang="scss">
.form-btm {
  width: 100%;
  display: flex;
  justify-content: center;
}
</style>