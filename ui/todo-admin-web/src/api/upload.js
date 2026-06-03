import request from '@/utils/request';

export function updateImage(data) {
    return request({
        url: '/upload/image',
        method: 'post',
        data,
        headers: {
            'Content-Type': 'multipart/form-data',
        }
    })
}
