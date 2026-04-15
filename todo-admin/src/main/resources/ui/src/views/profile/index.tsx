import { Avatar, Card, Descriptions, Divider, Space, Typography } from 'antd'
import { useAuth } from '@/store/authStore'

const { Title, Text } = Typography

export default function Profile() {
  const { userInfo, loading } = useAuth()

  if (loading) {
    return <div style={{ padding: 24 }}>加载中...</div>
  }

  if (!userInfo) {
    return <div style={{ padding: 24 }}>请先登录</div>
  }

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
            <Avatar size={80} src={userInfo.avatar} style={{ fontSize: 24 }}>
              {userInfo.nickName?.charAt(0)}
            </Avatar>
            <div>
              <Title level={3} style={{ margin: 0 }}>{userInfo.nickName}</Title>
              <Text type="secondary">{userInfo.userName}</Text>
            </div>
          </div>

          <Divider>基本信息</Divider>
          <Descriptions column={2} bordered>
            <Descriptions.Item label="用户ID">{userInfo.userId}</Descriptions.Item>
            <Descriptions.Item label="部门ID">{userInfo.deptId}</Descriptions.Item>
            <Descriptions.Item label="用户名">{userInfo.userName}</Descriptions.Item>
            <Descriptions.Item label="昵称">{userInfo.nickName}</Descriptions.Item>
            <Descriptions.Item label="邮箱">{userInfo.email}</Descriptions.Item>
            <Descriptions.Item label="手机号">{userInfo.phonenumber}</Descriptions.Item>
            <Descriptions.Item label="性别">{userInfo.sex === '0' ? '男' : userInfo.sex === '1' ? '女' : '未知'}</Descriptions.Item>
            <Descriptions.Item label="状态">{userInfo.status === '0' ? '正常' : '禁用'}</Descriptions.Item>
            <Descriptions.Item label="登录IP" span={2}>{userInfo.loginIp}</Descriptions.Item>
            <Descriptions.Item label="登录时间" span={2}>{userInfo.loginDate}</Descriptions.Item>
            <Descriptions.Item label="创建人" span={2}>{userInfo.createBy}</Descriptions.Item>
            <Descriptions.Item label="创建时间" span={2}>{userInfo.createTime}</Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>{userInfo.remark}</Descriptions.Item>
          </Descriptions>

          <Divider>角色信息</Divider>
          <div>
            {userInfo.roles?.map(role => (
              <Card key={role.roleId} size="small" style={{ marginBottom: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span><strong>{role.roleName}</strong> ({role.roleKey})</span>
                  <span>{role.status === '0' ? '启用' : '禁用'}</span>
                </div>
              </Card>
            ))}
          </div>
        </Space>
      </Card>
    </div>
  )
}