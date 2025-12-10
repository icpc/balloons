import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { useContestMaps } from './useContestMaps';

export function useFilteredBalloons() {
  const contest = useSelector((state: RootState) => state.contest);
  const balloons = useSelector((state: RootState) => state.balloons.items);
  const selectedHall = useSelector((state: RootState) => state.hall.selectedHall);
  const { teamMap } = useContestMaps(contest);

  return useMemo(() => {
    if (!selectedHall) return balloons;
    return balloons.filter((balloon) => {
      return teamMap[balloon.teamId]?.hall === selectedHall;
    });
  }, [balloons, selectedHall, teamMap]);
}
