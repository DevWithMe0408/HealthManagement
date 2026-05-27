// Shared atoms for User Profile artboards.
// Depends on DB tokens + dbFont from dashboard-shared.jsx (load that first).

// =========================================================================
// Page wrapper — Profile content sits inside the dashboard PageFrame
// =========================================================================
function ProfileMain({ children, mobile = false }) {
  return (
    <div
      style={{
        width: '100%',
        maxWidth: mobile ? '100%' : 880,
        margin: '0 auto',
        display: 'flex',
        flexDirection: 'column',
        gap: mobile ? 14 : 20,
        paddingBottom: 24,
      }}
    >
      {children}
    </div>
  );
}

// =========================================================================
// Section card — every section uses this shell
// =========================================================================
function SectionCard({
  title,
  subtitle,
  rightSlot,
  children,
  padding = 28,
  style,
  borderColor,
  background,
}) {
  return (
    <section
      style={{
        background: background || '#fff',
        border: `1px solid ${borderColor || DB.border}`,
        borderRadius: 20,
        padding,
        boxShadow:
          '0 1px 2px rgba(15,23,42,.04), 0 1px 3px rgba(15,23,42,.03)',
        ...style,
      }}
    >
      {(title || rightSlot) && (
        <header
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: 12,
            marginBottom: title ? 20 : 0,
          }}
        >
          <div style={{ minWidth: 0 }}>
            {title && (
              <h3
                style={{
                  margin: 0,
                  fontSize: 12,
                  fontWeight: 700,
                  color: DB.textMute,
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                }}
              >
                {title}
              </h3>
            )}
            {subtitle && (
              <div
                style={{
                  marginTop: 6,
                  fontSize: 13,
                  color: DB.textMute,
                  fontWeight: 400,
                  letterSpacing: 0,
                  textTransform: 'none',
                }}
              >
                {subtitle}
              </div>
            )}
          </div>
          {rightSlot}
        </header>
      )}
      {children}
    </section>
  );
}

// =========================================================================
// Avatar — initials on a brand-green gradient
// =========================================================================
function Avatar({ name = 'C', size = 80, fontSize }) {
  const initial = (name || 'U').trim().charAt(0).toUpperCase();
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: `linear-gradient(135deg, ${DB.green} 0%, #10b981 100%)`,
        color: '#fff',
        display: 'grid',
        placeItems: 'center',
        fontSize: fontSize || size * 0.42,
        fontWeight: 700,
        letterSpacing: '-0.01em',
        boxShadow:
          '0 8px 22px -8px rgba(5,150,105,.55), 0 2px 4px rgba(5,150,105,.18)',
        flexShrink: 0,
        userSelect: 'none',
      }}
    >
      {initial}
    </div>
  );
}

// =========================================================================
// Constitution pill (4 states)
// =========================================================================
const CONSTITUTION_STYLE = {
  GAY:      { label: 'GẦY',      bg: DB.blue50,   text: DB.blue700,    border: DB.blue300 },
  CAN_DOI:  { label: 'CÂN ĐỐI',  bg: DB.green100, text: DB.greenDark,  border: DB.green200 },
  THUA_CAN: { label: 'THỪA CÂN', bg: DB.amber100, text: DB.amber700,   border: DB.amber300 },
  BEO_PHI:  { label: 'BÉO PHÌ',  bg: DB.red100,   text: DB.red700,     border: '#fca5a5' },
};

function ConstitutionPill({ value = 'CAN_DOI' }) {
  const s = CONSTITUTION_STYLE[value] || CONSTITUTION_STYLE.CAN_DOI;
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        padding: '4px 10px',
        background: s.bg,
        color: s.text,
        border: `1px solid ${s.border}`,
        borderRadius: 999,
        fontSize: 11,
        fontWeight: 700,
        letterSpacing: '0.06em',
      }}
    >
      <span
        style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: s.text,
          opacity: 0.85,
        }}
      />
      {s.label}
    </span>
  );
}

// =========================================================================
// Field row — readonly key/value display
// =========================================================================
function FieldRow({ label, value, help, placeholder = 'Chưa cập nhật' }) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 6,
        minWidth: 0,
      }}
    >
      <div
        style={{
          fontSize: 12,
          fontWeight: 500,
          color: DB.textMute,
          letterSpacing: '0.01em',
        }}
      >
        {label}
      </div>
      <div
        style={{
          fontSize: 15,
          fontWeight: 500,
          color: value ? DB.ink : DB.textFaint,
          fontStyle: value ? 'normal' : 'italic',
          minHeight: 22,
        }}
      >
        {value || placeholder}
      </div>
      {help && (
        <div style={{ fontSize: 12, color: DB.textMute, marginTop: 2 }}>{help}</div>
      )}
    </div>
  );
}

