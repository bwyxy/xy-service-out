import { defineStore } from 'pinia'
import { login, register, getUserInfo } from '@/api/auth.ts'
import { ElMessage } from 'element-plus'
import { setToken, setCookies, removeToken, getCookies, getToken } from '@/utils/auth.ts'
import { WKIM, WKIMEvent } from 'easyjssdk'

const useUserStore = defineStore('user', {
    state() {
        return {
            userInfo: {} as any,
            wkIM: null as any,
            /** 当前正在聊天的频道信息 */
            currentChannel: null as any
        }
    },
    actions: {
        /** 用户登录 */
        async userLogin(params :any) :Promise<boolean> {

            const wssKey = import.meta.env.VITE_WSS_KEY

            try {
                const result = await login(params)

                if (result.code === 200) {
                    const { token, imAddr } = result.data

                    setToken(token)
                    setCookies(wssKey, imAddr.wss_addr)
                    return true
                }
                return false
            } catch {
                ElMessage.error("登录失败")
                return false
            }
        },
        /** 初始化webSocket 链接  */
        initWKIM() {
            const wssKey = import.meta.env.VITE_WSS_KEY
            const info = this.userInfo

            console.log('initWKIM', wssKey, info)
            this.wkIM = WKIM.init(getCookies(wssKey), {
                uid: String(info.uId),
                token: getToken()
            })
            this.wkIM.on(WKIMEvent.Connect, (connectResult :any) => {
                console.log('连接成功', connectResult)
            })
            this.wkIM.on(WKIMEvent.Disconnect, (disconnectInfo :any) => {
                console.log('连接断开', disconnectInfo)
            })
            this.wkIM.on(WKIMEvent.Message, (message :any) => {
                console.log('收到消息', message)
            })
            this.wkIM.on(WKIMEvent.Error, (error :any) => {
                console.log('连接发送错误', error)
            })

            this.wkIM.connect().then(() => {
                console.log("连接过程已启动，等待 Connect 事件...");
            }).then((sendAck :any) => {
                console.log("消息发送成功, Ack:", sendAck);
                // sendAck 包含 { messageId, messageSeq }
            })
            .catch((err :any) => {
                console.error("连接或初始发送失败:", err);
            });
        },
        async userRegister(params :any) :Promise<boolean> {
            try {
                const result = await register(params)

                return result.code === 200;

            } catch {
                return false
            }
        },

        async setUserInfo() :Promise<boolean> {
            const result = await getUserInfo()

            console.log(result)
            if (result.code === 200) {

                this.userInfo = result.data

                return true
            }
            throw Error("获取登录失败")
        },

        outLogin() {
            removeToken()
            this.userInfo = {} as any
        }
    }
})

export default useUserStore;