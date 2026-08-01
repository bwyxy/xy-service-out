import Cookies from 'js-cookie'

const TokenKey = 'xy_chat_token'

export function getToken() {
    return Cookies.get(TokenKey)
}

export function setToken(token :string) {
    return Cookies.set(TokenKey, token)
}

export function setCookies(key :string, value :string) {
    return Cookies.set(key, value)
}

export function getCookies(key :string) {
    return Cookies.get(key)
}

export function removeToken() {
    return Cookies.remove(TokenKey)
}