import { useState, useEffect, useCallback } from 'react';
import { Navigate } from 'react-router-dom';
import { InfoHolder, Volunteer } from '../types';
import backendUrls from '../util/backendUrls';
import { GlobalError } from '../components/GlobalError';

const VolunteerAccessView = ({ infoHolder }: { infoHolder: InfoHolder }) => {
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
    }
    catch (err) {
      console.error(err);
      setError('Не удалось загрузить список волонтеров');
    }
    finally {
      setIsLoading(false);
    }
  }, [infoHolder.token]);

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
    }
    catch (err) {
      console.error(err);
      setError('Не удалось обновить права доступа');
    }
    finally {
      setIsUpdating(null);
    }
  };

  if (isLoading) {
    return (
      <div className="container mt-5">
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Загрузка...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <main>
      <h1 className="sr-only">Управление доступом волонтёров</h1>
      {error && (
        <div className="form-error" role="alert">
          {error}
          <a onClick={() => setError(null)}> Закрыть</a>
        </div>
      )}
      <table className="volunteer-access">
        <thead>
          <tr>
            <th rowSpan={2} style={{ verticalAlign: 'bottom' }}>Логин</th>
            <th colSpan={2} style={{ textAlign: 'center' }}>Доступы</th>
          </tr>
          <tr>
            <th>Шарики</th>
            <th>Волонтеры</th>
          </tr>
        </thead>
        <tbody>
          {volunteers.length === 0
            ? (
                <tr>
                  <td colSpan={3} className="text-center">
                    {error ? 'Не удалось загрузить данные' : 'Нет волонтеров'}
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
                            {isUpdatingThis ? '...' : (volunteer.canAccess ? 'Отозвать' : 'Выдать')}
                          </a>
                        )}
                      </td>
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <span>
                            {volunteer.canManage ? '✔️ ' : '❌ '}
                          </span>
                          {isSelf && 'Это вы'}
                          {!isSelf && (
                            <a
                              onClick={() => void handleRoleChange(volunteer.id, 'canManage', !volunteer.canManage)}
                              className={isUpdatingThis ? 'disabled access-link' : 'access-link'}
                            >
                              {isUpdatingThis ? '...' : (volunteer.canManage ? 'Отозвать' : 'Выдать')}
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
  if (!infoHolder.info.login) {
    return <Navigate to="/login" />;
  }

  if (!infoHolder.info.canManage) {
    return <GlobalError title="Нет доступа" message="Вы не администратор." />;
  }

  return <VolunteerAccessView infoHolder={infoHolder} />;
};

export default VolunteerAccess;
