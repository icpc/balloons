import { useMemo } from 'react';
import { Balloon, Contest, Problem } from '../types';
import ProblemBox from './ProblemBox';

const ProblemBlock = ({ problem, solves, required, solved = false }: {
    problem: Problem,
    solves: number,
    required: number,
    solved?: boolean
}) => {
    const getUrgencyLevel = (count: number): string => {
        if (count > 100) return '5';
        if (count > 65) return '4';
        if (count > 30) return '3';
        if (count > 15) return '2';
        if (count > 0) return '1';
        return '';
    };
    return useMemo(() => (
        <div className={solved ? 'solved-problem' : ''} title={problem.name}>
            <ProblemBox problem={problem} />
            <div className="problem-solves">
                {solves}
            </div>
            {required > 0 && (
                <div className="balloons-require" urgency-level={getUrgencyLevel(required)}>
                    {required}
                </div>
            )}
        </div>
    ), [problem, solves, required, solved]);
};

const ProblemList = ({ contest, balloons }: { contest: Contest, balloons: Balloon[] }) => {
    return useMemo(() => (
        <div className="problem-list">
            {contest.problems.map(problem => {
                const solvedBalloons = balloons.filter(b => b.problemId === problem.id);
                const requiredCount = solvedBalloons.filter(
                    balloon => balloon.takenBy === null && !balloon.delivered
                ).length;

                return (
                    <ProblemBlock
                        key={problem.id}
                        problem={problem}
                        solves={solvedBalloons.length}
                        required={requiredCount}
                        solved={solvedBalloons.length > 0}
                    />
                );
            })}
        </div>

    ), [contest, balloons]);
};

export default ProblemList;
