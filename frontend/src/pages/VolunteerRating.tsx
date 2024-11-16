import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { Navigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import { GlobalError } from '../components/GlobalError';
import { useTranslation } from 'react-i18next';

interface VolunteerStats {
  login: string
  count: number
}

const RatingView = () => {
  const { t } = useTranslation();
  const balloons = useSelector((state: RootState) => state.balloons.items);

  const volunteerStats = useMemo(() => {
    const deliveredBalloons = balloons.filter(b => b.delivered && b.takenBy);

    const stats = new Map<string, number>();
    deliveredBalloons.forEach((balloon) => {
      if (balloon.takenBy) {
        stats.set(balloon.takenBy, (stats.get(balloon.takenBy) ?? 0) + 1);
      }
    });

    return Array.from(stats.entries())
      .map(([login, count]): VolunteerStats => ({ login, count }))
      .sort((a, b) => b.count - a.count);
  }, [balloons]);

  if (volunteerStats.length === 0) {
    return <p>{t('volunteers.rating.noDeliveredBalloons')}</p>;
  }

  return (
    <main>
      <h1 className="sr-only">{t('volunteers.rating.title')}</h1>
      <table>
        <thead>
          <tr>
            <th>{t('volunteers.rating.volunteer')}</th>
            <th>{t('volunteers.rating.delivered')}</th>
          </tr>
        </thead>
        <tbody>
          {volunteerStats.map(stat => (
            <tr key={stat.login}>
              <td>{stat.login}</td>
              <td>{stat.count}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
};

const VolunteerRating = ({ infoHolder }: { infoHolder: InfoHolder }) => {
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

  return <RatingView />;
};

export default VolunteerRating;
