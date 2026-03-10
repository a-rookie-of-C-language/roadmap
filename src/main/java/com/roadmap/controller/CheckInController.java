package com.roadmap.controller;

import com.roadmap.dto.ApiResponse;
import com.roadmap.dto.CheckInDTO;
import com.roadmap.service.CheckInService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /**
     * 提交一条打卡记录
     * POST /api/v1/checkins
     */
    @PostMapping
    public ApiResponse<CheckInDTO> checkIn(@RequestBody CheckInDTO dto) {
        return ApiResponse.success(checkInService.checkIn(dto));
    }

    /**
     * 获取用户所有打卡记录（按时间倒序）
     * GET /api/v1/checkins/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<CheckInDTO>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(checkInService.getCheckInsByUser(userId));
    }

    /**
     * 查询指定坐标附近的打卡记录
     * GET /api/v1/checkins/nearby?userId=1&lng=116.4&lat=39.9&radius=500
     */
    @GetMapping("/nearby")
    public ApiResponse<List<CheckInDTO>> getNearby(
            @RequestParam Long userId,
            @RequestParam double lng,
            @RequestParam double lat,
            @RequestParam(defaultValue = "500") double radius
    ) {
        return ApiResponse.success(checkInService.getNearby(userId, lng, lat, radius));
    }

    /**
     * 删除打卡记录
     * DELETE /api/v1/checkins/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        checkInService.deleteCheckIn(id);
        return ApiResponse.success(null);
    }
}
