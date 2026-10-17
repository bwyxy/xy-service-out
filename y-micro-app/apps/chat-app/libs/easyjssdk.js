/**
 * easyjssdk v2.0.1（悟空 IM JS SDK，ESM 产物，单文件无外部依赖）
 *
 * 来源：node_modules/.pnpm/easyjssdk@2.0.1/node_modules/easyjssdk/dist/esm/index.js
 * 原因：chat-app 为 HBuilderX 工程（无 package.json / node_modules），以内联方式引入。
 * 说明：SDK 内置 uni-app 适配器（detectPlatform 优先探测 uni.connectSocket），
 *       App 端可直接使用；其 Base64/UTF-8 依赖见 utils/polyfill.js。
 *
 * 升级 SDK 时请同步替换本文件。
 */
// import { v4 as uuidv4 } from 'uuid'; // Remove this line
// WebSocket readyState constants
const WS_CONNECTING = 0;
const WS_OPEN = 1;
const WS_CLOSING = 2;
const WS_CLOSED = 3;
// --- Platform Type Enum ---
var PlatformType;
(function (PlatformType) {
    PlatformType["Browser"] = "browser";
    PlatformType["NodeJS"] = "nodejs";
    PlatformType["WeChat"] = "wechat";
    PlatformType["Alipay"] = "alipay";
    PlatformType["UniApp"] = "uniapp";
})(PlatformType || (PlatformType = {}));
// --- WeChat Mini Program WebSocket Adapter ---
class WeChatWebSocketAdapter {
    constructor(url) {
        this.socketTask = null;
        this._readyState = WS_CONNECTING;
        this.onopen = null;
        this.onmessage = null;
        this.onerror = null;
        this.onclose = null;
        if (typeof wx === 'undefined') {
            throw new Error('WeChat Mini Program environment not detected');
        }
        this.socketTask = wx.connectSocket({
            url: url,
            success: () => {
                console.log('WeChat WebSocket connecting...');
            },
            fail: (err) => {
                console.error('WeChat WebSocket connection failed:', err);
                this._readyState = WS_CLOSED;
                if (this.onerror) {
                    this.onerror({ message: err.errMsg || 'Connection failed' });
                }
            }
        });
        this.socketTask.onOpen((res) => {
            this._readyState = WS_OPEN;
            if (this.onopen) {
                this.onopen(res);
            }
        });
        this.socketTask.onMessage((res) => {
            if (this.onmessage) {
                const data = res.data instanceof ArrayBuffer
                    ? new TextDecoder().decode(res.data)
                    : res.data;
                this.onmessage({ data });
            }
        });
        this.socketTask.onError((res) => {
            console.error('WeChat WebSocket error:', res);
            if (this.onerror) {
                this.onerror({ message: res.errMsg || 'WebSocket error' });
            }
        });
        this.socketTask.onClose((res) => {
            this._readyState = WS_CLOSED;
            if (this.onclose) {
                this.onclose({ code: res.code || 1000, reason: res.reason || '' });
            }
        });
    }
    get readyState() {
        return this._readyState;
    }
    send(data) {
        if (this._readyState !== WS_OPEN || !this.socketTask) {
            throw new Error('WebSocket is not open');
        }
        this.socketTask.send({
            data: data,
            fail: (err) => {
                console.error('WeChat WebSocket send failed:', err);
                if (this.onerror) {
                    this.onerror({ message: 'Send failed' });
                }
            }
        });
    }
    close(code, reason) {
        if (this._readyState === WS_CLOSED || this._readyState === WS_CLOSING) {
            return;
        }
        this._readyState = WS_CLOSING;
        if (this.socketTask) {
            this.socketTask.close({
                code: code || 1000,
                reason: reason || '',
                fail: (err) => {
                    console.error('WeChat WebSocket close failed:', err);
                }
            });
        }
    }
}
// --- Alipay Mini Program WebSocket Adapter ---
class AlipayWebSocketAdapter {
    constructor(url) {
        this._readyState = WS_CONNECTING;
        this.boundOnOpen = null;
        this.boundOnMessage = null;
        this.boundOnError = null;
        this.boundOnClose = null;
        this.onopen = null;
        this.onmessage = null;
        this.onerror = null;
        this.onclose = null;
        if (typeof my === 'undefined') {
            throw new Error('Alipay Mini Program environment not detected');
        }
        // Bind event handlers
        this.boundOnOpen = (res) => {
            this._readyState = WS_OPEN;
            if (this.onopen) {
                this.onopen(res);
            }
        };
        this.boundOnMessage = (res) => {
            if (this.onmessage) {
                const data = res.data instanceof ArrayBuffer
                    ? new TextDecoder().decode(res.data)
                    : res.data;
                this.onmessage({ data });
            }
        };
        this.boundOnError = (res) => {
            console.error('Alipay WebSocket error:', res);
            if (this.onerror) {
                this.onerror({ message: res.errorMessage || 'WebSocket error' });
            }
        };
        this.boundOnClose = (res) => {
            this._readyState = WS_CLOSED;
            this.cleanup();
            if (this.onclose) {
                this.onclose({ code: res.code || 1000, reason: res.reason || '' });
            }
        };
        // Register global event listeners
        my.onSocketOpen(this.boundOnOpen);
        my.onSocketMessage(this.boundOnMessage);
        my.onSocketError(this.boundOnError);
        my.onSocketClose(this.boundOnClose);
        // Connect
        my.connectSocket({
            url: url,
            success: () => {
                console.log('Alipay WebSocket connecting...');
            },
            fail: (err) => {
                console.error('Alipay WebSocket connection failed:', err);
                this._readyState = WS_CLOSED;
                this.cleanup();
                if (this.onerror) {
                    this.onerror({ message: err.errorMessage || 'Connection failed' });
                }
            }
        });
    }
    get readyState() {
        return this._readyState;
    }
    send(data) {
        if (typeof my === 'undefined') {
            throw new Error('Alipay Mini Program environment not detected');
        }
        if (this._readyState !== WS_OPEN) {
            throw new Error('WebSocket is not open');
        }
        my.sendSocketMessage({
            data: data,
            fail: (err) => {
                console.error('Alipay WebSocket send failed:', err);
                if (this.onerror) {
                    this.onerror({ message: 'Send failed' });
                }
            }
        });
    }
    close(code, reason) {
        if (typeof my === 'undefined') {
            return;
        }
        if (this._readyState === WS_CLOSED || this._readyState === WS_CLOSING) {
            return;
        }
        this._readyState = WS_CLOSING;
        my.closeSocket({
            code: code || 1000,
            reason: reason || '',
            fail: (err) => {
                console.error('Alipay WebSocket close failed:', err);
            }
        });
    }
    cleanup() {
        if (typeof my !== 'undefined') {
            my.offSocketOpen(this.boundOnOpen);
            my.offSocketMessage(this.boundOnMessage);
            my.offSocketError(this.boundOnError);
            my.offSocketClose(this.boundOnClose);
            this.boundOnOpen = null;
            this.boundOnMessage = null;
            this.boundOnError = null;
            this.boundOnClose = null;
        }
    }
}
// --- UniApp WebSocket Adapter ---
class UniAppWebSocketAdapter {
    constructor(url) {
        this.socketTask = null;
        this._readyState = WS_CONNECTING;
        this.onopen = null;
        this.onmessage = null;
        this.onerror = null;
        this.onclose = null;
        if (typeof uni === 'undefined') {
            throw new Error('UniApp environment not detected');
        }
        this.socketTask = uni.connectSocket({
            url: url,
            success: () => {
                console.log('UniApp WebSocket connecting...');
            },
            fail: (err) => {
                console.error('UniApp WebSocket connection failed:', err);
                this._readyState = WS_CLOSED;
                if (this.onerror) {
                    this.onerror({ message: err.errMsg || 'Connection failed' });
                }
            }
        });
        this.socketTask.onOpen((res) => {
            this._readyState = WS_OPEN;
            if (this.onopen) {
                this.onopen(res);
            }
        });
        this.socketTask.onMessage((res) => {
            if (this.onmessage) {
                const data = res.data instanceof ArrayBuffer
                    ? new TextDecoder().decode(res.data)
                    : res.data;
                this.onmessage({ data });
            }
        });
        this.socketTask.onError((res) => {
            console.error('UniApp WebSocket error:', res);
            if (this.onerror) {
                this.onerror({ message: res.errMsg || 'WebSocket error' });
            }
        });
        this.socketTask.onClose((res) => {
            this._readyState = WS_CLOSED;
            if (this.onclose) {
                this.onclose({ code: res.code || 1000, reason: res.reason || '' });
            }
        });
    }
    get readyState() {
        return this._readyState;
    }
    send(data) {
        if (this._readyState !== WS_OPEN || !this.socketTask) {
            throw new Error('WebSocket is not open');
        }
        this.socketTask.send({
            data: data,
            fail: (err) => {
                console.error('UniApp WebSocket send failed:', err);
                if (this.onerror) {
                    this.onerror({ message: 'Send failed' });
                }
            }
        });
    }
    close(code, reason) {
        if (this._readyState === WS_CLOSED || this._readyState === WS_CLOSING) {
            return;
        }
        this._readyState = WS_CLOSING;
        if (this.socketTask) {
            this.socketTask.close({
                code: code || 1000,
                reason: reason || '',
                fail: (err) => {
                    console.error('UniApp WebSocket close failed:', err);
                }
            });
        }
    }
}
// --- Platform Detection and WebSocket Factory ---
function detectPlatform() {
    // Check for UniApp first (it may also have wx defined in WeChat mini program mode)
    if (typeof uni !== 'undefined' && typeof uni.connectSocket === 'function') {
        return PlatformType.UniApp;
    }
    // Check for WeChat Mini Program
    if (typeof wx !== 'undefined' && typeof wx.connectSocket === 'function') {
        return PlatformType.WeChat;
    }
    // Check for Alipay Mini Program
    if (typeof my !== 'undefined' && typeof my.connectSocket === 'function') {
        return PlatformType.Alipay;
    }
    // Check for browser WebSocket
    if (typeof WebSocket !== 'undefined') {
        return PlatformType.Browser;
    }
    // Fallback to Node.js
    return PlatformType.NodeJS;
}
let currentPlatform;
function getPlatform() {
    if (currentPlatform === undefined) {
        currentPlatform = detectPlatform();
    }
    return currentPlatform;
}
// Factory function to create platform-appropriate WebSocket
function createWebSocket(url) {
    switch (getPlatform()) {
        case PlatformType.UniApp:
            return new UniAppWebSocketAdapter(url);
        case PlatformType.WeChat:
            return new WeChatWebSocketAdapter(url);
        case PlatformType.Alipay:
            return new AlipayWebSocketAdapter(url);
        case PlatformType.Browser:
            return new WebSocket(url);
        case PlatformType.NodeJS:
        default:
            try {
                // eslint-disable-next-line @typescript-eslint/no-var-requires
                const dynamicRequire = new Function('mod', 'return require(mod)');
                const Ws = dynamicRequire('ws');
                const WsImpl = Ws.WebSocket || Ws;
                return new WsImpl(url);
            }
            catch (e) {
                throw new Error('WebSocket is not available in this environment. Install \'ws\' package for Node.js.');
            }
    }
}
// Export platform info for debugging
export { PlatformType, getPlatform, currentPlatform };
// --- Enums and Types ---
/**
 * Channel Type Enum based on WuKongIM protocol
 */
