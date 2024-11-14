import { HTMLProps, useMemo } from 'react';
import { Problem } from '../types';

function isLightColor(color: string): boolean {
  const hex = color.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  const brightness = (r * 299 + g * 587 + b * 114) / 1000;
  return brightness > 128;
}

function styleProblem(problem: Problem): HTMLProps<HTMLDivElement> {
  if (problem.color) {
    return {
      style: {
        backgroundColor: problem.color,
        color: isLightColor(problem.color) ? 'black' : 'white',
      },
      className: 'problem',
    };
  }
  return { className: 'crossed problem' };
}

const ProblemBox = ({ problem }: { problem: Problem }) => {
  return useMemo(() => (
    <div {...styleProblem(problem)}>
      {problem.alias}
    </div>
  ), [problem]);
};

export default ProblemBox;
