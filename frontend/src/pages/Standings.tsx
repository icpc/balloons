import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { Navigate } from 'react-router-dom';
import { InfoHolder, Team } from '../types';
import { GlobalError } from '../components/GlobalError';
import ProblemBox from '../components/ProblemBox';

interface TeamStats {
  team: Team;
  solvedProblems: Set<string>;
}

const StandingsView = () => {
  const problems = useSelector((state: RootState) => state.problems.items);
  const balloons = useSelector((state: RootState) => state.balloons.items);

  const teamStats = useMemo(() => {
    // Group balloons by team
    const statsMap = new Map<string, TeamStats>();
    
    balloons.forEach(balloon => {
      if (!statsMap.has(balloon.team.id)) {
        statsMap.set(balloon.team.id, {
          team: balloon.team,
          solvedProblems: new Set()
        });
      }
      statsMap.get(balloon.team.id)!.solvedProblems.add(balloon.problemId);
    });

    // Convert to array and sort by team displayName
    return Array.from(statsMap.values())
      .sort((a, b) => a.team.displayName.localeCompare(b.team.displayName));
  }, [balloons]);

  return (
    <main>
      <h1 className="sr-only">Турнирная таблица</h1>
      <table className="standings">
        <thead>
          <tr>
            <th className="team-place">Место</th>
            {problems.map(problem => (
              <th className="team-problem" key={problem.id}>{problem.alias}</th>
            ))}
            <th className="team-name">Команда</th>
          </tr>
        </thead>
        <tbody>
          {teamStats.map((stats) => (
            <tr key={stats.team.id}>
              <td className="team-place">{stats.team.displayName}</td>
              {problems.map(problem => (
                <td className="team-problem" key={problem.id}>
                  {stats.solvedProblems.has(problem.id) ? (
                    <ProblemBox problem={problem} />
                  ) : null}
                </td>
              ))}
              <td className="team-name">{stats.team.fullName}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
};

const Standings = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  if (!infoHolder.info.login) {
    return <Navigate to="/login" />;
  }

  if (!infoHolder.info.canAccess) {
    return <GlobalError title="Forbidden" message="Ask organizer to give you access." />;
  }

  return <StandingsView />;
};

export default Standings;
