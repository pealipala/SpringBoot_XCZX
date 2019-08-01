package com.xuecheng.manage_course.client;

import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = XcServiceList.XC_SERVICE_MANAGE_CMS)
public interface CmsPageClient {
    /**
     * 远程调用cmsPage 查询
     * @author : yechaoze
     * @date : 2019/6/24 14:10
     * @param id :
     * @return : com.xuecheng.framework.domain.cms.CmsPage
     */
    @GetMapping("/cms/page/get/{id}")//标识远程调用类型
    public CmsPage findCmsPageById(@PathVariable("id") String id);

    /**
     * 远程调用 cmsPage页面保存
     * @author : yechaoze
     * @date : 2019/7/3 20:34
     * @param cmsPage :
     * @return : com.xuecheng.framework.domain.cms.response.CmsPageResult
     */
    @PostMapping("/cms/page/save")
    public CmsPageResult saveCmsPage(@RequestBody CmsPage cmsPage);


    @PostMapping("/cms/page/postPageQuick")
    public CmsPostPageResult postPageQuick(@RequestBody CmsPage cmsPage);
}
