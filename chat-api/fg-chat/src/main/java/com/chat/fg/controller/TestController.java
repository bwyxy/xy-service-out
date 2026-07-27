package com.chat.fg.controller;

import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    @Anonymous
    @GetMapping("/text")
    public R<?> test() {
        return R.ok("test");
    }
}
