import request from '@/utils/request';

const commonApi = '/contentItem';

export const addContentItem = (data) => {
  return request.post(`${commonApi}/add`, data);
};

export const updateContentItem = (data) => {
  return request.put(`${commonApi}/update`, data);
};

export const getContentItems = async (data) => {
  const res = await request.post(`${commonApi}/list`, data);
  return res.data;
};

export const getContentItemById = (id) => {
  return request.get(`${commonApi}/${id}`);
};

export const deleteContentItem = (id) => {
  return request.delete(`${commonApi}/${id}`);
};
