import request from "@/utils/request.ts";

export const getSystemUser = (data :any) :Promise<string[]> => {
    return request({
        url: "/wk/system/user",
        method: "post",
        data
    })
}

export const addSystemUser = (data :any) => {
    return request({
        url: "/wk/system/add",
        method: "post",
        data
    })
}

export const updateSystemUser = (data :any) => {
    return request({
        url: "/wk/system/update",
        method: "put",
        data
    })
}

export const removeSystemUser = (data :string[]) => {
    return request({
        url: "/wk/system/remove",
        method: "delete",
        data
    })
}
