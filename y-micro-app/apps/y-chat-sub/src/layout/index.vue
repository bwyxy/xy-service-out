<script setup lang="ts">
import {onMounted, toRefs} from 'vue'
import useUserStore from "@/store/user.ts";
import LayoutHeader from './Header/index.vue'
import AppMain from "./AppMain.vue";
import LayoutNav from './nav/index.vue'

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
      <layout-nav />

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
  display: flex;
  background: rgba(255, 255, 255, .4);
  border-top: 1px solid rgba(0, 0, 0, .06);
  //box-shadow: #d9f7be 6px 2px 16px 0, #d9f7be -6px -2px 16px 0px;
}
</style>
