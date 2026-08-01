<script setup lang="ts">
import {onMounted, toRefs} from 'vue'
import useUserStore from "@/store/user.ts";
import LayoutHeader from './Header/index.vue'
import AppMain from "./AppMain.vue";

const userStore = useUserStore();
const { wkIM } = toRefs(useUserStore())

onMounted(() => {
  if (!wkIM.value) {
    userStore.initWKIM()
  }
})
</script>

<template>
  <div class="app-container">

    <layout-header />

    <div class="main-container">
      <app-main />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.app-container {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.main-container {
  flex: 1;
  background: rgba(255, 255, 255, .4);
  border-radius: size(24) size(24) 0 0;
  box-shadow: #d9f7be 6px 2px 16px 0, #d9f7be -6px -2px 16px 0px;
}
</style>
