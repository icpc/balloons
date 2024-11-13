import { useMemo } from "react";
import { Problem, State } from "../types";
import ProblemBox from "./ProblemBox";

const ProblemBlock = ({ problem, solves }: { problem: Problem, solves: number }) => {
  return useMemo(() => <div title={problem.name}>
    <ProblemBox problem={problem} />
    <div className="problem-solves">
      {solves}
    </div>
  </div>, [problem, solves]);
};

const ProblemList = ({ contest, balloons }: State) => {
  return useMemo(() => (
      <div className="problem-list">
        {contest.problems.map(problem => (
          <ProblemBlock key={problem.id} problem={problem} solves={balloons.filter(b => b.problemId === problem.id).length} />
        ))}
      </div>
  ), [contest, balloons]);
};

export default ProblemList;