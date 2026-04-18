import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Card, Col, Form, Input, message, Row, Spin, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { fetchCaptchaImage } from '@/api/captcha'
import { useAuth } from '@/store/authStore'
import request, { type RuoYiResponse } from '@/utils/request'

function extractToken(body: RuoYiResponse<{ token?: string } | string> & { token?: string }) {
  const inner = body.data
  return (
    body.token ??
    (typeof inner === 'object' && inner !== null
      ? inner.token
      : typeof inner === 'string'
        ? inner
        : undefined)
  )
}

export default function Login() {
  const { setToken, fetchUserInfo } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const redirect = params.get('redirect') || '/'
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [captchaLoading, setCaptchaLoading] = useState(true)
  const [captchaImg, setCaptchaImg] = useState('')
  const [captchaUuid, setCaptchaUuid] = useState('')

  const loadCaptcha = useCallback(async () => {
    // setCaptchaLoading(true)
    // try {
    //   const { imgSrc, uuid } = await fetchCaptchaImage()
    //   setCaptchaImg(imgSrc)
    //   setCaptchaUuid(uuid)
    //   form.setFieldValue('code', undefined)
    // } catch (e) {
    //   message.error(e instanceof Error ? e.message : '获取验证码失败')
    // } finally {
    //   setCaptchaLoading(false)
    // }
  }, [form])

  useEffect(() => {
    void loadCaptcha()
  }, [loadCaptcha])

  async function onFinish(values: { username: string; password: string; code: string }) {
    // if (!captchaUuid) {
    //   message.warning('请先获取验证码')
    //   await loadCaptcha()
    //   return
    // }
    setSubmitting(true)
    try {
      const res = await request.post<RuoYiResponse<{ token?: string } | string>>(
        '/auth/login',
        {
          username: values.username,
          password: values.password,
          code: values.code,
          uuid: captchaUuid,
        },
      )
      const body = res.data as RuoYiResponse<{ token?: string } | string> & { token?: string }
      const token = extractToken(body)
      if (!token) {
        message.error('登录成功但未获取 token，请检查后端响应格式')
        return
      }
      setToken(token)
      
      // 获取用户信息
      try {
        await fetchUserInfo()
      } catch (error) {
        console.error('获取用户信息失败:', error)
      }
      
      message.success('登录成功')
      navigate(decodeURIComponent(redirect), { replace: true })
    } catch (err) {
      message.error(err instanceof Error ? err.message : '登录失败')
      await loadCaptcha()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        minHeight: 0,
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24,
        background:
          'linear-gradient(160deg, #0f172a 0%, #1e293b 42%, #334155 100%)',
      }}
    >
      <Card
        variant="borderless"
        style={{
          width: '100%',
          maxWidth: 420,
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.45)',
        }}
      >
          <div style={{ textAlign: 'center', marginBottom: 28 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>
              管理后台
            </Typography.Title>
            <Typography.Text type="secondary">
              请使用账号密码登录
            </Typography.Text>
          </div>

          <Form
            form={form}
            layout="vertical"
            size="large"
            onFinish={onFinish}
            requiredMark={false}
            autoComplete="off"
          >
            <Form.Item
              name="username"
              label="账号"
              rules={[{ required: true, message: '请输入账号' }]}
            >
              <Input
                prefix={<UserOutlined style={{ color: 'rgba(0,0,0,0.25)' }} />}
                placeholder="账号"
                autoComplete="username"
              />
            </Form.Item>
            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined style={{ color: 'rgba(0,0,0,0.25)' }} />}
                placeholder="密码"
                autoComplete="current-password"
              />
            </Form.Item>
            <Form.Item label="验证码" required>
              <Row gutter={10} align="middle">
                <Col flex="auto">
                  <Form.Item
                    name="code"
                    noStyle
                  >
                    <Input placeholder="图中字符" allowClear autoComplete="off" />
                  </Form.Item>
                </Col>
                <Col flex="none">
                  <button
                    type="button"
                    onClick={() => void loadCaptcha()}
                    disabled={captchaLoading}
                    style={{
                      height: 40,
                      width: 120,
                      padding: 0,
                      border: '1px solid #d9d9d9',
                      borderRadius: 8,
                      background: '#fafafa',
                      cursor: captchaLoading ? 'wait' : 'pointer',
                      overflow: 'hidden',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                    aria-label="刷新验证码"
                  >
                    {captchaLoading ? (
                      <Spin size="small" />
                    ) : captchaImg ? (
                      <img
                        src={captchaImg}
                        alt=""
                        style={{
                          maxHeight: 40,
                          maxWidth: 120,
                          objectFit: 'contain',
                          display: 'block',
                        }}
                      />
                    ) : null}
                  </button>
                </Col>
              </Row>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                看不清可点击图片更换
              </Typography.Text>
            </Form.Item>
            <Form.Item style={{ marginBottom: 8 }}>
              <Button type="primary" htmlType="submit" block loading={submitting}>
                登录
              </Button>
            </Form.Item>
          </Form>

          <Typography.Paragraph
            type="secondary"
            style={{ marginBottom: 0, textAlign: 'center', fontSize: 12 }}
          >
            API：{import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}
          </Typography.Paragraph>
      </Card>
    </div>
  )
}
