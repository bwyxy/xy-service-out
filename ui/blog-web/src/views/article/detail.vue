<script setup lang="ts">
import { useRoute } from 'vue-router'
import {onMounted, ref} from "vue";
import { formatDateTime } from '@bwyxy/utils'
import { articleInfo } from '@/api/blog.ts'
import {getComments, sendComment} from '@/api/comment.ts'
import {Calendar, Clock} from "@element-plus/icons-vue";
import WordPad from "@/components/WordPad/WordPad.vue";
import {ElMessage} from "element-plus";
import CommentItem from "@/components/CommentItem/CommentItem.vue";

const route = useRoute()
const emoji = ref<string>('🦐')
const article = ref<any>({})
const comments = ref<any[]>([])

const getArticleInfo = async (slug: string) => {
  const res :R<any> = await articleInfo(slug)

  if (res.code === 200) {
    article.value = res.data
    await getCommentTree()
  }
}
/** 获取评论树 */
const getCommentTree = async () => {
  const res :any = await getComments({
    commentType: "article",
    articleId: article.value.articleId
  })

  if (res.code === 200) {
    comments.value = res.data
  }
}
/** 评论 */
const handleComment = async (form :any) => {
  const params = {
    avatarEmoji: emoji.value,
    nickname: form.nickname,
    email: form.email,
    content: form.content,
    commentType: 'article',
    articleId: article.value.articleId,
    parentId: 0
  }
  const res :any = await sendComment(params)

  if (res.code === 200) {
    // @ts-ignore
    comments.value.push(res.data)
    ElMessage.success("感谢您的评论")
  }
}

onMounted(() => {
  const url = route.params.url || ''

  if (typeof url === "string" && url) {
    getArticleInfo(url)
  }
})
</script>

<template>
  <div class="article-detail">
    <div class="container">
      <div class="post-card">
        <!-- 封面图 -->
        <img class="post-cover" :src="article.coverImage || ''" alt="静谧书房，阳光洒落" loading="lazy">

        <div class="post-body">
          <div class="post-meta">
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDateTime(article.publishedTime || new Date(), 'yyyy-MM-dd') }}
            </span>
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              阅读约 2 分钟
            </span>
          </div>
          <h1 class="post-title">{{ article.title }}</h1>

          <div class="author-bar">
            <div class="author-info">
              <img class="avatar" src="@/assets/images/head.jpg" alt="作者头像">
              <div class="author-details">
                <span class="author-name">Mr.XiaoYi</span>
              </div>
            </div>
          </div>

          <!-- 文章正文 -->
          <div class="article-content">
            <p style="text-align: justify">{{ article.summary }}</p>
            <div class="article-content-text editor-content-view" v-html="article.content"></div>
          </div>

          <!-- 标签 -->
          <div class="post-tags">
            <i class="fas fa-tags" style="color:#b7a079;"></i>
            <span class="tag">设计哲学</span>
            <span class="tag">CSS精粹</span>
            <span class="tag">用户体验</span>
            <span class="tag">博客搭建</span>
          </div>

          <!-- 喜欢 + 收藏互动 -->
<!--          <div class="engagement">-->
<!--            <button class="like-btn" id="likeButton"><i class="far fa-heart"></i> <span id="likeCount">128</span> 喜欢</button>-->
<!--            <button class="bookmark-btn" id="bookmarkBtn"><i class="far fa-bookmark"></i> 收藏</button>-->
<!--          </div>-->

<!--          <div class="textarea-box">-->
<!--            <textarea id="textarea-box-input" placeholder="文明评论" />-->

<!--            <div class="textarea-box-footer">-->
<!--              <div class="send-btn">-->
<!--                <span>发送</span>-->
<!--              </div>-->
<!--            </div>-->
<!--          </div>-->

          <div class="word-footer">
            <word-pad
                theme="light"
                v-model="emoji"
                placeholder="在这里评论"
                @submit="handleComment"
            />
          </div>

          <div class="comment-main">
            <div v-for="(item, index) in comments" :key="index">
              <comment-item :item="item" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss">
.article-detail {
  width: 100%;
  min-height: 100vh;
  background: #f7f5f0;
}

/* 平滑滚动 & 优雅滚动条 */
::-webkit-scrollbar {
  width: 8px;
}
::-webkit-scrollbar-track {
  background: #e2ddd4;
  border-radius: 10px;
}
::-webkit-scrollbar-thumb {
  background: #b9aa97;
  border-radius: 10px;
}

/* 容器最大宽度 */
.container {
  max-width: 1250px;
  margin: 0 auto;
  padding: 14px 0 24px;
}

/* 文章卡片主体 (细腻阴影+圆润边角) */
.post-card {
  background: #ffffff;
  border-radius: 32px;
  box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  transition: transform 0.2s ease;
}

/* 封面图区域 */
.post-cover {
  width: 100%;
  max-height: 460px;
  object-fit: contain;
  display: block;
  transition: filter 0.3s;
}

