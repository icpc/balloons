import { useMemo } from 'react';
import { Balloon, Contest, Problem, Team } from '../types';
import ProblemBox from './ProblemBox';
import { useContestMaps } from '../hooks/useContestMaps';

const BalloonRow = ({ balloon, problem, team, actions }: {
  balloon: Balloon
  problem: Problem
  team: Team
  actions: (balloon: Balloon) => React.ReactNode
}) => {
  const actionContent = actions(balloon);

  const content = useMemo(() => (
    <div className="balloon-row">
      <ProblemBox problem={problem} />
      {balloon.isFTS ? <span className="fts">★</span> : <span></span>}
      <span className="team-place">{team.displayName}</span>
      <div className="actions">{actionContent}</div>
      <span className="team-name">{team.fullName}</span>
    </div>
  ), [balloon, problem, team, actionContent]);

  return content;
};

const BalloonList = ({ title, balloons, contest, actions }: {
  title: string
  balloons: Balloon[]
  contest: Contest
  actions: (balloon: Balloon) => React.ReactNode
}) => {
  const { problemsMap, teamMap } = useContestMaps(contest);

  if (balloons.length === 0) {
    return null;
  }

  return (
    <>
      <h2>
        {title}
        {' '}
        (
        {balloons.length}
        )
      </h2>
      <div className="balloon-list">
        {balloons.map((balloon) => {
          return (
            <BalloonRow
              key={balloon.runId}
              balloon={balloon}
              problem={problemsMap[balloon.problemId]}
              team={teamMap[balloon.teamId]}
              actions={actions}
            />
          );
        })}
      </div>
    </>
  );
};

export default BalloonList;
