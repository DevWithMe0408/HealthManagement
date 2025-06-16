package org.example.userservice.mapper;

import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.enums.Gender;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserAccountMapper {
    // Chuyển từ DTO cập nhật sang một User entity chỉ chứa các trường cần thiết cho việc cập nhật
    @Mapping(target = "id", ignore = true) // Không map id từ request
    @Mapping(target = "auth", ignore = true) // Không map auth từ request
    @Mapping(target = "age", ignore = true) // age sẽ được tính lại
    // Đảm bảo tên trường khớp, ví dụ:
    @Mapping(source = "phoneNumber", target = "phone") // Nếu User entity dùng 'phone'
    User toUserEntityForUpdate(UserRequestDTO dto);

    // Chuyển từ User entity (và Auth entity) sang DTO response
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "auth.username", target = "username")
    @Mapping(source = "auth.email", target = "email")
    @Mapping(source = "user.name", target = "name")
    @Mapping(source = "user.phoneNumber", target = "phone") // Hoặc user.phone
    @Mapping(source = "user.birthDate", target = "birthDate")
    @Mapping(source = "user.gender", target = "gender") // Cần xử lý Gender enum to String
    UserAccountDetailsResponse toUserAccountDetailsResponse(User user, Auth auth);

    // Helper để chuyển Gender enum sang String
    default String mapGender(Gender gender) {
        return gender != null ? gender.name() : null;
    }
    // Helper để lấy roles từ Auth
    default List<String> mapRoles(Auth auth) {
        return auth != null && auth.getRole() != null ? Collections.singletonList(auth.getRole().name()) : Collections.emptyList();
    }

    @AfterMapping // Gán roles sau khi các mapping cơ bản hoàn thành
    default void populateRoles(@MappingTarget UserAccountDetailsResponse target, Auth auth) {
        if (auth != null && auth.getRole() != null) {
            target.setRoles(Collections.singletonList(auth.getRole().name()));
        }
    }
}
