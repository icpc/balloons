import { configureStore } from '@reduxjs/toolkit';
import contestReducer from './contestSlice';
import balloonsReducer from './balloonsSlice';
import hallReducer from './hallSlice';

export const store = configureStore({
  reducer: {
    contest: contestReducer,
    balloons: balloonsReducer,
    hall: hallReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;