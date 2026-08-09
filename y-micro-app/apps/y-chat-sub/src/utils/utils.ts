export function isImageUrl(url: string): boolean {
    if (!url) return false;

    // 1. 剥离 ? 后面的参数 和 # 后面的锚点
    const path = url.split('?')[0].split('#')[0];

    // 2. 提取扩展名（转为小写）
    const ext = path.split('.').pop()?.toLowerCase();

    if (!ext) return false;

    // 3. 常见图片格式列表
    const imageExtensions = new Set([
        'jpg', 'jpeg', 'png', 'gif', 'bmp',
        'webp', 'svg', 'tiff', 'tif', 'ico',
        'avif', 'heic', 'heif'
    ]);

    return imageExtensions.has(ext);
}