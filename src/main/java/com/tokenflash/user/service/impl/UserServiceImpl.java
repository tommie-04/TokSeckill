package com.tokenflash.user.service.impl;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tokenflash.user.dto.LoginFormDTO;
import com.tokenflash.common.dto.Result;
import com.tokenflash.user.dto.UserDTO;
import com.tokenflash.user.entity.User;
import com.tokenflash.user.mapper.UserMapper;
import com.tokenflash.user.service.IUserService;
import com.tokenflash.user.auth.RegexUtils;
import com.tokenflash.user.auth.UserHolder;
import com.tokenflash.infrastructure.redis.UserSessionStore;
import com.tokenflash.infrastructure.redis.SignInCalendar;
import java.time.LocalDate;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import static com.tokenflash.common.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private UserSessionStore sessions;
    @Resource
    private SignInCalendar calendar;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }
        String code = RandomUtil.randomNumbers(6);

        sessions.saveCode(phone, code);

        log.debug("发送短信验证码成功，验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }
        String cacheCode = sessions.readCode(phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证码错误");
        }

        User user = query().eq("phone", phone).one();

        if (user == null) {
            user = createUserWithPhone(phone);
        }

        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        sessions.saveSession(token, userDTO);
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        calendar.mark(UserHolder.getUser().getId(), LocalDate.now());
        return Result.ok();
    }

    @Override
    public Result signCount() {
        return Result.ok(calendar.streak(UserHolder.getUser().getId(), LocalDate.now()));
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
