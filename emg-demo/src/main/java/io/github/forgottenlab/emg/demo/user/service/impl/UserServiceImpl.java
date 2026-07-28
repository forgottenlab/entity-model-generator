package io.github.forgottenlab.emg.demo.user.service.impl;

import io.github.forgottenlab.emg.demo.user.converter.UserConverter;
import io.github.forgottenlab.emg.demo.user.entity.UserEntity;
import io.github.forgottenlab.emg.demo.user.mapper.UserMapper;
import io.github.forgottenlab.emg.demo.user.model.request.UserPageRequest;
import io.github.forgottenlab.emg.demo.user.model.response.UserBaseResponse;
import io.github.forgottenlab.emg.demo.user.model.response.UserListResponse;
import io.github.forgottenlab.emg.demo.user.model.response.UserLoginResponse;
import io.github.forgottenlab.emg.demo.user.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务实现。
 *
 * <p>该类用于演示 AutoGen V1 在业务层的三种典型使用方式：</p>
 * <ol>
 *     <li>Entity -> BaseResponse</li>
 *     <li>Entity 列表 -> ListResponse 列表</li>
 *     <li>自动生成基础 Response + 手写扩展 Response</li>
 * </ol>
 */
public class UserServiceImpl implements UserService {

    /**
     * Mapper 示例对象。
     *
     * <p>这里直接 new 只是为了简化 demo，真实项目中通常由 Spring 注入。</p>
     */
    private final UserMapper userMapper = new UserMapper();

    /**
     * 查询单个用户详情。
     *
     * <p>流程：</p>
     * <ol>
     *     <li>从 Mapper 查询实体对象</li>
     *     <li>使用自动生成的 Converter 转换为 BaseResponse</li>
     * </ol>
     */
    @Override
    public UserBaseResponse getDetail(Long userId) {
        UserEntity entity = userMapper.selectById(userId);
        return UserConverter.toBaseResponse(entity);
    }

    /**
     * 查询用户列表。
     *
     * <p>流程：</p>
     * <ol>
     *     <li>从 Mapper 查询实体列表</li>
     *     <li>逐个转换为自动生成的 ListResponse</li>
     * </ol>
     */
    @Override
    public List<UserListResponse> getList(UserPageRequest request) {
        List<UserEntity> entities = userMapper.selectList(request.getUsername(), request.getStatus());
        List<UserListResponse> result = new ArrayList<>();
        for (UserEntity entity : entities) {
            result.add(UserConverter.toListResponse(entity));
        }
        return result;
    }

    /**
     * 用户登录。
     *
     * <p>该方法演示 AutoGen V1 推荐的扩展方式：</p>
     * <ul>
     *     <li>基础字段由自动生成的 BaseResponse 承接</li>
     *     <li>业务专属字段由手写扩展 Response 补充</li>
     * </ul>
     */
    @Override
    public UserLoginResponse login(String username, String password) {
        UserEntity entity = userMapper.selectById(1L);

        // 先用自动生成对象承接基础字段
        UserBaseResponse base = UserConverter.toBaseResponse(entity);

        // 再在手写扩展 Response 中补充业务专属字段
        UserLoginResponse response = new UserLoginResponse();
        response.setId(base.getId());
        response.setUsername(base.getUsername());
        response.setName(base.getName());
        response.setStatus(base.getStatus());
        response.setCreateTime(base.getCreateTime());
        response.setUpdateTime(base.getUpdateTime());
        response.setToken("mock-token-123456");
        response.setExpireTime(7200L);
        return response;
    }
}