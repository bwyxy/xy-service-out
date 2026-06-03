import request from '@/utils/request'

/** 博客配置列表查询 */
export async function getBlogConfigList(
    params,
) {
    const res = await request.post('/blog/list', params)

    console.log('获取博客配置列表响应:', res)
    return res.data
}

/** 获取博客配置信息 */
export async function getBlogConfigById(
    id,
) {
    const res= await request.get(
        `/blog/${id}`,
    )

    return res.data
}

/** 新增博客配置 */
export async function addBlogConfig(
    data,
) {
    const res = await request.post(
        '/blog/add',
        data,
    )
    return res.data
}

/** 更新博客配置 */
export async function updateBlogConfig(
    data,
) {
    const res = await request.put(
        '/blog/update',
        data,
    )
    return res.data
}
