/** websocket工具封装类
 * */
export enum WebSocketReadyState {
    CONNECTING = 0,
    OPEN = 1,
    CLOSING = 2,
    CLOSED = 3,
}

export interface WebSocketOptions {
    /** 连接地址 */
    url: string;
    /** 重连次数 */
    reconnectLimit?: number;
    /** 重连间隔 */
    reconnectInterval?: number;
    /** 心跳检查计时器 */
    heartBeatInterval?: number;
    /** 心跳检查发送的消息 */
    heartBeatMsg?: string;
    /** 连接成功回调 */
    onOpen?: (event: Event) => void;
    /** 接受消息回调 */
    onMessage?: (event: MessageEvent) => void;
    /** websocket关闭 回调*/
    onClose?: (event: CloseEvent) => void;
    /** websocket连接错误回调 */
    onError?: (event: Event) => void;
    /** websocket 重连回调 */
    onReconnect?: () => void;
}

export type RequiredWebSocketOptions = Required<WebSocketOptions>;

export class WebSocketService {
    private ws: WebSocket | null = null;
    private reconnectCount = 0;
    private heartBeatTimer: number | null = null;
    private reconnectTimer: number | null = null;
    private isManualClose = false;

    private options: RequiredWebSocketOptions;

    constructor(options: WebSocketOptions) {
        this.options = {
            reconnectLimit: options.reconnectLimit ?? 3,
            reconnectInterval: options.reconnectInterval ?? 3000,
            heartBeatInterval: options.heartBeatInterval ?? 30000,
            heartBeatMsg: options.heartBeatMsg ?? 'heartbeat',
            onOpen: options.onOpen ?? (() => {}),
            onMessage: options.onMessage ?? (() => {}),
            onClose: options.onClose ?? (() => {}),
            onError: options.onError ?? (() => {}),
            onReconnect: options.onReconnect ?? (() => {}),
            url: options.url,
        };

        this.createWebSocket();
    }

    /**
     * 创建 WebSocket 连接
     */
    private createWebSocket(): void {
        try {
            this.ws = new WebSocket(this.options.url);
            this.initEventHandle();
        } catch (error) {
            console.error('WebSocket 创建失败:', error);
            this.reconnect();
        }
    }

    /**
     * 初始化事件监听
     */
    private initEventHandle(): void {
        if (!this.ws) return;

        this.ws.onopen = (event: Event) => {
            console.log('WebSocket 连接成功');
            this.options.onOpen(event);
            this.startHeartBeat();
            this.reconnectCount = 0;
        };

        this.ws.onmessage = (event: MessageEvent) => {
            this.options.onMessage(event);
            this.resetHeartBeat();
        };

        this.ws.onclose = (event: CloseEvent) => {
            console.log('WebSocket 连接关闭', event);
            this.options.onClose(event);
            this.stopHeartBeat();

            if (!this.isManualClose) {
                this.reconnect();
            }
        };

        this.ws.onerror = (event: Event) => {
            console.error('WebSocket 错误:', event);
            this.options.onError(event);
            this.reconnect();
        };
    }

    /**
     * 发送消息
     */
    public send(data: string | ArrayBuffer | Blob | ArrayBufferView): boolean {
        if (this.ws && this.ws.readyState === WebSocketReadyState.OPEN) {
            this.ws.send(data as string | Blob);
            return true;
        } else {
            console.warn('WebSocket 未连接，消息发送失败');
            return false;
        }
    }

    /**
     * 发送 JSON 数据
     */
    public sendJson(data: any): boolean {
        try {
            const jsonString = JSON.stringify(data);
            return this.send(jsonString);
        } catch (error) {
            console.error('JSON 序列化失败:', error);
            return false;
        }
    }

    /**
     * 重连机制
     */
    private reconnect(): void {
        if (
            this.reconnectCount >= this.options.reconnectLimit ||
            this.isManualClose
        ) {
            console.log(`重连次数已达上限 ${this.options.reconnectLimit} 次，停止重连`);
            return;
        }

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
        }

        this.reconnectCount++;
        console.log(`第 ${this.reconnectCount} 次重连...`);

        this.reconnectTimer = window.setTimeout(() => {
            this.options.onReconnect();
            this.createWebSocket();
        }, this.options.reconnectInterval);
    }

    /**
     * 开始心跳检测
     */
    private startHeartBeat(): void {
        if (this.heartBeatTimer) {
            clearInterval(this.heartBeatTimer);
        }

        this.heartBeatTimer = window.setInterval(() => {
            if (this.ws && this.ws.readyState === WebSocketReadyState.OPEN) {
                this.send(this.options.heartBeatMsg);
            } else {
                this.stopHeartBeat();
            }
        }, this.options.heartBeatInterval);
    }

    /**
     * 重置心跳检测
     */
    private resetHeartBeat(): void {
        this.stopHeartBeat();
        this.startHeartBeat();
    }

    /**
     * 停止心跳检测
     */
    private stopHeartBeat(): void {
        if (this.heartBeatTimer) {
            clearInterval(this.heartBeatTimer);
            this.heartBeatTimer = null;
        }
    }

    /**
     * 获取连接状态
     */
    public getReadyState(): WebSocketReadyState {
        return this.ws?.readyState || WebSocketReadyState.CLOSED;
    }

    /**
     * 获取重连次数
     */
    public getReconnectCount(): number {
        return this.reconnectCount;
    }

    /**
     * 手动关闭连接
     */
    public close(): void {
        this.isManualClose = true;
        this.stopHeartBeat();

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }

        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
    }

    /**
     * 重新连接（手动）
     */
    public reconnectManually(): void {
        this.isManualClose = false;
        this.reconnectCount = 0;
        this.close();
        this.createWebSocket();
    }
}
