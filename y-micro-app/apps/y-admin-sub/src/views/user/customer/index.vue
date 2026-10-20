<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  listChatUser,
  getChatUser,
  updateChatUserStatus,
  listChatSession,
  forceLogoutSession,
  forceLogoutAllSessions,
  listChatBlacklist,
  addChatBlacklist,
  removeChatBlacklist,
  searchChatUser
} from '@/api/chatUser'
import BaseTable from '@/components/BaseTable/BaseTable.vue'
import { View, Lock, Unlock, SwitchButton, CircleClose, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const baseTableRef = ref()

/** 详情抽屉 */
const detailVisible = ref(false)
const detail = ref<any>({})
const sessions = ref<any[]>([])
const sessionsLoading = ref(false)

/** 黑名单弹窗 */
const blackVisible = ref(false)
const blackUser = ref<any>({})
const blackList = ref<any[]>([])
const blackLoading = ref(false)
const blackPageNum = ref(1)
const blackPageSize = ref(10)
const blackTotal = ref(0)
const blackTargetOptions = ref<any[]>([])
const blackForm = reactive<any>({
  targetUid: undefined,
  remark: ''
})

const columns = reactive<ColumnType[]>([
  {
    key: 'uId',
    label: 'UID',
    prop: 'uId',
    width: 110,
    render: { el: 'search' }
  },
  {
    key: 'nickname',
    label: '昵称',
    prop: 'nickname',
    render: { el: 'search' }
  },
  {
    key: 'phone',
    label: '手机号',
    prop: 'phone',
    width: 130,
    render: { el: 'search' }
  },
  {
    key: 'email',
    label: '邮箱',
    prop: 'email',
    render: { el: 'search' }
  },
  {
    key: 'status',
    label: '状态',
    prop: 'status',
    width: 90,
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '正常', value: '0', elTagType: 'success' },
        { label: '禁用', value: '1', elTagType: 'danger' }
      ]
    }
  },
  {
    key: 'online',
    label: '在线状态',
    prop: 'online',
    width: 100,
    type: 'tag',
    render: {
      el: 'select',
      list: [
        { label: '在线', value: '1', elTagType: 'success' },
        { label: '离线', value: '0', elTagType: 'info' }
      ]
    }
  },
  {
    key: 'createTime',
    label: '注册时间',
    prop: 'createTime',
    width: 170
  }
])

/** 查看详情：基本信息 + 登录设备 */
const handleDetail = async (row: any) => {
  const res = await getChatUser(row.uId)
  if (res.code !== 200) {
    ElMessage.error(res.msg || '查询失败')
    return
  }
  detail.value = res.data
  detailVisible.value = true
  await loadSessions(row.uId)
}

const loadSessions = async (uId: number | string) => {
  sessionsLoading.value = true
  try {
    const res = await listChatSession(uId)
    sessions.value = res.code === 200 ? (res.data || []) : []
  } finally {
    sessionsLoading.value = false
  }
}

/** 禁用/启用 */
const handleToggleStatus = (row: any) => {
  const disabled = row.status === '1'
  const action = disabled ? '启用' : '禁用'
  const tip = disabled
    ? `确定启用用户 "${row.nickname}" 吗？`
    : `确定禁用用户 "${row.nickname}" 吗？禁用后该用户全部在线设备会被强制下线。`
  ElMessageBox.confirm(tip, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await updateChatUserStatus(row.uId, disabled ? '0' : '1')
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      baseTableRef.value?.getTableData()
    } else {
      ElMessage.error(res.msg || `${action}失败`)
    }
  }).catch(() => {})
}

