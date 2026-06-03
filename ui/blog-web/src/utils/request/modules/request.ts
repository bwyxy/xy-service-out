import axios from "axios";
import type { AxiosInstance, AxiosResponse, AxiosError } from 'axios'
import NetworkRetryHandler from "./networkRetryHandler.ts";

// @ts-ignore
const service: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json'
    },
});

service.interceptors.request.use(
    (config) => {

        return config;
    },
    (error: AxiosError) => {
        return Promise.reject(error);
    },
)

service.interceptors.response.use(
    (response: AxiosResponse) => {

        return response;
    },
    (error: any) => {
        const { response } = error;
        if (response) {
            return Promise.reject(response.data);
        }
        return Promise.reject(error);
    }
)

// 初始化网络重试处理器（实例化即生效）
new NetworkRetryHandler(service);

export default service;
