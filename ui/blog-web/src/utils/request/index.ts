import service from "./modules/request.ts";

/**
 * 公共请求方法封装
 * */
class httpRequest {
    post<T>(url:string,params?:any):Promise<T> {
        return new Promise((resolve, reject) => {
            service.post(url,params).then((res:any) => {
                resolve(res.data)
            }).catch((err) => {
                reject(err)
            })
        })
    }
    get<T>(url:string,_params?:any, data?:any):Promise<T> {
        let pathUrl = url

        return new Promise((resolve, reject) => {
            service.get(pathUrl, data).then((res:any) => {
                resolve(res?.data)
            }).catch((err) => {
                reject(err)
            })
        })
    }
    delete<T>(url:string,params?:any):Promise<T> {
        return new Promise((resolve, reject) => {
            service.delete(url,params).then((res:any) => {
                resolve(res.data)
            }).catch((err) => {
                reject(err)
            })
        })
    }
    put<T>(url:string,params?:any):Promise<T> {
        return new Promise((resolve, reject) => {
            service.put(url,params).then((res:any) => {
                resolve(res.data)
            }).catch((err) => {
                reject(err)
            })
        })
    }
    uploadFile<T>(url:string, file: File):Promise<T>{
        const fd = new FormData()
        fd.append('file', file)
        return new Promise((resolve, reject) => {
            service.post(url,fd,{
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            }).then((res:any) => {
                resolve(res.data)
            }).catch((err) => {
                reject(err)
            })
        })
    }
}

/**
 * 公共请求方法封装
 * */
const http = new httpRequest();

export default http;
