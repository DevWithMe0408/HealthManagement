// 6 main sections for the User Profile page.
// Each section is independent — can be lifted into its own React component.

// =========================================================================
// SECTION 1 — Header (avatar + name + email + joined + constitution pill)
// =========================================================================
function S1ProfileHeader({
  name = 'Nguyễn Văn Chiến',
  email = 'chien.nguyen@healthcare.vn',
  joined = 'Tham gia từ 03/2026',
  constitution = 'CAN_DOI',
  mobile = false,
}) {
  return (
    <SectionCard padding={mobile ? 20 : 28}>
      <div
        style={{
          display: 'flex',
          flexDirection: mobile ? 'column' : 'row',
          alignItems: mobile ? 'flex-start' : 'center',
          gap: mobile ? 16 : 20,
          position: 'relative',
        }}
      >
        <Avatar name={name} size={mobile ? 64 : 80} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              flexWrap: 'wrap',
              marginBottom: 4,
            }}
          >
            <h2
              style={{
                margin: 0,
                fontSize: mobile ? 20 : 22,
                fontWeight: 700,
                color: DB.ink,
                letterSpacing: '-0.01em',
              }}
            >
              {name}
            </h2>
            <ConstitutionPill value={constitution} />
          </div>
          <div
            style={{
              fontSize: 14,
              color: DB.textMid,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              marginBottom: 2,
            }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path
                d="M4 4h16v16H4z M4 8l8 5 8-5"
                stroke="currentColor"
                strokeWidth="1.75"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            {email}
          </div>
          <div
            style={{
              fontSize: 13,
              color: DB.textMute,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
              <rect
                x="3"
                y="5"
                width="18"
                height="16"
                rx="2"
                stroke="currentColor"
                strokeWidth="1.75"
              />
              <path
                d="M3 9h18M8 3v4M16 3v4"
                stroke="currentColor"
                strokeWidth="1.75"
                strokeLinecap="round"
              />
            </svg>
            {joined}
          </div>
        </div>
        <button
          style={{
            position: mobile ? 'absolute' : 'static',
            top: 0,
            right: 0,
            background: 'transparent',
            border: 'none',
            color: DB.textMid,
            fontSize: 13.5,
            fontWeight: 500,
            cursor: 'pointer',
            fontFamily: 'inherit',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            padding: '6px 8px',
            borderRadius: 8,
          }}
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
            <path
              d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"
              stroke="currentColor"
              strokeWidth="1.75"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          Đăng xuất
        </button>
      </div>
    </SectionCard>
  );
}

// =========================================================================
// SECTION 2 — Personal info (view + edit modes)
// =========================================================================
const PERSONAL_DATA = {
  name: 'Nguyễn Văn Chiến',
  birthDate: '15/08/1998',
  age: 27,
  gender: 'MALE',
  phone: '0912 345 678',
};

const GENDER_LABEL = { MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác' };

function S2PersonalInfo({ editing = false, saving = false, mobile = false }) {
  return (
    <SectionCard
      title="Thông tin cá nhân"
      subtitle={
        editing
          ? 'Cập nhật thông tin cá nhân của bạn'
          : 'Các thông tin bạn đã nhập khi đăng ký'
      }
      rightSlot={!editing && <EditIconButton />}
      padding={mobile ? 20 : 28}
    >
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: mobile ? '1fr' : '1fr 1fr',
          gap: mobile ? 16 : 24,
          rowGap: mobile ? 16 : 22,
        }}
      >
        {editing ? (
          <>
            <TextInput
              label="Họ và tên"
              value={PERSONAL_DATA.name}
              required
            />
            <TextInput
              label="Ngày sinh"
              value={PERSONAL_DATA.birthDate}
              required
              help={`Bạn ${PERSONAL_DATA.age} tuổi`}
              rightAdornment={
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <rect
                    x="3"
                    y="5"
                    width="18"
                    height="16"
                    rx="2"
                    stroke="currentColor"
                    strokeWidth="1.75"
                  />
                  <path
                    d="M3 9h18M8 3v4M16 3v4"
                    stroke="currentColor"
                    strokeWidth="1.75"
                    strokeLinecap="round"
                  />
                </svg>
              }
            />
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label
                style={{
                  fontSize: 12,
                  fontWeight: 600,
                  color: DB.textMid,
                  letterSpacing: '0.01em',
                }}
              >
                Giới tính <span style={{ color: DB.red600 }}>*</span>
              </label>
              <Segmented
                value={PERSONAL_DATA.gender}
                options={[
                  { value: 'MALE', label: 'Nam' },
                  { value: 'FEMALE', label: 'Nữ' },
                  { value: 'OTHER', label: 'Khác' },
                ]}
              />
            </div>
            <TextInput
              label="Số điện thoại"
              value={PERSONAL_DATA.phone}
              help="Không bắt buộc"
              rightAdornment={
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path
                    d="M22 16.92V21a1 1 0 01-1.11 1 19.86 19.86 0 01-8.63-3.07 19.5 19.5 0 01-6-6A19.86 19.86 0 013.18 4.11 1 1 0 014.18 3h4.09a1 1 0 011 .75c.12.96.34 1.9.66 2.81a1 1 0 01-.23 1L8 9.21a16 16 0 006 6l1.65-1.7a1 1 0 011-.23c.91.32 1.85.54 2.81.66a1 1 0 01.75 1z"
                    stroke="currentColor"
                    strokeWidth="1.75"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              }
            />
          </>
        ) : (
          <>
            <FieldRow label="Họ và tên" value={PERSONAL_DATA.name} />
            <FieldRow
              label="Ngày sinh"
              value={PERSONAL_DATA.birthDate}
              help={`Bạn ${PERSONAL_DATA.age} tuổi`}
            />
            <FieldRow
              label="Giới tính"
              value={GENDER_LABEL[PERSONAL_DATA.gender]}
            />
            <FieldRow label="Số điện thoại" value={PERSONAL_DATA.phone} />
          </>
        )}
      </div>

      {editing && (
        <div
          style={{
            marginTop: 24,
            paddingTop: 20,
            borderTop: `1px solid ${DB.borderSoft}`,
            display: 'flex',
            gap: 10,
            justifyContent: mobile ? 'stretch' : 'flex-end',
            flexDirection: mobile ? 'column-reverse' : 'row',
          }}
        >
          <Button variant="ghost" fullWidth={mobile}>
            Hủy
          </Button>
          <Button
            variant="primary"
            loading={saving}
            fullWidth={mobile}
            leftIcon={
              !saving && (
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                  <path
                    d="M5 12l5 5L20 7"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              )
            }
          >
            {saving ? 'Đang lưu…' : 'Lưu thay đổi'}
          </Button>
        </div>
      )}
    </SectionCard>
  );
}

