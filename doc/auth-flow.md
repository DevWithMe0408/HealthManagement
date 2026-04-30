# Auth Flow — Đăng ký, Đăng nhập, Refresh Token

> Tài liệu mô tả luồng xác thực **mới sau refactor** của `user-service`.
> Dùng làm spec đối chiếu cho cả Backend (Spring Boot) và Frontend (React).

---

## 1. Quyết định kỹ thuật đã chốt

| # | Quyết định | Lý do |
|---|---|---|
| 1 | Tách 2 entity `Auth` và `User` | `Auth` chứa credential/role, `User` chứa profile. Separation of concerns. |
| 2 | FK đặt ở `User`: `user.auth_id → auth.id` | Lúc register: tạo `Auth` trước (entry point), rồi tạo `User` trỏ về. Không cần `update` lần 2 → hết bug FK NULL. |
| 3 | ID dùng **UUID v7**, lưu `CHAR(36)`, field Java kiểu `String` | Sinh client-side trước khi save → biết ID ngay từ đầu. UUID v7 có time-ordering nên insert không fragment index. `CHAR(36)` để dễ debug. |
| 4 | Bỏ JPA bidirectional (`@OneToOne(mappedBy=...)`) | Tránh lazy-loading khó kiểm soát, vòng lặp serialize. Quan hệ 1 chiều: `User.auth` (owning side với FK). |
| 5 | Response chuẩn: `DataResponse<T>` `{code, message, data}` | Format thống nhất cho FE. Lỗi đi kèm HTTP status đúng (4xx/5xx), không trả 200 với code lỗi trong body. |
| 6 | Mã lỗi format `MODULE-NNN` | Dễ tra cứu, dễ phân chia theo service/module. |
| 7 | Quản lý lỗi qua `enum ErrorCode` tập trung | Mỗi mã có sẵn HTTP status + default message. Không magic string rải rác. |
| 8 | `BusinessException` dùng chung, `GlobalExceptionHandler` map về `DataResponse` | Controller không cần `try/catch`. |
| 9 | Không dùng Outbox pattern (đồ án) | Nếu RabbitMQ fail lúc register → throw exception, rollback transaction. Đơn giản, đảm bảo nhất quán dữ liệu. |

---

## 2. Schema database (sau refactor)

### Bảng `auth`
| Column | Type | Constraint |
|---|---|---|
| `id` | `CHAR(36)` | PK, UUID v7 |
| `username` | `VARCHAR(50)` | UNIQUE, NOT NULL |
| `password` | `VARCHAR(255)` | NOT NULL (BCrypt hash) |
| `email` | `VARCHAR(100)` | UNIQUE, NOT NULL |
| `role` | `VARCHAR(20)` | NOT NULL (ENUM: `ROLE_USER`, `ROLE_ADMIN`) |

### Bảng `user`
| Column | Type | Constraint |
|---|---|---|
| `id` | `CHAR(36)` | PK, UUID v7 |
| `auth_id` | `CHAR(36)` | UNIQUE, NOT NULL, FK → `auth.id` |
| `name` | `VARCHAR(100)` | nullable |
| `phone` | `VARCHAR(20)` | nullable |
| `birth_date` | `DATE` | nullable |
| `gender` | `VARCHAR(10)` | nullable (ENUM: `MALE`, `FEMALE`, `OTHER`) |
| `age` | `INT` | nullable, computed |

### Bảng `refresh_token`
| Column | Type | Constraint |
|---|---|---|
| `id` | `CHAR(36)` | PK |
| `auth_id` | `CHAR(36)` | NOT NULL, FK → `auth.id` |
| `token` | `VARCHAR(255)` | UNIQUE, NOT NULL |
| `expiry_date` | `TIMESTAMP` | NOT NULL |

> **Lưu ý migration**: bản ghi `chien03` hiện tại (và bất kỳ Auth nào có `user_id IS NULL`) sẽ phải xoá trước khi đổi schema, hoặc tạo lại User tương ứng. Sau refactor, bug này không còn cơ hội xảy ra.

---

## 3. Luồng đăng ký — `POST /api/auth/register`

### Request
```json
{
  "username": "chien03",
  "password": "secret123",
  "email": "chien@example.com"
}
```

