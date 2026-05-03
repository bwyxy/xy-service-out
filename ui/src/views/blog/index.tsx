import { useCallback, useEffect, useState } from 'react'
import { Button, Form, Input, message, Modal, Space, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  addBlogConfig,
  getBlogConfigById,
  getBlogConfigList,
  updateBlogConfig,
  type BlogConfigCreate,
  type BlogConfigItem,
  type BlogConfigQuery,
} from '@/api/blog'

interface BlogEditFormValues {
  configKey: string
  configValue?: string
  description?: string
}

export default function Blog() {
  const [filterForm] = Form.useForm<BlogConfigQuery>()
  const [editForm] = Form.useForm<BlogEditFormValues>()
  const [list, setList] = useState<BlogConfigItem[]>([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | string | null>(null)

  const loadList = useCallback(async (params: BlogConfigQuery = {}) => {
    setLoading(true)
    try {
      const data = await getBlogConfigList(params)
      console.log('获取博客配置列表成功:', data)
      setList(data)
    } catch (error) {
      console.error('获取博客配置列表失败:', error)
      setList([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadList()
  }, [loadList])

  const openCreateModal = () => {
    setEditingId(null)
    editForm.resetFields()
    setModalOpen(true)
  }

  const openEditModal = async (id: number | string) => {
    setSaving(true)
    try {
      const item = await getBlogConfigById(id)
      editForm.setFieldsValue({
        configKey: item.configKey,
        configValue: item.configValue,
        description: item.description,
      })
      setEditingId(item.id)
      setModalOpen(true)
    } catch (error) {
      console.error('获取配置详情失败:', error)
      message.error('获取配置详情失败')
    } finally {
      setSaving(false)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await editForm.validateFields()
      setSaving(true)
      if (editingId !== null) {
        await updateBlogConfig({
          id: editingId,
          configKey: values.configKey,
          configValue: values.configValue,
          description: values.description,
        })
        message.success('更新成功')
      } else {
        await addBlogConfig(values as BlogConfigCreate)
        message.success('新增成功')
      }
      setModalOpen(false)
      void loadList(filterForm.getFieldsValue())
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message)
      }
    } finally {
      setSaving(false)
    }
  }

  const handleCancel = () => {
    setModalOpen(false)
    editForm.resetFields()
  }

  const onSearch = (values: BlogConfigQuery) => {
    void loadList(values)
  }

  const columns: ColumnsType<BlogConfigItem> = [
    {
      title: '配置 ID',
      dataIndex: 'id',
      align: 'center',
      key: 'id',
      width: 120,
    },
    {
      title: '配置标识',
      dataIndex: 'configKey',
      align: 'center',
      key: 'configKey',
      width: 200,
    },
    {
      title: '配置值',
      dataIndex: 'configValue',
      align: 'center',
      key: 'configValue',
    },
    {
      title: '描述',
      dataIndex: 'description',
      align: 'center',
      key: 'description',
    },
    {
      title: '操作',
      key: 'actions',
      width: 140,
      align: 'center',
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => void openEditModal(record.id)}>
            编辑
          </Button>
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
        <Form.Item name="configKey" label="配置标识">
          <Input placeholder="可选" allowClear />
        </Form.Item>
        <Form.Item name="id" label="配置 ID">
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
                void loadList()
              }}
            >
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <Space wrap style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={openCreateModal}>
          新增配置
        </Button>
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={{ pageSize: 10 }}
        bordered
      />

      <Modal
        title={editingId !== null ? '编辑博客配置' : '新增博客配置'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={handleCancel}
        confirmLoading={saving}
        width={600}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="configKey"
            label="配置标识"
            rules={[{ required: true, message: '请输入配置标识' }]}
          >
            <Input placeholder="请输入配置标识" />
          </Form.Item>
          <Form.Item name="configValue" label="配置值">
            <Input placeholder="请输入配置值" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={4} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
