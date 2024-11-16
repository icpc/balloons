import { Navigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import { GlobalError } from '../components/GlobalError';
import { useMemo } from 'react';
import ProblemList from '../components/ProblemList';
import BalloonList from '../components/BalloonList';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { useFilteredBalloons } from '../hooks/useFilteredBalloons';
import { useTranslation } from 'react-i18next';

const BalloonsView = () => {
  const { t } = useTranslation();
  const contest = useSelector((state: RootState) => state.contest);
  const filteredBalloons = useFilteredBalloons();

  const deliveredBalloons = useMemo(() => {
    return filteredBalloons.filter(balloon => balloon.delivered);
  }, [filteredBalloons]);

  return (
    <main className="balloons-main">
      <h2 className="sr-only">{t('balloons.deliveredTitle')}</h2>
      <div className="contest-name"><strong>{contest.name}</strong></div>
      <ProblemList contest={contest} balloons={filteredBalloons} />
      <BalloonList
        title={t('balloons.delivered')}
        balloons={deliveredBalloons}
        contest={contest}
        actions={balloon => (
          <span>
            {t('balloons.deliveredBy')}
            {balloon.takenBy ?? ''}
          </span>
        )}
      />
    </main>
  );
};

const DeliveredBalloons = ({ infoHolder }: { infoHolder: InfoHolder }) => {
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

  return <BalloonsView />;
};

export default DeliveredBalloons;
