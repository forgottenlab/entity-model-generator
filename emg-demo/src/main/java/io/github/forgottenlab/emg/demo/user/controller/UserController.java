package io.github.forgottenlab.emg.demo.user.controller;

import io.github.forgottenlab.emg.demo.user.model.request.UserDetailRequest;
import io.github.forgottenlab.emg.demo.user.model.request.UserPageRequest;
import io.github.forgottenlab.emg.demo.user.model.response.UserBaseResponse;
import io.github.forgottenlab.emg.demo.user.model.response.UserListResponse;
import io.github.forgottenlab.emg.demo.user.model.response.UserLoginResponse;
import io.github.forgottenlab.emg.demo.user.service.UserService;
import io.github.forgottenlab.emg.demo.user.service.impl.UserServiceImpl;

import java.util.List;

/**
 * Controller 示例。
 *
 * <p>该类不依赖 Spring 注解，目的是让 demo 在最小依赖下也能看懂。</p>
 *
 * <p>如果你将其放入真实 Spring Boot 项目，可以很自然地替换为：</p>
 * <ul>
 *     <li>{@code @RestController}</li>
 *     <li>{@code @RequestMapping}</li>
 *     <li>{@code @PostMapping / @GetMapping}</li>
 * </ul>
 *
 * <p>该类主要用于展示：自动生成模型在 controller 层的最终使用方式。</p>
 */
public class UserController {

    /**
     * 用户服务。
     *
     * <p>真实项目中通常由 Spring 注入。</p>
     */
    private final UserService userService = new UserServiceImpl();

    /**
     * 查询用户详情。
     *
     * @param request 详情查询请求
     * @return 自动生成的基础响应对象
     */
    public UserBaseResponse detail(UserDetailRequest request) {
        return userService.getDetail(request.getUserId());
    }

    /**
     * 查询用户列表。
     *
     * @param request 列表查询请求
     * @return 自动生成的列表响应对象集合
     */
    public List<UserListResponse> list(UserPageRequest request) {
        return userService.getList(request);
    }

    /**
     * 用户登录。
     *
     * @param username 用户名
     * @param password 密码
     * @return 手写扩展后的登录响应对象
     */
    public UserLoginResponse login(String username, String password) {
        return userService.login(username, password);
    }
}