// =========================================================================
// SECTION 3 — Goal (current + history)
// =========================================================================
const GOAL_HISTORY = [
  { goal: 'GIAM',    range: '03/2026 — hiện tại',   status: 'active'  },
  { goal: 'DUY_TRI', range: '11/2025 — 02/2026',    status: 'stopped' },
  { goal: 'TANG',    range: '06/2025 — 10/2025',    status: 'done'    },
];

function S3Goal({ historyOpen = false, history = GOAL_HISTORY, mobile = false }) {
  const goal = 'GIAM';
  const startDate = '01/03/2026';
  const duration = 'trong 6 tháng';
  const targetWeight = 65;
  const currentWeight = 71.2;
  const startWeight = 75;
  // progress to target (lower is better for GIAM)
  const lost = startWeight - currentWeight;
  const need = startWeight - targetWeight;
  const pct = Math.round((lost / need) * 100);

  const g = GOAL_STYLE[goal];

  return (
    <SectionCard
      title="Mục tiêu hiện tại"
      subtitle="Mục tiêu bạn đang theo đuổi cùng tiến độ"
      padding={mobile ? 20 : 28}
    >
      {/* Current goal card */}
      <div
        style={{
          background: g.bg,
          border: `1px solid ${g.border}`,
          borderRadius: 16,
          padding: mobile ? 18 : 22,
          display: 'flex',
          flexDirection: mobile ? 'column' : 'row',
          gap: mobile ? 16 : 20,
          alignItems: mobile ? 'flex-start' : 'center',
        }}
      >
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: 14,
            background: '#fff',
            display: 'grid',
            placeItems: 'center',
            fontSize: 28,
            boxShadow: '0 4px 12px -4px rgba(15,23,42,.1)',
            flexShrink: 0,
          }}
        >
          {g.icon}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div
            style={{
              fontSize: 11,
              fontWeight: 700,
              color: g.color,
              letterSpacing: '0.08em',
              marginBottom: 2,
            }}
          >
            ĐANG THEO
          </div>
          <div
            style={{
              fontSize: mobile ? 19 : 22,
              fontWeight: 700,
              color: DB.ink,
              letterSpacing: '-0.01em',
              marginBottom: 4,
            }}
          >
            {g.label}
          </div>
          <div style={{ fontSize: 13, color: DB.textMid }}>
            Bắt đầu {startDate} · {duration}
          </div>
        </div>
        <Button
          variant="secondary"
          rightIcon={
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path
                d="M9 18l6-6-6-6"
                stroke="currentColor"
                strokeWidth="2.25"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          }
        >
          Đổi mục tiêu
        </Button>
      </div>

      {/* Progress to target */}
      <div style={{ marginTop: 20 }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'baseline',
            marginBottom: 8,
          }}
        >
          <span style={{ fontSize: 13, color: DB.textMid, fontWeight: 500 }}>
            Tiến độ tới cân nặng mục tiêu
          </span>
          <span
            style={{
              fontSize: 13,
              color: DB.greenDark,
              fontWeight: 700,
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {pct}%
          </span>
        </div>
        <ProgressBar value={lost} max={need} />
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            marginTop: 8,
            fontSize: 12,
            color: DB.textMute,
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          <span>Bắt đầu: {startWeight} kg</span>
          <span style={{ color: DB.greenDark, fontWeight: 600 }}>
            Hiện tại: {currentWeight} kg
          </span>
          <span>Mục tiêu: {targetWeight} kg</span>
        </div>
      </div>

      {/* History collapsible */}
      <div
        style={{
          marginTop: 22,
          paddingTop: 18,
          borderTop: `1px solid ${DB.borderSoft}`,
        }}
      >
        <button
          style={{
            background: 'transparent',
            border: 'none',
            padding: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            width: '100%',
            cursor: 'pointer',
            fontFamily: 'inherit',
          }}
        >
          <span
            style={{
              fontSize: 13.5,
              fontWeight: 600,
              color: DB.text,
            }}
          >
            Lịch sử mục tiêu{' '}
            <span style={{ color: DB.textMute, fontWeight: 500 }}>
              ({history.length})
            </span>
          </span>
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            style={{
              transform: historyOpen ? 'rotate(180deg)' : 'rotate(0deg)',
              transition: 'transform .2s',
              color: DB.textMute,
            }}
          >
            <path
              d="M6 9l6 6 6-6"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>

        {historyOpen && (
          <div style={{ marginTop: 14 }}>
            {history.length === 0 ? (
              <div
                style={{
                  fontSize: 13.5,
                  color: DB.textMute,
                  textAlign: 'center',
                  padding: '24px 0',
                  fontStyle: 'italic',
                }}
              >
                Bạn chưa từng thay đổi mục tiêu
              </div>
            ) : (
              <div
                style={{
                  border: `1px solid ${DB.border}`,
                  borderRadius: 12,
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: mobile ? '1fr 1fr' : '1.2fr 1.5fr 1fr',
                    background: DB.borderSoft,
                    padding: '10px 14px',
                    fontSize: 11,
                    fontWeight: 700,
                    color: DB.textMute,
                    letterSpacing: '0.06em',
                    textTransform: 'uppercase',
                  }}
                >
                  <div>Mục tiêu</div>
                  {!mobile && <div>Khoảng thời gian</div>}
                  <div>Trạng thái</div>
                </div>
                {history.map((row, i) => {
                  const gg = GOAL_STYLE[row.goal];
                  const statusStyle = {
                    active:  { label: 'Đang theo',  bg: DB.green100, c: DB.greenDark },
                    done:    { label: 'Đã đạt',    bg: DB.blue100,  c: DB.blue700 },
                    stopped: { label: 'Đã dừng',   bg: DB.borderSoft, c: DB.textMid },
                  }[row.status];
                  return (
                    <div
                      key={i}
                      style={{
                        display: 'grid',
                        gridTemplateColumns: mobile ? '1fr 1fr' : '1.2fr 1.5fr 1fr',
                        padding: '12px 14px',
                        fontSize: 13.5,
                        color: DB.text,
                        borderTop: i === 0 ? 'none' : `1px solid ${DB.borderSoft}`,
                        alignItems: 'center',
                      }}
                    >
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 8,
                          fontWeight: 600,
                        }}
                      >
                        <span style={{ fontSize: 16 }}>{gg.icon}</span>
                        {gg.label}
                      </div>
                      {!mobile && (
                        <div
                          style={{
                            color: DB.textMid,
                            fontVariantNumeric: 'tabular-nums',
                          }}
                        >
                          {row.range}
                        </div>
                      )}
                      <div>
                        <span
                          style={{
                            display: 'inline-block',
                            padding: '3px 9px',
                            background: statusStyle.bg,
                            color: statusStyle.c,
                            borderRadius: 999,
                            fontSize: 11.5,
                            fontWeight: 600,
                          }}
                        >
                          {statusStyle.label}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
    </SectionCard>
  );
}

// =========================================================================
// SECTION 4 — Health settings (PBF method + units)
// =========================================================================
function PbfMethodCard({ icon, title, desc, selected, disabled = false, badge }) {
  return (
    <div
      style={{
        flex: 1,
        background: selected ? DB.green50 : '#fff',
        border: `2px solid ${selected ? DB.green : DB.border}`,
        borderRadius: 14,
        padding: 18,
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.55 : 1,
        position: 'relative',
        boxShadow: selected
          ? '0 4px 12px -4px rgba(5,150,105,.25)'
          : '0 1px 2px rgba(15,23,42,.03)',
        transition: 'all .15s',
        minWidth: 0,
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 8,
          marginBottom: 8,
        }}
      >
        <div
          style={{
            width: 44,
            height: 44,
            borderRadius: 12,
            background: selected ? '#fff' : DB.borderSoft,
            display: 'grid',
            placeItems: 'center',
            fontSize: 24,
            flexShrink: 0,
          }}
        >
          {icon}
        </div>
        {/* Radio dot */}
        <div
          style={{
            width: 20,
            height: 20,
            borderRadius: '50%',
            border: `2px solid ${selected ? DB.green : DB.border}`,
            background: '#fff',
            display: 'grid',
            placeItems: 'center',
            flexShrink: 0,
          }}
        >
          {selected && (
            <div
              style={{
                width: 10,
                height: 10,
                borderRadius: '50%',
                background: DB.green,
              }}
            />
          )}
        </div>
      </div>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginBottom: 4,
        }}
      >
        <h4
          style={{
            margin: 0,
            fontSize: 15,
            fontWeight: 700,
            color: DB.ink,
            letterSpacing: '-0.005em',
          }}
        >
          {title}
        </h4>
        {badge && (
          <span
            style={{
              fontSize: 10,
              fontWeight: 700,
              padding: '2px 6px',
              borderRadius: 4,
              background: selected ? DB.green : DB.borderSoft,
              color: selected ? '#fff' : DB.textMid,
              letterSpacing: '0.05em',
            }}
          >
            {badge}
          </span>
        )}
      </div>
      <p
        style={{
          margin: 0,
          fontSize: 13,
          color: DB.textMid,
          lineHeight: 1.55,
        }}
      >
        {desc}
      </p>
    </div>
  );
}

function S4HealthSettings({ pbfMethod = 'FORMULA', mobile = false }) {
  return (
    <SectionCard
      title="Cài đặt sức khỏe"
      subtitle="Tinh chỉnh cách hệ thống tính các chỉ số của bạn"
      padding={mobile ? 20 : 28}
    >
      {/* 4.1 PBF method */}
      <div>
        <div style={{ marginBottom: 14 }}>
          <h4
            style={{
              margin: 0,
              fontSize: 15,
              fontWeight: 700,
              color: DB.ink,
              marginBottom: 4,
            }}
          >
            Phương pháp tính % mỡ cơ thể
          </h4>
          <p style={{ margin: 0, fontSize: 13, color: DB.textMute }}>
            Chọn cách hệ thống tính chỉ số PBF của bạn
          </p>
        </div>
        <div
          style={{
            display: 'flex',
            flexDirection: mobile ? 'column' : 'row',
            gap: 12,
          }}
        >
          <PbfMethodCard
            icon="⚖️"
            title="Công thức Navy"
            badge="Mặc định"
            desc="Tính từ chiều cao + vòng eo + vòng cổ (+ vòng hông cho nữ). Nhanh, không cần thiết bị."
            selected={pbfMethod === 'FORMULA'}
          />
          <PbfMethodCard
            icon="🧠"
            title="Model AI"
            badge="BETA"
            desc="Sử dụng machine learning để dự đoán chính xác hơn. Cần ít nhất 30 ngày dữ liệu."
            selected={pbfMethod === 'MODEL_1'}
          />
        </div>
      </div>

      {/* 4.2 Units (disabled) */}
      <div
        style={{
          marginTop: 28,
          paddingTop: 22,
          borderTop: `1px solid ${DB.borderSoft}`,
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: mobile ? 'flex-start' : 'center',
            flexDirection: mobile ? 'column' : 'row',
            gap: 12,
          }}
        >
          <div>
            <h4
              style={{
                margin: 0,
                fontSize: 15,
                fontWeight: 700,
                color: DB.ink,
                marginBottom: 4,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              Đơn vị đo
              <span
                style={{
                  fontSize: 10,
                  fontWeight: 700,
                  padding: '2px 6px',
                  borderRadius: 4,
                  background: DB.borderSoft,
                  color: DB.textMute,
                  letterSpacing: '0.05em',
                }}
              >
                SẮP CÓ
              </span>
            </h4>
            <p style={{ margin: 0, fontSize: 13, color: DB.textMute }}>
              Hiển thị cân nặng và chiều cao theo đơn vị bạn quen
            </p>
          </div>
          <div style={{ opacity: 0.5, pointerEvents: 'none' }}>
            <Segmented
              value="KG"
              options={[
                { value: 'KG', label: 'kg / cm' },
                { value: 'LBS', label: 'lbs / in' },
              ]}
            />
          </div>
        </div>
      </div>
    </SectionCard>
  );
}

// =========================================================================
// SECTION 5 — Security (change password)
// =========================================================================
function S5Security({ showError = false, mobile = false }) {
  return (
    <SectionCard
      title="🔒 Bảo mật"
      subtitle="Thay đổi mật khẩu để bảo vệ tài khoản của bạn"
      padding={mobile ? 20 : 28}
    >
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: mobile ? '1fr' : '1fr 1fr',
          gap: 16,
          rowGap: 16,
        }}
      >
        <div style={{ gridColumn: mobile ? 'auto' : 'span 2' }}>
          <TextInput
            label="Mật khẩu hiện tại"
            type="password"
            value="••••••••"
            required
          />
        </div>
        <TextInput
          label="Mật khẩu mới"
          type="password"
          value={showError ? '••••' : ''}
          placeholder="Tối thiểu 8 ký tự"
          required
          error={
            showError
              ? 'Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa và số'
              : null
          }
          help={!showError ? 'Tối thiểu 8 ký tự, có chữ hoa và số' : null}
        />
        <TextInput
          label="Xác nhận mật khẩu mới"
          type="password"
          placeholder="Nhập lại mật khẩu mới"
          required
        />
      </div>

      <div
        style={{
          marginTop: 22,
          paddingTop: 18,
          borderTop: `1px solid ${DB.borderSoft}`,
          display: 'flex',
          justifyContent: mobile ? 'stretch' : 'flex-end',
        }}
      >
        <Button variant="primary" fullWidth={mobile}>
          Đổi mật khẩu
        </Button>
      </div>
    </SectionCard>
  );
}

// =========================================================================
// SECTION 6 — Danger zone
// =========================================================================
function S6DangerZone({ mobile = false }) {
  return (
    <SectionCard
      borderColor="#fecaca"
      background="#fef9f9"
      padding={mobile ? 20 : 24}
    >
      <div
        style={{
          display: 'flex',
          flexDirection: mobile ? 'column' : 'row',
          alignItems: mobile ? 'flex-start' : 'center',
          justifyContent: 'space-between',
          gap: 16,
        }}
      >
        <div style={{ display: 'flex', gap: 14, alignItems: 'flex-start' }}>
          <div
            style={{
              width: 40,
              height: 40,
              borderRadius: 10,
              background: '#fee2e2',
              color: DB.red700,
              display: 'grid',
              placeItems: 'center',
              flexShrink: 0,
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path
                d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0zM12 9v4M12 17h.01"
                stroke="currentColor"
                strokeWidth="1.75"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>
          <div>
            <h3
              style={{
                margin: 0,
                fontSize: 14,
                fontWeight: 700,
                color: DB.red700,
                marginBottom: 4,
                letterSpacing: '0.02em',
              }}
            >
              VÙNG NGUY HIỂM
            </h3>
            <p style={{ margin: 0, fontSize: 13, color: DB.textMid, maxWidth: 460 }}>
              Xóa tài khoản sẽ xóa vĩnh viễn toàn bộ dữ liệu sức khỏe, lịch sử
              mục tiêu và thực đơn của bạn. Hành động này không thể hoàn tác.
            </p>
          </div>
        </div>
        <Button variant="danger-outline" fullWidth={mobile}>
          Xóa tài khoản
        </Button>
      </div>
    </SectionCard>
  );
}

Object.assign(window, {
  S1ProfileHeader,
  S2PersonalInfo,
  S3Goal,
  S4HealthSettings,
  S5Security,
  S6DangerZone,
  PbfMethodCard,
});
