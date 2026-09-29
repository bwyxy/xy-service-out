<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getUserProfile, updateUserProfile, updateUserPwd } from '@/api/user'
import { uploadImage } from '@/api/upload'
import { validateImage } from '@/utils/file'
import useUserStore from '@/store/modules/user'
import { User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const loading = ref(false)
const activeTab = ref('profile')
const avatarUploading = ref(false)
const submitLoading = ref(false)
const pwdLoading = ref(false)

// 个人信息
const profile = ref<any>({})

// 基本资料表单
const profileFormRef = ref()
const profileForm = reactive<any>({
  nickname: '',
  sex: '0',
  email: '',
  phone: '',
  introduction: ''
})
const profileRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 修改密码表单
const pwdFormRef = ref()
const pwdForm = reactive<any>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 20, message: '长度在 5 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

function validateConfirmPwd(_rule: any, value: string, callback: any) {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const sexText = (sex: string) => {
  return { '0': '未知', '1': '男', '2': '女' }[sex || '0'] || '未知'
}

// 角色信息由 /auth/getInfo 返回，从用户 store 中获取
const roleText = () => {
  const roles = userStore.userInfo?.roles
  if (Array.isArray(roles) && roles.length > 0) {
    return roles.join('、')
  }
  return '-'
}

// 获取个人信息
const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await getUserProfile()
    if (res.code === 200) {
      profile.value = res.data || {}
      profileForm.nickname = profile.value.nickname || ''
      profileForm.sex = profile.value.sex || '0'
      profileForm.email = profile.value.email || ''
      profileForm.phone = profile.value.phone || ''
      profileForm.introduction = profile.value.introduction || ''
    }
  } finally {
    loading.value = false
  }
}

// 上传头像
const handleAvatarUpload = async (options: any) => {
  const file: File = options.file
  const error = validateImage(file, 2)
  if (error) {
    ElMessage.error(error)
    return
  }
  avatarUploading.value = true
  try {
    const res = await uploadImage(file)
    if (res.code === 200) {
      const r = await updateUserProfile({ avatar: res.data })
      if (r.code === 200) {
        ElMessage.success('头像更新成功')
        await fetchProfile()
        await userStore.getInfo()
      } else {
        ElMessage.error(r.msg || '头像更新失败')
      }
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } finally {
    avatarUploading.value = false
  }
}

// 保存基本资料
const submitProfile = async () => {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const res = await updateUserProfile({ ...profileForm })
    if (res.code === 200) {
      ElMessage.success('个人资料修改成功')
      await fetchProfile()
      // 同步刷新头部昵称/头像显示
      await userStore.getInfo()
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } finally {
    submitLoading.value = false
  }
}

// 重置基本资料表单
const resetProfile = () => {
  profileFormRef.value?.resetFields()
  profileForm.nickname = profile.value.nickname || ''
  profileForm.sex = profile.value.sex || '0'
  profileForm.email = profile.value.email || ''
  profileForm.phone = profile.value.phone || ''
  profileForm.introduction = profile.value.introduction || ''
}

// 修改密码
const submitPwd = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    const res = await updateUserPwd({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      pwdFormRef.value?.resetFields()
      pwdForm.oldPassword = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
    } else {
      ElMessage.error(res.msg || '密码修改失败')
    }
  } finally {
    pwdLoading.value = false
  }
}

onMounted(fetchProfile)
</script>