export var ChannelType;
(function (ChannelType) {
    /** Person channel */
    ChannelType[ChannelType["Person"] = 1] = "Person";
    /** Group channel */
    ChannelType[ChannelType["Group"] = 2] = "Group";
    /** Customer Service channel (Consider using Visitors channel instead) */
    ChannelType[ChannelType["CustomerService"] = 3] = "CustomerService";
    /** Community channel */
    ChannelType[ChannelType["Community"] = 4] = "Community";
    /** Community Topic channel */
    ChannelType[ChannelType["CommunityTopic"] = 5] = "CommunityTopic";
    /** Info channel (with concept of temporary subscribers) */
    ChannelType[ChannelType["Info"] = 6] = "Info";
    /** Data channel */
    ChannelType[ChannelType["Data"] = 7] = "Data";
    /** Temporary channel */
    ChannelType[ChannelType["Temp"] = 8] = "Temp";
    /** Live channel (does not save recent session data) */
    ChannelType[ChannelType["Live"] = 9] = "Live";
    /** Visitors channel (replaces CustomerService for new implementations) */
    ChannelType[ChannelType["Visitors"] = 10] = "Visitors";
})(ChannelType || (ChannelType = {}));
/**
 * SDK Event Names Enum
 */
export var Event;
(function (Event) {
    /** Connection successfully established and authenticated */
    Event["Connect"] = "connect";
    /** Disconnected from server */
    Event["Disconnect"] = "disconnect";
    /** Received a message */
    Event["Message"] = "message";
    /** An error occurred (WebSocket error, connection error, etc.) */
    Event["Error"] = "error";
    /** Received acknowledgment for a sent message */
    Event["SendAck"] = "sendack";
    /** The SDK is attempting to reconnect */
    Event["Reconnecting"] = "reconnecting";
    /** Received a custom event notification from the server */
    Event["CustomEvent"] = "customevent";
})(Event || (Event = {}));
/**
 * Reason codes for operation results and disconnect causes.
 * Mirrors the server-side ReasonCode (Go) values; do not reorder to preserve numeric mapping.
 */
