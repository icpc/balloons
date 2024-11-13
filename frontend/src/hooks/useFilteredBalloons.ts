import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';

export function useFilteredBalloons() {
  const contest = useSelector((state: RootState) => state.contest);
  const balloons = useSelector((state: RootState) => state.balloons.items);
  const selectedHall = useSelector((state: RootState) => state.halls.selectedHall);

  return useMemo(() => {
    if (!selectedHall) return balloons;
    return balloons.filter(balloon => {
      const team = contest.teams.find(t => t.id === balloon.teamId);
      return team?.hall === selectedHall;
    });
  }, [balloons, contest.teams, selectedHall]);
} 