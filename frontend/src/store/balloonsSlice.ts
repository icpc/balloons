import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Balloon } from '../types';

interface BalloonsState {
  items: Balloon[]
}

const initialState: BalloonsState = {
  items: [],
};

export const balloonsSlice = createSlice({
  name: 'balloons',
  initialState,
  reducers: {
    updateBalloon: (state, action: PayloadAction<Balloon>) => {
      const index = state.items.findIndex(b => b.runId === action.payload.runId);
      if (index >= 0) {
        state.items[index] = action.payload;
      }
      else {
        state.items.push(action.payload);
      }
    },
    deleteBalloon: (state, action: PayloadAction<string>) => {
      state.items = state.items.filter(b => b.runId !== action.payload);
    },
    setBalloons: (state, action: PayloadAction<Balloon[]>) => {
      state.items = action.payload;
    },
  },
});

export const { updateBalloon, deleteBalloon, setBalloons } = balloonsSlice.actions;
export default balloonsSlice.reducer;
