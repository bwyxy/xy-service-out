# 实战教程：手机端死活调不出摄像头？聊聊安全上下文（Secure Context）

> 现象：桌面 Chrome 通话一切正常，手机连同一个 Wi-Fi、访问同一个地址，**摄像头和麦克风就是调不出来**。
> 更诡异的是——**控制台连报错都没有**，`navigator.mediaDevices` 打印出来是 `undefined`。
> 根源不在代码，在浏览器的**安全上下文（Secure Context）**机制。

---

## 1. 现象：不是报错，是"根本没有这个 API"

先把最容易误导人的一点说清楚。

一般我们写媒体采集是这样的：

```ts
const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
```

如果 `getUserMedia` 因为权限被拒或设备不存在而失败，你会得到一个**明确的异常**（`NotAllowedError` / `NotFoundError`）。

但在非安全上下文下，问题出在**更早一层**：

```ts
console.log(navigator.mediaDevices)   // undefined
```

**`navigator.mediaDevices` 这个属性本身是 `undefined`。** 于是 `navigator.mediaDevices.getUserMedia(...)` 会抛一个和"摄像头"毫无关系的错：

```
TypeError: Cannot read properties of undefined (reading 'getUserMedia')
```

**这就是排查的第一道坎**：错误信息指向"读取 undefined 的属性"，你会以为是代码写错了、组件没初始化、或者某个 polyfill 没加载——**绝不会第一时间想到"浏览器把 API 藏起来了"**。

> **记住这条**：如果媒体相关 API 报的是 `undefined` 而不是权限类异常，**先怀疑安全上下文，别查代码。**

---

## 2. 原理：什么是 Secure Context

W3C 定义了 **Secure Context（安全上下文）** 这个概念。浏览器只会在安全上下文里，才暴露那些**高敏感能力的 API**，包括：

- `navigator.mediaDevices`（摄像头/麦克风/屏幕共享）—— 本文主角
- `navigator.geolocation`（精确地理位置）
- `crypto.subtle`（Web Crypto 强加密）
- `navigator.serviceWorker`（Service Worker）
- `navigator.clipboard`（剪贴板读写）

**判定规则**（简化版）：

| 场景 | 是否 Secure Context |
| --- | --- |
| `https://` 任意域名 | ✅ 是 |
| `http://localhost` / `http://127.0.0.1` | ✅ 是（特例） |
| `file://` | ✅ 通常是 |
| **`http://192.168.x.x`（内网 IP）** | ❌ **否** |
| `http://example.com`（明文公网） | ❌ 否 |

**关键就是那一行内网 IP**。而手机真机调试时，我们恰恰**必须用内网 IP**——因为 `localhost` 在手机上指向手机自己，不是你的开发机。

于是形成了死结：

- 用 `localhost` → 是安全上下文，但**手机访问不到开发机**；
- 用 `192.168.x.x` → 手机能访问了，但**不是安全上下文，媒体 API 消失**。

**注意 `localhost` 的特例**：标准里 `localhost`、`127.0.0.1`、`::1` 被明确认定为 **potentially trustworthy origin**（潜在可信来源），即使走 HTTP 也算安全上下文。这就是为什么**桌面端用 `http://localhost:9001` 一切正常，手机端换 IP 就崩**。

---

## 3. 怎么确认自己遇到了这个问题

不要猜，直接用浏览器提供的判断 API：

```ts
console.log('isSecureContext:', window.isSecureContext)
```

- `false` → 你确实在非安全上下文，媒体 API 不可用是**浏览器有意为之**；
- `true` → 那是别的问题（权限、HTTPS 证书、设备占用……）。

再加一行兜底探测：

```ts
if (!navigator.mediaDevices) {
  console.warn('navigator.mediaDevices 不可用，可能处于非安全上下文')
}
```

**建议把这行检查加到通话功能的初始化里**，遇到问题能一眼定位，而不是让用户看到一个莫名其妙的 `TypeError`。

---

## 4. 方案一（开发期临时方案）：Vite 自签证书 HTTPS

既然是"HTTP + 内网 IP 不是安全上下文"，最直接的想法就是：**把 dev server 换成 HTTPS。**

Vite 支持直接传入证书，用 `vite-plugin-mkcert` 或自签证书都可以。当时的实现是加了个环境开关 `VITE_DEV_HTTPS=1`，开启时用自签证书起 HTTPS。

起起来之后，用 curl 验证端口通了：

```bash
curl -k https://192.168.5.111:9443
```

**但这条路很快暴露了它的问题：**

1. **自签证书在手机上要手动信任**——手机上打开会看到"不安全"警告，需要进系统设置里安装并信任根证书，流程繁琐；
2. **iOS 尤其麻烦**——不仅要安装描述文件，还要去「设置 → 通用 → 关于本机 → 证书信任设置」里**手动打开信任开关**，少一步都不行；
3. **每个开发者、每台手机都要重复这套操作**，团队协作成本高；
4. **证书过期后又要重来一遍**，而且排查问题时很难区分"是证书没信任"还是"是代码有问题"。

