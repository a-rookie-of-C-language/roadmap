package com.roadmap.dto.compat;

public class CowResponse<T> {

    private T data;
    private CowMeta meta;

    public CowResponse() {
    }

    public CowResponse(T data, CowMeta meta) {
        this.data = data;
        this.meta = meta;
    }

    public static <T> CowResponse<T> of(T data) {
        return new CowResponse<>(data, null);
    }

    public static <T> CowResponse<T> of(T data, CowMeta meta) {
        return new CowResponse<>(data, meta);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public CowMeta getMeta() {
        return meta;
    }

    public void setMeta(CowMeta meta) {
        this.meta = meta;
    }
}