/** 强制下线全部设备 */
const handleForceAll = (row: any) => {
  ElMessageBox.confirm(`确定强制下线用户 "${row.nickname}" 的全部设备吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await forceLogoutAllSessions(row.uId)
    if (res.code === 200) {
      ElMessage.success('已强制下线')
      if (detailVisible.value && detail.value.uId === row.uId) {
        await loadSessions(row.uId)
      }
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  }).catch(() => {})
}

/** 强制下线单个设备 */
const handleForceDevice = (session: any) => {
  ElMessageBox.confirm('确定强制下线该设备吗？', '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await forceLogoutSession(session.token)
    if (res.code === 200) {
      ElMessage.success('已强制下线')
      await loadSessions(detail.value.uId)
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  }).catch(() => {})
}

/** 打开黑名单管理 */
const handleBlacklist = async (row: any) => {
  blackUser.value = row
  blackForm.targetUid = undefined
  blackForm.remark = ''
  blackTargetOptions.value = []
  blackPageNum.value = 1
  blackVisible.value = true
  await loadBlacklist()
}

const loadBlacklist = async () => {
  blackLoading.value = true
  try {
    const res = await listChatBlacklist(blackUser.value.uId, {
      pageNum: blackPageNum.value,
      pageSize: blackPageSize.value
    })
    if (res.code === 200) {
      blackList.value = res.data.list || []
      blackTotal.value = res.data.total || 0
    }
  } finally {
    blackLoading.value = false
  }
}

/** 远程搜索目标用户 */
const searchBlackTarget = async (keyword: string) => {
  if (!keyword) {
    blackTargetOptions.value = []
    return
  }
  const res = await searchChatUser({ keyword, limit: 20 })
  if (res.code === 200) {
    blackTargetOptions.value = (res.data || []).filter((item: any) => item.uId !== blackUser.value.uId)
  }
}

const handleAddBlacklist = async () => {
  if (!blackForm.targetUid) {
    ElMessage.warning('请选择要拉黑的用户')
    return
  }
  const res = await addChatBlacklist({
    uId: blackUser.value.uId,
    targetUid: blackForm.targetUid,
    remark: blackForm.remark
  })
  if (res.code === 200) {
    ElMessage.success('已加入黑名单')
    blackForm.targetUid = undefined
    blackForm.remark = ''
    blackPageNum.value = 1
    await loadBlacklist()
  } else {
    ElMessage.error(res.msg || '操作失败')
  }
}

const handleRemoveBlacklist = (row: any) => {
  ElMessageBox.confirm(`确定将 "${row.targetNickname || row.targetUid}" 移出黑名单吗？`, '系统提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await removeChatBlacklist(row.id)
    if (res.code === 200) {
      ElMessage.success('已移出黑名单')
      await loadBlacklist()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  }).catch(() => {})
}
</script>

<template>
  <div>
    <base-table ref="baseTableRef" :columns="columns" :api="listChatUser">
      <template #operate="{ row }">
        <el-button :icon="View" size="small" text type="primary" @click="handleDetail(row)">详情</el-button>
        <el-button
            :icon="row.status === '1' ? Unlock : Lock"
            size="small"
            text
            :type="row.status === '1' ? 'success' : 'warning'"
            @click="handleToggleStatus(row)"
        >
          {{ row.status === '1' ? '启用' : '禁用' }}
        </el-button>
        <el-button
            v-if="!!Number(row.onlineDeviceCount)"
            :icon="SwitchButton"
            size="small"
            text
            type="danger"
            @click="handleForceAll(row)"
        >
          强退
        </el-button>
        <el-button :icon="CircleClose" size="small" text type="info" @click="handleBlacklist(row)">黑名单</el-button>
      </template>
    </base-table>

    <!-- 用户详情 -->
    <el-drawer v-model="detailVisible" title="用户详情" size="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="UID">{{ detail.uId }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === '1' ? 'danger' : 'success'">
            {{ detail.status === '1' ? '禁用' : '正常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="在线状态">
          <el-tag :type="!!Number(detail.onlineDeviceCount) ? 'success' : 'info'">
            {{ !!Number(detail.onlineDeviceCount) ? `在线（${detail.onlineDeviceCount} 台设备）` : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="是否绑定聊天">{{ detail.isBindChat === '1' ? '已绑定' : '未绑定' }}</el-descriptions-item>
      </el-descriptions>

      <div class="session-header">
        <span>登录设备</span>
        <el-button
            v-if="sessions.length > 0"
            :icon="SwitchButton"
            size="small"
            type="danger"
            plain
            @click="handleForceAll(detail)"
        >
          全部强退
        </el-button>
      </div>

      <el-table v-loading="sessionsLoading" :data="sessions" border size="small">
        <el-table-column prop="deviceTypeLabel" label="设备类型" width="90" align="center" />
        <el-table-column prop="deviceName" label="设备描述" show-overflow-tooltip />
        <el-table-column prop="ip" label="登录IP" width="130" align="center" />
        <el-table-column prop="loginTime" label="登录时间" width="160" align="center" />
        <el-table-column prop="lastActiveTime" label="最后活跃" width="160" align="center" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="danger" @click="handleForceDevice(row)">强退</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span>当前无在线设备</span>
        </template>
      </el-table>
    </el-drawer>

    <!-- 黑名单管理 -->
    <el-dialog v-model="blackVisible" :title="`黑名单管理 - ${blackUser.nickname || ''}`" width="720px" append-to-body>
      <el-form :inline="true">
        <el-form-item label="拉黑用户">
          <el-select
              v-model="blackForm.targetUid"
              filterable
              remote
              clearable
              reserve-keyword
              placeholder="输入昵称/手机号/UID 搜索"
              :remote-method="searchBlackTarget"
              style="width: 260px"
          >
            <el-option
                v-for="item in blackTargetOptions"
                :key="item.uId"
                :label="`${item.nickname}（${item.phone || item.uId}）`"
                :value="item.uId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="blackForm.remark" placeholder="选填" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button :icon="Plus" type="primary" @click="handleAddBlacklist">加入黑名单</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="blackLoading" :data="blackList" border size="small">
        <el-table-column prop="targetUid" label="被拉黑UID" width="110" align="center" />
        <el-table-column prop="targetNickname" label="昵称" />
        <el-table-column prop="targetPhone" label="手机号" width="130" align="center" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="110" align="center" />
        <el-table-column prop="createTime" label="拉黑时间" width="160" align="center" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button :icon="Delete" size="small" text type="danger" @click="handleRemoveBlacklist(row)">移除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span>暂无黑名单记录</span>
        </template>
      </el-table>

      <div class="base-pagination">
        <el-pagination
            v-model:current-page="blackPageNum"
            v-model:page-size="blackPageSize"
            size="small"
            background
            layout="total, prev, pager, next"
            :total="blackTotal"
            @change="loadBlacklist"
        />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: size(24) 0 size(12);
  font-weight: 600;
}

.base-pagination {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: size(16);
}
</style>
