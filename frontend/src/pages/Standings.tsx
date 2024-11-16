import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { Navigate } from 'react-router-dom';
import { InfoHolder, Team } from '../types';
import { GlobalError } from '../components/GlobalError';
import ProblemBox from '../components/ProblemBox';
import { useFilteredBalloons } from '../hooks/useFilteredBalloons';
import { useTranslation } from 'react-i18next';

interface TeamStats {
  team: Team
  solvedProblems: Set<string>
}

const StandingsView = () => {
  const { t } = useTranslation();
  const contest = useSelector((state: RootState) => state.contest);
  const selectedHall = useSelector((state: RootState) => state.hall.selectedHall);
  const filteredBalloons = useFilteredBalloons();

  const teamStats: TeamStats[] = useMemo(() => {
    const statsMap = new Map<string, Set<string>>();

    filteredBalloons.forEach((balloon) => {
      if (!statsMap.has(balloon.teamId)) {
        statsMap.set(balloon.teamId, new Set());
      }
      statsMap.get(balloon.teamId)!.add(balloon.problemId);
    });

    return contest.teams
      .filter(team => !selectedHall || team.hall === selectedHall)
      .map((team) => {
        return {
          team,
          solvedProblems: statsMap.get(team.id) ?? new Set(),
        };
      });
  }, [filteredBalloons, contest, selectedHall]);

  return (
    <main>
      <h1 className="sr-only">{t('standings.title')}</h1>
      <table className="standings">
        <thead>
          <tr>
            <th className="team-place">{t('standings.place')}</th>
            {contest.problems.map(problem => (
              <th className="team-problem" key={problem.id}>{problem.alias}</th>
            ))}
            <th className="team-name">{t('standings.team')}</th>
          </tr>
        </thead>
        <tbody>
          {teamStats.map(({ team, solvedProblems }) => (
            <tr key={team.id}>
              <td className="team-place">{team.displayName}</td>
              {contest.problems.map(problem => (
                <td className="team-problem" key={problem.id}>
                  {solvedProblems.has(problem.id)
                    ? (
                        <ProblemBox problem={problem} />
                      )
                    : null}
                </td>
              ))}
              <td className="team-name">{team.fullName}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
};

const Standings = ({ infoHolder }: { infoHolder: InfoHolder }) => {
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

  return <StandingsView />;
};

export default Standings;
