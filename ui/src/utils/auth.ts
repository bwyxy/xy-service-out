import { TOKEN_KEY } from '@/constants/cache'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  window.dispatchEvent(new CustomEvent('admin-token-cleared'))
}
