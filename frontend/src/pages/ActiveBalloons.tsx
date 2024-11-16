import { Navigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import { GlobalError } from '../components/GlobalError';
import { useMemo } from 'react';
import ProblemList from '../components/ProblemList';
import BalloonList from '../components/BalloonList';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { useWebSocket } from '../contexts/WebSocketContext';
import { useFilteredBalloons } from '../hooks/useFilteredBalloons';
import { useTranslation } from 'react-i18next';

const BalloonsView = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const { t } = useTranslation();
  const ws = useWebSocket();
  const contest = useSelector((state: RootState) => state.contest);
  const filteredBalloons = useFilteredBalloons();

  const myBalloons = useMemo(() => {
    return filteredBalloons.filter(balloon => balloon.takenBy === infoHolder.info.login && !balloon.delivered);
  }, [filteredBalloons, infoHolder.info.login]);

  const queuedBalloons = useMemo(() => {
    return filteredBalloons.filter(balloon => balloon.takenBy === null && !balloon.delivered);
  }, [filteredBalloons]);

  const takenBalloons = useMemo(() => {
    return filteredBalloons.filter(balloon => balloon.takenBy !== null && !balloon.delivered);
  }, [filteredBalloons]);

  return (
    <main className="balloons-main">
      <h2 className="sr-only">{t('balloons.title')}</h2>
      <div className="contest-name"><strong>{contest.name}</strong></div>
      <ProblemList contest={contest} balloons={filteredBalloons} />
      <BalloonList
        title={t('balloons.youCarry')}
        balloons={myBalloons}
        contest={contest}
        actions={balloon => (
          <>
            <a onClick={() => ws?.send(JSON.stringify({ type: 'deliverBalloon', runId: balloon.runId }))}>
              {t('balloons.actions.done')}
            </a>
            <a onClick={() => ws?.send(JSON.stringify({ type: 'dropBalloon', runId: balloon.runId }))}>
              {t('balloons.actions.drop')}
            </a>
          </>
        )}
      />
      <BalloonList
        title={t('balloons.available')}
        balloons={queuedBalloons}
        contest={contest}
        actions={balloon => (
          <a onClick={() => ws?.send(JSON.stringify({ type: 'takeBalloon', runId: balloon.runId }))}>
            {t('balloons.actions.take')}
          </a>
        )}
      />
      <BalloonList
        title={t('balloons.inProgress')}
        balloons={takenBalloons}
        contest={contest}
        actions={balloon => (
          <span>
            {t('balloons.carriedBy')}
            {balloon.takenBy}
          </span>
        )}
      />
    </main>
  );
};

const ActiveBalloons = ({ infoHolder }: { infoHolder: InfoHolder }) => {
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

  return <BalloonsView infoHolder={infoHolder} />;
};

export default ActiveBalloons;
