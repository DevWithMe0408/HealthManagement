package org.example.healthdataservice.entity.enums;

public enum IndicatorCategory {
    BASE, // Chỉ số cơ bản người dùng nhập hoặc đo trực tiếp
    CALCULATED, // Chỉ số được tính toán bởi hệ thống
    USER_PROVIDED_CALCULATED, // Chỉ số tính toán nhưng do người dùng nhập (từ máy đo)
}
