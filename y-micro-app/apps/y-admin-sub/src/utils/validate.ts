/**
 * 判断url是否是http或https
 * @param {string} path
 * @returns {Boolean}
 */
export function isHttp(url: string | string[]): boolean {
    return url.indexOf('http://') !== -1 || url.indexOf('https://') !== -1
}

/**
 * @param {string} path
 * @returns {Boolean}
 */
export const isExternal = (path: string): boolean => {
    return /^(https?:|mailto:|tel:)/.test(path);
};

/**
 * @param {string} str
 * @returns {Boolean}
 */
export const validUsername = (str: string): boolean => {
    const valid_map = ["admin", "editor"];
    return valid_map.indexOf(str.trim()) >= 0;
};

/**
 * @param {string} url
 * @returns {Boolean}
 */
export const validURL = (url: string): boolean => {
    const reg = /^(https?|ftp):\/\/([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]+))*$/;
    return reg.test(url);
};

/**
 * @param {string} str
 * @returns {Boolean}
 */
export const validLowerCase = (str: string): boolean => {
    const reg = /^[a-z]+$/;
    return reg.test(str);
};

/**
 * @param {string} str
 * @returns {Boolean}
 */
export const validUpperCase = (str: string): boolean => {
    const reg = /^[A-Z]+$/;
    return reg.test(str);
};

/**
 * @param {string} str
 * @returns {Boolean}
 */
export const validAlphabets = (str: string): boolean => {
    const reg = /^[A-Za-z]+$/;
    return reg.test(str);
};

/**
 * @param {string} email
 * @returns {Boolean}
 */
export const validEmail = (email: string): boolean => {
    const reg = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return reg.test(email);
};

/**
 * @param {string} str
 * @returns {Boolean}
 */
export const isString = (str: any): boolean => {
    if (typeof str === "string" || str instanceof String) {
        return true;
    }
    return false;
};

/**
 * @param {Array} arg
 * @returns {Boolean}
 */
export const isArray = (arg: any): boolean => {
    if (typeof Array.isArray === "undefined") {
        return Object.prototype.toString.call(arg) === "[object Array]";
    }
    return Array.isArray(arg);
};