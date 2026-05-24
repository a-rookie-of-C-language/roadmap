package com.roadmap.dto.compat;

public class CowMeta {

    private long totalCount;
    private Integer pageNo;
    private Integer pageSize;

    public CowMeta() {
    }

    public CowMeta(long totalCount, Integer pageNo, Integer pageSize) {
        this.totalCount = totalCount;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
