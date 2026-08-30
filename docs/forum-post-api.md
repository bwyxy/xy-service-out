# 论坛帖子模块 API 接口文档

## 基础信息

- 基础路径：`/api/forum/posts`
- 认证方式：Header `token` + JWT
- 响应格式：`R<T>`

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

## 接口列表

### 1. 查询帖子列表

- **URL**：`POST /api/forum/posts`
- **说明**：支持分页、排序、条件筛选，客户端默认只返回已发布帖子
- **请求参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| boardId | Long | 否 | 版块ID，0或不传表示全部 |
| title | String | 否 | 标题关键字，支持模糊搜索 |
| status | Integer | 否 | 状态：0草稿 1审核中 2已发布 3拒绝 4下架 |
| isTop | Integer | 否 | 是否置顶：0否 1是 |
| isEssence | Integer | 否 | 是否精华：0否 1是 |
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认10 |

- **响应数据**：`PageResult<ForumPost>`

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "list": [
      {
        "postId": 1,
        "authorId": 100,
        "boardId": 1,
        "title": "帖子标题",
        "summary": "摘要",
        "content": "正文内容",
        "coverImage": "https://xxx/cover.jpg",
        "status": 2,
        "isTop": 0,
        "isEssence": 1,
        "viewCount": 100,
        "likeCount": 20,
        "commentCount": 5,
        "publishedTime": "2026-08-27 10:00:00"
      }
    ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 2. 查询帖子详情

- **URL**：`GET /api/forum/posts/{postId}`
- **说明**：每调用一次会触发浏览量统计（30分钟内同一用户不重复统计）
- **路径参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | Long | 是 | 帖子ID |

- **响应数据**：`ForumPost`

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "postId": 1,
    "title": "帖子标题",
    "content": "正文内容",
    "viewCount": 101,
    "likeCount": 20,
    "commentCount": 5
  }
}
```

### 3. 点赞 / 取消点赞

- **URL**：`POST /api/forum/posts/{postId}/like`
- **说明**：同一用户重复调用会切换点赞状态
- **路径参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | Long | 是 | 帖子ID |

- **响应数据**：`Boolean`，true 表示已点赞，false 表示已取消

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": true
}
```

### 4. 查询当前用户点赞状态

- **URL**：`GET /api/forum/posts/{postId}/like`
- **路径参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | Long | 是 | 帖子ID |

- **响应数据**：`Boolean`

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": true
}
```

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 401 | 未登录或 token 失效 |
| 500 | 服务器内部错误 |

## 错误信息示例

```json
{
  "code": 500,
  "msg": "帖子不存在",
  "data": null
}
```
