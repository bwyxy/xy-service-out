/**
* 判断一个值是否为空对象（即 `{}`）。
* - 仅当值为普通对象（非 `null`、非数组、非其他内置对象）且自身可枚举属性个数为 0 时返回 `true`。
* - 使用 `Object.keys()` 检查，忽略 Symbol 属性和不可枚举属性。
*
* @param obj 待检测的值
* @returns 如果为空对象则返回 `true`，否则返回 `false`
*/
export function isEmptyObject(obj: unknown): obj is Record<PropertyKey, never> {
    return (
        obj !== null &&
        typeof obj === 'object' &&
        !Array.isArray(obj) &&
        Object.keys(obj).length === 0
    );
}
