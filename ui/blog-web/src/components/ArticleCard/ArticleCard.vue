<script setup lang="ts">
import { useRouter } from 'vue-router'
import {Calendar, Clock, Right} from "@element-plus/icons-vue";
import { formatDateTime } from '@bwyxy/utils'

defineProps<{
  article: any
}>()

const router = useRouter()

const toPage = (slug :string, articleId :string) => {
  router.push({
    path: `/article/${slug || articleId || ''}`
  })
}
</script>

<template>
  <div class="article-card" @click="toPage(article.slug, article.articleId)">
    <div class="article-cover">
      <el-image style="width: 100%; height: 100%" :src="article.coverImage" alt="cover" />
    </div>

    <div class="card-main">
      <div class="card-top">
        <div class="card-content">
          <el-icon><Calendar /></el-icon>
          <span>{{ formatDateTime(article.publishedTime, 'yyyy-MM-dd') }}</span>
        </div>
        <div class="card-content">
          <el-icon><Clock /></el-icon>
          <span>2min</span>
        </div>
      </div>
      <div class="article-title">
        <p>{{ article.title }}</p>
      </div>
      <div class="article-summary">
        <p>{{ article.summary }}</p>
      </div>

      <div class="card-btm">
        <div class="card-btm-user">
          <img src="@/assets/images/head.jpg" alt="head" />
          <span>Mr.XiaoYi</span>
        </div>
        <div class="card-read">
          <span>阅读</span>
          <el-icon><Right /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@mixin textEllipsis($num) {
  word-break: break-all;
  word-wrap: break-word;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: $num;
  line-clamp: $num;
  -webkit-box-orient: vertical;
}

.article-card {
  width: 100%;
  min-height: 48px;
  background: #fff;
  border-radius: 20px;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.35s ease;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow:
      0 4px 8px rgba(0, 0, 0, 0.06),
      0 16px 32px rgba(0, 0, 0, 0.1);

    .article-cover :deep(.el-image__inner) {
      transform: scale(1.06);
    }

    .card-read {
      color: #1677ff;

      .el-icon {
        transform: translateX(3px);
      }
    }
  }
}

.article-cover {
  width: 100%;
  height: 220px;
  overflow: hidden;
  border-radius: 20px 20px 0 0;

  :deep(.el-image__inner) {
    transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
  }
}

.card-main {
  padding: 14px 24px 24px 24px;
}

.card-top {
  display: flex;
  margin-bottom: 6px;
}

.card-content {
  font-size: 12px;
  letter-spacing: 0.5px;
  color: #8c95a0;
  display: flex;
  align-items: center;
  margin-left: 8px;

  span {
    margin-left: 4px;
  }

  &:nth-child(1) {
    margin-left: 0;
  }
}

.article-title {
  min-height: 44px;
  margin-bottom: 6px;

  p {
    font-size: 17px;
    font-weight: 700;
    color: #1a1a1a;
    letter-spacing: 0.5px;
    text-align: justify;
    margin: 0;
    line-height: 1.5;
    @include textEllipsis(2);
  }
}

.article-summary {
  font-size: 13px;
  line-height: 1.7;
  letter-spacing: 0.3px;
  margin: 10px 0 20px 0;
  color: #6b7280;
  @include textEllipsis(3);

  p {
    text-align: justify;
    margin: 0;
  }
}

.card-btm {
  border-top: 1px solid #f5f5f5;
  padding-top: 20px;
  display: flex;
  align-items: center;
}

.card-btm-user {
  display: flex;
  align-items: center;

  img {
    width: 30px;
    height: 30px;
    border-radius: 50%;
  }

  span {
    font-size: 13px;
    font-weight: 500;
    letter-spacing: 0.5px;
    margin-left: 8px;
    color: #555;
  }
}

.card-read {
  flex: 1;
  font-size: 13px;
  color: #4096ff;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  transition: color 0.3s ease;

  span {
    margin-right: 6px;
  }

  .el-icon {
    transition: transform 0.3s ease;
  }
}
</style>