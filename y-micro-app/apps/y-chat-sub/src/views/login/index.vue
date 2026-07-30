<script setup lang="ts">
import Login from './modules/Login.vue'
import Register from './modules/Register.vue'
import {computed, ref} from "vue";

const type = ref<string>('login')

const mainComponent = computed(() => {
  if (type.value === 'login') {
    return Login
  } else {
    return Register
  }
})

const handleSwitch = () => {
  type.value = type.value === 'login' ? 'register' : 'login'
}
</script>

<template>
  <div class="login-container">
    <transition
      enter-active-class="animate__animated animate__fadeInUp"
      leave-active-class="animate__animated animate__fadeOutUp"
    >
      <component
          :is="mainComponent"
          @switch="handleSwitch"
      ></component>
    </transition>
  </div>
</template>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>