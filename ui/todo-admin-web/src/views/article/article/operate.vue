<script setup>
import {onMounted, onUnmounted, reactive, ref} from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getArticleById, addArticle, updateArticle } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'
import UploadImage from "@/components/UploadImage/UploadImage.vue";
import EditorBox from "@/components/EditorBox/EditorBox.vue";

const route = useRoute()
const router = useRouter()
const statusType = reactive([
  { value: '0', label: '草稿' },
  { value: '1', label: '已发布' },
  { value: '2', label: '私密' },
])
const params = reactive({
  articleId: null,
  title: '',
  coverImage: '',
  publishedTime: '',
  categoryId: '',
  tagIds: [],
  summary: '',
  slug: '',
  content: '',
  status: '0'
})

const rules = reactive({
  title: [{ required: true, message: '标题不能为空', trigger: 'blur'}]
})
const categoryList = ref([])
const tagList = ref([])
const isLoading = ref(false)
const isTagLoading = ref(false)
const formRefs = ref(null)

const getArticleInfo = async (id) => {
  try {
    const info = await getArticleById(id)

    params.title = info.title || ''
    params.coverImage = info.coverImage || ''
    params.publishedTime = info.publishedTime || ''
    params.categoryId = info.categoryId || ''
    params.tagIds = info.tagIds || []
    params.summary = info.summary || ''
    params.slug = info.slug || ''
    params.content = info.content || ''
    params.status = info.status || '0'
  } catch (e) {
    console.error(e)
  }
}

const remoteCategoryMethod = async (query) => {
  if (query !== '') {
    isLoading.value = true
    const res = await getCategoryList({
      pageNum: 1,
      pageSize: 10,
      name: query
    })

    isLoading.value = false
    categoryList.value = res.list
  } else {
    categoryList.value = []
  }
}

const remoteTagMethod = async (query) => {
  if (query !== '') {
    isTagLoading.value = true

    const res = await getTagList({
      pageNum: 1,
      pageSize: 10,
      name: query
    })

    isTagLoading.value = false
    tagList.value = res.list
  } else {
    tagList.value = []
  }
}

const handleSubmit = async () => {
  const valid = await formRefs.value.validate()

  if (!valid) return;

  if (params.articleId) {
    await updateArticle(params)
  } else {
    await addArticle(params)
  }
  resetData()
  router.back()
}

const resetData = () => {
  params.articleId = null
  params.title = ''
  params.coverImage = ''
  params.publishedTime = ''
  params.categoryId = ''
  params.tagIds = []
  params.summary = ''
  params.slug = ''
  params.content = ''
  params.status = '0'
}

onMounted(() => {
  if (route.query.id) {
    const id = parseInt(route.query.id)
    params.articleId = id
    getArticleInfo(id)
  }
})

onUnmounted(() => {
  resetData()
})

</script>

<template>
  <div class="app-container">
    <el-form :model="params" ref="formRefs" :rules="rules" label-width="80px">
      <el-form-item label="文章标题" prop="title">
        <el-input v-model="params.title" placeholder="文章标题" />
      </el-form-item>
      <el-form-item label="URL别名" prop="slug">
        <el-input v-model="params.slug" placeholder="URL别名" />
      </el-form-item>
      <el-form-item label="封面" prop="coverImage">
        <upload-image v-model="params.coverImage" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select
            v-model="params.categoryId"
            placeholder="分类"
            style="flex: 1"
            :loading="isLoading"
            filterable
            remote
            :remote-method="remoteCategoryMethod"
        >
          <el-option
            v-for="item in categoryList"
            :key="item.categoryId"
            :value="item.categoryId"
            :label="item.name"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标签" type="Array" prop="tagIds">
        <div style="flex: 1; display: flex;align-items: center">
          <el-select
              v-model="params.tagIds"
              suffix-icon="Search"
              style="flex: 1"
              placeholder="标签"
              :loading="isTagLoading"
              filterable
              multiple
              remote
              :remote-method="remoteTagMethod"
          >
            <el-option
              v-for="item in tagList"
              :key="item.tagId"
              :value="item.tagId"
              :label="item.name"
            />
          </el-select>
          <el-button type="primary" icon="Plus" style="margin-left: 14px">添加标签</el-button>
        </div>
      </el-form-item>
      <el-form-item label="发布时间" prop="publishedTime">
        <el-date-picker
            v-model="params.publishedTime"
            style="flex: 1"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="发布时间"
        />
      </el-form-item>
      <el-form-item label="摘要" prop="summary">
        <el-input v-model="params.summary" type="textarea" placeholder="摘要" />
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <editor-box v-model="params.content"></editor-box>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="params.status">
          <el-radio v-for="item in statusType" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button @click="router.back()">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">

</style>
