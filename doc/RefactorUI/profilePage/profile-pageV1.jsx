// Full Profile page composers (desktop + mobile) — uses Sidebar/TopHeader
// from dashboard-shared.jsx but with no active nav item (Profile is reached
// via the user-menu in TopHeader, not from the sidebar list).

// =========================================================================
// Profile page shell — desktop
// =========================================================================
function ProfilePageShell({ children, userName = 'Chiến' }) {
  return (
    <div
      style={{
        ...dbFont,
        background: DB.bg,
        width: '100%',
        height: '100%',
        display: 'flex',
        overflow: 'hidden',
        position: 'relative',
      }}
    >
      <Sidebar active="__profile__" />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <TopHeader userName={userName} />
        <main
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '32px 40px 48px',
            background: DB.bg,
          }}
        >
          <PageHeader />
          {children}
        </main>
      </div>
    </div>
  );
}

// =========================================================================
// Profile page shell — mobile
// =========================================================================
function ProfilePageShellMobile({ children, userName = 'Chiến' }) {
  return (
    <div
      style={{
        ...dbFont,
        background: DB.bg,
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        position: 'relative',
      }}
    >
      <header
        style={{
          background: '#fff',
          borderBottom: `1px solid ${DB.border}`,
          padding: '12px 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexShrink: 0,
          gap: 12,
        }}
      >
        <button
          style={{
            background: 'transparent',
            border: 'none',
            padding: 4,
            cursor: 'pointer',
            color: DB.text,
            display: 'flex',
          }}
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path
              d="M15 18l-6-6 6-6"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
        <span
          style={{
            fontSize: 16,
            fontWeight: 700,
            color: DB.ink,
            letterSpacing: '-0.005em',
          }}
        >
          Hồ sơ của tôi
        </span>
        <div style={{ width: 22 }} />
      </header>
      <main
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: 16,
          background: DB.bg,
        }}
      >
        {children}
      </main>
      <nav
        style={{
          background: '#fff',
          borderTop: `1px solid ${DB.border}`,
          padding: '8px 12px',
          display: 'flex',
          justifyContent: 'space-around',
          flexShrink: 0,
        }}
      >
        {[
          { icon: <NavIcon kind="home" />, label: 'Tổng quan' },
          { icon: <NavIcon kind="pencil" />, label: 'Cập nhật' },
          { icon: <NavIcon kind="beaker" />, label: 'Thực đơn' },
          { icon: <NavIcon kind="bell" />, label: 'Thông báo' },
        ].map((it, i) => (
          <div
            key={i}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 3,
              padding: '6px 10px',
              color: DB.textMute,
              fontWeight: 500,
              fontSize: 10.5,
            }}
          >
            {it.icon}
            {it.label}
          </div>
        ))}
      </nav>
    </div>
  );
}

function NavIcon({ kind, size = 20 }) {
  const paths = {
    home: 'M3 10l9-7 9 7v10a2 2 0 01-2 2h-4v-7H9v7H5a2 2 0 01-2-2V10z',
    pencil: 'M11 4H4v16h16v-7M18.5 2.5a2.121 2.121 0 113 3L12 15l-4 1 1-4 9.5-9.5z',
    beaker: 'M9 3h6M10 3v6L5 19a2 2 0 002 2h10a2 2 0 002-2L14 9V3',
    bell: 'M6 8a6 6 0 0112 0c0 7 3 9 3 9H3s3-2 3-9M10 21a2 2 0 004 0',
  };
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d={paths[kind]} />
    </svg>
  );
}

// =========================================================================
// Page header with breadcrumb + title (desktop)
// =========================================================================
function PageHeader() {
  return (
    <div
      style={{
        maxWidth: 880,
        margin: '0 auto 24px',
        display: 'flex',
        flexDirection: 'column',
        gap: 6,
      }}
    >
      <div
        style={{
          fontSize: 12,
          color: DB.textMute,
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          fontWeight: 500,
        }}
      >
        <span>Trang chủ</span>
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
          <path
            d="M9 18l6-6-6-6"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
        <span style={{ color: DB.text }}>Hồ sơ của tôi</span>
      </div>
      <h1
        style={{
          margin: 0,
          fontSize: 26,
          fontWeight: 700,
          color: DB.ink,
          letterSpacing: '-0.02em',
        }}
      >
        Hồ sơ của tôi
      </h1>
      <p style={{ margin: 0, fontSize: 14, color: DB.textMid }}>
        Quản lý thông tin cá nhân, mục tiêu và cài đặt sức khỏe của bạn
      </p>
    </div>
  );
}

