package com.xy.admin.controller.encode;

import com.xy.common.annotation.Anonymous;
import com.xy.common.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/encode")
@RequiredArgsConstructor
public class EncodeController {

    @Autowired
    private EncryptionUtils encryptionUtils;

    @Anonymous
    @PostMapping("/password")
    public String encode(@RequestParam("password") String password) {
        return encryptionUtils.encode(password);
    }
}
