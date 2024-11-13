import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface HallsState {
  selectedHall: string | null;
}

const initialState: HallsState = {
  selectedHall: localStorage.getItem('selectedHall')
};

export const hallsSlice = createSlice({
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

export const { setSelectedHall } = hallsSlice.actions;
export default hallsSlice.reducer; 