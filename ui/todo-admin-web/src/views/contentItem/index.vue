<script setup>
import { getContentItems, addContentItem, updateContentItem, getContentItemById, deleteContentItem } from '@/api/contentItem'
import {onMounted, reactive, ref} from "vue";
import ModelDialog from "@/components/ModelDialog/ModelDialog.vue";
import EditorBox from "@/components/EditorBox/EditorBox.vue";
import UploadImage from "@/components/UploadImage/UploadImage.vue";
import { ElMessageBox } from 'element-plus'

const types = reactive([
  { type: 'link',label: '友链' },
  { type: 'work',label: '工作经验' },
  { type: 'project',label: '项目经验' },
  { type: 'notice',label: '公告' },
  { type: 'meProject', label: '个人项目' },
  { type: 'portfolio', label: '作品集' }
])

const params = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  type: '',
  total: 0,
  loading: false
})
const formParams = reactive({
  contentId: null,
  title: '',
  content: '',
  type: 'link',
  description: '',
  linkUrl: '',
  coverImage: '',
  techStack: '',
  images: '',
  startDate: '',
  endDate: '',
  sortOrder: 0,
  status: 1,
  extraData: ''
})

const isShow = ref(false)
const contentItemList = ref([])
const queryRef = ref(null)
const formRefs = ref(null)

const getContentItemList = async () => {
  params.loading = true
  try {
    const res = await getContentItems(params)

    contentItemList.value = res.list
    params.total = res.total
  } catch (err) {
    console.log(err)
  } finally {
    params.loading = false
  }
}

const reset = () => {
  queryRef.value.resetFields()
  getContentItemList()
}

const textContentType = (type) => {
  const item = types.find(item => item.type === type)

  return item.label || ''
}

const optionContentItem = (contentId) => {
  if (contentId) {
    formParams.contentId = contentId
    setFormValue(contentId)
  }
  isShow.value = true
}

const setFormValue = async (id) => {
  try {
    const res = await getContentItemById(id)

    const info = res.data

    formParams.title = info.title || ''
    formParams.type = info.type  || ''
    formParams.content = info.content  || ''
    formParams.description = info.description  || ''
    formParams.linkUrl = info.linkUrl  || ''
    formParams.coverImage = info.coverImage  || ''
    formParams.techStack = info.techStack  || ''
    formParams.images = info.images  || ''
    formParams.startDate = info.startDate  || ''
    formParams.endDate = info.endDate  || ''
    formParams.sortOrder = info.sortOrder || ''
    formParams.status = info.status
    formParams.extraData = info.extraData || ''

  } catch (err) {
    console.error(err)
  }
}

const handleDel = (id) => {
  ElMessageBox.confirm(
      `是否确认删除${id}?`,
      '是否删除',
      {
        type: 'warning'
      }
  ).then(async () => {
    await deleteContentItem(id)

    await getContentItemList()
  }).catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRefs.value.validate()

  if (!valid) return;

  if (formParams.contentId) {
    await updateContentItem(formParams)
  } else {
    await addContentItem(formParams)
  }

  handleClose()
  await getContentItemList()
}

const handleClose = () => {
  isShow.value = false
  formParams.contentId = null
  formRefs.value.resetFields()

}

onMounted(() => {
  getContentItemList()
})
</script>

