import { getUserInfo } from '@/api/user.ts'
import {Button} from "antd";

export default function Home() {
  return (
    <div>
      <h2>首页</h2>
      <p>已通过路由守卫，仅在持有 Token 时可见。</p>
        <Button onClick={() => getUserInfo()}>获取用户信息</Button>
    </div>
  )
}