// =========================================================================
// FULL PROFILE — desktop, view mode (default)
// =========================================================================
function ProfileDesktopView() {
  return (
    <ProfilePageShell>
      <ProfileMain>
        <S1ProfileHeader />
        <S2PersonalInfo />
        <S3Goal />
        <S4HealthSettings pbfMethod="FORMULA" />
        <S5Security />
        <S6DangerZone />
      </ProfileMain>
    </ProfilePageShell>
  );
}

// =========================================================================
// FULL PROFILE — desktop, edit mode (Section 2 active)
// =========================================================================
function ProfileDesktopEdit({ saving = false }) {
  return (
    <ProfilePageShell>
      <ProfileMain>
        <S1ProfileHeader />
        <S2PersonalInfo editing saving={saving} />
        <S3Goal />
        <S4HealthSettings pbfMethod="FORMULA" />
        <S5Security />
        <S6DangerZone />
      </ProfileMain>
    </ProfilePageShell>
  );
}

// =========================================================================
// FULL PROFILE — desktop, with goal-history expanded (bonus state)
// =========================================================================
function ProfileDesktopHistoryOpen() {
  return (
    <ProfilePageShell>
      <ProfileMain>
        <S1ProfileHeader />
        <S2PersonalInfo />
        <S3Goal historyOpen />
        <S4HealthSettings pbfMethod="FORMULA" />
        <S5Security />
        <S6DangerZone />
      </ProfileMain>
    </ProfilePageShell>
  );
}

// =========================================================================
// FULL PROFILE — mobile stack
// =========================================================================
function ProfileMobile() {
  return (
    <ProfilePageShellMobile>
      <ProfileMain mobile>
        <S1ProfileHeader mobile />
        <S2PersonalInfo mobile />
        <S3Goal mobile />
        <S4HealthSettings pbfMethod="FORMULA" mobile />
        <S5Security mobile />
        <S6DangerZone mobile />
      </ProfileMain>
    </ProfilePageShellMobile>
  );
}

// =========================================================================
// FULL PROFILE — loading skeleton
// =========================================================================
function ProfileLoadingDesktop() {
  return (
    <ProfilePageShell>
      <ProfileSkeleton />
    </ProfilePageShell>
  );
}

// =========================================================================
// Modal-over-page composites (artboards for modals show real context)
// =========================================================================
function ProfileWithGoalModal() {
  return (
    <ProfilePageShell>
      <ProfileMain>
        <S1ProfileHeader />
        <S2PersonalInfo />
        <S3Goal />
        <S4HealthSettings pbfMethod="FORMULA" />
      </ProfileMain>
      <GoalChangeModal selected="TANG" />
    </ProfilePageShell>
  );
}

function ProfileWithDeleteModal() {
  return (
    <ProfilePageShell>
      <ProfileMain>
        <S1ProfileHeader />
        <S2PersonalInfo />
        <S3Goal />
        <S4HealthSettings pbfMethod="FORMULA" />
        <S6DangerZone />
      </ProfileMain>
      <DeleteAccountModal />
    </ProfilePageShell>
  );
}

function ProfileWithToast() {
  return (
    <ProfilePageShell>
      <ProfileMain>
        <S1ProfileHeader />
        <S2PersonalInfo />
        <S3Goal />
        <S4HealthSettings pbfMethod="FORMULA" />
      </ProfileMain>
      {/* Floating toast top-right */}
      <div
        style={{
          position: 'absolute',
          top: 88,
          right: 32,
          zIndex: 60,
        }}
      >
        <Toast message="Đã cập nhật thông tin" />
      </div>
    </ProfilePageShell>
  );
}

Object.assign(window, {
  ProfilePageShell,
  ProfilePageShellMobile,
  PageHeader,
  ProfileDesktopView,
  ProfileDesktopEdit,
  ProfileDesktopHistoryOpen,
  ProfileMobile,
  ProfileLoadingDesktop,
  ProfileWithGoalModal,
  ProfileWithDeleteModal,
  ProfileWithToast,
});
