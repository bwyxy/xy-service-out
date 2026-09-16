<script setup lang="ts">
import Sidebar from './Sidebar/index.vue'
import HeaderBar from './Header/index.vue'
import AppMain from './AppMain.vue'

</script>

<template>
  <div class="app-wrapper">

    <HeaderBar />

    <div class="main-container">

      <Sidebar />

      <div class="app-main">
        <app-main />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-wrapper {
  display: flex;
  width: 100%;
  height: 100vh;
  //background: linear-gradient(170deg, #bae0ff, #fafafa, #fff);
  //background: #f0f0f0;
  overflow: hidden;
  flex-direction: column;
}

.main-container {
  flex: 1;
  //background: rgba(255, 255, 255, .6);
  display: flex;
  //border-radius: size(24) size(24) 0 0;
  //box-shadow: rgba(0, 0, 0, .04) 4px 2px 6px 0, rgba(0, 0, 0, .04) -4px -2px 6px 0px;
  box-sizing: border-box;
  //padding: 0 size(24) size(24);
  overflow: hidden;
}

.app-main {
  flex: 1;
  box-sizing: border-box;
  padding: 24px;
  overflow-y: auto;
}
</style>