<script setup lang="ts">

</script>

<template>
  <div class="user-blurb">
    <div class="user-blurb-head">
      <div class="user-head">
        <img src="@/assets/images/head.jpg" alt="head" />
      </div>
      <div class="user-blurb-name">Mr.XiaoYi</div>
    </div>
    <div class="user-blurb-statistics">
      <div class="statistics-item">
        <div class="statistics-item-content">
          <span class="num-unit">999</span>
          <span>文章数</span>
        </div>
      </div>
      <div class="statistics-item">
        <div class="statistics-item-content">
          <span class="num-unit">999</span>
          <span>点赞量</span>
        </div>
      </div>
      <div class="statistics-item">
        <div class="statistics-item-content">
          <span class="num-unit">999</span>
          <span>友链</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@mixin flexCenter {
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-blurb {
  width: 100%;
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  border-radius: 20px;
  box-sizing: border-box;
  padding: 40px 32px 32px;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow:
      0 2px 4px rgba(0, 0, 0, 0.06),
      0 8px 24px rgba(0, 0, 0, 0.08);
  }
}

.user-head {
  width: 72px;
  height: 72px;
  box-shadow:
    rgb(204, 219, 232) 3px 3px 6px 0 inset,
    rgba(255, 255, 255, 0.5) -3px -3px 6px 1px inset,
    0 4px 16px rgba(64, 150, 255, 0.12);
  border-radius: 50%;
  cursor: pointer;
  padding: 4px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    transition: transform 0.4s ease;
  }

  &:hover {
    img {
      transform: scale(1.15);
    }
  }
}

.user-blurb-head {
  width: 100%;
  box-sizing: border-box;
  padding-bottom: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.user-blurb-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #1a1a1a;
}

.user-blurb-statistics {
  display: flex;
  border-top: 1px solid #f0f2f5;
  padding-top: 20px;
}

.statistics-item {
  flex: 1;
  @include flexCenter;
  flex-direction: column;

  span:nth-child(1) {
    font-size: 24px;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: #1a1a1a;
  }

  span:nth-child(2) {
    font-size: 11px;
    color: #999;
    letter-spacing: 0.5px;
    margin-top: 2px;
  }

  &:nth-child(1) {
    align-items: flex-start;
  }

  &:last-child {
    align-items: flex-end;
  }

  &-content {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
}

.num-unit {
  position: relative;
  background: linear-gradient(135deg, #4096ff, #1677ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;

  &:after {
    content: '+';
    font-size: 12px;
    background: linear-gradient(135deg, #4096ff, #1677ff);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    position: absolute;
    right: -10px;
    top: -1px;
  }
}
</style>