export var ReasonCode;
(function (ReasonCode) {
    /** Unknown error */
    ReasonCode[ReasonCode["Unknown"] = 0] = "Unknown";
    /** Success */
    ReasonCode[ReasonCode["Success"] = 1] = "Success";
    /** Authentication failed */
    ReasonCode[ReasonCode["AuthFail"] = 2] = "AuthFail";
    /** Subscriber does not exist in the channel */
    ReasonCode[ReasonCode["SubscriberNotExist"] = 3] = "SubscriberNotExist";
    /** In blacklist */
    ReasonCode[ReasonCode["InBlacklist"] = 4] = "InBlacklist";
    /** Channel does not exist */
    ReasonCode[ReasonCode["ChannelNotExist"] = 5] = "ChannelNotExist";
    /** User not on node */
    ReasonCode[ReasonCode["UserNotOnNode"] = 6] = "UserNotOnNode";
    /** Sender is offline; message cannot be delivered */
    ReasonCode[ReasonCode["SenderOffline"] = 7] = "SenderOffline";
    /** Message key error; the message is invalid */
    ReasonCode[ReasonCode["MsgKeyError"] = 8] = "MsgKeyError";
    /** Payload decode failed */
    ReasonCode[ReasonCode["PayloadDecodeError"] = 9] = "PayloadDecodeError";
    /** Forwarding send packet failed */
    ReasonCode[ReasonCode["ForwardSendPacketError"] = 10] = "ForwardSendPacketError";
    /** Not allowed to send */
    ReasonCode[ReasonCode["NotAllowSend"] = 11] = "NotAllowSend";
    /** Connection kicked */
    ReasonCode[ReasonCode["ConnectKick"] = 12] = "ConnectKick";
    /** Not in whitelist */
    ReasonCode[ReasonCode["NotInWhitelist"] = 13] = "NotInWhitelist";
    /** Failed to query user token */
    ReasonCode[ReasonCode["QueryTokenError"] = 14] = "QueryTokenError";
    /** System error */
    ReasonCode[ReasonCode["SystemError"] = 15] = "SystemError";
    /** Invalid channel ID */
    ReasonCode[ReasonCode["ChannelIDError"] = 16] = "ChannelIDError";
    /** Node matching error */
    ReasonCode[ReasonCode["NodeMatchError"] = 17] = "NodeMatchError";
    /** Node not matched */
    ReasonCode[ReasonCode["NodeNotMatch"] = 18] = "NodeNotMatch";
    /** Channel is banned */
    ReasonCode[ReasonCode["Ban"] = 19] = "Ban";
    /** Unsupported header */
    ReasonCode[ReasonCode["NotSupportHeader"] = 20] = "NotSupportHeader";
    /** clientKey is empty */
    ReasonCode[ReasonCode["ClientKeyIsEmpty"] = 21] = "ClientKeyIsEmpty";
    /** Rate limit */
    ReasonCode[ReasonCode["RateLimit"] = 22] = "RateLimit";
    /** Unsupported channel type */
    ReasonCode[ReasonCode["NotSupportChannelType"] = 23] = "NotSupportChannelType";
    /** Channel disbanded */
    ReasonCode[ReasonCode["Disband"] = 24] = "Disband";
    /** Sending is banned */
    ReasonCode[ReasonCode["SendBan"] = 25] = "SendBan";
})(ReasonCode || (ReasonCode = {}));
/**
 * Device Flag Enum based on WuKongIM protocol
 */
