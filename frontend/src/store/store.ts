import { configureStore } from '@reduxjs/toolkit';
import problemsReducer from './problemsSlice';
import balloonsReducer from './balloonsSlice';

export const store = configureStore({
  reducer: {
    problems: problemsReducer,
    balloons: balloonsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;