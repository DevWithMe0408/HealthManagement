// Modals, toast, and loading skeleton for Profile

// =========================================================================
// Modal shell — overlay + centered card
// =========================================================================
function ModalShell({ children, width = 480, onClose }) {
  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        background: 'rgba(15, 23, 42, 0.45)',
        backdropFilter: 'blur(2px)',
        display: 'grid',
        placeItems: 'center',
        padding: 24,
        zIndex: 50,
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: width,
          background: '#fff',
          borderRadius: 20,
          boxShadow:
            '0 20px 50px -12px rgba(15,23,42,.35), 0 8px 16px -8px rgba(15,23,42,.18)',
          overflow: 'hidden',
          animation: 'profileFadeIn .25s ease-out',
        }}
      >
        {children}
      </div>
    </div>
  );
}

// =========================================================================
// Goal change modal — 3 cards (Giảm / Duy trì / Tăng)
// =========================================================================
function GoalChangeModal({ selected = 'TANG' }) {
  const options = [
    {
      value: 'GIAM',
      icon: '📉',
      label: 'GIẢM CÂN',
      desc: 'Tạo deficit calo. Hệ thống đề xuất khẩu phần nhỏ hơn.',
    },
    {
      value: 'DUY_TRI',
      icon: '⚖️',
      label: 'DUY TRÌ',
      desc: 'Cân bằng năng lượng. Giữ trọng lượng hiện tại.',
    },
    {
      value: 'TANG',
      icon: '📈',
      label: 'TĂNG CÂN',
      desc: 'Surplus calo lành mạnh. Phù hợp xây cơ.',
    },
  ];

  return (
    <ModalShell width={520}>
      <div style={{ padding: '24px 28px 0' }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: 16,
            marginBottom: 4,
          }}
        >
          <h3
            style={{
              margin: 0,
              fontSize: 19,
              fontWeight: 700,
              color: DB.ink,
              letterSpacing: '-0.01em',
            }}
          >
            Đổi mục tiêu
          </h3>
          <button
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: DB.textMute,
              padding: 4,
              display: 'flex',
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path
                d="M18 6L6 18M6 6l12 12"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
            </svg>
          </button>
        </div>
        <p style={{ margin: 0, fontSize: 13.5, color: DB.textMid, marginBottom: 18 }}>
          Mục tiêu mới sẽ áp dụng cho các đề xuất thực đơn sắp tới. Mục tiêu cũ
          được lưu vào lịch sử.
        </p>
      </div>

      <div
        style={{
          padding: '0 28px',
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
        }}
      >
        {options.map((opt) => {
          const isSelected = opt.value === selected;
          const isCurrent = opt.value === 'GIAM'; // simulate current goal
          return (
            <div
              key={opt.value}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 14,
                padding: '14px 16px',
                border: `2px solid ${isSelected ? DB.green : DB.border}`,
                background: isSelected ? DB.green50 : '#fff',
                borderRadius: 12,
                cursor: 'pointer',
                position: 'relative',
              }}
            >
              <div
                style={{
                  width: 44,
                  height: 44,
                  borderRadius: 11,
                  background: '#fff',
                  border: `1px solid ${DB.border}`,
                  display: 'grid',
                  placeItems: 'center',
                  fontSize: 22,
                  flexShrink: 0,
                }}
              >
                {opt.icon}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    fontSize: 14.5,
                    fontWeight: 700,
                    color: DB.ink,
                    letterSpacing: '0.02em',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                  }}
                >
                  {opt.label}
                  {isCurrent && (
                    <span
                      style={{
                        fontSize: 10,
                        fontWeight: 700,
                        padding: '2px 6px',
                        borderRadius: 4,
                        background: DB.borderSoft,
                        color: DB.textMid,
                        letterSpacing: '0.05em',
                      }}
                    >
                      HIỆN TẠI
                    </span>
                  )}
                </div>
                <div style={{ fontSize: 12.5, color: DB.textMute, marginTop: 2 }}>
                  {opt.desc}
                </div>
              </div>
              <div
                style={{
                  width: 22,
                  height: 22,
                  borderRadius: '50%',
                  border: `2px solid ${isSelected ? DB.green : DB.border}`,
                  background: '#fff',
                  display: 'grid',
                  placeItems: 'center',
                  flexShrink: 0,
                }}
              >
                {isSelected && (
                  <div
                    style={{
                      width: 12,
                      height: 12,
                      borderRadius: '50%',
                      background: DB.green,
                    }}
                  />
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div
        style={{
          padding: '20px 28px 24px',
          marginTop: 14,
          display: 'flex',
          gap: 10,
          justifyContent: 'flex-end',
          borderTop: `1px solid ${DB.borderSoft}`,
          background: '#fafbfa',
        }}
      >
        <Button variant="ghost">Hủy</Button>
        <Button variant="primary">Xác nhận đổi</Button>
      </div>
    </ModalShell>
  );
}

// =========================================================================
// Delete account confirm modal
// =========================================================================
function DeleteAccountModal() {
  return (
    <ModalShell width={460}>
      <div style={{ padding: '28px 28px 0' }}>
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: 16,
            background: '#fee2e2',
            color: DB.red700,
            display: 'grid',
            placeItems: 'center',
            marginBottom: 18,
          }}
        >
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
            <path
              d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0zM12 9v4M12 17h.01"
              stroke="currentColor"
              strokeWidth="1.75"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <h3
          style={{
            margin: 0,
            fontSize: 20,
            fontWeight: 700,
            color: DB.ink,
            marginBottom: 8,
            letterSpacing: '-0.01em',
          }}
        >
          Xóa tài khoản vĩnh viễn?
        </h3>
        <p style={{ margin: 0, fontSize: 14, color: DB.textMid, lineHeight: 1.6 }}>
          Bạn sắp xóa tài khoản{' '}
          <span style={{ fontWeight: 600, color: DB.ink }}>
            chien.nguyen@healthcare.vn
          </span>{' '}
          cùng toàn bộ dữ liệu liên quan:
        </p>
        <ul
          style={{
            margin: '12px 0 0',
            padding: 0,
            listStyle: 'none',
            display: 'flex',
            flexDirection: 'column',
            gap: 6,
          }}
        >
          {[
            'Lịch sử chỉ số sức khỏe (cân nặng, BMI, PBF…)',
            'Mục tiêu đang theo và toàn bộ lịch sử mục tiêu',
            'Thực đơn được đề xuất và phản hồi',
          ].map((line, i) => (
            <li
              key={i}
              style={{
                fontSize: 13,
                color: DB.textMid,
                display: 'flex',
                alignItems: 'flex-start',
                gap: 8,
              }}
            >
              <span style={{ color: DB.red600, marginTop: 1 }}>•</span>
              {line}
            </li>
          ))}
        </ul>

        <div
          style={{
            marginTop: 18,
            padding: '12px 14px',
            background: '#fef2f2',
            border: '1px solid #fecaca',
            borderRadius: 10,
            fontSize: 12.5,
            color: DB.red700,
            display: 'flex',
            gap: 8,
            alignItems: 'flex-start',
          }}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0, marginTop: 1 }}>
            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
            <path d="M12 8v4M12 16h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
          <span>
            <strong>Không thể hoàn tác.</strong> Sau 30 ngày dữ liệu sẽ bị xóa
            khỏi máy chủ vĩnh viễn.
          </span>
        </div>
      </div>

      <div
        style={{
          padding: '20px 28px 24px',
          marginTop: 22,
          display: 'flex',
          gap: 10,
          justifyContent: 'flex-end',
          background: '#fafbfa',
          borderTop: `1px solid ${DB.borderSoft}`,
        }}
      >
        <Button variant="ghost">Hủy</Button>
        <Button variant="danger">Tôi hiểu, vẫn xóa</Button>
      </div>
    </ModalShell>
  );
}