// =========================================================================
// Text input — used in edit mode
// =========================================================================
function TextInput({
  label,
  value,
  placeholder,
  help,
  error,
  type = 'text',
  rightAdornment,
  disabled = false,
  required = false,
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, minWidth: 0 }}>
      <label
        style={{
          fontSize: 12,
          fontWeight: 600,
          color: DB.textMid,
          letterSpacing: '0.01em',
          display: 'flex',
          alignItems: 'center',
          gap: 4,
        }}
      >
        {label}
        {required && <span style={{ color: DB.red600 }}>*</span>}
      </label>
      <div style={{ position: 'relative' }}>
        <input
          type={type}
          defaultValue={value || ''}
          placeholder={placeholder}
          disabled={disabled}
          readOnly
          style={{
            width: '100%',
            padding: '11px 14px',
            paddingRight: rightAdornment ? 44 : 14,
            border: `1.5px solid ${error ? DB.red600 : DB.border}`,
            borderRadius: 10,
            fontSize: 14.5,
            color: DB.ink,
            background: disabled ? DB.borderSoft : '#fff',
            outline: 'none',
            fontFamily: 'inherit',
            boxShadow: error
              ? `0 0 0 3px ${DB.red50}`
              : '0 1px 2px rgba(15,23,42,.03)',
          }}
        />
        {rightAdornment && (
          <div
            style={{
              position: 'absolute',
              right: 12,
              top: '50%',
              transform: 'translateY(-50%)',
              color: DB.textMute,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {rightAdornment}
          </div>
        )}
      </div>
      {error ? (
        <div
          style={{
            fontSize: 12,
            color: DB.red600,
            display: 'flex',
            alignItems: 'center',
            gap: 4,
          }}
        >
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
            <path
              d="M12 8v4M12 16h.01"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            />
          </svg>
          {error}
        </div>
      ) : help ? (
        <div style={{ fontSize: 12, color: DB.textMute }}>{help}</div>
      ) : null}
    </div>
  );
}

// =========================================================================
// Segmented control (Nam/Nữ/Khác)
// =========================================================================
function Segmented({ options, value }) {
  return (
    <div
      style={{
        display: 'inline-flex',
        background: DB.borderSoft,
        padding: 4,
        borderRadius: 10,
        gap: 2,
        width: '100%',
      }}
    >
      {options.map((opt) => {
        const active = opt.value === value;
        return (
          <div
            key={opt.value}
            style={{
              flex: 1,
              textAlign: 'center',
              padding: '8px 12px',
              borderRadius: 8,
              fontSize: 13.5,
              fontWeight: active ? 600 : 500,
              color: active ? DB.greenDark : DB.textMid,
              background: active ? '#fff' : 'transparent',
              boxShadow: active
                ? '0 1px 3px rgba(15,23,42,.08), 0 1px 1px rgba(15,23,42,.04)'
                : 'none',
              cursor: 'pointer',
              transition: 'all .15s',
              userSelect: 'none',
            }}
          >
            {opt.label}
          </div>
        );
      })}
    </div>
  );
}

