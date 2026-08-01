<script setup lang="ts">
import { toRefs } from 'vue'
import { getConversation } from '@/api/chat.ts'
import { onMounted, reactive, ref} from "vue";
import { ElMessage } from 'element-plus'
import useUserStore from "@/store/user.ts";
import { decodePayload } from '@/utils/wk.ts'

const { wkIM } = toRefs(useUserStore())
const params = reactive({
  msgCount: 1
})
const conversations = ref<any[]>([])
const initConversation = async () => {
  try {
    const response = await getConversation(params)

    if (response.code === 200) {
      conversations.value = response.data
    }
  } catch {
    ElMessage.error('获取会话列表失败')
  }
}



onMounted(() => {
  initConversation()
})
</script>

<template>
  <div class="contact-list">
    <div v-for="(item, index) in conversations" :key="index" class="">
      <img :src="item.avatar || ''" alt="avatar" />
      <div>
        <div>
          <span>{{ item.name }}</span>
        </div>
<!--        <span>{{}}</span>-->
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.contact-list {
  width: 100%;
  flex: 1;
  overflow: hidden;
  overflow-y: scroll;
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}
</style>