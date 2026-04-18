import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        minHeight: 0,
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24,
        textAlign: 'center',
      }}
    >
      <h1>404</h1>
      <p>页面不存在</p>
      <Link to="/">返回首页</Link>
    </div>
  )
}
