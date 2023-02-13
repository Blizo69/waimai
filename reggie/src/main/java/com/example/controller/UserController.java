package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.common.BaseContext;
import com.example.common.R;
import com.example.entity.User;
import com.example.service.UserService;
import com.example.utils.SMSUtils;
import com.example.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送验证码短信
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user){
        //获取手机号
        String phone = user.getPhone();

        if (!StringUtils.isEmpty(phone)){
            //随机生成4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code为：{}",code);

            //调用aliyun地短信服务发送短信
//            SMSUtils.sendMessage("阿里云短信测试","SMS_154950909",phone,code);

            //将生成的验证码保存到redis当中
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            R.success("手机验证码发送成功！");
        }
        return R.error("手机验证码发送失败！");
    }

    @PostMapping("/login")
    public R<User> login(@RequestBody Map map,HttpSession session){
//        log.info("map为{}",map.toString());
        //获取手机号
        String phone = (String) map.get("phone");
        //获取验证码
        String code = (String) map.get("code");
        //从redis中获取发送的验证码
        String codeMsg = (String) redisTemplate.opsForValue().get(phone);
        //对比接收的code和发送的code是否相同
        if (codeMsg != null && code.equals(codeMsg)){
            //判断用户是否已经注册
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            if(user == null){
                //还未注册过，利用当前手机号码注册账号
                user = new User();
                user.setPhone(phone);
                userService.save(user);
            }
            //保存该登录用户的id
            session.setAttribute("user",user.getId());

            //登录成功，删除验证码
            redisTemplate.delete(phone);

            return R.success(user);
        }
        return R.error("登录失败！");
    }
}
