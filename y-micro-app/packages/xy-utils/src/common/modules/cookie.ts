/**
 * cookie 浏览器缓存
 */
const domain = ''
export function gCookie() {

}

//  时间单位为秒
export function setCookie(c_name: string, value: any, expire?: any) {
    var date: any = new Date()
    date.setSeconds(date.getSeconds() + expire)
    document.cookie = c_name + '=' + escape(value) + '; expires=' + date.toGMTString() + '; domain=' + domain + ';path=/'
}

export function getCookie(c_name: string) {
    let c_start: number
    let c_end: number
    if (document.cookie.length > 0) {
        c_start = document.cookie.indexOf(c_name + '=')
        if (c_start != -1) {
            c_start = c_start + c_name.length + 1
            c_end = document.cookie.indexOf(';', c_start)
            if (c_end == -1) c_end = document.cookie.length
            return unescape(document.cookie.substring(c_start, c_end))
        }
    }
    return ''
}
export function delCookie(c_name:string) {
    setCookie(c_name, '', -1)
}