// =========================================================================
// Primary / secondary / danger button
// =========================================================================
function Button({
  children,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  disabled = false,
  leftIcon,
  rightIcon,
  style,
}) {
  const isPrimary = variant === 'primary';
  const isGhost = variant === 'ghost';
  const isDanger = variant === 'danger';
  const isDangerOutline = variant === 'danger-outline';
  const isSecondary = variant === 'secondary';

  const sizes = {
    sm: { padY: 8, padX: 14, fz: 13 },
    md: { padY: 11, padX: 20, fz: 14 },
    lg: { padY: 13, padX: 24, fz: 15 },
  };
  const sz = sizes[size];

  let bg = '#fff';
  let color = DB.text;
  let border = `1px solid ${DB.border}`;
  let shadow = '0 1px 2px rgba(15,23,42,.04)';

  if (isPrimary) {
    bg = `linear-gradient(135deg, ${DB.green} 0%, #10b981 100%)`;
    color = '#fff';
    border = 'none';
    shadow = '0 6px 16px -6px rgba(5,150,105,.55), 0 2px 4px -1px rgba(5,150,105,.25)';
  } else if (isSecondary) {
    bg = DB.green50;
    color = DB.greenDark;
    border = `1px solid ${DB.green200}`;
  } else if (isGhost) {
    bg = 'transparent';
    color = DB.textMid;
    border = 'none';
    shadow = 'none';
  } else if (isDanger) {
    bg = DB.red600;
    color = '#fff';
    border = 'none';
    shadow = '0 6px 16px -6px rgba(220,38,38,.45)';
  } else if (isDangerOutline) {
    bg = '#fff';
    color = DB.red700;
    border = `1.5px solid #fca5a5`;
  }

  if (disabled || loading) {
    bg = DB.borderSoft;
    color = DB.textFaint;
    border = `1px solid ${DB.border}`;
    shadow = 'none';
  }

  return (
    <button
      style={{
        background: bg,
        color,
        border,
        boxShadow: shadow,
        padding: `${sz.padY}px ${sz.padX}px`,
        borderRadius: 10,
        fontSize: sz.fz,
        fontWeight: 600,
        fontFamily: 'inherit',
        cursor: disabled || loading ? 'not-allowed' : 'pointer',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        width: fullWidth ? '100%' : 'auto',
        transition: 'all .15s',
        ...style,
      }}
    >
      {loading && (
        <span
          style={{
            width: 14,
            height: 14,
            borderRadius: '50%',
            border: '2px solid currentColor',
            borderTopColor: 'transparent',
            animation: 'profileSpin 0.7s linear infinite',
          }}
        />
      )}
      {!loading && leftIcon}
      {children}
      {rightIcon}
    </button>
  );
}

// =========================================================================
// Pencil edit icon button
// =========================================================================
function EditIconButton({ label = 'Chỉnh sửa' }) {
  return (
    <button
      style={{
        background: 'transparent',
        border: `1px solid ${DB.border}`,
        padding: '6px 10px 6px 10px',
        borderRadius: 8,
        color: DB.textMid,
        cursor: 'pointer',
        fontSize: 12.5,
        fontWeight: 500,
        fontFamily: 'inherit',
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
      }}
    >
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
        <path
          d="M12 20h9M16.5 3.5a2.121 2.121 0 113 3L7 19l-4 1 1-4L16.5 3.5z"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      {label}
    </button>
  );
}

// =========================================================================
// Progress bar
// =========================================================================
function ProgressBar({ value = 0, max = 100, color, height = 10 }) {
  const pct = Math.min(100, Math.max(0, (value / max) * 100));
  return (
    <div
      style={{
        background: DB.borderSoft,
        height,
        borderRadius: 999,
        overflow: 'hidden',
        position: 'relative',
      }}
    >
      <div
        style={{
          width: `${pct}%`,
          height: '100%',
          background:
            color ||
            `linear-gradient(90deg, ${DB.green} 0%, #10b981 100%)`,
          borderRadius: 999,
          transition: 'width .35s ease',
        }}
      />
    </div>
  );
}

// =========================================================================
// Goal icon + style map
// =========================================================================
const GOAL_STYLE = {
  GIAM:    { label: 'GIẢM CÂN', icon: '📉', color: DB.green,  bg: DB.green50,  border: DB.green200 },
  DUY_TRI: { label: 'DUY TRÌ',  icon: '⚖️', color: DB.blue600, bg: DB.blue50,   border: DB.blue300 },
  TANG:    { label: 'TĂNG CÂN', icon: '📈', color: DB.orange600, bg: DB.orange50, border: '#fdba74' },
};

// =========================================================================
// Style injection (one place)
// =========================================================================
function ProfileStyles() {
  return (
    <style>{`
      @keyframes profileSpin {
        to { transform: rotate(360deg); }
      }
      @keyframes profileShimmer {
        0% { background-position: -400% 0; }
        100% { background-position: 400% 0; }
      }
      @keyframes profileFadeIn {
        from { opacity: 0; transform: translateY(4px); }
        to { opacity: 1; transform: translateY(0); }
      }
    `}</style>
  );
}

Object.assign(window, {
  ProfileMain,
  SectionCard,
  Avatar,
  ConstitutionPill,
  CONSTITUTION_STYLE,
  FieldRow,
  TextInput,
  Segmented,
  Button,
  EditIconButton,
  ProgressBar,
  GOAL_STYLE,
  ProfileStyles,
});
