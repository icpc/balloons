import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface HallState {
  selectedHall: string | null;
}

const initialState: HallState = {
  selectedHall: localStorage.getItem('selectedHall')
};

export const hallSlice = createSlice({
  name: 'halls',
  initialState,
  reducers: {
    setSelectedHall: (state, action: PayloadAction<string | null>) => {
      state.selectedHall = action.payload;
      if (action.payload) {
        localStorage.setItem('selectedHall', action.payload);
      } else {
        localStorage.removeItem('selectedHall');
      }
    },
  },
});

export const { setSelectedHall } = hallSlice.actions;
export default hallSlice.reducer; 