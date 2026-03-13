package io.github.heran.emg.user.mapper;

import io.github.heran.emg.user.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper 示例。
 *
 * <p>该类不依赖真实 MyBatis / MyBatis-Plus，
 * 仅用于演示 AutoGen V1 推荐的分层方式：</p>
 * <ol>
 *     <li>单表查询优先返回 Entity</li>
 *     <li>Service 层再将 Entity 转换为自动生成的 DTO / Response</li>
 * </ol>
 *
 * <p>真实项目中，这里通常应替换为 MyBatis 或 MyBatis-Plus 的 Mapper 接口。</p>
 */
public class UserMapper {

    /**
     * 根据主键查询用户实体。
     */
    public UserEntity selectById(Long id) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername("tom");
        entity.setPassword("secret");
        entity.setNickname("Tom Cat");
        entity.setStatus(1);
        entity.setCreateTime(LocalDateTime.now().minusDays(3));
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    /**
     * 查询用户列表。
     *
     * <p>当前仅用于演示列表转换逻辑，因此直接复用单对象构造结果。</p>
     */
    public List<UserEntity> selectList(String username, Integer status) {
        List<UserEntity> result = new ArrayList<>();
        result.add(selectById(1L));
        result.add(selectById(2L));
        return result;
    }
}