// =========================================================================
// Toast — floating top-right notification
// =========================================================================
function Toast({ message = 'Đã cập nhật thông tin', variant = 'success' }) {
  const isSuccess = variant === 'success';
  return (
    <div
      style={{
        background: '#fff',
        border: `1px solid ${isSuccess ? DB.green200 : DB.border}`,
        borderLeft: `4px solid ${isSuccess ? DB.green : DB.blue600}`,
        borderRadius: 12,
        padding: '14px 18px',
        boxShadow: '0 12px 28px -8px rgba(15,23,42,.18), 0 4px 8px -2px rgba(15,23,42,.08)',
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        minWidth: 280,
        maxWidth: 380,
        animation: 'profileFadeIn .25s ease-out',
      }}
    >
      <div
        style={{
          width: 32,
          height: 32,
          borderRadius: 999,
          background: isSuccess ? DB.green100 : DB.blue100,
          color: isSuccess ? DB.greenDark : DB.blue700,
          display: 'grid',
          placeItems: 'center',
          flexShrink: 0,
        }}
      >
        {isSuccess ? (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path
              d="M5 12l5 5L20 7"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        ) : (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
            <path d="M12 16v-4M12 8h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        )}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: DB.ink }}>
          {message}
        </div>
        <div style={{ fontSize: 12, color: DB.textMute, marginTop: 1 }}>
          Mọi thay đổi đã được lưu vào máy chủ.
        </div>
      </div>
      <button
        style={{
          background: 'transparent',
          border: 'none',
          padding: 4,
          cursor: 'pointer',
          color: DB.textMute,
          display: 'flex',
        }}
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path
            d="M18 6L6 18M6 6l12 12"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
      </button>
    </div>
  );
}

// =========================================================================
// Loading skeleton — for the whole page while data fetches
// =========================================================================
function SkeletonLine({ w = '60%', h = 14, mt = 0 }) {
  return (
    <div
      style={{
        width: w,
        height: h,
        marginTop: mt,
        borderRadius: 6,
        background:
          'linear-gradient(90deg, #f1f3f2 0%, #e6eae8 50%, #f1f3f2 100%)',
        backgroundSize: '400% 100%',
        animation: 'profileShimmer 1.6s ease-in-out infinite',
      }}
    />
  );
}

function ProfileSkeleton({ mobile = false }) {
  return (
    <ProfileMain mobile={mobile}>
      {/* S1 header skeleton */}
      <SectionCard padding={mobile ? 20 : 28}>
        <div style={{ display: 'flex', gap: 20, alignItems: 'center' }}>
          <div
            style={{
              width: mobile ? 64 : 80,
              height: mobile ? 64 : 80,
              borderRadius: '50%',
              background:
                'linear-gradient(90deg, #e6eae8 0%, #d8ddda 50%, #e6eae8 100%)',
              backgroundSize: '400% 100%',
              animation: 'profileShimmer 1.6s ease-in-out infinite',
              flexShrink: 0,
            }}
          />
          <div style={{ flex: 1 }}>
            <SkeletonLine w="60%" h={20} />
            <SkeletonLine w="45%" h={14} mt={10} />
            <SkeletonLine w="30%" h={12} mt={8} />
          </div>
        </div>
      </SectionCard>

      {/* S2 info skeleton */}
      <SectionCard padding={mobile ? 20 : 28}>
        <SkeletonLine w="35%" h={12} />
        <div
          style={{
            marginTop: 20,
            display: 'grid',
            gridTemplateColumns: mobile ? '1fr' : '1fr 1fr',
            gap: 22,
          }}
        >
          {[1, 2, 3, 4].map((i) => (
            <div key={i}>
              <SkeletonLine w="30%" h={10} />
              <SkeletonLine w="70%" h={16} mt={8} />
            </div>
          ))}
        </div>
      </SectionCard>

      {/* S3 goal skeleton */}
      <SectionCard padding={mobile ? 20 : 28}>
        <SkeletonLine w="25%" h={12} />
        <div
          style={{
            marginTop: 18,
            height: 90,
            background:
              'linear-gradient(90deg, #f1f3f2 0%, #e6eae8 50%, #f1f3f2 100%)',
            backgroundSize: '400% 100%',
            animation: 'profileShimmer 1.6s ease-in-out infinite',
            borderRadius: 14,
          }}
        />
        <SkeletonLine w="100%" h={10} mt={20} />
      </SectionCard>
    </ProfileMain>
  );
}

Object.assign(window, {
  ModalShell,
  GoalChangeModal,
  DeleteAccountModal,
  Toast,
  ProfileSkeleton,
});
