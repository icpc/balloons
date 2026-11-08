import { useTranslation } from 'react-i18next';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { Balloon, Contest, Problem } from '../types';
import ProblemBox from './ProblemBox';

function pendingClass(pending: number, threshold: number): string | undefined {
  if (pending === 0) return 'pending-zero';
  if (pending > 4 * threshold) return 'pending-high';
  if (pending >= 2 * threshold) return 'pending-mid';
  if (pending >= threshold) return 'pending-low';
  return undefined;
}

const ProblemBlock = ({ problem, pending, solves, threshold }: { problem: Problem, pending: number, solves: number, threshold: number }) => (
  <div title={problem.name} className={solves === 0 ? 'unsolved' : undefined}>
    <ProblemBox problem={problem} />
    <div className="problem-stats">
      <div className={pendingClass(pending, threshold)}>{pending}</div>
      <div>{solves}</div>
    </div>
  </div>
);

const ProblemList = ({ contest, balloons }: { contest: Contest, balloons: Balloon[] }) => {
  const stats = contest.problems.map((problem) => {
    const problemBalloons = balloons.filter(b => b.problemId === problem.id);
    return {
      problem,
      pending: problemBalloons.filter(b => !b.takenBy && !b.delivered).length,
      solves: problemBalloons.length,
    };
  });
  // filter preserves contest order within each group
  const ordered = [...stats.filter(s => s.solves > 0), ...stats.filter(s => s.solves === 0)];
  const { t } = useTranslation();
  const selectedHall = useSelector((state: RootState) => state.hall.selectedHall);
  const teams = selectedHall ? contest.teams.filter(t => t.hall === selectedHall).length : contest.teams.length;
  const threshold = Math.ceil(teams / 20);
  return (
    <div className="problem-list">
      {ordered.map(s => <ProblemBlock key={s.problem.id} {...s} threshold={threshold} />)}
      <div className="problem-stats-labels">
        <div className="problem" />
        <div className="problem-stats">
          <div>{t('balloons.inQueue')}</div>
          <div>{t('balloons.total')}</div>
        </div>
      </div>
    </div>
  );
};

export default ProblemList;
