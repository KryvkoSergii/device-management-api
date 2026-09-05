WITH rows_to_insert AS (
    SELECT GREATEST(0, 1000000 - COUNT(*))::INTEGER AS value
    FROM devices
)
INSERT INTO devices (id, name, brand, state, created_at, updated_at, version)
SELECT gen_random_uuid(),
       'Performance device ' || sequence_number,
       (ARRAY['Cisco', 'Juniper', 'Ubiquiti', 'Fortinet', 'Aruba'])
           [1 + (sequence_number % 5)],
       (ARRAY['AVAILABLE', 'IN_USE', 'INACTIVE'])
           [1 + (sequence_number % 3)],
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM rows_to_insert,
     LATERAL generate_series(1, rows_to_insert.value) AS sequence_number;

ANALYZE devices;
