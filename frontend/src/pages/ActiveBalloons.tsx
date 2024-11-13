import { Navigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import { GlobalError } from '../components/GlobalError';
import { useMemo } from 'react';
import ProblemList from '../components/ProblemList';
import BalloonList from '../components/BalloonList';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { useWebSocket } from '../contexts/WebSocketContext';

const BalloonsView = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const ws = useWebSocket();
  const problems = useSelector((state: RootState) => state.problems.items);
  const balloons = useSelector((state: RootState) => state.balloons.items);

  const myBalloons = useMemo(() => {
    return balloons.filter(balloon => balloon.takenBy === infoHolder.info.login && !balloon.delivered);
  }, [balloons, infoHolder.info.login]);
  const queuedBalloons = useMemo(() => {
    return balloons.filter(balloon => balloon.takenBy === null && !balloon.delivered);
  }, [balloons]);
  const takenBalloons = useMemo(() => {
    return balloons.filter(balloon => balloon.takenBy !== null && !balloon.delivered);
  }, [balloons]);

  return (
    <main className="balloons-main">
      <h2 className="sr-only">Шарики</h2>
      <div className="contest-name"><strong>{infoHolder.info.contestName}</strong></div>
      <ProblemList problems={problems} balloons={balloons} />
      <BalloonList title="Вы несёте" balloons={myBalloons} problems={problems}
        actions={(balloon) => <>
          <a onClick={() => ws?.send(JSON.stringify({ type: "deliverBalloon", runId: balloon.runId }))}>Готово</a>
          <a onClick={() => ws?.send(JSON.stringify({ type: "dropBalloon", runId: balloon.runId }))}>Отказаться</a>
        </>} />
      <BalloonList title="Можно нести" balloons={queuedBalloons} problems={problems}
        actions={(balloon) => <a onClick={() => ws?.send(JSON.stringify({ type: "takeBalloon", runId: balloon.runId }))}>Взять</a>} />
      <BalloonList title="В пути" balloons={takenBalloons} problems={problems}
        actions={(balloon) => <span>Несёт {balloon.takenBy}</span>} />
    </main>
  );
};

const ActiveBalloons = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  if (!infoHolder.info.login) {
    return <Navigate to="/login" />;
  }

  if (!infoHolder.info.canAccess) {
    return <GlobalError title="Forbidden" message="Ask organizer to give you access." />;
  }

  return <BalloonsView infoHolder={infoHolder} />
};

export default ActiveBalloons;
