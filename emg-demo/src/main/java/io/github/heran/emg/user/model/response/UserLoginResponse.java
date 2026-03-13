package io.github.heran.emg.user.model.response;

/**
 * 手写扩展 Response 示例。
 *
 * <p>说明：</p>
 * <ol>
 *     <li>{@code UserBaseResponse} 是由 AutoGen 自动生成的基础返回对象</li>
 *     <li>登录场景额外需要 {@code token}、{@code expireTime}</li>
 *     <li>因此通过继承基础响应对象来扩展业务专属字段</li>
 * </ol>
 *
 * <p>这也是 AutoGen V1 推荐的复杂返回模型处理方式。</p>
 */
public class UserLoginResponse extends UserBaseResponse {

    /**
     * 登录令牌。
     */
    private String token;

    /**
     * 令牌过期时间（单位：秒）。
     */
    private Long expireTime;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }
}