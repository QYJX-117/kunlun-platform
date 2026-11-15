package com.kunlun.platform.controller;

import com.kunlun.platform.entity.DispatchTask;
import com.kunlun.platform.service.DispatchExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dispatch/tasks")
public class DispatchExecutionController {

    private final DispatchExecutionService dispatchExecutionService;

    public DispatchExecutionController(DispatchExecutionService dispatchExecutionService) {
        this.dispatchExecutionService = dispatchExecutionService;
    }

    @PutMapping("/{id}/depart")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<Map<String, Object>> depart(@PathVariable Long id) {
        DispatchTask task = dispatchExecutionService.depart(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", task);
        result.put("message", "车辆已出发");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/arrive")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<Map<String, Object>> arrive(@PathVariable Long id) {
        DispatchTask task = dispatchExecutionService.arrive(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", task);
        result.put("message", "任务已完成，库存已更新");
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 400);
        result.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 500);
        result.put("message", "服务器内部错误");
        return ResponseEntity.status(500).body(result);
    }
}
