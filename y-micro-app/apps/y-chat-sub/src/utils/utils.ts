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

/**
 * 将日期转为相对时间描述
 * @param {string|number|Date} time - 可被 new Date() 解析的时间
 * @param {string} [lang='zh'] - 语言，目前仅支持 'zh'
 * @returns {string} 如 "今天 14:30"、"昨天 08:20"、"3天前"、"2个月前"、"2023-12-01"
 */
export function formatRelativeTime(time :string, _lang = 'zh') {
    const date = new Date(time);
    if (isNaN(date.getTime())) return '无效时间';

    const now = new Date();
    const diff = now.getTime() - date.getTime(); // 毫秒差
    // const diffMinutes :number = Math.floor(diff / (1000 * 60));
    // const diffHours :number = Math.floor(diff / (1000 * 60 * 60));
    const diffDays = Math.floor(diff / (1000 * 60 * 60 * 24));

    // 获取今天零点
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterdayStart = new Date(todayStart);
    yesterdayStart.setDate(yesterdayStart.getDate() - 1);

    // 判断是否为今天
    if (date >= todayStart) {
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `今天 ${hours}:${minutes}`;
    }

    // 判断是否为昨天
    if (date >= yesterdayStart && date < todayStart) {
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `昨天 ${hours}:${minutes}`;
    }

    // 1~6 天前
    if (diffDays < 7) {
        return `${diffDays}天前`;
    }

    // 7~29 天前（显示具体日期？或者按周？这里我们显示“X天前”）
    // 若希望显示“1周前”，可再细分，但按常见习惯，我们保留天数，或转为周
    // 简易版：30天以内显示天数
    if (diffDays < 30) {
        return `${diffDays}天前`;
    }

    // 30天~365天显示月数
    if (diffDays < 365) {
        const months = Math.floor(diffDays / 30);
        return `${months}个月前`;
    }

    // 超过一年显示具体日期（或显示“X年前”）
    // 这里展示具体日期，更清晰
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}