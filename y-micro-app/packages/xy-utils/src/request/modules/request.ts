import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import NetworkRetryHandler from "../handler/NetworkRetryHandler";

export interface RequestConfig extends AxiosRequestConfig {
    // 可以扩展自定义字段，比如是否显示全局loading等
    showLoading?: boolean;
}

export interface CreateAxiosOptions {
    // 每个应用的独立配置，会与公共默认配置合并
    config?: RequestConfig;
    // 拦截器可自定义
    interceptors?: {
        request?: (config: RequestConfig) => RequestConfig;
        response?: (response: AxiosResponse) => AxiosResponse;
        responseError?: (error: any) => any;
    };
}

/**
 * 创建 Axios 实例的函数
 * @param options 每个应用独立传入的配置
 * @returns AxiosInstance
 */
export function createAxios(options: CreateAxiosOptions = {}): AxiosInstance {
    // 公共默认配置
    const defaultConfig: RequestConfig = {
        timeout: 15000,
        headers: {
            'Content-Type': 'application/json',
        },
        // 其他通用配置
    };

    // 合并配置（应用配置优先级更高）
    const finalConfig: RequestConfig = {
        ...defaultConfig,
        ...options.config,
        headers: {
            ...defaultConfig.headers,
            ...options.config?.headers,
        },
    };

    console.log('finalConfig=====', finalConfig)
    const instance = axios.create(finalConfig);

    // 公共请求拦截器
    instance.interceptors.request.use(
        (config: any) => {

            return config;
        },
        // @ts-ignore
        (error) => Promise.reject(error)
    );

    // 公共响应拦截器（通用错误处理）
    instance.interceptors.response.use(
        (response: AxiosResponse) => {

            console.log('response=====', response);
            return response;
        },
        (error) => {
            // 网络错误、超时等统一处理
            console.error('网络请求失败：', error.message);
            // @ts-ignore
            return Promise.reject(error);
        }
    );

    // 如果应用提供了自定义拦截器，再挂载（优先级高于公共拦截器，或者按需）
    if (options.interceptors) {
        if (options.interceptors.request) {
            instance.interceptors.request.use(
                // @ts-ignore
                options.interceptors.request,
                // @ts-ignore
                (error) => Promise.reject(error)
            );
        }
        if (options.interceptors.response) {
            instance.interceptors.response.use(
                options.interceptors.response,
                // @ts-ignore
                options.interceptors.responseError || ((error) => Promise.reject(error))
            );
        }
    }

    // 初始化网络重试处理器
    new NetworkRetryHandler(instance)

    return instance;
}
