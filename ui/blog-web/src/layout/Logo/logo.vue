<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useAppStore from "@/store/modules/app.ts";
import { isUrl } from '@/utils/utils.ts'

const { blogInfo } = storeToRefs(useAppStore())
</script>

<template>
  <div class="blog-logo">
    <div v-if="isUrl(blogInfo.blogLogo)" class="blog-logo-img">
      <el-image style="width: 100%; height: 100%" :src="blogInfo.blogLogo || ''" alt="logo" />
    </div>
    <span v-else>XIAOYILOG</span>
  </div>
</template>

<style lang="scss" scoped>
.blog-logo {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  span {
    font-size: 20px;
    color: #fff;
    font-weight: 700;
    letter-spacing: 2px;
  }
}

.blog-logo-img {
  width: 120px;
  height: 70%;
}
</style>