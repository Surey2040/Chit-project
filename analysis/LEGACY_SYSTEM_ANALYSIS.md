# Jothi Vel Cits legacy system analysis

## Scope and evidence

- Analysed archive: `soft.rar`
- Archive SHA-256: `FF28945458006C78080A21F90D5BC712A573C95AB8EAC42EDEC3424948EB52C3`
- The archive contains the deployed Windows application, symbols, configuration databases, report/menu metadata, dependencies, and the SQL backup utility.
- It does **not** contain the actual SQL Server `cits` database backup (`.bak`) or live business rows.
- Credentials and encryption material discovered in configuration were intentionally not copied into this report.

## Confirmed legacy architecture

- Windows Forms application on .NET Framework 4.8.
- DevExpress 10.2 grid/report UI.
- Primary business database: Microsoft SQL Server.
- Configured instance: `localhost\r2`; database: `cits`.
- Supporting SQLite databases:
  - `menu.db`: modules, activities, menus and access metadata.
  - `wascript.db`: application startup/configuration script.
  - `winbdsinfo.db`: encrypted SQL connection information.
  - `bb_excel.db`: Excel report/template metadata.
  - `backinfo.db`: separate backup-tool users and backup configuration.
- SQL backup tool writes backups to `F:\`, retains 10 days, and has S3 upload disabled.
- ClosedXML/OpenXML are used for spreadsheet exports. AWS S3 and MailKit libraries are present, but S3 is disabled in the supplied configuration.

## Confirmed feature map

### Master

- City / State / Country
- Customer
- Customer import
- CITS slab and slab details
- CITS/group creation and customer linking
- CITS/collection import

### Transactions

- Collection entry
- Settlement entry
- Collection register
- Settlement register

### Reports

- CITS Ledger and export
- Pending Register and export/print

### System

- Active date
- Users, roles, role access and user exceptions
- User status and access list
- Logoff

## Recovered domain model

The executable and PDB confirm these primary legacy objects/tables:

- `customer`
- `customerbranch`
- `citsslab`
- `citsslabdetail`
- `cits`
- `citslinkedcustomer`
- `citscollection`
- `citssettlement`
- `citscompleted`
- user/role/access tables
- city/state/country reference tables

Important relationships:

1. A customer can have one or more customer branches/details.
2. A chit points to a chit slab.
3. A slab has ordered installment rows with amount, miscellaneous amount and settlement amount.
4. `citslinkedcustomer` is the many-to-many membership between a chit and a customer branch; it also stores ticket/serial/in-charge information.
5. Collections and settlements are recorded against both chit and customer branch.

## Recovered accounting meaning

- `Collection Amount`: actual money received from the member (`citscollection`).
- `Settlement Amount`: scheduled/expected monthly amount generated from slab rows (`citssettlement` rows of type `AUTO`).
- `Balance`: collection minus scheduled settlement. A negative value means the member is behind schedule.
- `Delivery Amount`: payout/manual settlement amount. The executable's ledger query separates it from AUTO settlement rows.
- `Payable Amount`: sum of scheduled AUTO settlement rows up to the selected date.
- `Paid Amount`: sum of actual collection rows up to the selected date.
- `Pending Amount`: payable minus paid.
- `Last Paid Date`: latest qualifying collection date.
- `Pending Due`: installment labels/notes for unpaid due rows, concatenated for the Pending Register.

This explains the old screen's green Delivery cells: they represent a recorded payout/delivery, not an extra collection.

## Ledger and Pending behavior

### Ledger

- Joins chit, slab, linked customer, customer/customer-branch and location data.
- Consolidated mode totals collection, scheduled settlement and delivery by member/chit.
- Detail/monthly mode unions transaction types and groups by year/month/member/chit.
- Balance is derived, not an independently editable value.

### Pending Register

- Builds expected payable rows from AUTO settlements up to the selected date.
- Compares them with collections.
- Excludes or treats completed memberships through `citscompleted`.
- Returns customer code, ticket serial, old code, mobile, address/city, chit, slab value, payable, paid, pending, last-paid date and pending installment labels.

## Gaps in the current Android Room database

The current version-9 Room schema is a prototype and must not receive production legacy data as-is.

- `MemberEntity.selectedChitId` allows only one chit per customer; legacy supports many-to-many membership.
- Customer master and customer branch/ticket identity are mixed into one member row.
- There is no explicit scheduled due/AUTO settlement table.
- There is no delivery/manual settlement table.
- There is no completion/closure table.
- Amount storage is inconsistent (`int` rupees in some tables and `long` paise in payments).
- Dates are strings in different display formats rather than canonical dates.
- Foreign keys, unique constraints and accounting indexes are insufficient.
- Payment reversal/void, created-by, audit and sync state are missing.
- Users, roles and permissions are not mapped.
- Several dashboard/report values are still demo/static values rather than database-derived values.

## Recommended production data model

Use normalized Room tables and store all money as `Long` paise and all business dates as ISO dates/epoch days.

- `customers`
- `customer_branches`
- `chit_slabs`
- `chit_slab_installments`
- `chits`
- `chit_memberships`
- `scheduled_dues`
- `collections`
- `deliveries`
- `membership_completions`
- `users`, `roles`, `user_roles`, `role_permissions`
- `audit_events`
- `sync_outbox` and `sync_state` if coexistence/sync is required

Required constraints include unique legacy IDs, unique customer code, unique chit number, unique `(chit_id, ticket_serial)`, foreign keys, and idempotent import keys.

## Exact legacy-to-Android mapping

| Legacy | Android target |
|---|---|
| `customer` | `customers` |
| `customerbranch` | `customer_branches` |
| `citsslab` | `chit_slabs` |
| `citsslabdetail` | `chit_slab_installments` |
| `cits` | `chits` |
| `citslinkedcustomer` | `chit_memberships` |
| `citscollection` | `collections` |
| `citssettlement` type `AUTO` | `scheduled_dues` |
| manual/non-AUTO `citssettlement` | `deliveries` |
| `citscompleted` | `membership_completions` |
| user/role/access tables | app users and permissions |

Legacy primary IDs must be retained in dedicated `legacy_id` columns for reconciliation.

## Recommended deployment solution

Do not connect the Android APK directly to SQL Server and never embed the SQL `sa` credentials in the APK.

Recommended phased solution:

1. Restore a copy of the real `.bak` into an isolated SQL Server instance.
2. Build a read-only migration/export tool or local API that reads the legacy schema.
3. Import into the normalized Room schema using deterministic legacy IDs.
4. Run reconciliation reports for every member/chit:
   - collection total
   - scheduled settlement total
   - delivery total
   - balance
   - payable/paid/pending as of a fixed date
   - pending installment list
5. Pilot the Android app in read-only mode and compare it with the Windows reports.
6. Enable collection writes only after totals match exactly.
7. During coexistence, use a secured REST API plus an outbox/idempotency mechanism; do not let two systems write independently without synchronization.
8. After cutover, keep encrypted mobile backups, restore validation, audit logs and a final read-only legacy snapshot.

## Backup/restore recommendation

- Create a versioned encrypted backup package containing the Room DB, schema version, manifest, creation time and SHA-256 checksum.
- Use Android Storage Access Framework so the user chooses the backup destination.
- Restore into a temporary location first, verify checksum/schema/foreign keys, then atomically replace the active DB.
- Never overwrite the active DB before validation.
- Preserve at least the last three known-good backups.

## Data required before implementation can be certified

At least one of the following is required:

1. Preferred: the latest SQL Server `.bak` file from `F:\`.
2. A read-only SQL Server connection available on the same LAN.
3. SQL exports of schema plus the required tables.

Before sharing a backup, make a copy and avoid modifying the original. The supplied archive alone is enough to design the migration, but not enough to prove row counts or financial totals.

## Implementation order

1. Freeze current prototype DB and remove production dependence on demo/static totals.
2. Add normalized Room schema and tested migrations.
3. Implement legacy importer and reconciliation engine.
4. Bind Ledger and Pending screens to real repository queries.
5. Bind Collection and Delivery writes with audit/reversal handling.
6. Implement users/roles.
7. Implement encrypted backup/restore.
8. Run golden-data and end-to-end tests, then pilot/cut over.