**所以这个方案最终被回退了**，`vite.config.ts` 里的 HTTPS 开关改回了原样：

```ts
// 当前 vite.config.ts —— 没有 HTTPS 开关
const isTauri = !!process.env.TAURI_ENV_PLATFORM

const config = {
  base: isTauri ? './' : '/chat',
  server: {
    port: 9001,
    host: '0.0.0.0',
    cors: true,
    origin: '//localhost:9001',
    proxy: {
      '/api': {
        target: 'http://localhost:9080',
        changeOrigin: true,
        rewrite: (path: any) => path.replace(/^\/api/, ''),
      },
    },
  },
}
```

> **诚实记录这个决策**：自签证书 HTTPS 是**开发期的临时手段**，能跑通验证，但它不是"可交付"的方案——它把成本转移给了每一个使用者（手动信任证书）。

---

## 5. 方案二（最终选择）：真实域名 + 真正的 HTTPS

最终采用的是**用真实域名 + 正式证书**：

1. 把一个域名解析到开发机/测试环境（内网 DNS 或 hosts）；
2. 用正式证书（Let's Encrypt 等）配好 HTTPS；
3. 手机访问 `https://dev.your-domain.com` → **天然是安全上下文**，`navigator.mediaDevices` 直接可用。

对比一下两条路的成本：

| 维度 | 自签证书 | 真实域名 HTTPS |
| --- | --- | --- |
| 手机端额外操作 | 装证书 + 开信任开关 | **无** |
| iOS 兼容性 | 麻烦，容易翻车 | 直接可用 |
| 团队成员上手 | 每人重复配置 | 一次配好全员受益 |
| 证书过期 | 需重新信任 | 自动续期（Let's Encrypt） |
| 与生产环境一致性 | 低 | **高** |

**结论**：只要需要手机真机联调媒体能力，**早点上真实域名 HTTPS**，别在自签证书上耗时间。自签证书适合"我就自己临时验一下"，不适合"团队要长期开发"。

---

## 6. 一个容易混淆的坑：HTTPS 不等于权限已授予

上了 HTTPS 之后，你会拿到 `navigator.mediaDevices`，但**不代表就能拿到画面**。接下来还有权限关卡：

- **浏览器权限**：用户点"拒绝"后，站点会被记住拒绝状态，需要**手动去地址栏的锁图标里改**，重新调用 `getUserMedia` 不会弹窗；
- **系统权限**：macOS / iOS / Android 还会在系统层面再拦一层——尤其是 iOS Safari，**必须由用户手势（如点击按钮）触发** `getUserMedia`，页面加载时自动调用会被直接拒绝。

**标准写法是把采集绑到用户点击上**：

```ts
async function startCall() {   // 必须由 <button @click="startCall"> 触发
  if (!navigator.mediaDevices) {
    alert('当前环境不支持音视频通话，请使用 HTTPS 访问')
    return
  }
  const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
}
```

另外记得在调用前先枚举设备，给用户一个清晰的失败提示，而不是抛 `TypeError`：

```ts
const devices = await navigator.mediaDevices.enumerateDevices()
const hasCamera = devices.some(d => d.kind === 'videoinput')
const hasMic = devices.some(d => d.kind === 'audioinput')
```

---

## 7. 排查清单（建议收藏）

手机端"调不出摄像头"，按这个顺序过一遍：

1. **访问地址是 `https://` 还是 `http://` + 内网 IP？**
   → 后者必然是**非安全上下文**，媒体 API 不可用。这是最常见原因。
2. **`window.isSecureContext` 是 `true` 吗？**
   → `false` 就别再查代码了，先把访问方式改成 HTTPS。
3. **`navigator.mediaDevices` 是 `undefined` 吗？**
   → 是的话，100% 是安全上下文问题（不是权限问题）。
4. **是 HTTPS，但拿不到流？**
   → 查浏览器站点权限 + 系统权限。
5. **iOS Safari 上自动调用失败？**
   → 必须由**用户手势**触发（点击事件里调用）。别在 `mounted` 里直接采集。
6. **之前点过"拒绝"？**
   → 站点权限被记住了，去地址栏锁图标里手动改回来。

---

## 总结

这件事的技术含量不高，但它是一类**"知识盲区型"问题**的典型代表：

> **代码没错、日志没报，但功能就是不工作——因为你对运行环境的某个隐含规则不知情。**

三个要点：

1. **`localhost` 是安全上下文的特例**——这是"桌面能跑、手机不能跑"的根源，因为桌面用 localhost、手机必须用 IP；
2. **非安全上下文下 API 是"消失"而非"报错"**——所以看到 `undefined` 要先怀疑环境，再怀疑代码；
3. **早用真实域名 HTTPS**——自签证书是开发期临时手段，成本转移给了每个使用者，**不适合长期协作**。

下次再遇到"某个浏览器 API 莫名其妙是 `undefined`"，别急着查代码——**先打一行 `window.isSecureContext`。**