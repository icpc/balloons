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
      const balloons = state.items.filter(b => b.runId !== action.payload.runId);

      const insertIndex = balloons.findIndex(b => b.time > action.payload.time);
      if (insertIndex === -1) {
        balloons.push(action.payload);
      } else {
        balloons.splice(insertIndex, 0, action.payload);
      }

      state.items = balloons;
    },
    deleteBalloon: (state, action: PayloadAction<string>) => {
      state.items = state.items.filter(b => b.runId !== action.payload);
    },
    setBalloons: (state, action: PayloadAction<Record<string, Balloon>>) => {
      state.items = Object.values(action.payload).sort((a, b) => a.time - b.time);
    },
  },
});

export const { updateBalloon, deleteBalloon, setBalloons } = balloonsSlice.actions;
export default balloonsSlice.reducer;
