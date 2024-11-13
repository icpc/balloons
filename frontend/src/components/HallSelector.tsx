import { useMemo, useRef, useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { setSelectedHall } from '../store/hallsSlice';
import { useClickOutside } from '../hooks/useClickOutside';

const HallSelector = () => {
  const dispatch = useDispatch();
  const contest = useSelector((state: RootState) => state.contest);
  const selectedHall = useSelector((state: RootState) => state.halls.selectedHall);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useClickOutside(dropdownRef, () => setIsOpen(false));

  const halls = useMemo(() => {
    const hallSet = new Set(contest.teams.map(team => team.hall).filter(Boolean));
    return Array.from(hallSet).sort();
  }, [contest.teams]);

  useEffect(() => {
    if (selectedHall && !halls.includes(selectedHall)) {
      dispatch(setSelectedHall(null));
    }
  }, [halls, selectedHall, dispatch]);

  if (halls.length === 0) return null;

  const handleSelect = (hall: string | null) => {
    dispatch(setSelectedHall(hall));
    setIsOpen(false);
  };

  return (
    <div className="hall-dropdown" ref={dropdownRef}>
      <span 
        className="hall-dropdown-toggle" 
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
        aria-haspopup="true"
      >
        {selectedHall ? `Холл ${selectedHall}` : 'Все холлы'}
        <span className="dropdown-arrow"></span>
      </span>
      {isOpen && (
        <ul className="hall-dropdown-menu" role="menu">
          <li 
            className={!selectedHall ? 'active' : ''} 
            onClick={() => handleSelect(null)}
          >
            Все холлы
          </li>
          {halls.map(hall => (
            <li
              key={hall}
              className={selectedHall === hall ? 'active' : ''}
              onClick={() => handleSelect(hall)}
            >
              Холл {hall}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default HallSelector; 