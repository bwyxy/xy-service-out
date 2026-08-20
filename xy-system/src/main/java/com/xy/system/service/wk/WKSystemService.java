package com.xy.system.service.wk;

import com.xy.common.client.WuKongClient;
import com.xy.common.client.vo.ResultVO;
import com.xy.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Component
public class WKSystemService {

    @Autowired
    private WuKongClient wuKongClient;

    /**
     * 获取系统用户
     * */
    public List<String> getSystemUids() {
        return wuKongClient.getSystemUids();
    }

    /**
     * 添加系统用户
     * */
    public void addSystemUids(@RequestBody List<String> systemUids) {
        ResultVO<?> response = wuKongClient.addSystemUids(systemUids);

        if (response.getStatus() != 200) {
            throw new BusinessException(response.getMsg());
        }
    }

    /**
     * 删除系统用户
     * */
    public void removeSystemUids(@RequestBody List<String> systemUids) {
        ResultVO<?> response = wuKongClient.removeSystemUids(systemUids);

        if (response.getStatus() != 200) {
            throw new BusinessException(response.getMsg());
        }
    }
}