export var DeviceFlag;
(function (DeviceFlag) {
    /** Mobile APP */
    DeviceFlag[DeviceFlag["App"] = 0] = "App";
    /** Web Browser */
    DeviceFlag[DeviceFlag["Web"] = 1] = "Web";
    /** Desktop App */
    DeviceFlag[DeviceFlag["Desktop"] = 2] = "Desktop";
})(DeviceFlag || (DeviceFlag = {}));
// --- WKIM Class ---
export class WKIM {
    constructor(url, auth) {
        this.ws = null;
        this.isConnected = false;
        this.connectionPromise = null;
        this.pingInterval = null;
        this.pingTimeout = null;
        this.PING_INTERVAL_MS = 25 * 1000; // Send ping every 25 seconds
        this.PONG_TIMEOUT_MS = 10 * 1000; // Expect pong within 10 seconds
        this.pendingRequests = new Map();
        this.eventListeners = new Map();
        // Reconnection properties
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.initialReconnectDelay = 1000;
        this.isReconnecting = false;
        this.manualDisconnect = false;
        this.beforeUnloadHandler = null;
        this.url = url;
        this.auth = auth || {};
        this.sessionId = this.generateUUID(); // Unique session identifier
        // Ensure unique deviceId for each session
        if (!this.auth.deviceId || this.auth.deviceId === '') {
            this.auth.deviceId = `web_${this.sessionId.slice(0, 8)}_${Date.now()}`;
        }
        // Ensure Event enum values are used for internal map keys
        Object.values(Event).forEach(event => this.eventListeners.set(event, []));
        // Setup beforeunload handler to cleanup connection on page refresh/close
        this.setupBeforeUnloadHandler();
    }
    /**
     * Initializes the WKIM instance.
     * @param url WebSocket server URL (e.g., "ws://localhost:5100")
     * @param auth Authentication options { uid, token, ... }
     * @param options Configuration options { singleton: boolean }
     * @returns A WKIM instance
     */
    static init(url, auth, options = {}) {
        if (!url || !auth || !auth.uid || !auth.token) {
            throw new Error("URL, uid, and token are required for initialization.");
        }
        // If singleton mode is enabled and there's an existing instance, disconnect it first
        if (options.singleton && WKIM.globalInstance) {
            console.log("Destroying previous global instance...");
            WKIM.globalInstance.destroy();
        }
        const instance = new WKIM(url, auth);
        if (options.singleton !== false) {
            WKIM.globalInstance = instance;
        }
        return instance;
    }
    /**
     * Establishes connection and authenticates with the server.
     * Returns a Promise that resolves on successful connection/authentication,
     * or rejects on failure.
     */
    connect() {
        return new Promise((resolve, reject) => {
            var _a;
            if (this.isConnected || ((_a = this.ws) === null || _a === void 0 ? void 0 : _a.readyState) === WS_CONNECTING) {
                console.warn("Connection already established or in progress.");
                // If already connected, resolve immediately. If connecting, wait for existing promise.
                if (this.isConnected) {
                    resolve();
                }
                else if (this.connectionPromise) {
                    this.connectionPromise.resolve = resolve; // Chain the promises
                    this.connectionPromise.reject = reject;
                }
                else {
                    reject(new Error("Already connecting, but no connection promise found."));
                }
                return;
            }
            // On a new connect call, reset manual disconnect flag
            this.manualDisconnect = false;
            this.connectionPromise = { resolve, reject };
            try {
                console.log(`Connecting to ${this.url}... (Platform: ${getPlatform()})`);
                this.ws = createWebSocket(this.url);
                this.ws.onopen = () => {
                    console.log("WebSocket connection opened. Authenticating...");
                    this.sendConnectRequest();
                };
                this.ws.onmessage = (event) => {
                    this.handleMessage(event.data);
                };
                this.ws.onerror = (event) => {
                    const errorMessage = event.message || (event.error ? event.error.message : 'WebSocket error');
                    console.error("WebSocket error:", errorMessage, event);
                    this.emit(Event.Error, event.error || new Error(errorMessage));
                    // The 'onclose' event will be fired next, which will handle cleanup and reconnection logic.
                };
                this.ws.onclose = (event) => {
                    const wasConnected = this.isConnected;
                    console.log(`WebSocket connection closed. Code: ${event.code}, Reason: ${event.reason}`);
                    if (this.connectionPromise && !this.isConnected) { // Reject connect promise if closed before connect ack
                        this.connectionPromise.reject(new Error(`Connection closed before authentication (Code: ${event.code})`));
                    }
                    this.cleanupConnection(); // Clean up state like intervals, pending requests.
                    this.emit(Event.Disconnect, { code: event.code, reason: event.reason });
                    // Only try to reconnect if we were previously connected and it wasn't a manual disconnect.
                    if (wasConnected && !this.manualDisconnect) {
                        this.tryReconnect();
                    }
                };
            }
            catch (error) {
                console.error("Failed to create WebSocket:", error);
                this.emit(Event.Error, error);
                if (this.connectionPromise) {
                    this.connectionPromise.reject(error);
                    this.connectionPromise = null;
                }
                this.cleanupConnection();
            }
        });
    }
    /**
     * Disconnects from the server.
     */
    disconnect() {
        console.log("Manual disconnect initiated.");
        this.manualDisconnect = true;
        this.isReconnecting = false; // Stop any ongoing reconnection attempts
        this.cleanupBeforeUnloadHandler(); // Remove page unload listeners
        this.handleDisconnect(true, "Manual disconnection");
    }
    /**
     * Completely destroys the SDK instance, cleaning up all resources.
     * Call this when you no longer need the SDK instance.
     */
    destroy() {
        console.log("Destroying SDK instance...");
        this.disconnect();
        this.eventListeners.clear();
        this.pendingRequests.clear();
        // Clear global instance if this is it
        if (WKIM.globalInstance === this) {
            WKIM.globalInstance = null;
        }
    }
    /**
     * Sends a message to a specific channel.
     * @param channelId Target channel ID
     * @param channelType Target channel type (e.g., WKIM.ChannelType.Person)
     * @param payload Message payload (must be a JSON-serializable object)
     * @param options Optional: { clientMsgNo, header, setting, msgKey, expire, topic }
     * @returns Promise resolving with { messageId, messageSeq } on server ack, or rejecting on error.
     */
    send(channelId, channelType, payload, options = {}) {
        if (!this.isConnected || !this.ws || this.ws.readyState !== WS_OPEN) {
            return Promise.reject(new Error("Not connected. Call connect() first."));
        }
        if (typeof payload !== 'object' || payload === null) {
            return Promise.reject(new Error("Payload must be a non-null object."));
        }
        const header = options.header || {};
        header.redDot = true;
        const clientMsgNo = options.clientMsgNo || this.generateUUID(); // Generate a unique message ID if not provided
        const params = {
            clientMsgNo: clientMsgNo,
            channelId: channelId,
            channelType: channelType,
            payload: this._encodePayloadToBase64(payload),
            header: header,
            topic: options.topic,
            setting: options.setting,
        };
        return this.sendRequest('send', params);
    }
    /**
     * Registers an event listener.
     * @param eventName The event to listen for (e.g., WKIM.Event.Message)
     * @param callback The function to call when the event occurs
     */
    on(eventName, callback) {
        var _a;
        if (this.eventListeners.has(eventName)) {
            (_a = this.eventListeners.get(eventName)) === null || _a === void 0 ? void 0 : _a.push(callback);
        }
        else {
            console.warn(`Attempted to register listener for unknown event: ${eventName}`);
        }
    }
    /**
    * Removes an event listener.
    * @param eventName The event to stop listening for
    * @param callback The specific callback function to remove
    */
    off(eventName, callback) {
        if (this.eventListeners.has(eventName)) {
            const listeners = this.eventListeners.get(eventName);
            if (listeners) {
                const index = listeners.indexOf(callback);
                if (index > -1) {
                    listeners.splice(index, 1);
                }
            }
        }
    }
    // --- Private Methods ---
    emit(eventName, ...args) {
        const listeners = this.eventListeners.get(eventName);
        if (listeners) {
            listeners.forEach(callback => {
                try {
                    callback(...args);
                }
                catch (error) {
                    console.error(`Error in event listener for ${eventName}:`, error);
                }
            });
        }
    }
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
    sendConnectRequest() {
        var _a;
        const params = {
            uid: this.auth.uid,
            token: this.auth.token,
            deviceId: this.auth.deviceId,
            deviceFlag: (_a = this.auth.deviceFlag) !== null && _a !== void 0 ? _a : DeviceFlag.Web, // Default to WEB
            clientTimestamp: Date.now(),
            // Add version, clientKey if needed
        };
        this.sendRequest('connect', params, 5000) // 5s timeout for connect
            .then(result => {
            console.log("Authentication successful:", result);
            this.isConnected = true;
            // Reset reconnection state on successful connect
            this.reconnectAttempts = 0;
            this.isReconnecting = false;
            this.manualDisconnect = false;
            this.startPing();
            this.emit(Event.Connect, result);
            if (this.connectionPromise) {
                this.connectionPromise.resolve();
                this.connectionPromise = null;
            }
        })
            .catch(error => {
            console.error("Authentication failed:", error);
            this.emit(Event.Error, new Error(`Authentication failed: ${error.message || JSON.stringify(error)}`));
            if (this.connectionPromise) {
                this.connectionPromise.reject(error);
                this.connectionPromise = null;
            }
            // Don't start reconnection on auth failure, it's a permanent error.
            this.handleDisconnect(false, "Authentication failed"); // Close connection on auth failure
            this.cleanupConnection(); // Ensure ws is closed
        });
    }
    sendRequest(method, params, timeoutMs = 15000) {
        return new Promise((resolve, reject) => {
            if (!this.ws || this.ws.readyState !== WS_OPEN) {
                return reject(new Error("WebSocket is not open."));
            }
            const requestId = this.generateUUID(); // Generate a unique request ID
            const request = {
                method: method,
                params: params,
                id: requestId
            };
            const timeoutTimer = setTimeout(() => {
                this.pendingRequests.delete(requestId);
                reject(new Error(`Request timeout for method ${method} (id: ${requestId})`));
            }, timeoutMs);
            this.pendingRequests.set(requestId, { resolve, reject, timeoutTimer });
            try {
                console.debug(`--> Sending request (id: ${requestId}):`, JSON.stringify(request));
                this.ws.send(JSON.stringify(request));
            }
            catch (error) {
                clearTimeout(timeoutTimer);
                this.pendingRequests.delete(requestId);
                console.error(`Error sending request (id: ${requestId}):`, error);
                reject(error);
            }
        });
    }
    sendNotification(method, params) {
        if (!this.ws || this.ws.readyState !== WS_OPEN) {
            console.error("Cannot send notification, WebSocket is not open.");
            return;
        }
        const notification = {
            method: method,
            params: params
        };
        console.debug(`--> Sending notification:`, JSON.stringify(notification));
        try {
            this.ws.send(JSON.stringify(notification));
        }
        catch (error) {
            console.error(`Error sending notification (${method}):`, error);
            this.emit(Event.Error, new Error(`Failed to send notification ${method}: ${error}`));
        }
    }
    handleMessage(data) {
        console.debug("<-- Received raw:", data);
        let message;
        try {
            message = JSON.parse(data.toString());
        }
        catch (error) {
            console.error("Failed to parse incoming message:", error, data);
            this.emit(Event.Error, new Error(`Failed to parse message: ${error}`));
            return;
        }
        if ('id' in message) { // It's a Response
            this.handleResponse(message);
        }
        else if ('method' in message) { // It's a Notification
            this.handleNotification(message);
        }
        else {
            console.warn("Received unknown message format:", message);
        }
    }
    handleResponse(response) {
        console.debug(`<-- Handling response (id: ${response.id}):`, response);
        const pending = this.pendingRequests.get(response.id);
        if (pending) {
            clearTimeout(pending.timeoutTimer);
            this.pendingRequests.delete(response.id);
            if (response.error) {
                pending.reject(response.error);
            }
            else {
                pending.resolve(response.result);
            }
        }
        else {
            console.warn(`Received response for unknown request ID: ${response.id}`);
        }
    }
    handleNotification(notification) {
        var _a, _b;
        console.debug(`<-- Handling notification (${notification.method}):`, notification.params);
        switch (notification.method) {
            case 'recv':
                const messageData = notification.params;
                if (typeof messageData.payload === 'string') {
                    try {
                        messageData.payload = JSON.parse(this._decodeBase64ToStr(messageData.payload));
                    }
                    catch (e) {
                        // Keep original string if decode/parse fails
                    }
                }
                this.emit(Event.Message, messageData);
                // Automatically acknowledge receipt
                this.sendRecvAck(messageData.header, messageData.messageId, messageData.messageSeq);
                break;
            case 'pong':
                this.handlePong();
                break;
            case 'disconnect':
                console.warn('Server initiated disconnect:', notification.params);
                this.emit(Event.Disconnect, notification.params); // Emit server reason
                this.handleDisconnect(false, `Server disconnected: ${((_a = notification.params) === null || _a === void 0 ? void 0 : _a.reason) || ((_b = notification.params) === null || _b === void 0 ? void 0 : _b.reasonCode)}`); // Close locally
                break;
            case 'event':
                this.handleEventNotification(notification.params);
                break;
            // Handle other notifications if needed
            default:
                console.warn(`Received unhandled notification method: ${notification.method}`);
        }
    }
    /**
     * Encode payload object to base64 string.
     * WuKongIM server expects payload as []byte (base64 in JSON).
     */
    _encodePayloadToBase64(payload) {
        const json = JSON.stringify(payload);
        // Browser environment
        if (typeof TextEncoder !== 'undefined' && typeof btoa !== 'undefined') {
            const bytes = new TextEncoder().encode(json);
            let binary = '';
            for (let i = 0; i < bytes.length; i++) {
                binary += String.fromCharCode(bytes[i]);
            }
            return btoa(binary);
        }
        // Node.js environment
        if (typeof Buffer !== 'undefined') {
            return Buffer.from(json, 'utf-8').toString('base64');
        }
        // WeChat Mini Program environment
        if (typeof wx !== 'undefined' && (wx === null || wx === void 0 ? void 0 : wx.arrayBufferToBase64)) {
            const encoder = new TextEncoder();
            const bytes = encoder.encode(json);
            return wx.arrayBufferToBase64(bytes.buffer);
        }
        throw new Error('No base64 encoding method available');
    }
    /**
     * Decode base64 string to UTF-8 string.
     */
    _decodeBase64ToStr(base64Str) {
        // Browser environment
        if (typeof atob !== 'undefined' && typeof TextDecoder !== 'undefined') {
            const binary = atob(base64Str);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
            }
            return new TextDecoder().decode(bytes);
        }
        // Node.js environment
        if (typeof Buffer !== 'undefined') {
            return Buffer.from(base64Str, 'base64').toString('utf-8');
        }
        // WeChat Mini Program environment
        if (typeof wx !== 'undefined' && (wx === null || wx === void 0 ? void 0 : wx.base64ToArrayBuffer)) {
            const buf = wx.base64ToArrayBuffer(base64Str);
            return new TextDecoder().decode(new Uint8Array(buf));
        }
        throw new Error('No base64 decoding method available');
    }
    sendRecvAck(header, messageId, messageSeq) {
        // Per protocol, recvack is a request, but usually doesn't need a response processed
        // Sending as a notification might be simpler if the server allows it,
        // but sticking to the doc: send as request, ignore response.
        const params = { header, messageId, messageSeq };
        this.sendNotification('recvack', params);
        // Alternative: Send as notification if server supports it (non-standard)
        // this.sendNotification('recvack', params);
    }
    /**
     * Handles incoming event notifications from the server
     * @param params Event notification parameters
     */
    handleEventNotification(params) {
        try {
            const eventData = {
                header: params.header,
                id: params.id,
                type: params.type,
                timestamp: params.timestamp,
                data: params.data
            };
            // Validate required fields
            if (!eventData.id || !eventData.type) {
                console.error('Invalid event notification: missing required fields', params);
                this.emit(Event.Error, new Error('Invalid event notification: missing required fields'));
                return;
            }
            // Try to parse data if it's a JSON string
            if (typeof eventData.data === 'string') {
                try {
                    eventData.data = JSON.parse(eventData.data);
                }
                catch (e) {
                    // Keep as string if not valid JSON
                    console.debug('Event data is not JSON, keeping as string');
                }
            }
            console.log(`Event notification received: type=${eventData.type}, id=${eventData.id}`);
            // Emit the custom event to registered listeners
            this.emit(Event.CustomEvent, eventData);
        }
        catch (error) {
            console.error('Error handling event notification:', error);
            this.emit(Event.Error, new Error(`Failed to handle event notification: ${error}`));
        }
    }
    startPing() {
        this.stopPing(); // Clear existing timers
        this.pingInterval = setInterval(() => {
            if (this.ws && this.ws.readyState === WS_OPEN) {
                this.sendRequest('ping', {}, this.PONG_TIMEOUT_MS)
                    .then(this.handlePong.bind(this)) // Technically pong is a notification, but use req/res for timeout
                    .catch(err => {
                    console.error("Ping failed or timed out:", err);
                    this.emit(Event.Error, new Error(`Ping timeout: ${(err === null || err === void 0 ? void 0 : err.message) || err}`));
                    // Treat ping timeout as an unhealthy connection: close and reconnect
                    if (!this.manualDisconnect) {
                        this.handleDisconnect(false, "Ping timeout");
                        this.tryReconnect();
                    }
                });
            }
            else {
                this.stopPing(); // Stop if WS is not open
            }
        }, this.PING_INTERVAL_MS);
        console.log(`Ping interval started (${this.PING_INTERVAL_MS}ms).`);
    }
    stopPing() {
        if (this.pingInterval) {
            clearInterval(this.pingInterval);
            this.pingInterval = null;
            console.log("Ping interval stopped.");
        }
        if (this.pingTimeout) {
            clearTimeout(this.pingTimeout);
            this.pingTimeout = null;
        }
    }
    handlePong() {
        // console.debug("Pong received.");
        // Reset pong timeout if using one (mainly handled by sendRequest timeout now)
    }
    handleDisconnect(graceful, reason) {
        console.log(`Handling disconnect. Graceful: ${graceful}, Reason: ${reason}`);
        if (this.ws) {
            this.stopPing();
            if (graceful && this.ws.readyState === WS_OPEN) {
                // Use standard close codes: 1000 (normal), 3000-4999 (custom)
                this.ws.close(1000, "Client disconnected"); // Normal closure
            }
            else if (this.ws.readyState === WS_CONNECTING || this.ws.readyState === WS_OPEN) {
                // Force close with custom code for abnormal situations
                this.ws.close(3001, reason.substring(0, 123)); // Custom code, limit reason length
            }
        }
        this.cleanupConnection(); // Clean up state regardless of how close happened
    }
    cleanupConnection() {
        console.log("Cleaning up connection resources.");
        this.isConnected = false;
        this.stopPing();
        // Reject any pending requests
        this.pendingRequests.forEach((pending) => {
            clearTimeout(pending.timeoutTimer);
            pending.reject(new Error("Connection closed"));
        });
        this.pendingRequests.clear();
        // Clear connection promise if it exists and hasn't resolved/rejected
        if (this.connectionPromise) {
            // Only reject if we're not in a reconnection loop that will try again.
            if (!this.isReconnecting && !this.isConnected) {
                this.connectionPromise.reject(new Error("Connection closed during operation"));
            }
            this.connectionPromise = null;
        }
        // Don't nullify ws immediately if onclose handler needs it, but ensure no further ops
        if (this.ws) {
            // Remove listeners to prevent potential memory leaks and duplicate handling
            this.ws.onopen = null;
            this.ws.onmessage = null;
            this.ws.onerror = null;
            this.ws.onclose = null;
            // Consider setting this.ws = null here or after a short delay if needed
        }
        // Do NOT clear eventListeners here, user might want to reconnect.
    }
    // --- Reconnection Methods ---
    setupBeforeUnloadHandler() {
        // Only setup in browser environment
        if (typeof window !== 'undefined') {
            this.beforeUnloadHandler = () => {
                console.log('Page unloading, closing WebSocket connection...');
                this.manualDisconnect = true;
                this.isReconnecting = false;
                if (this.ws && this.ws.readyState === WS_OPEN) {
                    this.ws.close(1000, 'Page unloaded');
                }
            };
            window.addEventListener('beforeunload', this.beforeUnloadHandler);
            window.addEventListener('pagehide', this.beforeUnloadHandler);
        }
    }
    cleanupBeforeUnloadHandler() {
        if (typeof window !== 'undefined' && this.beforeUnloadHandler) {
            window.removeEventListener('beforeunload', this.beforeUnloadHandler);
            window.removeEventListener('pagehide', this.beforeUnloadHandler);
            this.beforeUnloadHandler = null;
        }
    }
    tryReconnect() {
        if (this.isReconnecting || this.manualDisconnect) {
            return;
        }
        // The onclose event handler should have already called cleanupConnection.
        this.isReconnecting = true;
        this.scheduleReconnect();
    }
    scheduleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error("Max reconnect attempts reached. Giving up.");
            this.isReconnecting = false;
            this.reconnectAttempts = 0;
            this.emit(Event.Error, new Error("Reconnection failed."));
            return;
        }
        const delay = this.initialReconnectDelay * Math.pow(2, this.reconnectAttempts);
        this.reconnectAttempts++;
        console.log(`Will attempt to reconnect in ${delay / 1000}s (Attempt ${this.reconnectAttempts}).`);
        this.emit(Event.Reconnecting, { attempt: this.reconnectAttempts, delay });
        setTimeout(() => {
            // Check if a manual disconnect happened while waiting
            if (!this.isReconnecting) {
                console.log("Reconnection aborted.");
                return;
            }
            this.connect().catch(() => {
                // connect() rejects if it fails. Schedule the next attempt.
                if (this.isReconnecting) {
                    this.scheduleReconnect();
                }
            });
        }, delay);
    }
}
WKIM.globalInstance = null;
// Export ChannelType and Event enums alongside the class for easier use
export { ChannelType as WKIMChannelType, Event as WKIMEvent, DeviceFlag as WKIMDeviceFlag };
//# sourceMappingURL=index.js.map