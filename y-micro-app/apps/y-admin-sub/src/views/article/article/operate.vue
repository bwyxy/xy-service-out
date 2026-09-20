<script setup lang="ts">
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleInfo, addArticle, updateArticle } from '@/api/blog/article'
import { getCategorys } from '@/api/blog/category'
import { getTagList } from '@/api/blog/tag'
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { ElMessage } from 'element-plus'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()

const articleId = route.query.id ? Number(route.query.id) : undefined
const isEdit = !!articleId

const formRef = ref()
const submitLoading = ref(false)
const categoryOptions = ref<any[]>([])
const tagOptions = ref<any[]>([])

const form = reactive<any>({
  articleId: undefined,
  title: '',
  slug: '',
  summary: '',
  content: '',
  coverImage: '',
  status: '1',
  categoryId: undefined,
  tagIds: [],
  publishedTime: ''
})

const rules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const statusOptions = [
  { label: '草稿', value: '0' },
  { label: '已发布', value: '1' },
  { label: '私密', value: '2' }
]

const editorRef = ref<any>(null)
const toolbarConfig = {
  toolbarKeys: [
    'bold',
    'italic',
    'underline',
    'through',
    'fontFamily',   // 如果报错，删除
    'fontSize',
    'color',
    'bgColor',
    'headerSelect',
    'blockquote',
    'codeBlock',
    'insertLink',
    'insertImage',
    'insertTable',
    'bulletedList',
    'numberedList',
    'xy',         // 如果报错，删除
    'indent',
    'undo',
    'redo',
    'uploadImage'
  ],
  modalAppendToBody: false,
  excludeKeys: []
}
const editorConfig = {
  placeholder: '请输入文章内容...',
  hoverbarKeys: {
    image: {
      menuKeys: ['imageWidth30', 'imageWidth50', 'blockquote', 'codeBlock','imageWidth100', 'editImage', 'viewImageLink', 'deleteImage'],
    },
  }
}

const loadCategories = async () => {
  const res = await getCategorys({ pageNum: 1, pageSize: 999 })
  if (res.code === 200) {
    categoryOptions.value = res.data?.list || []
  }
}

const loadTags = async () => {
  const res = await getTagList({ pageNum: 1, pageSize: 999 })
  if (res.code === 200) {
    tagOptions.value = res.data?.list || []
  }
}

const loadArticle = async () => {
  if (!articleId) return
  const res = await getArticleInfo(articleId)
  if (res.code === 200) {
    Object.assign(form, res.data)
    if (!form.tagIds) form.tagIds = []
  }
}

const handleCreated = (editor: any) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = { ...form }
    const res = isEdit ? await updateArticle(data) : await addArticle(data)
    if (res.code === 200) {
      ElMessage.success(isEdit ? '修改成功' : '新增成功')
      router.push('/blog/article/article')
    } else {
      ElMessage.error(res.msg || (isEdit ? '修改失败' : '新增失败'))
    }
  } finally {
    submitLoading.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadCategories()
  loadTags()
  loadArticle()
})
</script>

<template>
  <div class="article-operate">
    <div class="card-header">
      <span>{{ isEdit ? '编辑文章' : '添加文章' }}</span>
      <el-button @click="goBack">返回</el-button>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-row :gutter="24">
        <el-col :span="16">
          <el-form-item label="文章标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入文章标题" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%" clearable>
              <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.name" :value="item.categoryId" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发布时间">
            <el-date-picker v-model="form.publishedTime" type="datetime" placeholder="请选择发布时间" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="URL别名">
        <el-input v-model="form.slug" placeholder="请输入URL别名" />
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%" clearable>
          <el-option v-for="item in tagOptions" :key="item.tagId" :label="item.name" :value="item.tagId" />
        </el-select>
      </el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入文章摘要" />
      </el-form-item>
      <el-form-item label="封面">
        <image-upload v-model="form.coverImage" />
      </el-form-item>
      <el-form-item label="正文" prop="content">
        <div class="editor-wrapper">
          <Toolbar style="border-bottom: 1px solid #ccc" :editor="editorRef" :defaultConfig="toolbarConfig" />
          <Editor v-model="form.content" :defaultConfig="editorConfig" style="height: 500px; overflow-y: hidden;" @onCreated="handleCreated" />
        </div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
        <el-button @click="goBack">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.article-operate {
  background: #fff;
  border-radius: 13px;
  box-sizing: border-box;
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-sizing: border-box;
  padding: 0 0 0 18px;
  margin-bottom: 12px;
  span {
    color: #1f1f1f;
    font-size: 18px;
    font-weight: bold;
    letter-spacing: 1px;
  }
}

.editor-wrapper {
  border: 1px solid #ccc;
  z-index: 100;
}
</style>