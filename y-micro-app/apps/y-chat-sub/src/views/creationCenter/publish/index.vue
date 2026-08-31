<script setup lang="ts">
import {reactive} from "vue";
import {CaretBottom, Upload} from '@element-plus/icons-vue'

const params = reactive({
  title: '',
  content: '',
  coverImage: '',
  visibility: 1
})
const visibilitys = reactive([
  {id: 1, name: '公开'},
  {id: 2, name: '好友可见'},
  {id: 3, name: '私密'}
])
</script>

<template>
  <div class="publish">
    <div class="publish-lt">
      <el-upload
          action="#"
          class="publish-upload"
          :show-file-list="false"
      >
        <div class="option-col">
          <el-icon><Upload /></el-icon>
          <span>上传图文</span>
        </div>
      </el-upload>
    </div>
    <div class="publish-rt">

      <div class="publish-label">标题</div>

      <div class="title-box">
        <el-input v-model="params.title" placeholder="标题" />
      </div>

      <div class="publish-label">内容</div>

      <div class="content-box">
        <el-input
            class="content-box-textarea"
            v-model="params.content"
            type="textarea"
            placeholder="内容"
        />
      </div>

      <div class="publish-option">
        <el-select
            v-model="params.visibility"
            :suffixIcon="CaretBottom"
        >
          <el-option
              v-for="item in visibilitys"
              :key="item.id"
              :value="item.id"
              :label="item.name"
          />
        </el-select>
        <el-button type="primary">提交</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.publish {
  width: 100%;
  height: 100%;
  display: flex;
  box-sizing: border-box;
  padding: size(32);
  &-lt {
    flex: 1;
    box-sizing: border-box;
    padding-right: size(32);
    display: flex;
    flex-direction: column;
  }
  &-rt {
    width: 320px;
    box-sizing: border-box;
    padding-left: size(16);
    display: flex;
    flex-direction: column;
  }
}

.publish-label {
  font-size: 16px;
  font-weight: 700;
  color: #1f1f1f;
  letter-spacing: 2px;
}

.publish-option {
  display: flex;
  :deep(.el-select) {
    width: 110px;
    .el-select__wrapper {
      padding-right: 6px;
    }
    .el-icon {
      color: #52c41a !important;
      font-size: 24px;
    }
  }
  .el-button {
    flex: 1;
    margin-left: 12px;
  }
}

.title-box {
  margin: 14px 0;
}

.content-box {
  flex: 1;
  margin: 14px 0;
  :deep(.el-textarea) {
    width: 100%;
    height: 100%;
    .el-textarea__inner {
      height: 100%;
    }
  }
}

.publish-upload {
  flex: 1;
  background: rgba(0, 0, 0, 0.06);
  box-sizing: border-box;
  border: 1.3px solid rgba(0, 0, 0, .1);
  border-radius: 13px;
  :deep(.el-upload) {
    width: 100%;
    height: 100%;
  }
  .option-col {
    width: 100%;
    height: 100%;
    @include flexCenter;
    flex-direction: column;
    .el-icon {
      font-size: 64px;
      color: #8c8c8c;
    }
    span {
      font-size: 14px;
      letter-spacing: 2px;
    }
  }
}
</style>