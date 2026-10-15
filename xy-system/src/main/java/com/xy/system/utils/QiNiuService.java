package com.xy.system.utils;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.qiniu.util.Json;
import com.xy.common.entity.SysResource;
import com.xy.common.qiniu.QiNiuProperties;
import com.xy.system.service.ISysResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class QiNiuService {

    @Autowired
    private ISysResourceService sysResourceService;

    private final QiNiuProperties qiNiuProperties;

    public QiNiuService(QiNiuProperties qiNiuProperties) {
        this.qiNiuProperties = qiNiuProperties;
    }

    public String upload(MultipartFile file) throws IOException {
        return upload(file.getBytes(), file.getOriginalFilename(), file.getContentType(), file.getSize());
    }

    /**
     * 按字节上传，供分发模块迁移外链图片时复用
     * */
    public String upload(byte[] bytes, String originalFilename, String contentType) {
        return upload(bytes, originalFilename, contentType, bytes == null ? 0L : bytes.length);
    }

    private String upload(byte[] bytes, String originalFilename, String contentType, long fileSize) {
        Configuration cfg = new Configuration(Region.region2());

        UploadManager uploadManager = new UploadManager(cfg);

        Auth auth = Auth.create(qiNiuProperties.getAccessKey(), qiNiuProperties.getSecretKey());

        String upToken = auth.uploadToken(qiNiuProperties.getBucket());

        String fileName = UUID.randomUUID() + "-" + originalFilename;

        try {
            Response response = uploadManager.put(bytes, fileName, upToken);

            DefaultPutRet putRet = Json.decode(response.bodyString(), DefaultPutRet.class);

            String url = qiNiuProperties.getDomain() + "/" + putRet.key;

            SysResource sysResource = new SysResource();
            sysResource.setResourceName(fileName);
            sysResource.setResourceKey(putRet.key);
            sysResource.setResourceUrl(url);
            sysResource.setFileSize(fileSize);
            sysResource.setFileType(contentType);
            addResource(sysResource);

            return url;
        } catch (QiniuException e) {
            throw new RuntimeException("上传图片失败:"+ e.getMessage());
        }
    }

    /** 添加资源 */
    private void addResource(SysResource sysResource) {
        sysResourceService.addSysResource(sysResource);
    }
}
