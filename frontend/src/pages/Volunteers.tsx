import { useState, useEffect, useCallback, useMemo, useRef, ReactNode } from 'react';
import { useSelector } from 'react-redux';
import { Navigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { RootState } from '../store/store';
import { InfoHolder, Role, Volunteer } from '../types';
import backendUrls from '../util/backendUrls';
import { GlobalError } from '../components/GlobalError';

// Also the order of sections when sorting by role
const ROLES: Role[] = ['MANAGER', 'VOLUNTEER', 'PENDING'];

type SortKey = 'role' | 'login' | 'balloons';

interface Row {
  login: string
  count: number
  role: Role
}

const byLogin = (a: Row, b: Row) => a.login.localeCompare(b.login);

const sorters: Record<SortKey, (a: Row, b: Row) => number> = {
  role: (a, b) => ROLES.indexOf(a.role) - ROLES.indexOf(b.role) || byLogin(a, b),
  login: byLogin,
  balloons: (a, b) => b.count - a.count || byLogin(a, b),
};

const useDeliveredCounts = () => {
  const balloons = useSelector((state: RootState) => state.balloons.items);

  return useMemo(() => {
    const counts = new Map<string, number>();
    balloons.forEach((balloon) => {
      if (balloon.delivered && balloon.takenBy) {
        counts.set(balloon.takenBy, (counts.get(balloon.takenBy) ?? 0) + 1);
      }
    });
    return counts;
  }, [balloons]);
};

const RatingView = () => {
  const { t } = useTranslation();
  const counts = useDeliveredCounts();

  if (counts.size === 0) {
    return <p>{t('volunteers.noDeliveredBalloons')}</p>;
  }

  const rows = Array.from(counts, ([login, count]): Row => ({ login, count, role: 'VOLUNTEER' })).sort(sorters.balloons);

  return (
    <table>
      <thead>
        <tr>
          <th>{t('volunteers.login')}</th>
          <th className="delivered">{t('volunteers.delivered')}</th>
        </tr>
      </thead>
      <tbody>
        {rows.map(row => (
          <tr key={row.login}>
            <td>{row.login}</td>
            <td className="delivered">{row.count}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

const Modal = ({ title, onClose, children }: { title: string, onClose: () => void, children: ReactNode }) => {
  const { t } = useTranslation();
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    if (!ref.current?.open) ref.current?.showModal();
  }, []);

  return (
    <dialog ref={ref} onClose={onClose} onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="dialog-body">
        <h2>{title}</h2>
        {children}
        {/* Last in DOM so that the first field gets the initial focus */}
        <button type="button" className="dialog-close" aria-label={t('common.close')} onClick={onClose}>×</button>
      </div>
    </dialog>
  );
};

const RoleSelect = ({ defaultValue }: { defaultValue: Role }) => {
  const { t } = useTranslation();

  return (
    <select id="role" name="role" defaultValue={defaultValue}>
      {ROLES.map(role => <option key={role} value={role}>{t(`volunteers.roles.${role}`)}</option>)}
    </select>
  );
};

const ManageView = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const { t } = useTranslation();
  const counts = useDeliveredCounts();
  const [volunteers, setVolunteers] = useState<Volunteer[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [sort, setSort] = useState<SortKey>('role');
  // Volunteer being edited, 'new' for the "Add volunteer" popup
  const [modal, setModal] = useState<Volunteer | 'new' | null>(null);

  const fetchVolunteers = useCallback(async () => {
    try {
      const response = await fetch(backendUrls.getVolunteers(), {
        headers: { Authorization: `Bearer ${infoHolder.token}` },
      });
      if (!response.ok) {
        throw new Error('Failed to fetch volunteers');
      }
      setVolunteers(await response.json() as Volunteer[]);
    } catch (err) {
      console.error(err);
      setError(t('volunteers.loadError'));
    }
  }, [infoHolder.token, t]);

  useEffect(() => {
    void fetchVolunteers();
  }, [fetchVolunteers]);

  const save = async (method: 'POST' | 'PATCH', url: string, body: object) => {
    if (isSaving) return;

    setIsSaving(true);
    setError(null);
    try {
      const response = await fetch(url, {
        method,
        headers: {
          'Authorization': `Bearer ${infoHolder.token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
      });

      if (response.status === 409) {
        setError(t('auth.errors.userExists'));
        return;
      }
      if (!response.ok) {
        throw new Error('Failed to save volunteer');
      }

      setModal(null);
      await fetchVolunteers();
    } catch (err) {
      console.error(err);
      setError(t('volunteers.updateError'));
    } finally {
      setIsSaving(false);
    }
  };

  const openModal = (value: Volunteer | 'new' | null) => {
    setError(null);
    setModal(value);
  };

  // Submits named form fields as JSON
  const submitTo = (method: 'POST' | 'PATCH', url: string) => (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    void save(method, url, Object.fromEntries(new FormData(e.currentTarget)));
  };

  const errorBox = error ? <div className="form-error" role="alert">{error}</div> : null;
  const saveButton = <button type="submit" disabled={isSaving}>{isSaving ? t('common.loading') : t('common.save')}</button>;
  const passwordInput = <input type="password" id="password" name="password" required minLength={6} autoComplete="new-password" />;

  if (volunteers === null) {
    return errorBox ?? <p>{t('common.loading')}</p>;
  }

  const rows = volunteers
    .map(volunteer => ({ ...volunteer, count: counts.get(volunteer.login) ?? 0 }))
    .sort(sorters[sort]);

  return (
    <>
      {!modal && errorBox}
      <p className="sort">
        {t('volunteers.sortBy')}
        {(Object.keys(sorters) as SortKey[]).map(key => (
          <a key={key} className={sort === key ? 'active' : undefined} onClick={() => setSort(key)}>
            {t(`volunteers.sort.${key}`)}
          </a>
        ))}
      </p>
      <table className="volunteers">
        <thead>
          <tr>
            <th>{t('volunteers.login')}</th>
            <th className="delivered">{t('volunteers.delivered')}</th>
            <th>{t('volunteers.role')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.map(row => (
            <tr key={row.id}>
              <td>{row.login}</td>
              <td className="delivered">{row.count}</td>
              <td>{t(`volunteers.roles.${row.role}`)}</td>
              <td>
                {row.role === 'PENDING'
                  ? (
                      <a onClick={() => void save('PATCH', backendUrls.patchVolunteer(row.id), { role: 'VOLUNTEER' })}>
                        {t('volunteers.approve')}
                      </a>
                    )
                  : <a onClick={() => openModal(row)}>{t('volunteers.edit')}</a>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        <button onClick={() => openModal('new')}>{t('volunteers.add')}</button>
      </p>

      {modal === 'new' && (
        <Modal title={t('volunteers.add')} onClose={() => openModal(null)}>
          <form onSubmit={submitTo('POST', backendUrls.getVolunteers())}>
            <label htmlFor="login">{t('auth.username')}</label>
            <input type="text" id="login" name="login" required autoComplete="off" autoCapitalize="none" />
            <label htmlFor="password">{t('auth.password')}</label>
            {passwordInput}
            <label htmlFor="role">{t('volunteers.role')}</label>
            <RoleSelect defaultValue="VOLUNTEER" />
            {errorBox}
            {saveButton}
          </form>
        </Modal>
      )}

      {modal && modal !== 'new' && (
        <Modal title={modal.login} onClose={() => openModal(null)}>
          {modal.login !== infoHolder.info.login && (
            <form onSubmit={submitTo('PATCH', backendUrls.patchVolunteer(modal.id))}>
              <label htmlFor="role">{t('volunteers.changeRole')}</label>
              <RoleSelect defaultValue={modal.role} />
              {saveButton}
            </form>
          )}
          <form onSubmit={submitTo('PATCH', backendUrls.patchVolunteer(modal.id))}>
            <label htmlFor="password">{t('volunteers.changePassword')}</label>
            {passwordInput}
            {errorBox}
            {saveButton}
          </form>
        </Modal>
      )}
    </>
  );
};

const Volunteers = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const { t } = useTranslation();

  if (!infoHolder.info.login) {
    return <Navigate to="/login" />;
  }

  if (!infoHolder.info.canAccess) {
    return (
      <GlobalError
        title={t('errors.noAccess')}
        message={t('errors.contactAdmin')}
      />
    );
  }

  return (
    <main>
      <h1 className="sr-only">{t('navigation.volunteers')}</h1>
      {infoHolder.info.canManage ? <ManageView infoHolder={infoHolder} /> : <RatingView />}
    </main>
  );
};

export default Volunteers;
