package com.lwb.guahao.webapp.service;

import com.lwb.guahao.common.ApiRet;
import com.lwb.guahao.common.Constants;
import com.lwb.guahao.common.model.PerUserBan;
import com.lwb.guahao.common.model.Reservation;
import com.lwb.guahao.common.qo.util.PerUserBanQo;
import com.lwb.guahao.common.util.SecurityUtil;
import com.lwb.guahao.common.model.PerUser;
import com.lwb.guahao.webapp.dao.PerUserBanDao;
import com.lwb.guahao.webapp.dao.PerUserDao;
import com.lwb.guahao.webapp.dao.ReservationDao;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Created by Lu Weibiao on 2015/2/14 22:02.
 */
@Service
@Transactional
public class PerUserService {
    private final static Logger logger = Logger.getLogger(PerUserService.class);
    @Resource
    private PerUserDao perUserDao;
    @Resource
    private PerUserBanDao perUserBanDao;

    /**
     * 个人账号注册
     * @param user
     * @return
     */
    public ApiRet<PerUser> register(final PerUser user){
        ApiRet<PerUser> apiRet = new ApiRet<PerUser>();
        if(!isRegistered(user)){
            PerUser newUser = new PerUser();
            BeanUtils.copyProperties(user, newUser);
            newUser.setPassword(SecurityUtil.password(newUser.getPassword()));
            newUser.setAccountStatusCode(Constants.AccountStatus.UN_VERIFIED);
            newUser.setIsEmailBound(false);
            newUser.setIsMobileBound(false);
            newUser.setCreateDateTime(new Date());
            perUserDao.save(newUser);
            apiRet.setRet(ApiRet.RET_SUCCESS);
            apiRet.setMsg("注册成功");
            apiRet.setData(newUser);
        } else {
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("账号已被注册，请检查邮箱和身份证");
        }
        return apiRet;
    }

    /**
     * 判断用户是否已注册
     * 身份证和邮箱都必须对应唯一账号，但允许多个账号绑定同一个手机号
     * @param user
     * @return
     */
    @Transactional(readOnly = true)
    public boolean isRegistered(final PerUser user){
        boolean isRegistered;
        if(perUserDao.existsByEmail(user.getEmail()) || perUserDao.existsByIdCard(user.getIdCard())){
            //erUserDao.existsByMobilePhone(user.getMobilePhone())

            isRegistered = true;
        } else{
            isRegistered = false;
        }
        return isRegistered;
    }

    /**
     * 验证指定用户是否在禁止预约挂号
     * - 验证指定用户的账号状态处于正常状态
     * - 指定用户没有在预约黑名单中
     * @param perUserId
     * @return
     */
    @Transactional(readOnly = true)
    public ApiRet<Boolean> isForbiddenToReserve(final Integer perUserId) {
        ApiRet<Boolean> apiRet = new ApiRet<Boolean>();

        PerUser perUser = perUserDao.get(perUserId);
        if(perUser == null){
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("用户不存在");
            return apiRet;
        }
        //验证指定用户的账号状态处于正常状态
        if(!perUser.getAccountStatusCode().equals(Constants.AccountStatus.NORMAL)){
            apiRet.setRet(ApiRet.RET_SUCCESS);
            apiRet.setData(Boolean.TRUE);
            return apiRet;
        }
        //指定用户没有在预约黑名单中
        PerUserBanQo perUserBanQo = new PerUserBanQo();
        perUserBanQo.setPerUserId(perUserId);
        Date now = new Date();
        perUserBanQo.setExpireDateTimeStart(now);
        if(perUserBanDao.existsBy(perUserBanQo)){
            apiRet.setRet(ApiRet.RET_SUCCESS);
            apiRet.setData(Boolean.TRUE);
            apiRet.setMsg("用户目前被禁止进行预约操作");
            return apiRet;
        }

        apiRet.setRet(ApiRet.RET_SUCCESS);
        apiRet.setData(Boolean.FALSE);
        return apiRet;
    }

}