### Xử lý (trong `AuthServiceImpl.registerUser`)
```
1. Validate input (username/password/email không rỗng)
2. Check trùng:
   - existsByUsername → throw BusinessException(AUTH-001) nếu trùng
   - existsByEmail    → throw BusinessException(AUTH-002) nếu trùng
3. Sinh ID:
   - String authId = UuidV7.generate()
   - String userId = UuidV7.generate()
4. Tạo entity:
   - Auth auth = new Auth(authId, username, bcrypt(password), email, ROLE_USER)
   - User user = new User(userId, auth, name="Unknown", ...defaults)
5. Persist (1 transaction):
   - authRepository.save(auth)
   - userRepository.save(user)   // FK auth_id ghi luôn ở insert này
6. Publish event:
   - rabbitTemplate.convertAndSend(USER_EVENTS_EXCHANGE,
       USER_CREATED_ROUTING_KEY,
       new UserCreatedEvent(userId, username, email))
   - Nếu Rabbit fail → throw BusinessException(AUTH-500) → rollback toàn bộ
7. Return DataResponse.success(null)
```

### Response thành công (`HTTP 200`)
```json
{
  "code": null,
  "message": "Success",
  "data": null
}
```

### Response lỗi (vd username trùng — `HTTP 409`)
```json
{
  "code": "AUTH-001",
  "message": "Username đã tồn tại",
  "data": null
}
```

---

## 4. Luồng đăng nhập — `POST /api/auth/login`

### Request
```json
{
  "username": "chien03",
  "password": "secret123"
}
```

### Xử lý (trong `AuthServiceImpl.loginUser`)
```
1. authenticationManager.authenticate(UsernamePasswordAuthenticationToken)
   → Spring Security gọi UserDetailsServiceImpl.loadUserByUsername(username)
     a. Auth auth = authRepository.findByUsername(username)
        → throw UsernameNotFoundException nếu không có
     b. User user = userRepository.findByAuthId(auth.getId())
     c. return new CustomUserDetails(user.getId(), auth.getUsername(),
          auth.getPassword(), auth.getRole(), ...)
   → Spring tự verify BCrypt password
   → Nếu sai → AuthenticationException
2. SecurityContextHolder.getContext().setAuthentication(authentication)
3. String jwt = jwtUtil.generateToken(authentication)
   ⭐ KHÔNG query DB lại — đọc thẳng userId, username, role từ CustomUserDetails
4. RefreshToken refreshToken = refreshTokenService.createOrRotate(auth)
   ⭐ Truyền Auth entity, không tìm lại theo username
5. Return DataResponse.success(new TokenRefreshResponse(jwt, refreshToken.getToken()))
```

### JWT claims
```json
{
  "sub": "chien03",
  "userId": "<UUID v7 của user>",
  "roles": "ROLE_USER",
  "iat": 1714134000,
  "exp": 1714137600
}
```

### Response thành công (`HTTP 200`)
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### Response lỗi (sai mật khẩu — `HTTP 401`)
```json
{
  "code": "AUTH-003",
  "message": "Sai username hoặc password",
  "data": null
}
```

### Cải tiến so với hiện tại
- **Trước**: 3 query DB / login (loadUserByUsername, generateToken, createRefreshToken).
- **Sau**: 2 query DB / login (chỉ trong loadUserByUsername).
- `JwtUtil` không còn phụ thuộc `AuthRepository`.
- Controller không còn `try/catch` — `GlobalExceptionHandler` lo hết.

---

## 5. Luồng refresh token — `POST /api/auth/refresh-token`

### Request
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Xử lý
```
1. RefreshToken rt = refreshTokenRepo.findByToken(token)
   → throw BusinessException(AUTH-004) nếu không có
2. if (rt.expiryDate < now):
   - delete(rt)
   - throw BusinessException(AUTH-005) "Refresh token expired"
3. Auth auth = rt.getAuth()
4. User user = userRepository.findByAuthId(auth.getId())
5. Build Authentication object từ auth + user
6. String newJwt = jwtUtil.generateToken(authentication)
7. Return DataResponse.success(new TokenRefreshResponse(newJwt, rt.getToken()))
```

### Response thành công (`HTTP 200`)
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## 6. `DataResponse<T>` contract

```java
public class DataResponse<T> {
    private String code;       // null khi success, "MODULE-NNN" khi lỗi
    private String message;    // "Success" hoặc message lỗi
    private T data;            // payload (null nếu lỗi)

    public static <T> DataResponse<T> success(T data) {
        return new DataResponse<>(null, "Success", data);
    }

    public static <T> DataResponse<T> error(String code, String message) {
        return new DataResponse<>(code, message, null);
    }
}
```

**Rule cho FE:**
- Đọc HTTP status code TRƯỚC (`200/4xx/5xx`).
- Nếu `2xx`: lấy `response.data.data` để hiển thị.
- Nếu `4xx/5xx`: hiển thị `response.data.message`, dùng `response.data.code` để xử lý logic (vd. `AUTH-003` → focus lại form login).

---

## 7. Catalog mã lỗi (`enum ErrorCode`)

