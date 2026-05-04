import { useCallback, useEffect, useState } from 'react'
import { Button, Form, Input, message, Modal, Popconfirm, Space, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  addCategory,
  deleteCategory,
  getCategoryById,
  getCategoryList,
  updateCategory,
  type CategoryCreate,
  type CategoryItem,
  type CategoryListQuery,
  type CategoryListResponse,
} from '@/api/category'

interface CategoryEditFormValues {
  name: string
  slug?: string
  description?: string
}

export default function Category() {
  const [filterForm] = Form.useForm<{ name?: string }>()
  const [editForm] = Form.useForm<CategoryEditFormValues>()
  const [list, setList] = useState<CategoryItem[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | string | null>(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  })

  const loadList = useCallback(async (params: CategoryListQuery) => {
    setLoading(true)
    try {
      const data: CategoryListResponse = await getCategoryList(params)
      setList(data.list)
      setTotal(data.total)
    } catch (error) {
      console.error('获取分类列表失败:', error)
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
      const item = await getCategoryById(id)
      editForm.setFieldsValue({
        name: item.name,
        slug: item.slug,
        description: item.description,
      })
      setEditingId(item.categoryId)
      setModalOpen(true)
    } catch (error) {
      console.error('获取分类详情失败:', error)
      message.error('获取分类详情失败')
    } finally {
      setSaving(false)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await editForm.validateFields()
      setSaving(true)
      if (editingId !== null) {
        await updateCategory({
          categoryId: Number(editingId),
          name: values.name,
          slug: values.slug,
          description: values.description,
        })
        message.success('更新成功')
      } else {
        await addCategory(values as CategoryCreate)
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
      await deleteCategory(id)
      message.success('删除成功')
      void loadList({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        name: filterForm.getFieldValue('name'),
      })
    } catch (error) {
      console.error('删除分类失败:', error)
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

  const columns: ColumnsType<CategoryItem> = [
    {
      title: '分类 ID',
      dataIndex: 'categoryId',
      align: 'center',
      key: 'categoryId',
      width: 100,
    },
    {
      title: '分类名称',
      dataIndex: 'name',
      align: 'center',
      key: 'name',
      width: 160,
    },
    {
      title: 'URL 别名',
      dataIndex: 'slug',
      align: 'center',
      key: 'slug',
      width: 180,
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
          <Button type="link" onClick={() => void openEditModal(record.categoryId)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除这个分类吗？"
            onConfirm={() => void handleDelete(record.categoryId)}
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
        <Form.Item name="name" label="分类名称">
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
          新增分类
        </Button>
      </Space>

      <Table
        rowKey="categoryId"
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
        title={editingId !== null ? '编辑分类' : '新增分类'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={handleCancel}
        confirmLoading={saving}
        width={600}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="name"
            label="分类名称"
            rules={[{ required: true, message: '请输入分类名称' }]}
          >
            <Input placeholder="请输入分类名称" />
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
