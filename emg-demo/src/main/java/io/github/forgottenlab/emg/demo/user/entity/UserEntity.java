package io.github.forgottenlab.emg.demo.user.entity;

import io.github.forgottenlab.emg.annotations.*;

import java.time.LocalDateTime;

/**
 * 用户实体。
 *
 * <p>说明：</p>
 * <ol>
 *     <li>对应数据库 {@code user} 表</li>
 *     <li>同时作为 EMG V1 标准模型与 V2 自定义 View 的生成源</li>
 *     <li>通过字段注解分别控制 DTO / Response / ListResponse 与 View</li>
 * </ol>
 */
@AutoModel(
        value = "User",
        generateDto = true,
        generateBaseResponse = true,
        generateListResponse = true,
        generateConverter = true
)
@AutoView("basic")
@AutoView(value = "detail", name = "UserProfileView")
public class UserEntity {

    /**
     * 主键。
     */
    @ViewGroups({"basic", "detail"})
    private Long id;

    /**
     * 用户名。
     */
    @ViewGroups({"basic", "detail"})
    private String username;

    /**
     * 密码。
     *
     * <p>该字段不应出现在 DTO 和 Response 中。</p>
     */
    @DtoIgnore
    @ResponseIgnore
    private String password;

    /**
     * 昵称。
     *
     * <p>对外返回时重命名为 {@code name}。</p>
     */
    @ResponseAlias("name")
    private String nickname;

    /**
     * 手机号，仅进入 detail View。
     */
    @ViewGroups("detail")
    private String phone;

    /**
     * 用户状态。
     */
    private Integer status;

    /**
     * 创建时间。
     *
     * <p>详情页可展示，但列表页不展示。</p>
     */
    @ListIgnore
    private LocalDateTime createTime;

    /**
     * 更新时间。
     *
     * <p>详情页可展示，但列表页不展示。</p>
     */
    @ListIgnore
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
