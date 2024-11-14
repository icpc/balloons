import { useMemo } from 'react';
import { Contest, Problem, Team } from '../types';

export const useContestMaps = (contest: Contest) => {
  const problemsMap = useMemo(() => {
    return contest.problems.reduce((acc, problem) => {
      acc[problem.id] = problem;
      return acc;
    }, {} as Record<string, Problem>);
  }, [contest.problems]);

  const teamMap = useMemo(() => {
    return contest.teams.reduce((acc, team) => {
      acc[team.id] = team;
      return acc;
    }, {} as Record<string, Team>);
  }, [contest.teams]);

  return { problemsMap, teamMap };
};
