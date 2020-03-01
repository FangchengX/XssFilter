package com.adoujia.xssdemo.controller;

import com.adoujia.xssdemo.dto.XssTestDTO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author fangcheng
 * @since 2020-03-01 14:11
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@Slf4j
public class XssTestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void xssFilterTest() throws Exception {
        String html = "<style type=\"text/css\"></style><p>text</p>";
        XssTestDTO xssTestDTO = new XssTestDTO();
        xssTestDTO.setHtml(html);
        xssTestDTO.setNormal(html);
        String content =
                mockMvc
                        .perform(
                                post("/xss/test")
                                        .content(JSON.toJSONString(xssTestDTO))
                                        .contentType(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();

        XssTestDTO result = JSON.parseObject(content, XssTestDTO.class);
        Document htmlDoc = Jsoup.parse(result.getHtml());
        Elements htmlElements = htmlDoc.select("style");
        Assert.assertEquals(1, htmlElements.size());
        Document normalDoc = Jsoup.parse(result.getNormal());
        Elements normalElements = normalDoc.select("style");
        Assert.assertEquals(0, normalElements.size());
        log.info(JSON.toJSONString(result));
    }
}
