<script setup lang="ts">
import { storeToRefs } from 'pinia'
import useAppStore from "@/store/modules/app.ts";
import {ArrowDownBold} from "@element-plus/icons-vue";
import UserBlurb from "@/components/UserBlurb/UserBlurb.vue";
import StatisticsBox from "@/components/StatisticsBox/StatisticsBox.vue";
import RingChart from "@/components/RingChart/RingChart.vue";
import CategoryTags from "@/components/CategoryTags/CategoryTags.vue";
import { categoryList } from '@/api/category.ts'
import {onMounted, reactive, ref} from "vue";
import {articleList} from "@/api/blog.ts";
import ArticleCard from "@/components/ArticleCard/ArticleCard.vue";

const { blogInfo } = storeToRefs(useAppStore())

const searchVal = ref<string>('')
const categorys = ref<any[]>([])
const currentCategoryId = ref<number>(0)
const articles = ref<any[]>([])
const loadingArticles = ref(true)
const pager = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
  categoryId: 0
})

const handleScrollDown = () => {
  window.scrollTo({
    top: window.innerHeight,
    behavior: 'smooth'
  })
}

const getCategoryList = async () => {
  const res = await categoryList({
    keyword: searchVal.value
  })

  if (res.code === 200) {
    categorys.value = res.data
    categorys.value.unshift({
      categoryId: 0,
      name: '全部'
    })
  }
}

const changeCategory = (id: number) => {
  pager.pageNum = 1
  pager.categoryId = id
  getArticleList()
}

const getArticleList = async () => {
  loadingArticles.value = true

  const res = await articleList(pager)

  if (res.code === 200) {
    articles.value = res.data.list
    pager.total = res.data.total
  }

  loadingArticles.value = false
}

const handlePageChange = (page: number) => {
  pager.pageNum = page
  getArticleList()
  window.scrollTo({
    top: window.innerHeight + 100,
    behavior: 'smooth'
  })
}

const initPage = () => {
  getCategoryList()
  getArticleList()
}

onMounted(() => {
  initPage()
})
</script>

<template>
  <div class="home">
    <div class="home-banner">
      <img class="home-banner-bg" src="@/assets/images/banner.jpg" alt="banner" />
      <div class="banner-overlay"></div>

      <div class="banner-particles">
        <span
          v-for="i in 20"
          :key="i"
          class="banner-particle"
          :style="{
            left: (Math.random() * 100) + '%',
            animationDelay: (Math.random() * 6) + 's',
            animationDuration: (6 + Math.random() * 8) + 's',
            width: (2 + Math.random() * 3) + 'px',
            height: (2 + Math.random() * 3) + 'px',
            opacity: 0.15 + Math.random() * 0.35,
          }"
        ></span>
      </div>

      <div class="home-banner-content">
        <div class="blog-title-wrapper">
          <span class="blog-title-line"></span>
          <h1 class="blog-title">{{ blogInfo.bannerTitle || '' }}</h1>
          <span class="blog-title-line"></span>
        </div>
        <p class="blog-moco">{{ blogInfo.motto || '' }}</p>

        <div class="banner-arrow-btm" @click="handleScrollDown">
          <div class="arrow-ring">
            <el-icon><ArrowDownBold /></el-icon>
          </div>
          <span class="arrow-label">探索</span>
        </div>
      </div>
    </div>

    <div class="home-content-wrapper">
      <div class="home-main innerContent">
        <div class="home-main-lt">
          <user-blurb />

          <statistics-box title="最近评论" />

          <statistics-box title="热门文章排行" />

          <statistics-box>
            <div class="chart-box">
              <ring-chart label="今日访问量" />
            </div>
          </statistics-box>

          <statistics-box>
            <div class="chart-box">
              <ring-chart color="#1668dc" label="网站访问量" />
            </div>
          </statistics-box>
        </div>

        <div class="home-main-rt">
          <category-tags
            v-model="currentCategoryId"
            :categoryList="categorys"
            @change="changeCategory"
          />

          <div class="article-main">
            <template v-if="loadingArticles">
              <div v-for="n in 4" :key="n" class="article-item">
                <div class="article-skeleton">
                  <div class="skeleton-cover"></div>
                  <div class="skeleton-body">
                    <div class="skeleton-line short"></div>
                    <div class="skeleton-line medium"></div>
                    <div class="skeleton-line long"></div>
                    <div class="skeleton-line medium"></div>
                  </div>
                </div>
              </div>
            </template>

            <template v-else>
              <div
                v-for="(item, index) in articles"
                :key="item.articleId"
                class="article-item"
                :style="{ animationDelay: (index * 0.08) + 's' }"
              >
                <article-card :article="item" />
              </div>
            </template>
          </div>

          <div v-if="articles.length === 0 && !loadingArticles" class="empty-state">
            <span class="empty-icon">📝</span>
            <p>暂无文章</p>
          </div>

          <div v-if="pager.total > 0" class="article-pagination">
            <el-pagination
              background
              :pager-count="5"
              layout="prev, pager, next"
              :total="pager.total"
              :page-size="pager.pageSize"
              :current-page="pager.pageNum"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@mixin flexCenter {
  display: flex;
  align-items: center;
  justify-content: center;
}

