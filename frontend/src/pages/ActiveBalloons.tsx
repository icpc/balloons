import { Navigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import { GlobalError } from '../components/GlobalError';
import { useCallback, useMemo } from 'react';
import ProblemList from '../components/ProblemList';
import BalloonList from '../components/BalloonList';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { useWebSocket } from '../contexts/WebSocketContext';
import { useFilteredBalloons } from '../hooks/useFilteredBalloons';
import { useTranslation } from 'react-i18next';
import { useContestMaps } from '../hooks/useContestMaps';
import { Balloon, Command, Team } from '../types';

function teamId(balloon: Balloon, teamMap: Record<string, Team>): string {
  return teamMap[balloon.teamId]?.displayName ?? balloon.teamId;
}

const BalloonsView = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const { t } = useTranslation();
  const ws = useWebSocket();
  const contest = useSelector((state: RootState) => state.contest);
  const filteredBalloons = useFilteredBalloons();
  const { teamMap } = useContestMaps(contest);

  const myBalloons = useMemo(() => {
    return filteredBalloons
      .filter(balloon => balloon.takenBy === infoHolder.info.login && !balloon.delivered)
      .sort((a, b) => teamId(a, teamMap).localeCompare(teamId(b, teamMap)));
  }, [filteredBalloons, infoHolder.info.login, teamMap]);

  const queuedBalloons = useMemo(() => {
    return filteredBalloons.filter(balloon => balloon.takenBy === null && !balloon.delivered);
  }, [filteredBalloons]);

  const takenBalloons = useMemo(() => {
    return filteredBalloons.filter(balloon => balloon.takenBy !== null && !balloon.delivered);
  }, [filteredBalloons]);

  const send = useCallback((type: Command['type'], runId: string) => {
    ws?.send(JSON.stringify({ type, runId }));
  }, [ws]);

  const myActions = useCallback((balloon: Balloon) => (
    <>
      <a onClick={() => send('deliverBalloon', balloon.runId)}>{t('balloons.actions.done')}</a>
      <a onClick={() => send('dropBalloon', balloon.runId)}>{t('balloons.actions.drop')}</a>
    </>
  ), [send, t]);

  const queuedActions = useCallback((balloon: Balloon) => (
    <a onClick={() => send('takeBalloon', balloon.runId)}>{t('balloons.actions.take')}</a>
  ), [send, t]);

  const takenActions = useCallback((balloon: Balloon) => (
    <span>
      {t('balloons.carriedBy')}
      {balloon.takenBy}
    </span>
  ), [t]);

  return (
    <main className="balloons-main">
      <h2 className="sr-only">{t('balloons.title')}</h2>
      <div className="contest-name"><strong>{contest.name}</strong></div>
      <ProblemList contest={contest} balloons={filteredBalloons} />
      <BalloonList
        title={t('balloons.youCarry')}
        balloons={myBalloons}
        contest={contest}
        actions={myActions}
      />
      <BalloonList
        title={t('balloons.available')}
        balloons={queuedBalloons}
        contest={contest}
        actions={queuedActions}
      />
      <BalloonList
        title={t('balloons.inProgress')}
        balloons={takenBalloons}
        contest={contest}
        actions={takenActions}
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