```java
public enum ErrorCode {
    // ===== AUTH module =====
    USERNAME_TAKEN      ("AUTH-001", HttpStatus.CONFLICT,     "Username đã tồn tại"),
    EMAIL_TAKEN         ("AUTH-002", HttpStatus.CONFLICT,     "Email đã tồn tại"),
    INVALID_CREDENTIAL  ("AUTH-003", HttpStatus.UNAUTHORIZED, "Sai username hoặc password"),
    REFRESH_TOKEN_NOT_FOUND ("AUTH-004", HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"),
    REFRESH_TOKEN_EXPIRED   ("AUTH-005", HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn"),
    JWT_INVALID         ("AUTH-006", HttpStatus.UNAUTHORIZED, "JWT không hợp lệ"),
    JWT_EXPIRED         ("AUTH-007", HttpStatus.UNAUTHORIZED, "JWT đã hết hạn"),
    EVENT_PUBLISH_FAILED("AUTH-500", HttpStatus.INTERNAL_SERVER_ERROR, "Không thể publish event"),

    // ===== USER module =====
    USER_NOT_FOUND      ("USER-001", HttpStatus.NOT_FOUND, "User không tồn tại"),

    // ===== Generic =====
    VALIDATION_FAILED   ("COMMON-001", HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    INTERNAL_ERROR      ("COMMON-500", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
    // constructor + getters
}
```

> Mỗi service (`health-data-service`, `nutrition-service`...) sẽ có `ErrorCode` riêng với prefix tương ứng (`HEALTH-NNN`, `NUTR-NNN`).

---

## 8. `BusinessException`

```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
```

Dùng:
```java
if (authRepository.existsByUsername(username)) {
    throw new BusinessException(ErrorCode.USERNAME_TAKEN);
}
```

---

## 9. `GlobalExceptionHandler`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<DataResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.getHttpStatus())
            .body(DataResponse.error(ec.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<DataResponse<Void>> handleAuth(AuthenticationException ex) {
        ErrorCode ec = ErrorCode.INVALID_CREDENTIAL;
        return ResponseEntity.status(ec.getHttpStatus())
            .body(DataResponse.error(ec.getCode(), ec.getDefaultMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DataResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // gom error message từ field errors
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(ec.getHttpStatus())
            .body(DataResponse.error(ec.getCode(), <gộp message>));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DataResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.getHttpStatus())
            .body(DataResponse.error(ec.getCode(), ec.getDefaultMessage()));
    }
}
```

---

## 10. Controller "đúng chuẩn" sau refactor

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public DataResponse<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.registerUser(req);
        return DataResponse.success(null);
    }

    @PostMapping("/login")
    public DataResponse<TokenRefreshResponse> login(@Valid @RequestBody LoginRequest req) {
        return DataResponse.success(authService.loginUser(req));
    }

    @PostMapping("/refresh-token")
    public DataResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest req) {
        return DataResponse.success(authService.refreshAccessToken(req.getRefreshToken()));
    }
}
```

**Nguyên tắc:** Controller chỉ nhận request, gọi service, bọc response. Mọi logic (validate trùng, sinh ID, persist, publish event, sinh JWT...) nằm trong `AuthServiceImpl`. Mọi exception do `GlobalExceptionHandler` xử lý.

---

## 11. API contract tóm tắt cho FE

| Method | Path | Public? | Request body | Success data |
|---|---|---|---|---|
| POST | `/api/auth/register` | ✅ | `{username, password, email}` | `null` |
| POST | `/api/auth/login` | ✅ | `{username, password}` | `{accessToken, refreshToken}` |
| POST | `/api/auth/refresh-token` | ✅ | `{refreshToken}` | `{accessToken, refreshToken}` |

**Header cho mọi request authenticated:**
```
Authorization: Bearer <accessToken>
```

**Sau khi gateway validate JWT, các service downstream sẽ thấy headers:**
- `X-User-Id`: UUID của user
- `X-Username`: username
- `X-Roles`: role string (vd `ROLE_USER`)

---

## 12. Lưu ý khi implement

1. **Thư viện UUID v7**: thêm dependency `com.github.f4b6a3:uuid-creator` vào `pom.xml` của `user-service` (và các service cần sinh UUID).
2. **Migration data hiện tại**: trước khi đổi schema, drop hoặc backup `user_db`. Hibernate `ddl-auto: update` không tự đổi `BIGINT → CHAR(36)` được — phải drop tay hoặc migration script.
3. **JWT secret + expiration** giữ nguyên (`jwt.secret`, `jwt.expiration`, `jwt.refresh-token-expiration` trong `application.yml`).
4. **Gateway**: `JwtFilter` ở `api-gateway` cần đọc claim `userId` dạng String (không phải Long nữa).
5. **`health-data-service`**: nếu đang lưu `userId` dạng `Long`, phải đổi sang `String`. `UserCreatedEvent.userId` đã là String nên không vướng phía event.
