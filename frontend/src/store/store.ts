import { configureStore } from '@reduxjs/toolkit';
import contestReducer from './contestSlice';
import balloonsReducer from './balloonsSlice';
import hallsReducer from './hallsSlice';

export const store = configureStore({
  reducer: {
    contest: contestReducer,
    balloons: balloonsReducer,
    halls: hallsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;