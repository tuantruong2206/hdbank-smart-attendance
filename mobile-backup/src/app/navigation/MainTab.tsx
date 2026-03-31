import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import HomeScreen from '../../features/home/screens/HomeScreen';
import CheckInScreen from '../../features/checkin/screens/CheckInScreen';
import HistoryScreen from '../../features/history/screens/HistoryScreen';
import LeaveScreen from '../../features/leave/screens/LeaveScreen';
import ProfileScreen from '../../features/profile/screens/ProfileScreen';

const Tab = createBottomTabNavigator();

export default function MainTab() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: '#1677ff' },
        headerTintColor: '#fff',
        tabBarActiveTintColor: '#1677ff',
      }}
    >
      <Tab.Screen name="Trang chủ" component={HomeScreen} />
      <Tab.Screen name="Chấm công" component={CheckInScreen} />
      <Tab.Screen name="Lịch sử" component={HistoryScreen} />
      <Tab.Screen name="Nghỉ phép" component={LeaveScreen} />
      <Tab.Screen name="Cá nhân" component={ProfileScreen} />
    </Tab.Navigator>
  );
}
