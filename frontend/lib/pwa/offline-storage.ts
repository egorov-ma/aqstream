import { openDB, type IDBPDatabase } from 'idb';

const DB_NAME = 'aqstream-checkin';
const DB_VERSION = 1;

interface PendingCheckIn {
  registrationId: string;
  eventId: string;
  timestamp: number;
  confirmationCode: string;
}

interface CachedRegistration {
  id: string;
  eventId: string;
  confirmationCode: string;
  status: string;
  firstName: string;
  lastName: string | null;
  email: string;
  ticketTypeName: string;
  checkedInAt: string | null;
}

type CheckInDB = {
  'pending-checkins': {
    key: string;
    value: PendingCheckIn;
  };
  'cached-registrations': {
    key: string;
    value: CachedRegistration;
    indexes: { 'by-event': string; 'by-code': string };
  };
};

let dbPromise: Promise<IDBPDatabase<CheckInDB>> | null = null;

/**
 * Открывает базу данных IndexedDB.
 */
async function getDB(): Promise<IDBPDatabase<CheckInDB>> {
  if (!dbPromise) {
    dbPromise = openDB<CheckInDB>(DB_NAME, DB_VERSION, {
      upgrade(db) {
        // Store для pending check-ins (офлайн)
        if (!db.objectStoreNames.contains('pending-checkins')) {
          db.createObjectStore('pending-checkins', { keyPath: 'registrationId' });
        }

        // Store для кешированных регистраций
        if (!db.objectStoreNames.contains('cached-registrations')) {
          const store = db.createObjectStore('cached-registrations', { keyPath: 'id' });
          store.createIndex('by-event', 'eventId');
          store.createIndex('by-code', 'confirmationCode');
        }
      },
    });
  }
  return dbPromise;
}

/**
 * Сохраняет pending check-in для синхронизации при восстановлении сети.
 */
export async function savePendingCheckIn(
  registrationId: string,
  eventId: string,
  confirmationCode: string
): Promise<void> {
  const db = await getDB();
  await db.put('pending-checkins', {
    registrationId,
    eventId,
    confirmationCode,
    timestamp: Date.now(),
  });
}

/**
 * Получает все pending check-ins.
 */
export async function getPendingCheckIns(): Promise<PendingCheckIn[]> {
  const db = await getDB();
  return db.getAll('pending-checkins');
}

/**
 * Удаляет pending check-in после успешной синхронизации.
 */
export async function removePendingCheckIn(registrationId: string): Promise<void> {
  const db = await getDB();
  await db.delete('pending-checkins', registrationId);
}

/**
 * Кеширует регистрации события для офлайн поиска.
 */
export async function cacheRegistrations(registrations: CachedRegistration[]): Promise<void> {
  const db = await getDB();
  const tx = db.transaction('cached-registrations', 'readwrite');
  await Promise.all([
    ...registrations.map((reg) => tx.store.put(reg)),
    tx.done,
  ]);
}

/**
 * Ищет регистрацию по confirmation code в кеше.
 */
export async function findCachedRegistration(
  eventId: string,
  confirmationCode: string
): Promise<CachedRegistration | undefined> {
  const db = await getDB();
  const registrations = await db.getAllFromIndex(
    'cached-registrations',
    'by-event',
    eventId
  );
  return registrations.find(
    (reg) => reg.confirmationCode.toUpperCase() === confirmationCode.toUpperCase()
  );
}

/**
 * Получает все кешированные регистрации события.
 */
export async function getCachedRegistrations(eventId: string): Promise<CachedRegistration[]> {
  const db = await getDB();
  return db.getAllFromIndex('cached-registrations', 'by-event', eventId);
}

/**
 * Очищает кеш регистраций события.
 */
export async function clearCachedRegistrations(eventId: string): Promise<void> {
  const db = await getDB();
  const tx = db.transaction('cached-registrations', 'readwrite');
  const registrations = await tx.store.index('by-event').getAll(eventId);
  await Promise.all([
    ...registrations.map((reg) => tx.store.delete(reg.id)),
    tx.done,
  ]);
}

/**
 * Проверяет, есть ли pending check-ins для синхронизации.
 */
export async function hasPendingCheckIns(): Promise<boolean> {
  const pending = await getPendingCheckIns();
  return pending.length > 0;
}