<template>
  <div v-loading="loading" class="profile-wrapper">
    <el-row :gutter="16">
      <!-- 左侧：个人信息卡片 -->
      <el-col :xs="24" :md="8">
        <el-card shadow="hover" class="user-card">
          <div class="avatar-box">
            <el-upload
              action="#"
              :http-request="handleAvatarUpload"
              :show-file-list="false"
              accept="image/*"
              class="avatar-upload"
            >
              <div class="avatar" :class="{ uploading: avatarUploading }">
                <img v-if="profile.avatar" :src="profile.avatar" alt="avatar" />
                <el-icon v-else class="avatar-placeholder"><User /></el-icon>
                <div class="avatar-mask">{{ avatarUploading ? '上传中...' : '点击更换头像' }}</div>
              </div>
            </el-upload>
          </div>
          <h3 class="user-name">{{ profile.nickname || profile.username }}</h3>
          <p class="user-introduction">{{ profile.introduction || '这个人很懒，什么都没有留下' }}</p>
          <div class="user-meta">
            <div class="meta-item">
              <span class="meta-label">用户名</span>
              <span class="meta-value">{{ profile.username || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">性别</span>
              <span class="meta-value">{{ sexText(profile.sex) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">角色</span>
              <span class="meta-value">{{ roleText() }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">邮箱</span>
              <span class="meta-value">{{ profile.email || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">手机号</span>
              <span class="meta-value">{{ profile.phone || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">创建时间</span>
              <span class="meta-value">{{ profile.createTime || '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：修改资料 / 修改密码 -->
      <el-col :xs="24" :md="16">
        <el-card shadow="hover">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="profile">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="80px"
                class="profile-form"
              >
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" />
                </el-form-item>
                <el-form-item label="性别" prop="sex">
                  <el-radio-group v-model="profileForm.sex">
                    <el-radio label="0">未知</el-radio>
                    <el-radio label="1">男</el-radio>
                    <el-radio label="2">女</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="请输入邮箱" maxlength="50" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="11" />
                </el-form-item>
                <el-form-item label="个人简介" prop="introduction">
                  <el-input
                    v-model="profileForm.introduction"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入个人简介"
                    maxlength="200"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="submitLoading" @click="submitProfile">保存修改</el-button>
                  <el-button @click="resetProfile">重 置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="pwd">
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="80px"
                class="profile-form"
              >
                <el-form-item label="旧密码" prop="oldPassword">
                  <el-input
                    v-model="pwdForm.oldPassword"
                    type="password"
                    show-password
                    placeholder="请输入旧密码"
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="pwdForm.newPassword"
                    type="password"
                    show-password
                    placeholder="请输入新密码"
                  />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input
                    v-model="pwdForm.confirmPassword"
                    type="password"
                    show-password
                    placeholder="请再次输入新密码"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="pwdLoading" @click="submitPwd">保存修改</el-button>
                  <el-button @click="pwdFormRef?.resetFields()">重 置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.profile-wrapper {
  width: 100%;
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
  // 跟随系统主题切换背景色
  background-color: var(--app-bg-color);
  transition: background-color 0.4s ease;
}

// 左侧用户卡片
.user-card {
  text-align: center;
  margin-bottom: 16px;
}

.avatar-box {
  margin-bottom: 16px;
}

.avatar-upload {
  display: inline-block;
  cursor: pointer;

  :deep(.el-upload) {
    display: block;
  }
}

.avatar {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 0 auto;
  border-radius: 50%;
  overflow: hidden;
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.3s ease;

  &.uploading {
    opacity: 0.6;
  }

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
  }

  .avatar-placeholder {
    font-size: 48px;
  }

  .avatar-mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #fff;
    background: rgba(0, 0, 0, 0.45);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover .avatar-mask {
    opacity: 1;
  }
}

.user-name {
  margin: 0;
  font-size: 20px;
  color: var(--app-text-color);
  transition: color 0.4s ease;
}

.user-introduction {
  margin: 8px 0 16px;
  font-size: 13px;
  color: var(--app-text-secondary);
  transition: color 0.4s ease;
}

.user-meta {
  text-align: left;
  border-top: 1px solid var(--app-border-color);
  padding-top: 16px;
  transition: border-color 0.4s ease;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 4px;
  font-size: 14px;

  &:not(:last-child) {
    border-bottom: 1px dashed var(--app-border-color);
  }

  .meta-label {
    color: var(--app-text-secondary);
    transition: color 0.4s ease;
  }

  .meta-value {
    color: var(--app-text-color);
    word-break: break-all;
    transition: color 0.4s ease;
  }
}

// 右侧表单
.profile-form {
  max-width: 560px;
  padding-top: 8px;
}
</style>
