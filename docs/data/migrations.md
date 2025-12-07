# Database Migrations

Управление схемой БД через **Liquibase**. Каждый сервис управляет своими миграциями.

## Структура

```
services/event-service/event-service-db/src/main/resources/db/changelog/
├── db.changelog-master.xml           # Главный файл
└── changes/
    ├── 001-create-events-table.xml
    ├── 002-create-ticket-types-table.xml
    └── ...
```

## Правила миграций

| Правило | Описание |
|---------|----------|
| **Backward Compatible** | Старый код должен работать с новой схемой |
| **Additive Only** | Не удалять колонки напрямую |
| **Всегда Rollback** | Каждый changeset имеет `<rollback>` |
| **Не изменять applied** | После apply changeset неизменяем |

**Именование:** `NNN-краткое-описание.xml` (001-create-events-table.xml)

## Пример changeset

```xml
<changeSet id="001-create-events" author="aqstream">
    <createTable tableName="events" schemaName="event_service">
        <column name="id" type="uuid"><constraints primaryKey="true"/></column>
        <column name="tenant_id" type="uuid"><constraints nullable="false"/></column>
        <column name="title" type="varchar(255)"><constraints nullable="false"/></column>
        <column name="created_at" type="timestamptz" defaultValueComputed="NOW()"/>
    </createTable>
    <createIndex tableName="events" schemaName="event_service" indexName="idx_events_tenant">
        <column name="tenant_id"/>
    </createIndex>
    <rollback><dropTable tableName="events" schemaName="event_service"/></rollback>
</changeSet>
```

## Добавление колонки с NOT NULL

```xml
<changeSet id="008-add-visibility" author="aqstream">
    <!-- 1. Nullable -->
    <addColumn tableName="events" schemaName="event_service">
        <column name="visibility" type="varchar(50)"/>
    </addColumn>
    <!-- 2. Fill default -->
    <update tableName="events" schemaName="event_service">
        <column name="visibility" value="PUBLIC"/>
        <where>visibility IS NULL</where>
    </update>
    <!-- 3. NOT NULL -->
    <addNotNullConstraint tableName="events" schemaName="event_service"
        columnName="visibility" defaultNullValue="PUBLIC"/>
    <rollback><dropColumn tableName="events" schemaName="event_service" columnName="visibility"/></rollback>
</changeSet>
```

## Row Level Security

```xml
<changeSet id="007-enable-rls" author="aqstream">
    <sql>
        ALTER TABLE event_service.events ENABLE ROW LEVEL SECURITY;
        CREATE POLICY tenant_isolation ON event_service.events
            FOR ALL USING (tenant_id = current_setting('app.tenant_id')::uuid);
    </sql>
    <rollback>
        <sql>DROP POLICY IF EXISTS tenant_isolation ON event_service.events;</sql>
    </rollback>
</changeSet>
```

## Команды

```bash
# Применить
./gradlew :services:event-service:liquibaseUpdate

# Откатить последний
./gradlew :services:event-service:liquibaseRollbackCount -PliquibaseCommandValue=1

# Посмотреть SQL без применения
./gradlew :services:event-service:liquibaseUpdateSQL

# Статус
./gradlew :services:event-service:liquibaseStatus
```

## Troubleshooting

| Проблема | Решение |
|----------|---------|
| Checksum mismatch | `UPDATE databasechangelog SET md5sum = NULL WHERE id = 'xxx'` |
| Lock не снимается | `UPDATE databasechangeloglock SET locked = false` |
| Откат не работает | Добавить `<rollback>` и выполнить SQL вручную |
