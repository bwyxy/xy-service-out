/**
 * 公共vite 配置文件
 * */
export default {
    plugins: [
        {
            name: 'wasm-mime-type',
            configureServer(server :any) {
                server.middlewares.use((req :any, res :any, next :any) => {
                    if (req.url?.includes('.wasm')) {
                        const originalSetHeader = res.setHeader.bind(res)
                        res.setHeader = (name: string, value: string | number | string[]) => {
                            if (String(name).toLowerCase() === 'content-type') {
                                return originalSetHeader('Content-Type', 'application/wasm')
                            }
                            return originalSetHeader(name, value)
                        }
                    }
                    next()
                })
            }
        },
    ],
    build: {
        sourcemap: false,
        minify: 'esbuild',
        chunkSizeWarningLimit: 3800,
        // 资源输出目录
        assetsDir: 'assets',
        assetsInlineLimit: 4096,
        rollupOptions: {
            output: {
                manualChunks(id :any) {
                    if (id.includes('node_modules')) {
                        // 获取 node_modules/ 之后的路径
                        const path = id.split('node_modules/')[1];

                        // 如果是 pnpm 的 .pnpm 目录
                        if (path.startsWith('.pnpm/')) {
                            // 提取真实包名：如 .pnpm/@vueuse+core@14.3.0_... 提取 @vueuse/core
                            // 或者 .pnpm/vue@3.2.0_... 提取 vue
                            const parts = path.split('/');
                            let pkg = parts[1]; // 第二部分，如 @vueuse+core@14.3.0_...
                            // 去掉版本号（@version）和尾随的 _ 等
                            if (pkg.startsWith('@')) {
                                // scoped 包：@vueuse+core  -> @vueuse/core
                                pkg = pkg.replace(/\+/, '/');
                            } else {
                                // 普通包：vue@3.2.0  -> vue
                                pkg = pkg.split('@')[0];
                            }
                            // 如果还有 + 号（如某些包名中含 +，但少见），可做进一步清理
                            return pkg;
                        } else {// 非 pnpm（扁平结构），直接取第一个目录名
                            return path.split('/')[0];
                        }
                    }
                },
            }
        }
    }
}