@mixin cardBase {
  background: #fff;
  border-radius: 20px;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.04);
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow:
      0 2px 4px rgba(0, 0, 0, 0.06),
      0 8px 24px rgba(0, 0, 0, 0.08);
  }
}

.home {
  background: linear-gradient(180deg, #f5f6fa 0%, #eef0f5 50%, #f5f6fa 100%);
  padding-bottom: 48px;
  min-height: 100vh;
}

.home-banner {
  width: 100%;
  height: 100vh;
  position: relative;
  overflow: hidden;

  &-bg {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.35) 0%,
    rgba(0, 0, 0, 0.15) 40%,
    rgba(0, 0, 0, 0.35) 100%
  );
  z-index: 1;
}

.banner-particles {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  overflow: hidden;
}

.banner-particle {
  position: absolute;
  bottom: -10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  animation: particleFloat linear infinite;
}

@keyframes particleFloat {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 0.5;
  }
  90% {
    opacity: 0.3;
  }
  100% {
    transform: translateY(-100vh) scale(0.3);
    opacity: 0;
  }
}

.home-banner-content {
  position: absolute;
  inset: 0;
  z-index: 2;
  @include flexCenter;
  flex-direction: column;
  padding-bottom: 100px;
  box-sizing: border-box;
}

.blog-title-wrapper {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 22px;
}

.blog-title-line {
  display: block;
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
}

.blog-title {
  font-size: 48px;
  color: #fff;
  font-weight: 800;
  letter-spacing: 6px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  margin: 0;
}

.blog-moco {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 400;
  letter-spacing: 4px;
  margin-top: 0;
  margin-bottom: 48px;
}

.banner-arrow-btm {
  position: absolute;
  bottom: 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.arrow-ring {
  width: 44px;
  height: 44px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  @include flexCenter;
  font-size: 18px;
  color: #fff;
  transition: all 0.3s ease;
  animation: arrowBounce 2s ease-in-out infinite;

  &:hover {
    border-color: rgba(255, 255, 255, 0.6);
    background: rgba(255, 255, 255, 0.08);
  }
}

.arrow-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 3px;
}

@keyframes arrowBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(8px); }
}

.home-content-wrapper {
  position: relative;
  z-index: 3;
  margin-top: -40px;
  padding-top: 24px;
}

.home-main {
  display: flex;
  gap: 24px;

  &-lt {
    width: 320px;
    min-height: 120px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
  }

  &-rt {
    flex: 1;
    min-width: 0;
  }
}

.chart-box {
  margin: 24px 0;
}

.article-main {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.article-item {
  width: calc(50% - 12px);
  animation: cardReveal 0.5s ease both;
}

@keyframes cardReveal {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.empty-state {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  color: #999;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  p {
    font-size: 14px;
    letter-spacing: 1px;
  }
}

.article-skeleton {
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
}

.skeleton-cover {
  height: 200px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-body {
  padding: 20px 24px 24px;
}

.skeleton-line {
  height: 14px;
  border-radius: 7px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  margin-bottom: 12px;

  &.short { width: 40%; }
  &.medium { width: 75%; }
  &.long { width: 100%; }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.article-pagination {
  width: 100%;
  display: flex;
  justify-content: center;
  margin: 36px 0 0 0;

  :deep(.el-pagination) {
    width: 100%;

    .el-pager {
      flex: 1;
      justify-content: center;

      .number {
        width: 40px;
        height: 40px;
        font-size: 16px;
        border-radius: 50px;
        background: #fff;
        box-shadow:
          0 1px 2px rgba(0, 0, 0, 0.04),
          0 4px 12px rgba(0, 0, 0, 0.04);
        transition: all 0.3s ease;

        &:hover {
          color: #4096ff;
        }
      }

      .is-active {
        background: #4096ff;
        color: #fff;
        box-shadow: 0 4px 16px rgba(64, 150, 255, 0.3);
      }
    }

    .btn-next, .btn-prev {
      width: 40px;
      height: 40px;
      font-size: 16px;
      border-radius: 50px;
      background: #fff;
      box-shadow:
        0 1px 2px rgba(0, 0, 0, 0.04),
        0 4px 12px rgba(0, 0, 0, 0.04);
      transition: all 0.3s ease;
    }
  }
}

@media (max-width: 1024px) {
  .home-main {
    flex-direction: column;

    &-lt {
      width: 100%;
      max-width: 400px;
      margin: 0 auto;
    }

    &-rt {
      margin-left: 0;
    }
  }

  .article-item {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .blog-title-wrapper {
    gap: 16px;
  }

  .blog-title-line {
    width: 32px;
  }

  .blog-title {
    font-size: 32px;
    letter-spacing: 4px;
  }

  .blog-moco {
    font-size: 15px;
    letter-spacing: 2px;
  }

  .article-item {
    width: 100%;
  }
}
</style>