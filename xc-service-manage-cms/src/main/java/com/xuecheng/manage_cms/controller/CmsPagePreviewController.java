package com.xuecheng.manage_cms.controller;

import com.xuecheng.framework.web.BaseController;
import com.xuecheng.manage_cms.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

@Controller
@RequestMapping(value = "/cms")
public class CmsPagePreviewController extends BaseController{

    @Autowired
    private PageService pageService;

    /**
     * 页面预览
     * @author : yechaoze
     * @date : 2019/6/15 22:57
     * @param pageId :
     * @return : void
     */
    @RequestMapping(value = "/preview/{pageId}",method = RequestMethod.GET)
    public void preview(@PathVariable String pageId) throws IOException {
        //执行静态化
        String html = pageService.getHtml(pageId);
        //通过respons将对象输出
        ServletOutputStream outputStream = response.getOutputStream();
        response.setHeader("Content‐type","text/html;charset=utf‐8");
        outputStream.write(html.getBytes("utf-8"));
    }

}
