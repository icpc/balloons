import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Problem } from '../types';

interface ProblemsState {
  items: Problem[];
}

const initialState: ProblemsState = {
  items: [],
};

export const problemsSlice = createSlice({
  name: 'problems',
  initialState,
  reducers: {
    setProblems: (state, action: PayloadAction<Problem[]>) => {
      state.items = action.payload;
    },
  },
});

export const { setProblems } = problemsSlice.actions;
export default problemsSlice.reducer; 