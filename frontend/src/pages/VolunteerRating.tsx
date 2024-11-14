import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { Navigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import { GlobalError } from '../components/GlobalError';

interface VolunteerStats {
  login: string
  count: number
}

const RatingView = () => {
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
    return <p>Нет доставленных шариков</p>;
  }

  return (
    <main>
      <h1 className="sr-only">Рейтинг волонтёров</h1>
      <table>
        <thead>
          <tr>
            <th>Волонтёр</th>
            <th>Доставлено</th>
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
  if (!infoHolder.info.login) {
    return <Navigate to="/login" />;
  }

  if (!infoHolder.info.canAccess) {
    return <GlobalError title="Нет доступа" message="Сообщите организатору ваш логин, чтобы его получить." />;
  }

  return <RatingView />;
};

export default VolunteerRating;
