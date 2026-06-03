<template>
  <div class="ring-chart" :style="{ '--color': updateProgress(60) }">
    <div class="ring-chart-content">
      <span class="ring-chart-value">10</span>
      <span class="ring-chart-label">{{ label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'

const TRACK_COLOR = '#f0f0f0';

const prop = defineProps<{
  color?: string
  value?: number
  total?: number
  label?: string
}>()

const { color } = toRefs(prop)
function updateProgress(percent :number) {
  const ACTIVE_COLOR = color.value || '#e0529c';

  let validPercent = Math.min(100, Math.max(0, percent));

  const finishAngle = (validPercent / 100) * 360;

  let gradientCss = ''
  if (validPercent === 0) {
    gradientCss = `conic-gradient(from -90deg, ${TRACK_COLOR} 0deg, ${TRACK_COLOR} 360deg)`;
  } else if (validPercent === 100) {
    gradientCss = `conic-gradient(from -90deg, ${ACTIVE_COLOR} 0deg, ${ACTIVE_COLOR} 360deg)`;
  } else {
    gradientCss = `conic-gradient(from -90deg, ${ACTIVE_COLOR} 0deg, ${ACTIVE_COLOR} ${finishAngle}deg, ${TRACK_COLOR} ${finishAngle}deg, ${TRACK_COLOR} 360deg)`;
  }

  return gradientCss;
}
</script>

<style lang="scss" scoped>
@mixin flexCenter {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ring-chart {
  width: 180px;
  height: 180px;
  box-shadow: rgb(204, 219, 232) 3px 3px 6px 0 inset, rgba(255, 255, 255, 0.5) -3px -3px 6px 1px inset;
  border-radius: 50%;
  position: relative;
  &:before {
    content: '';
    width: 120px;
    height: 120px;
    background: #fff;
    border-radius: 50%;
    box-shadow: rgb(204, 219, 232, .3) 0 0 0 1px,
                rgb(204, 219, 232, .3) 0 1px 1px -0.5px,
                rgb(204, 219, 232, .3) 0 2px 2px -1px,
                rgb(204, 219, 232, .3) 0 3px 3px -1.5px,
                rgb(204, 219, 232, .3) 0 5px 5px -2.5px,
                rgb(204, 219, 232, .3) 0 10px 10px -5px,
                rgb(204, 219, 232, .3) 0 24px 24px -8px;
    z-index: 2;
    position: absolute;
    left: 30px;
    top: 30px;
  }
  &:after {
    content: '';
    width: calc(100% - 4px);
    height: calc(100% - 4px);
    transition: .4s;
    background: var(--color);
    //background: linear-gradient(135deg, #ffffff 0%, #f8faff 100%);
    border-radius: 50%;
    position: absolute;
    left: 2px;
    top: 2px;
  }
}

.ring-chart-content {
  width: 120px;
  height: 120px;
  background: #fff;
  border-radius: 50%;
  z-index: 3;
  @include flexCenter;
  flex-direction: column;
  position: absolute;
  left: 30px;
  top: 30px;
}

.ring-chart-value {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 1px;
}

.ring-chart-label {
  font-size: 12px;
  color: #888;
  letter-spacing: 1px;
}
</style>