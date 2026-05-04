import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Form, Image, Input, Select, Space, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getArticleList, type ArticleItem } from '@/api/article'

const statusMap: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '私密',
}

export default function Article() {
  const [form] = Form.useForm<{ title?: string; status?: number }>()
  const [list, setList] = useState<ArticleItem[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 })
  const [total, setTotal] = useState(0)
  const navigate = useNavigate()

  const fetchData = async () => {
        setLoading(true)
        try {
        const data = await getArticleList({
            pageNum: pagination.current,
            pageSize: pagination.pageSize,
        })
        setList(data.list)
        setTotal(data.total)
        } catch (error) {
        console.error('获取文章列表失败:', error)
        setList([])
        setTotal(0)
        } finally {
        setLoading(false)
        }
    }

  useEffect(() => {
    console.log('初始化界面', pagination.current, pagination.pageSize)
    fetchData()
  }, [pagination.current, pagination.pageSize])

  const handleSearch = (values: { title?: string; status?: number }) => {
    setPagination((prev) => ({ ...prev, current: 1 }))
    const fetchData = async () => {
      setLoading(true)
      try {
        const data = await getArticleList({
          pageNum: 1,
          pageSize: pagination.pageSize,
          title: values.title,
          status: values.status,
        })
        setList(data.list)
        setTotal(data.total)
      } catch (error) {
        console.error('获取文章列表失败:', error)
        setList([])
        setTotal(0)
      } finally {
        setLoading(false)
      }
    }
    void fetchData()
  }

  const handleTableChange = (pager: any) => {
    setPagination({
      current: pager.current,
      pageSize: pager.pageSize,
    })
  }

  const columns: ColumnsType<ArticleItem> = [
    {
      title: '文章 ID',
      dataIndex: 'articleId',
      key: 'articleId',
      align: 'center',
    },
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      align: 'center',
    },
    {
      title: '封面',
      dataIndex: 'coverImage',
      key: 'coverImage',
      align: 'center',
      render: (coverImage) =>
        coverImage ? <Image width={80} src={coverImage} alt="封面" /> : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      align: 'center',
      render: (value) => statusMap[value] ?? '-',
    },
    {
      title: '作者',
      dataIndex: 'authorId',
      key: 'authorId',
      align: 'center',
    },
    {
      title: '发布时间',
      dataIndex: 'publishedTime',
      key: 'publishedTime',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'actions',
      width: 140,
      align: 'center',
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/article/edit?articleId=${record.articleId}`)}>
            编辑
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>

      <Form
        form={form}
        layout="inline"
        onFinish={handleSearch}
        style={{ marginBottom: 16 }}
      >
        <Form.Item name="title" label="文章名称">
          <Input placeholder="可选" allowClear />
        </Form.Item>
        <Form.Item name="status" label="状态">
          <Select allowClear style={{ width: 140 }}>
            <Select.Option value={0}>草稿</Select.Option>
            <Select.Option value={1}>已发布</Select.Option>
            <Select.Option value={2}>私密</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              查询
            </Button>
            <Button
              onClick={() => {
                form.resetFields()
                setPagination((prev) => ({ ...prev, current: 1 }))
                const fetchData = async () => {
                  setLoading(true)
                  try {
                    const data = await getArticleList({
                      pageNum: 1,
                      pageSize: pagination.pageSize,
                    })
                    setList(data.list)
                    setTotal(data.total)
                  } catch (error) {
                    console.error('获取文章列表失败:', error)
                    setList([])
                    setTotal(0)
                  } finally {
                    setLoading(false)
                  }
                }
                void fetchData()
              }}
            >
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

       <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => navigate('/article/add')}>
          新增文章
        </Button>
      </Space>

      <Table
        rowKey="articleId"
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
    </div>
  )
}
