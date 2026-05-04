
import { useCallback, useEffect, useState } from 'react'
import { Button, Form, Input, message, Modal, Popconfirm, Space, Table, Typography, Tag as AntTag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  addTag,
  deleteTag,
  getTagById,
  getTagList,
  updateTag,
  type TagCreate,
  type TagItem,
  type TagListQuery,
  type TagListResponse,
} from '@/api/tag'

interface TagEditFormValues {
  name: string
  color?: string
  slug?: string
  description?: string
}

export default function Tag() {
  const [filterForm] = Form.useForm<{ name?: string }>()
  const [editForm] = Form.useForm<TagEditFormValues>()
  const [list, setList] = useState<TagItem[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | string | null>(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  })

  const loadList = useCallback(async (params: TagListQuery) => {
    setLoading(true)
    try {
      const data: TagListResponse = await getTagList(params)
      setList(data.list)
      setTotal(data.total)
    } catch (error) {
      console.error('获取标签列表失败:', error)
      setList([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
  }, [loadList, pagination.current, pagination.pageSize])

  const openCreateModal = () => {
    setEditingId(null)
    editForm.resetFields()
    setModalOpen(true)
  }

  const openEditModal = async (id: number | string) => {
    setSaving(true)
    try {
      const item = await getTagById(id)
      editForm.setFieldsValue({
        name: item.name,
        color: item.color,
        slug: item.slug,
        description: item.description,
      })
      setEditingId(item.tagId)
      setModalOpen(true)
    } catch (error) {
      console.error('获取标签详情失败:', error)
      message.error('获取标签详情失败')
    } finally {
      setSaving(false)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await editForm.validateFields()
      setSaving(true)
      if (editingId !== null) {
        await updateTag({
          tagId: Number(editingId),
          name: values.name,
          color: values.color,
          slug: values.slug,
          description: values.description,
        })
        message.success('更新成功')
      } else {
        await addTag(values as TagCreate)
        message.success('新增成功')
      }
      setModalOpen(false)
      void loadList({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        name: filterForm.getFieldValue('name'),
      })
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message)
      }
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number | string) => {
    try {
      await deleteTag(id)
      message.success('删除成功')
      void loadList({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        name: filterForm.getFieldValue('name'),
      })
    } catch (error) {
      console.error('删除标签失败:', error)
      message.error('删除失败')
    }
  }

  const handleCancel = () => {
    setModalOpen(false)
    editForm.resetFields()
  }

  const onSearch = (values: { name?: string }) => {
    setPagination(prev => ({ ...prev, current: 1 }))
    void loadList({
      pageNum: 1,
      pageSize: pagination.pageSize,
      name: values.name,
    })
  }

  const handleTableChange = (pagination: any) => {
    setPagination({
      current: pagination.current,
      pageSize: pagination.pageSize,
    })
  }

  const columns: ColumnsType<TagItem> = [
    {
      title: '标签 ID',
      dataIndex: 'tagId',
      align: 'center',
      key: 'tagId',
      width: 100,
    },
    {
      title: '标签名称',
      dataIndex: 'name',
      align: 'center',
      key: 'name',
      width: 150,
    },
    {
      title: '颜色',
      dataIndex: 'color',
      align: 'center',
      key: 'color',
      width: 120,
      render: (color: string) => color ? <AntTag color={color}>{color}</AntTag> : '-',
    },
    {
      title: 'URL 别名',
      dataIndex: 'slug',
      align: 'center',
      key: 'slug',
      width: 150,
    },
    {
      title: '描述',
      dataIndex: 'description',
      align: 'center',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      align: 'center',
      key: 'createTime',
      width: 180,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      align: 'center',
      key: 'updateTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'actions',
      width: 140,
      align: 'center',
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => void openEditModal(record.tagId)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除这个标签吗？"
            onConfirm={() => void handleDelete(record.tagId)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>

      <Form
        form={filterForm}
        layout="inline"
        onFinish={onSearch}
        style={{ marginBottom: 16 }}
      >
        <Form.Item name="name" label="标签名称">
          <Input placeholder="可选" allowClear />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              查询
            </Button>
            <Button
              onClick={() => {
                filterForm.resetFields()
                setPagination(prev => ({ ...prev, current: 1 }))
                void loadList({
                  pageNum: 1,
                  pageSize: pagination.pageSize,
                })
              }}
            >
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <Space wrap style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={openCreateModal}>
          新增标签
        </Button>
      </Space>

      <Table
        rowKey="tagId"
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={{
          ...pagination,
          total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
        }}
        onChange={handleTableChange}
        bordered
      />

      <Modal
        title={editingId !== null ? '编辑标签' : '新增标签'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={handleCancel}
        confirmLoading={saving}
        width={600}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="name"
            label="标签名称"
            rules={[{ required: true, message: '请输入标签名称' }]}
          >
            <Input placeholder="请输入标签名称" />
          </Form.Item>
          <Form.Item name="color" label="颜色">
            <Input placeholder="请输入颜色值，如 #ff0000" />
          </Form.Item>
          <Form.Item name="slug" label="URL 别名">
            <Input placeholder="请输入 URL 别名" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={4} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
