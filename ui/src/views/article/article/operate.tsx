import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {Button, DatePicker, Form, Image, Input, message, Radio, Space, Upload} from 'antd'
import { UploadProps, RcFile } from 'antd/es/upload'
import { Editor, Toolbar } from '@wangeditor/editor-for-react'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import dayjs from 'dayjs'

import {
  addArticle,
  getArticleById,
  updateArticle,
  type ArticleCreate,
} from '@/api/article'
import {updateImage} from "@/api/upload.ts";
import {PlusOutlined} from "@ant-design/icons";

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '私密' },
]

interface ArticleFormValues {
  title: string
  slug?: string
  summary?: string
  coverImage?: string
  status?: number
  authorId?: number
  categoryId?: number
  publishedTime?: string
}

const defaultEditorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入文章正文...',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: (url: string, alt?: string, href?: string) => void) {
        try {
          const formData = new FormData();
          formData.append('file', file);

          const response :R<any> = await updateImage(formData)

          if (response.code === 200) {
            insertFn(response.data, file.name, response.data);
          }
        } catch (error: any) {
          // 7. 错误处理
          console.error('图片上传失败:', error);
        }
      }
    },
  },
  hoverbarKeys: {
    image: {
      menuKeys: ['imageWidth30', 'imageWidth50', 'imageWidth100', 'editImage', 'viewImageLink', 'deleteImage'],
    },
  },
}

const defaultToolbarConfig: Partial<IToolbarConfig> = {
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
    'todo',         // 如果报错，删除
    'indent',
    'undo',
    'redo',
    'uploadImage'
  ],
  modalAppendToBody: false,
  excludeKeys: []
}

// function isEmptyHtml(html: string) {
//   if (!html) return true
//   const trimmed = html.replace(/<(br|p)\s*\/?>(\s*)?/gi, '').replace(/<[^>]+>/g, '').trim()
//   return trimmed === ''
// }

export default function ArticleOperate() {
  const [form] = Form.useForm<ArticleFormValues>()
  const [coverImage, setCoverImage] = useState('')

  const navigate = useNavigate()
  const [searchParams] = useSearchParams();
  const articleId = searchParams.get('articleId');

  // const params = useParams<{ id?: string }>()
  // const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [editor, setEditor] = useState<IDomEditor | null>(null)
  const [content, setContent] = useState('<p><br></p>')
  // const [contentError, setContentError] = useState<string | null>(null)

  // const isEdit = Boolean(params.id)

  useEffect(() => {
    form.resetFields()
    setContent('<p><br></p>')
    if (articleId) {
      loadArticle(articleId)
      return
    }
  }, [])

  const loadArticle = async (articleId :any) => {
    try {
      const data = await getArticleById(articleId)

      // console.log(data)
      form.setFieldsValue({
        title: data.title,
        slug: data.slug,
        summary: data.summary,
        coverImage: data.coverImage,
        status: data.status,
        authorId: data.authorId,
        categoryId: data.categoryId,
        publishedTime: data.publishedTime,
      })
      if (data.coverImage) {
        setCoverImage(data.coverImage)
      }
      setContent(data.content || '<p><br></p>')
    } catch (error) {
      console.error('获取文章详情失败:', error)
    }
  }

  const customRequest: UploadProps['customRequest'] = async ({ file, onSuccess, onError }) => {
    const formData = new FormData();
    formData.append('file', file as RcFile);

    try {
      // @ts-ignore
      const res :R<string> = await updateImage(formData)

      if (res.code === 200) {
        form.setFieldsValue({
          coverImage: res.data
        })
        setCoverImage(res.data)
        onSuccess?.(res.data)
      }
    } catch (error) {
      onError?.(error as Error);
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      if (articleId) {
        await updateArticle({
          articleId: Number(articleId),
          title: values.title,
          content: content,
          slug: values.slug,
          summary: values.summary,
          coverImage: coverImage,
          status: values.status,
          authorId: values.authorId,
          categoryId: values.categoryId,
          publishedTime: values.publishedTime,
        })
        message.success('文章更新成功')
      } else {
        await addArticle({
          ...values,
          content: content,
        } as ArticleCreate)
        message.success('文章新增成功')
      }
      navigate(-1)
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
      <div>
        <Form form={form} labelAlign="left" style={{position: "relative"}}>
          <Form.Item
              name="title"
              label="文章标题"
              rules={[{ required: true, message: '请输入文章标题' }]}
          >
            <Input placeholder="请输入文章标题" />
          </Form.Item>

          <Form.Item
              name="coverImage"
              label="封面地址"
              getValueFromEvent={(e) => e?.file?.response}
          >
            <Upload
                name="avatar"
                listType="picture-card"
                showUploadList={false}
                customRequest={customRequest}
                className="avatar-uploader"
                onChange={(info) => {
                  if (info.file.status === 'done') {
                    // 获取上传后的图片 URL
                    const imageUrl = info.file.response;
                    // 或者通过 info.file.url 获取
                    setCoverImage(imageUrl);
                  }
                }}
            >
              <div className="update-content-box">
                {coverImage ?
                    <Image
                        style={{ maxWidth: "100%", maxHeight: '100%' }}
                        src={coverImage}
                        alt="cover"
                    />
                    :
                    <PlusOutlined />
                }
              </div>
            </Upload>
          </Form.Item>

          <Form.Item
              name="publishedTime"
              label="发布时间"
              normalize={(value) => value?.format('YYYY-MM-DD HH:mm:ss')}
              getValueProps={(value) => ({ value: value ? dayjs(value) : null })}
          >
            <DatePicker defaultValue={dayjs()} format={'YYYY-MM-DD HH:mm:ss'} />
            {/*<Input placeholder="请输入发布时间（例如 2026-05-03 12:00:00）" />*/}
          </Form.Item>

          <Form.Item name="categoryId" label={null}>
            <Input placeholder="请输入分类 ID" />
          </Form.Item>

          <Form.Item name="summary" label={null}>
            <Input.TextArea rows={3} placeholder="请输入摘要" />
          </Form.Item>

          <Form.Item name="slug" label={null}>
            <Input placeholder="请输入 URL 别名" />
          </Form.Item>

          <div style={{ border: '1px solid #d9d9d9', background: '#fff', marginBottom: '24px' }}>
            <Toolbar
                editor={editor}
                defaultConfig={defaultToolbarConfig}
                style={{borderBottom: '1px solid #d9d9d9'}}
            />
            <Editor
                defaultConfig={defaultEditorConfig}
                value={content}
                onCreated={(editor) => {
                  setEditor(editor)
                }}
                onChange={(editor: IDomEditor) => setContent(editor.getHtml())}
                style={{ minHeight: 300 }}
            />
          </div>

          <Form.Item name="status">
            <Radio.Group defaultValue={1} options={statusOptions}></Radio.Group>
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" loading={saving} onClick={handleSubmit}>
                保存
              </Button>
              <Button onClick={() => navigate(-1)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </div>
  )
}
