<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { findGroupMembers } from '@/api/group.ts'
import { resolveMemberName } from '@/utils/chat.ts'

/**
 * 群通话成员选择器
 *
 * 用于「发起群通话」时选择初始成员，以及「通话中追加邀请」时选择新增成员。
 * 成员来源优先使用父组件传入的 members（ChatWindow 已加载的群成员），
 * 未传入时按 groupId 自行拉取（全局浮层场景）。
 * */

const props = withDefaults(defineProps<{
  modelValue: boolean
  /** 群频道 ID，如 g_9（未传 members 时用于自行拉取） */
  groupId?: string
  /** 群成员列表（可选，传入则不再请求） */
  members?: GroupMember[]
  /** 可选人数上限（默认 8 = 上限 9 人减去自己） */
  max?: number
  /** 需要排除的成员（自己、已在通话中的成员） */
  excludeUids?: Array<string | number>
}>(), {
  max: 8,
  excludeUids: () => [],
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', uids: Array<string | number>): void
}>()

const keyword = ref('')
const selected = ref<string[]>([])
const loaded = ref<GroupMember[]>([])
const loading = ref(false)

const source = computed<GroupMember[]>(() => (props.members && props.members.length ? props.members : loaded.value))

const candidates = computed<GroupMember[]>(() => {
  const excluded = new Set(props.excludeUids.map(String))
  const kw = keyword.value.trim()
  return source.value.filter((m) => {
    if (excluded.has(String(m.uid))) {
      return false
    }
    if (!kw) {
      return true
    }
    return displayName(m).includes(kw)
  })
})

const full = computed(() => selected.value.length >= props.max)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

function displayName(member: GroupMember): string {
  return resolveMemberName(member, String(member.uid))
}

function isSelected(uid: number): boolean {
  return selected.value.includes(String(uid))
}

/** 已达上限后，未选中的成员不可再选 */
function isDisabled(uid: number): boolean {
  return full.value && !isSelected(uid)
}

function toggle(uid: number): void {
  const key = String(uid)
  if (selected.value.includes(key)) {
    selected.value = selected.value.filter((v) => v !== key)
    return
  }
  if (full.value) {
    return
  }
  selected.value = [...selected.value, key]
}

function selectAll(): void {
  const rest = candidates.value
    .filter((m) => !isSelected(m.uid))
    .map((m) => String(m.uid))
  selected.value = [...selected.value, ...rest].slice(0, props.max)
}

function handleConfirm(): void {
  if (!selected.value.length) {
    return
  }
  emit('confirm', [...selected.value])
  visible.value = false
}

async function loadMembers(): Promise<void> {
  if ((props.members && props.members.length) || !props.groupId || loading.value) {
    return
  }
  loading.value = true
  try {
    const res = await findGroupMembers(props.groupId) as unknown as { code: number; data: GroupMember[] }
    if (res?.code === 200) {
      loaded.value = res.data || []
    }
  } catch (e) {
    console.warn('[GroupCallMemberPicker] 加载群成员失败', e)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      keyword.value = ''
      selected.value = []
      void loadMembers()
    }
  },
)
</script>

<template>
  <el-dialog
      v-model="visible"
      title="选择通话成员"
      width="420px"
      align-center
      append-to-body
      class="group-call-picker"
  >
    <div class="picker-search">
      <el-input v-model="keyword" placeholder="搜索成员" clearable />
      <el-button text type="primary" :disabled="full" @click="selectAll">全选</el-button>
    </div>

    <div v-loading="loading" class="picker-list">
      <div
          v-for="member in candidates"
          :key="member.uid"
          class="picker-item"
          :class="{ selected: isSelected(member.uid), disabled: isDisabled(member.uid) }"
          @click="!isDisabled(member.uid) && toggle(member.uid)"
      >
        <img v-if="member.avatar" class="item-avatar" :src="member.avatar" alt="avatar" />
        <div v-else class="item-avatar item-avatar--text">{{ displayName(member).slice(0, 1) }}</div>
        <span class="item-name">{{ displayName(member) }}</span>
        <el-icon v-if="isSelected(member.uid)" class="item-check"><Check /></el-icon>
      </div>
      <div v-if="!candidates.length && !loading" class="picker-empty">暂无可邀请成员</div>
    </div>

    <div class="picker-hint">已选 {{ selected.length }} / {{ max }} 人</div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selected.length" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.group-call-picker {
  .picker-search {
    display: flex;
    align-items: center;
    gap: size(8);
  }

  .picker-list {
    margin-top: size(12);
    max-height: size(320);
    overflow-y: auto;

    .picker-item {
      display: flex;
      align-items: center;
      gap: size(10);
      padding: size(8) size(6);
      border-radius: size(6);
      cursor: pointer;

      &:hover {
        background: rgba(0, 0, 0, .04);
      }

      &.selected {
        background: rgba(64, 158, 255, .08);
      }

      &.disabled {
        opacity: .45;
        cursor: not-allowed;
      }

      .item-avatar {
        width: size(36);
        height: size(36);
        border-radius: 50%;
        object-fit: cover;

        &--text {
          display: flex;
          align-items: center;
          justify-content: center;
          background: $theme-color;
          color: #fff;
          font-size: size(15);
        }
      }

      .item-name {
        flex: 1;
        font-size: size(14);
        color: #333;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .item-check {
        color: $theme-color;
      }
    }

    .picker-empty {
      padding: size(32) 0;
      text-align: center;
      color: #999;
      font-size: size(13);
    }
  }

  .picker-hint {
    margin-top: size(10);
    font-size: size(12);
    color: #999;
    text-align: right;
  }
}
</style>
