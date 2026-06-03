<script setup lang="ts">
import {onMounted, ref} from "vue";
import useAppStore from "@/store/modules/app.ts";
import SplashScreen from "@/components/SplashScreen/index.vue";

const appStore = useAppStore();
const showSplash = ref(true);

const onSplashDone = () => {
  console.log('onSplashDone')
  sessionStorage.setItem('_splash_done', '1');
  showSplash.value = false;
};

onMounted(() => {
  appStore.setBlogInfo()
})
</script>

<template>
  <SplashScreen v-if="showSplash" @done="onSplashDone" />
  <router-view />
</template>

<style scoped>

</style>
