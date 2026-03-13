package io.github.heran.emg.user.model.request;

/**
 * 手写 Request 示例：查询用户详情。
 *
 * <p>按 AutoGen V1 规则，Request 仍然建议手写，
 * 因为它通常需要承载具体业务参数、校验规则和接口语义。</p>
 */
public class UserDetailRequest {

    /**
     * 用户主键。
     */
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}