import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Contest } from '../types';

const initialState: Contest = {
  name: 'Loading',
  teams: [],
  problems: [],
};

export const problemsSlice = createSlice({
  name: 'problems',
  initialState,
  reducers: {
    setContest: (state, action: PayloadAction<Contest>) => {
      state.name = action.payload.name;
      state.teams = action.payload.teams;
      state.problems = action.payload.problems;
    },
  },
});

export const { setContest } = problemsSlice.actions;
export default problemsSlice.reducer; 