/* 文章主体内边距 */
.post-body {
  box-sizing: border-box;
  padding: 2.5rem 2.8rem;
}

/* 文章元数据行 */
.post-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.8rem;
  font-size: 0.85rem;
  color: #6f6a60;
  border-bottom: 1px solid #edeae3;
  padding-bottom: 1.2rem;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.meta-item i {
  font-size: 0.9rem;
  width: 1.2rem;
  color: #bfaa8a;
}

.category {
  background: #f2ede5;
  padding: 0.2rem 0.9rem;
  border-radius: 40px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #7a5a3a;
  letter-spacing: 0.3px;
}

/* 文章标题 */
.post-title {
  font-family: 'Source Serif 4', Georgia, serif;
  font-weight: 700;
  font-size: 2.6rem;
  line-height: 1.25;
  margin-bottom: 1.2rem;
  color: #211f1c;
  letter-spacing: -0.3px;
}

/* 作者栏 */
.author-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 2rem;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid #f0ece4;
}
.author-info {
  display: flex;
  align-items: center;
  gap: 14px;
}
.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 5px 12px rgba(0, 0, 0, 0.05);
}
.author-details {
  display: flex;
  flex-direction: column;
}
.author-name {
  font-weight: 700;
  font-size: 1.05rem;
  color: #2b3b4c;
}
.author-bio {
  font-size: 0.75rem;
  color: #88806f;
}
.social-share {
  display: flex;
  gap: 12px;
}
.social-share a {
  background: #f3f0ea;
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #6b5a45;
  transition: all 0.2s;
  font-size: 1rem;
  text-decoration: none;
}
.social-share a:hover {
  background: #e5d9cc;
  color: #4d3a27;
  transform: translateY(-2px);
}

/* 正文排版 */
.article-content {
  font-family: 'Source Serif 4', Georgia, 'Times New Roman', serif;
  font-size: 1.125rem;
  line-height: 1.75;
  color: #2e3b3e;
}
.article-content p {
  margin-bottom: 1.6rem;
}
.article-content h2 {
  font-family: 'Inter', sans-serif;
  font-weight: 600;
  font-size: 1.8rem;
  margin: 2rem 0 1rem;
  letter-spacing: -0.2px;
  color: #2a3a44;
}
.article-content h3 {
  font-family: 'Inter', sans-serif;
  font-weight: 600;
  font-size: 1.4rem;
  margin: 1.6rem 0 0.8rem;
  color: #3d4f5c;
}
.article-content blockquote {
  border-left: 5px solid #dcb47c;
  background: #fefaf5;
  padding: 1.2rem 1.8rem;
  margin: 1.8rem 0;
  font-style: normal;
  color: #4a5b62;
  border-radius: 18px;
  font-weight: 400;
  box-shadow: inset 0 1px 4px rgba(0, 0, 0, 0.02), 0 2px 7px rgba(0, 0, 0, 0.02);
}
.article-content code {
  background: #f2efe7;
  padding: 0.2rem 0.4rem;
  border-radius: 8px;
  font-size: 0.9em;
  font-family: 'JetBrains Mono', monospace;
  color: #bc6f42;
}
.article-content pre {
  background: #1e2a2e;
  color: #ebdbbd;
  padding: 1.2rem;
  border-radius: 20px;
  overflow-x: auto;
  margin: 1.6rem 0;
  font-size: 0.85rem;
}
.article-content img {
  max-width: 100%;
  border-radius: 24px;
  margin: 1.5rem 0;
  box-shadow: 0 12px 28px -12px rgba(0, 0, 0, 0.15);
}
.article-content hr {
  margin: 2rem 0;
  border: none;
  height: 1px;
  background: linear-gradient(to right, #e7dfd3, transparent);
}

/* 标签区域 */
.post-tags {
  margin-top: 2.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.tag {
  background: #f0ece4;
  padding: 0.35rem 1rem;
  border-radius: 40px;
  font-size: 0.7rem;
  font-weight: 500;
  color: #786e5c;
  transition: all 0.2s;
}
.tag:hover {
  background: #e5d8ca;
  color: #4c3e2c;
}

/* 点赞&互动区 */
.engagement {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin: 2rem 0 2rem;
  padding: 1rem 0;
  border-top: 1px solid #f0e9e0;
  border-bottom: 1px solid #f0e9e0;
}
.like-btn, .bookmark-btn {
  background: transparent;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  padding: 8px 18px;
  border-radius: 60px;
  cursor: pointer;
  transition: all 0.2s;
  background: #faf8f3;
  color: #5e5544;
}
.like-btn i, .bookmark-btn i {
  font-size: 1.1rem;
}
.like-btn:hover, .bookmark-btn:hover {
  background: #ede5db;
  transform: translateY(-2px);
}
.liked {
  color: #cd5c5c;
}
.liked i {
  font-weight: 900;
}

.word-footer {
  margin-top: 48px;
}

.comment-main {
  padding-top: 48px;
  margin-top: 48px;
  border-top: 1px dashed #ccc;
}
</style>