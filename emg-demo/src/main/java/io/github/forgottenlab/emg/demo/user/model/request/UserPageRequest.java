package io.github.forgottenlab.emg.demo.user.model.request;

/**
 * 手写 Request 示例：用户列表查询。
 *
 * <p>该类用于演示 AutoGen V1 的设计原则：</p>
 * <ul>
 *     <li>Request 不自动生成</li>
 *     <li>Request 由业务手写</li>
 *     <li>因为入参强依赖具体接口语义和校验规则</li>
 * </ul>
 */
public class UserPageRequest {

    /**
     * 用户名关键字。
     */
    private String username;

    /**
     * 用户状态。
     */
    private Integer status;

    /**
     * 页码。
     */
    private Integer pageNum;

    /**
     * 每页条数。
     */
    private Integer pageSize;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}