package com.example.filter;

import com.alibaba.fastjson.JSON;
import com.example.common.BaseContext;
import com.example.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1.获取本次请求的URI
        String url = request.getRequestURI();
        log.info("本次请求拦截到的url{}",url);

        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",//移动端发送验证短信
                "/user/login"//移动端登录
        };
        //2.判断本次请求是否需要处理
        boolean check = check(urls, url);
        //3.如果不需要处理，则直接放行
        if (check){
            log.info("本次请求{}不需要处理",url);
            filterChain.doFilter(request,response);
            return;
        }

        //4-1.判断登录状态，如果后台用户雇员已经登录，则直接放行
        if(request.getSession().getAttribute("employee") != null){
            log.info("用户已登录，用户id为{}",request.getSession().getAttribute("employee"));
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);
            //已经登录
            filterChain.doFilter(request,response);
            return;
        }

        //4-2.判断登录状态，如果顾客用户已经登录，则直接放行
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为{}",request.getSession().getAttribute("user"));
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            //已经登录
            filterChain.doFilter(request,response);
            return;
        }

        log.info("用户未登录");
        //4.如果未登录返回未登录结果
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     * 路径资源映射
     * @param urls
     * @param requestURL
     * @return
     */
    public boolean check(String[] urls,String requestURL){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURL);
            if (match){
                return match;
            }
        }
        return false;
    }

}
