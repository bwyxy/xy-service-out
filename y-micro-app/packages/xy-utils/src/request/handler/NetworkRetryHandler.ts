import { AxiosInstance, AxiosRequestConfig, AxiosError, AxiosResponse } from 'axios';

// 待重试的请求项
interface PendingRequest {
    config: AxiosRequestConfig;     // 原始请求配置
    resolve: (value: any) => void;  // 成功回调
    reject: (reason?: any) => void; // 失败回调
    retryCount: number;             // 当前已重试次数
}

/**
 * 网络重试处理器
 * */
/**
 * 网络重试处理器
 * */
class NetworkRetryHandler {
    private pendingRequests: PendingRequest[] = [];  // 待重试队列
    private isOnlineListenerAttached = false;        // 是否已监听 online 事件
    private maxRetries = 3;                          // 最大重试次数（可配置）

    constructor(private axiosInstance: AxiosInstance) {
        this.setupInterceptors();
        this.initOnlineListener();
    }

    /** 设置请求和响应拦截器 */
    private setupInterceptors() {
        // 响应拦截器：处理网络错误
        this.axiosInstance.interceptors.response.use(
            (response: AxiosResponse) => response, // 正常响应直接返回
            (error: AxiosError) => this.handleNetworkError(error)
        );
    }

    /** 判断是否为网络断开错误 */
    private isNetworkDisconnectedError(error: AxiosError): boolean {
        // 无响应且错误信息包含常见网络断开标识
        return !error.response && (
            error.message === 'Network Error' ||
            // @ts-ignore
            error.message?.includes('ERR_INTERNET_DISCONNECTED') ||
            error.code === 'ERR_NETWORK'
        );
    }

    private handleNetworkError(error: AxiosError): Promise<any> {
        if (this.isNetworkDisconnectedError(error) && error.config) {
            // 检查重试次数是否超限
            const retryCount = (error.config as any).__retryCount || 0;
            if (retryCount >= this.maxRetries) {
                // @ts-ignore
                return Promise.reject(error); // 超过最大重试次数，直接拒绝
            }

            // 增加重试计数
            (error.config as any).__retryCount = retryCount + 1;

            // 返回一个新的 Promise，存入队列，等待网络恢复后重试
            // @ts-ignore
            return new Promise((resolve, reject) => {
                this.pendingRequests.push({
                    config: error.config!,
                    resolve,
                    reject,
                    retryCount: retryCount + 1,
                });
            });
        }
        // 其他错误直接抛出
        // @ts-ignore
        return Promise.reject(error);
    }

    /** 初始化网络恢复监听 */
    private initOnlineListener() {
        if (this.isOnlineListenerAttached) return;
        this.isOnlineListenerAttached = true;

        window.addEventListener('online', () => {
            this.retryAllPendingRequests();
        });
    }

    /** 网络恢复后，重试所有待重试的请求 */
    // @ts-ignore
    private async retryAllPendingRequests() {
        const requestsToRetry = [...this.pendingRequests];
        this.pendingRequests = []; // 清空原队列，避免重复重试

        for (const pending of requestsToRetry) {
            try {
                // 重新发起请求
                const response = await this.axiosInstance(pending.config);
                pending.resolve(response); // 将结果返回给原调用者
            } catch (err) {
                // 重试时可能再次遇到网络错误，但拦截器会重新加入队列
                // 如果错误不是网络错误，则直接 reject
                if (this.isNetworkDisconnectedError(err as AxiosError)) {
                    // 再次网络断开，重新入队（但重试次数已经+1，会继续等待）
                    // 注意：这里需要避免重复计数，因为 err.config 的 __retryCount 已更新
                    this.pendingRequests.push(pending);
                } else {
                    pending.reject(err);
                }
            }
        }
    }

    /** 手动清空待重试队列（例如用户主动取消或页面卸载时） */
    public clearPendingRequests() {
        for (const pending of this.pendingRequests) {
            pending.reject(new Error('Request cancelled due to manual clear'));
        }
        this.pendingRequests = [];
    }
}

// 导出网络重试处理器
export default NetworkRetryHandler;
