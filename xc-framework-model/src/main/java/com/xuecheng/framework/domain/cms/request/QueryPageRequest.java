package com.xuecheng.framework.domain.cms.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 接受页面查询的查询条件
 * @author : yechaoze
 * @date : 2019/5/30 8:37
 */
@Data
public class QueryPageRequest {
    //站点id
    @ApiModelProperty("站点id")
    private String siteId;
    //页面ID
    @ApiModelProperty("页面ID")
    private String pageId;
    //页面名称
    @ApiModelProperty("页面名称")
    private String pageName;
    //页面别名
    @ApiModelProperty("页面别名")
    private String pageAliase;
    //模版id
    @ApiModelProperty("模版id")
    private String templateId;
}
