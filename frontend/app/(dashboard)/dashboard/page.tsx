import type { Metadata } from 'next';
import { DashboardContent } from './dashboard-content';

export const metadata: Metadata = {
  title: 'Обзор - AqStream',
  description: 'Обзор вашей организации и событий',
};

export default function DashboardPage() {
  return <DashboardContent />;
}
