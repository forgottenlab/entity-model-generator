package io.github.forgottenlab.emg.demo.user.service;

import io.github.forgottenlab.emg.demo.user.model.request.UserPageRequest;
import io.github.forgottenlab.emg.demo.user.model.response.UserBaseResponse;
import io.github.forgottenlab.emg.demo.user.model.response.UserListResponse;
import io.github.forgottenlab.emg.demo.user.model.response.UserLoginResponse;

import java.util.List;

/**
 * 用户服务接口。
 *
 * <p>用于演示 AutoGen V1 在 service 层的典型使用方式：</p>
 * <ul>
 *     <li>单对象查询返回 BaseResponse</li>
 *     <li>列表查询返回 ListResponse 集合</li>
 *     <li>业务扩展场景返回手写扩展 Response</li>
 * </ul>
 */
public interface UserService {

    /**
     * 查询用户详情。
     *
     * @param userId 用户主键
     * @return 自动生成的基础响应对象
     */
    UserBaseResponse getDetail(Long userId);

    /**
     * 查询用户列表。
     *
     * @param request 列表查询请求
     * @return 自动生成的列表响应对象集合
     */
    List<UserListResponse> getList(UserPageRequest request);

    /**
     * 用户登录。
     *
     * <p>这里演示“自动生成基础 Response + 手写扩展字段”的组合使用方式。</p>
     *
     * @param username 用户名
     * @param password 密码
     * @return 手写扩展后的登录响应对象
     */
    UserLoginResponse login(String username, String password);
}