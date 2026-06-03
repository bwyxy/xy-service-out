import http from "@/utils/request/index.ts";

export const categoryList = (params :any) :Promise<R<any>> => {
    return http.post("/web/category/list", params);
}