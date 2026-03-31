import React from 'react';
import { Text } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import HomeScreen from '../../features/home/screens/HomeScreen';
import CheckInScreen from '../../features/checkin/screens/CheckInScreen';
import HistoryScreen from '../../features/history/screens/HistoryScreen';
import LeaveScreen from '../../features/leave/screens/LeaveScreen';
import ChatbotScreen from '../../features/chatbot/screens/ChatbotScreen';
import ProfileScreen from '../../features/profile/screens/ProfileScreen';

const Tab = createBottomTabNavigator();

// Simple text-based icon component (avoids needing vector-icons package)
function TabIcon({ label, focused }: { label: string; focused: boolean }) {
  const icons: Record<string, string> = {
    'Trang chu': '\u2302',     // House
    'Cham cong': '\u2713',     // Checkmark
    'Lich su': '\u2630',       // Trigram / list
    'Nghi phep': '\u2708',     // Airplane
    'Tro ly AI': '\u2601',     // Cloud (chat)
    'Ca nhan': '\u263A',       // Smiley
  };

  return (
    <Text
      style={{
        fontSize: 22,
        color: focused ? '#1677ff' : '#999',
      }}
    >
      {icons[label] ?? '?'}
    </Text>
  );
}

export default function MainTab() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerStyle: { backgroundColor: '#1677ff' },
        headerTintColor: '#fff',
        tabBarActiveTintColor: '#1677ff',
        tabBarInactiveTintColor: '#999',
        tabBarIcon: ({ focused }) => (
          <TabIcon label={route.name} focused={focused} />
        ),
        tabBarLabelStyle: { fontSize: 11 },
      })}
    >
      <Tab.Screen name="Trang chu" component={HomeScreen} />
      <Tab.Screen name="Cham cong" component={CheckInScreen} />
      <Tab.Screen name="Lich su" component={HistoryScreen} />
      <Tab.Screen name="Nghi phep" component={LeaveScreen} />
      <Tab.Screen name="Tro ly AI" component={ChatbotScreen} />
      <Tab.Screen name="Ca nhan" component={ProfileScreen} />
    </Tab.Navigator>
  );
}
