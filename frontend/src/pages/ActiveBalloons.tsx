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

const BalloonsView = ({ infoHolder }: { infoHolder: InfoHolder }) => {
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
      <h2 className="sr-only">Шарики</h2>
      <div className="contest-name"><strong>{contest.name}</strong></div>
      <ProblemList contest={contest} balloons={filteredBalloons} />
      <BalloonList title="Вы несёте" balloons={myBalloons} contest={contest}
        actions={(balloon) => <>
          <a onClick={() => ws?.send(JSON.stringify({ type: "deliverBalloon", runId: balloon.runId }))}>Готово</a>
          <a onClick={() => ws?.send(JSON.stringify({ type: "dropBalloon", runId: balloon.runId }))}>Отказаться</a>
        </>} />
      <BalloonList title="Можно нести" balloons={queuedBalloons} contest={contest}
        actions={(balloon) => <a onClick={() => ws?.send(JSON.stringify({ type: "takeBalloon", runId: balloon.runId }))}>Взять</a>} />
      <BalloonList title="В пути" balloons={takenBalloons} contest={contest}
        actions={(balloon) => <span>Несёт {balloon.takenBy}</span>} />
    </main>
  );
};

const ActiveBalloons = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  if (!infoHolder.info.login) {
    return <Navigate to="/login" />;
  }

  if (!infoHolder.info.canAccess) {
    return <GlobalError title="Нет доступа" message="Сообщите организатору ваш логин, чтобы его получить." />;
  }

  return <BalloonsView infoHolder={infoHolder} />
};

export default ActiveBalloons;
