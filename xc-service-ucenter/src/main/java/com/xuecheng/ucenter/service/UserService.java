package com.xuecheng.ucenter.service;

import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompanyUserRepository;
import com.xuecheng.ucenter.dao.XcMenuMapper;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private XcUserRepository xcUserRepository;
    @Autowired
    private XcCompanyUserRepository xcCompanyUserRepository;
    @Autowired
    private XcMenuMapper xcMenuMapper;

    /**
     * 根据账号查询用户信息
     * @author : yechaoze
     * @date : 2019/7/27 13:57
     * @param userName :
     * @return : com.xuecheng.framework.domain.ucenter.ext.XcUserExt
     */
    public XcUserExt getUserExt(String userName) {
        //根据账号查询XcUser信息
        XcUser xcUser = this.findXcUserByUsername(userName);
        if (xcUser==null){
            return null;
        }
        //根据用户id查询用户所属的公司id
        String userId = xcUser.getId();
        XcCompanyUser xcCompanyUser = this.findCompanyIdByUserId(userId);
        if (xcCompanyUser==null){
            return null;
        }
        String companyId = xcCompanyUser.getCompanyId();
        XcUserExt xcUserExt=new XcUserExt();
        List<XcMenu> menus = xcMenuMapper.selectPermissionByUserId(userId);
        BeanUtils.copyProperties(xcUser,xcUserExt);
        xcUserExt.setCompanyId(companyId);
        xcUserExt.setPermissions(menus);
        return xcUserExt;
    }

    /**
     * 根据账号查询XcUser信息
     * @author : yechaoze
     * @date : 2019/7/27 14:01
     * @param userName : 
     * @return : com.xuecheng.framework.domain.ucenter.XcUser
     */
    private XcUser findXcUserByUsername(String userName){
        return xcUserRepository.findByUsername(userName);
    }

    /**
     * * 根据用户id查询用户所属的公司id
     * @author : yechaoze
     * @date : 2019/7/27 14:02
     * @param userId : 
     * @return : com.xuecheng.framework.domain.ucenter.XcCompanyUser
     */
    private XcCompanyUser findCompanyIdByUserId(String userId){
        return xcCompanyUserRepository.findByUserId(userId);
    }
}