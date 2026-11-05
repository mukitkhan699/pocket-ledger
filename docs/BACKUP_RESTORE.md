# Backup and Restore

Backup is available from **Manage → Backup & restore**. Files are written through Android's
Storage Access Framework, so no storage permission is required.

## JSON backup (full restore)

`BackupManager.exportJson` writes a UTF-8 JSON document:

```json
{
  "format": "pocket-ledger-backup",
  "version": 1,
  "exported_at": "2026-11-04T02:00:00.000Z",
  "user_id": "…",
  "tables": {
    "categories": [ … ],
    "meal_settings": [ … ],
    "meal_entries": [ … ],
    "transactions": [ … ],
    "loans": [ … ],
    "loan_repayments": [ … ],
    "user_settings": [ … ]
  }
}
```

Only active rows (`deleted = 0`) are exported. The profile row is not needed because the account
identity already exists on the device.

Restore semantics:

- The file must contain `format = "pocket-ledger-backup"` and a matching `user_id`.
- Rows are merged by primary key inside one transaction; existing rows with the same ID are updated,
  missing rows are inserted. Rows not present in the backup are left untouched.
- Restored rows are enqueued for cloud sync.
- Foreign keys are enforced; if a referenced row is missing the restore transaction rolls back and
  the error is shown to the user.
- A different user's backup is rejected.

## CSV export (transactions only)

`BackupManager.exportTransactionsCsv` writes a spreadsheet-friendly file with columns:

`date, type, category, description, amount_bdt, notes, id`

Amounts are plain decimal BDT values. CSV export is read-only and cannot be imported by the app.
Fields containing commas, quotes, or newlines are quoted and escaped.

## Limitations

- Backup/restore does not include the local `profiles`, `sync_queue`, `sync_metadata`, or
  `sync_conflicts` tables.
- Deleted (tombstoned) rows are not exported.
- Restore does not delete rows that exist locally but are absent from the backup.
- Large databases are processed in memory; this is intended for personal finance data volumes.
