import { useState, useEffect, useCallback } from 'react';
import { Navigate } from 'react-router-dom';
import { InfoHolder, Volunteer } from '../types';
import backendUrls from '../util/backendUrls';
import { GlobalError } from '../components/GlobalError';
import { useTranslation } from 'react-i18next';

const VolunteerAccessView = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const { t } = useTranslation();
  const [volunteers, setVolunteers] = useState<Volunteer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUpdating, setIsUpdating] = useState<string | null>(null);

  const fetchVolunteers = useCallback(async () => {
    try {
      const response = await fetch(backendUrls.getVolunteers(), {
        headers: {
          Authorization: `Bearer ${infoHolder.token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch volunteers');
      }

      const data = await response.json() as Volunteer[];
      setVolunteers(data);
      setError(null);
    } catch (err) {
      console.error(err);
      setError(t('volunteers.loadError'));
    } finally {
      setIsLoading(false);
    }
  }, [infoHolder.token, t]);

  useEffect(() => {
    void fetchVolunteers();
  }, [fetchVolunteers]);

  const handleRoleChange = async (id: number, role: 'canAccess' | 'canManage', newValue: boolean) => {
    if (isUpdating) return;

    setIsUpdating(String(id));
    try {
      const response = await fetch(backendUrls.patchVolunteer(id), {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${infoHolder.token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ [role]: newValue }),
      });

      if (!response.ok) {
        throw new Error('Failed to update volunteer');
      }

      setVolunteers(volunteers.map(v =>
        v.id === id ? { ...v, [role]: newValue } : v,
      ));
    } catch (err) {
      console.error(err);
      setError(t('volunteers.updateError'));
    } finally {
      setIsUpdating(null);
    }
  };

  if (isLoading) {
    return (
      <div className="container mt-5">
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">{t('volunteers.loading')}</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <main>
      <h1 className="sr-only">{t('volunteers.title')}</h1>
      {error && (
        <div className="form-error" role="alert">
          {error}
          <a onClick={() => setError(null)}>
            {' '}
            {t('common.close')}
          </a>
        </div>
      )}
      <table className="volunteer-access">
        <thead>
          <tr>
            <th rowSpan={2} style={{ verticalAlign: 'bottom' }}>{t('volunteers.table.login')}</th>
            <th colSpan={2} style={{ textAlign: 'center' }}>{t('volunteers.table.access')}</th>
          </tr>
          <tr>
            <th>{t('volunteers.table.balloons')}</th>
            <th>{t('volunteers.table.volunteers')}</th>
          </tr>
        </thead>
        <tbody>
          {volunteers.length === 0
            ? (
                <tr>
                  <td colSpan={3} className="text-center">
                    {error ? t('volunteers.loadError') : t('volunteers.noVolunteers')}
                  </td>
                </tr>
              )
            : (
                volunteers.map((volunteer) => {
                  const isSelf = volunteer.login === infoHolder.info?.login;
                  const isUpdatingThis = isUpdating === String(volunteer.id);

                  return (
                    <tr key={volunteer.id}>
                      <td>{volunteer.login}</td>
                      <td>
                        <span>
                          {volunteer.canAccess ? '✔️ ' : '❌ '}
                        </span>
                        {!isSelf && (!volunteer.canAccess || !volunteer.canManage) && (
                          <a
                            onClick={() => void handleRoleChange(volunteer.id, 'canAccess', !volunteer.canAccess)}
                            className={isUpdatingThis ? 'disabled access-link' : 'access-link'}
                          >
                            {isUpdatingThis ? '...' : (volunteer.canAccess ? t('volunteers.table.revoke') : t('volunteers.table.grant'))}
                          </a>
                        )}
                      </td>
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <span>
                            {volunteer.canManage ? '✔️ ' : '❌ '}
                          </span>
                          {isSelf && t('volunteers.table.thatsYou')}
                          {!isSelf && (
                            <a
                              onClick={() => void handleRoleChange(volunteer.id, 'canManage', !volunteer.canManage)}
                              className={isUpdatingThis ? 'disabled access-link' : 'access-link'}
                            >
                              {isUpdatingThis ? '...' : (volunteer.canManage ? t('volunteers.table.revoke') : t('volunteers.table.grant'))}
                            </a>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
        </tbody>
      </table>
    </main>
  );
};

const VolunteerAccess = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const { t } = useTranslation();

  if (!infoHolder.info.login) {
    return <Navigate to="/login" />;
  }

  if (!infoHolder.info.canManage) {
    return (
      <GlobalError
        title={t('errors.noAccess')}
        message={t('errors.notAdmin')}
      />
    );
  }

  return <VolunteerAccessView infoHolder={infoHolder} />;
};

export default VolunteerAccess;
