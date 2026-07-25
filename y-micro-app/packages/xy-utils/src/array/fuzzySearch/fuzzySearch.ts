/**
 * 根据关键字查找数组
 * */
export function fuzzySearch<T extends Record<string, any>>(
    items: T[],
    keyword: string,
    keys?: (keyof T)[]
): T[] {
    if (!keyword.trim()) return items;

    const searchTerm = keyword.toLowerCase();

    return items.filter(item => {
        const searchableKeys = keys || Object.keys(item) as (keyof T)[];

        return searchableKeys.some(key => {
            const value = item[key];
            // 只对字符串类型进行模糊匹配
            if (typeof value === 'string') {
                return value.toLowerCase().includes(searchTerm);
            }
            if (typeof value === 'number') {
                return value.toString().includes(searchTerm);
            }
            return false;
        });
    });
}