<template>
  <div class="app-container">
    <el-form :model="params" ref="queryRef" :inline="true">
      <el-form-item label="标题" prop="title">
        <el-input v-model="params.title" placeholder="标题" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="params.type" placeholder="选择类型">
          <el-option
            v-for="item in types"
            :key="item.type"
            :value="item.type"
            :label="item.label"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getContentItemList">搜索</el-button>
        <el-button icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="optionContentItem('')"
        >新增</el-button>
      </el-col>
    </el-row>

    <el-table
        v-loading="params.loading"
        :data="contentItemList"
        row-key="contentId"
    >
      <el-table-column property="contentId" label="ID" align="center" />
      <el-table-column property="title" label="标题" align="center" />
      <el-table-column label="类型" align="center">
        <template #default="{ row }">
          <span>{{ textContentType(row.type) }}</span>
        </template>
      </el-table-column>
      <el-table-column property="description" label="描述" show-overflow-tooltip align="center" />
      <el-table-column property="content" label="内容" show-overflow-tooltip align="center" />
      <el-table-column label="操作" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="optionContentItem(row.contentId)">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDel(row.contentId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="total-pagination">
      <el-pagination
          v-model:current-page="params.pageNum"
          :page-size="params.pageSize"
          background
          layout="total, prev, pager, next"
          :total="params.total"
          @current-change="getContentItemList"
      />
    </div>

    <model-dialog v-model="isShow" width="80%" :title="`${formParams.contentId ? '编辑' : '添加'}配置`" @close="handleClose">
      <el-form :model="formParams" ref="formRefs" class="content-item" label-width="70px">
        <div class="content-item-lt">
          <el-form-item label="标题" prop="title">
            <el-input v-model="formParams.title" placeholder="标题" />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-radio-group v-model="formParams.type">
              <el-radio v-for="item in types" :key="item.type" :label="item.type">{{ item.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="关联链接" prop="linkUrl">
            <el-input v-model="formParams.linkUrl" placeholder="关联链接" />
          </el-form-item>
          <el-form-item label="技术栈" prop="techStack">
            <el-input v-model="formParams.techStack" placeholder="技术栈: 多个用;分开" />
          </el-form-item>
          <el-form-item label="开始时间" prop="startDate">
            <el-date-picker v-model="formParams.startDate" placeholder="开始时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
          <el-form-item label="结束时间" prop="endDate">
            <el-date-picker v-model="formParams.endDate" placeholder="结束时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
          <el-form-item label="封面图" prop="coverImage">
            <upload-image v-model="formParams.coverImage" />
          </el-form-item>
          <el-form-item label="图片集" prop="images">
            <upload-image v-model="formParams.images" :multiple="true" />
          </el-form-item>
        </div>
        <div class="content-item-rt">
          <div class="content-item-rt_top">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="formParams.sortOrder" placeholder="排序"></el-input-number>
            </el-form-item>
            <el-form-item label="描述" prop="description">
              <el-input v-model="formParams.description" type="textarea" placeholder="描述" />
            </el-form-item>
          </div>
          <div class="content-item-rt_btm">
            <el-form-item label-width="0" prop="content">
              <editor-box v-model="formParams.content"></editor-box>
            </el-form-item>
            <el-form-item label="是否显示" prop="status">
              <el-radio-group v-model="formParams.status">
                <el-radio :label="1">显示</el-radio>
                <el-radio :label="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="拓展字段" prop="extraData">
              <el-input v-model="formParams.extraData" type="textarea" placeholder="拓展字段" />
            </el-form-item>
          </div>
        </div>
      </el-form>
      <template #footer>
        <div style="display: flex;justify-content: center;">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确认</el-button>
        </div>
      </template>
    </model-dialog>
  </div>
</template>

<style scoped lang="scss">
.content-item {
  display: flex;
  //align-items: flex-start;
}

.content-item-lt {
  width: calc(50% - 8px);
  box-shadow: rgba(50, 50, 93, 0.25) 0 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
  border-radius: 8px;
  box-sizing: border-box;
  padding: 13px;
  min-height: 200px;
  margin-right: 8px;
}

.content-item-rt {
  width: calc(50% - 8px);
  min-height: 120px;
  margin-left: 8px;
  &_top {
    width: 100%;
    background: #fff;
    box-shadow: rgba(50, 50, 93, 0.25) 0 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
    margin-bottom: 16px;
    box-sizing: border-box;
    padding: 13px;
    border-radius: 8px;
  }
  &_btm {
    width: 100%;
    background: #fff;
    box-shadow: rgba(50, 50, 93, 0.25) 0 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
    box-sizing: border-box;
    padding: 13px;
    border-radius: 8px;
  }
}
</style>
