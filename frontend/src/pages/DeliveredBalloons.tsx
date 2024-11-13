import { Navigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import { GlobalError } from '../components/GlobalError';
import { useMemo } from 'react';
import ProblemList from '../components/ProblemList';
import BalloonList from '../components/BalloonList';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';

const BalloonsView = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const problems = useSelector((state: RootState) => state.problems.items);
  const balloons = useSelector((state: RootState) => state.balloons.items);

  const deliveredBalloons = useMemo(() => {
    return balloons.filter(balloon => balloon.delivered);
  }, [balloons]);

  return (
    <main className="balloons-main">
      <h2 className="sr-only">Доставленные шарики</h2>
      <div className="contest-name"><strong>{infoHolder.info.contestName}</strong></div>
      <ProblemList problems={problems} balloons={balloons} />
      <BalloonList title="Доставлены" balloons={deliveredBalloons} problems={problems}
        actions={(balloon) => <span>Доставлен {balloon.takenBy ?? ''}</span>} />
    </main>
  );
};

const DeliveredBalloons = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  if (!infoHolder.info.login) {
    return <Navigate to="/login" />;
  }

  if (!infoHolder.info.canAccess) {
    return <GlobalError title="Forbidden" message="Ask organizer to give you access." />;
  }

  return <BalloonsView infoHolder={infoHolder} />
};

export default DeliveredBalloons;
