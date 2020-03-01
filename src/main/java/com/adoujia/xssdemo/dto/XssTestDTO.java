package com.adoujia.xssdemo.dto;

import com.adoujia.xssdemo.annoations.XssRule;
import lombok.Data;

/**
 * @author fangcheng
 * @since 2020-03-01 13:34
 */
@Data
public class XssTestDTO {
    String normal;
    @XssRule
    String html;
}
