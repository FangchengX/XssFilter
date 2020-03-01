package com.adoujia.xssdemo.controller;

import com.adoujia.xssdemo.dto.XssTestDTO;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fangcheng
 * @since 2020-03-01 13:14
 */
@RestController
@Slf4j
public class XssTestController {

    @PostMapping("/xss/test")
    public XssTestDTO xssFilterTest(
            @RequestBody @Valid XssTestDTO xssTestDTO
    ) {
        return xssTestDTO;
    }
}
