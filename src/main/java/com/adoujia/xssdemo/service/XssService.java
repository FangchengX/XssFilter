package com.adoujia.xssdemo.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Service;

/**
 * @author fangcheng
 * @since 2020-03-01 13:51
 */
@Service
public class XssService {

    private static Whitelist HTML_WHITELIST;

    /**
     * HTML 过滤策略
     */
    static {
        HTML_WHITELIST = Whitelist.relaxed();
        HTML_WHITELIST.addAttributes("style", "type");
    }

    /**
     * 执行不同的html过滤策略
     * @param input 过滤前字符串
     * @param allowHtml 是否允许传入html
     * @return
     */
    public String doXssClean(String input, boolean allowHtml) {
        if (allowHtml) {
            return Jsoup.clean(input, HTML_WHITELIST);
        } else {
            return Jsoup.clean(input, Whitelist.relaxed());
        }
    }
}
