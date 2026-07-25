/**
 * 将日期对象或时间戳转换为指定格式
 * @param {Date|string|number} date - 日期对象、时间戳或日期字符串
 * @param {string} format - 格式字符串
 * @returns {string} 格式化后的日期字符串
 */
export function formatDateTime(date :Date | string | number, format :string = 'yyyy-MM-dd HH:mm:ss') :string {
    // 处理输入
    let d;
    if (date instanceof Date) {
        d = date;
    } else if (typeof date === 'number' || !isNaN(Number(date))) {
        d = new Date(Number(date));
    } else {
        const formatDate = date.replace(/\./g, '/')
        d = new Date(formatDate);
    }

    // 检查日期是否有效
    if (isNaN(d.getTime())) {
        throw new Error('Invalid date');
    }

    const year :number = d.getFullYear();
    const month :number = d.getMonth() + 1;
    const day :number = d.getDate();
    const hours :number = d.getHours();
    const minutes :number = d.getMinutes();
    const seconds :number = d.getSeconds();

    // 格式化数字为两位数
    const pad = (n :number) :string => n < 10 ? '0' + n : String(n);

    // 根据格式返回对应字符串
    return format
        .replace('yyyy', String(year))
        .replace('yy', String(year).slice(-2))
        .replace('MM', pad(month))
        .replace('M', String(month))
        .replace('dd', pad(day))
        .replace('d', String(day))
        .replace('HH', pad(hours))
        .replace('H', String(hours))
        .replace('mm', pad(minutes))
        .replace('m', String(minutes))
        .replace('ss', pad(seconds))
        .replace('s', String(seconds));
}
