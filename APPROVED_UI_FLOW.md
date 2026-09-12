# Jothi Vel Chits - Approved Mobile UI Flow

## Design direction

- Primary: deep maroon (`#6B1E1E`)
- Accent: warm gold, success green, overdue red, ledger blue
- Background: warm off-white with white cards
- Navigation: Home, Groups, Collect, Ledger, More
- UX rule: primary collection actions must be reachable in one or two taps
- Data rule: money shown in Indian grouping; server and Room continue to store paise

## 1. Login to Home

1. User completes the existing PIN/login flow.
2. App opens `MainHostActivity` and the approved app shell.
3. Home displays:
   - Today Collection
   - Pending
   - Delivery Due
   - Settlement Due
   - Collection, Settlement, Pending and Ledger quick actions
   - Recent financial activity
4. Pending metric and quick action open the Pending screen.
5. Collection opens Add Collection.
6. Ledger opens Customer Ledger.

## 2. Customer ledger flow

1. Open Ledger from the bottom navigation or Home quick action.
2. Search using customer name or mobile number.
3. Switch between Consolidated and Details views.
4. Customer summary shows identity, code, phone, area, chit value and dates.
5. Financial summary shows Collection, Settlement, Balance and Delivery separately.
6. Monthly Installments shows paid/due status and amount for every installment.
7. `Add collection` carries the selected customer into the collection form.

## 3. Collection flow

1. Open Add Collection directly or from a ledger/pending customer.
2. Select/confirm Customer.
3. Select Chit/subscription.
4. Confirm transaction date.
5. App displays the calculated Due Amount.
6. Enter Amount Received.
7. Select Cash, UPI or Bank.
8. UPI/Bank displays Reference/UTR input.
9. Add optional notes.
10. Tap `Save & Send Receipt`.
11. App validates positive amount and required fields.
12. Success dialog shows customer, amount, mode and receipt number.
13. User can share the receipt or finish and return Home.

Backend integration after legacy configuration arrives:

- resolve the selected customer subscription and due installment;
- write an immutable collection transaction;
- update derived installment balance in one transaction;
- generate the real receipt number/PDF;
- queue WhatsApp/SMS delivery;
- store locally as pending sync when offline.

## 4. Pending dues flow

1. Open Pending from Home.
2. Search by customer or mobile.
3. Filter by All, Area, Chit or Agent.
4. Each card shows customer, phone, area, chit, payable, paid, pending, last-paid date, due date and pending installment numbers.
5. `Call` opens the phone dialer.
6. `WhatsApp` opens a pre-filled pending reminder.
7. `Collect` carries that customer to Add Collection.
8. Completing collection returns to Home; live database integration will refresh pending totals automatically.

## 5. Groups flow

The existing group-management flow remains available from Groups. It will be restyled after the legacy database fields are confirmed:

- Chit master/list
- Chit slab/value and date range
- Customer-slot linking
- In-charge/agent assignment
- Monthly schedule generation
- Auction and delivery/payout

## 6. More flow

- Backup database
- Restore database
- Export CSV
- Log out

These actions reuse the existing Activity Result launchers in `MainHostActivity`.

## 7. Required live-data mapping

The approved UI currently contains safe preview/fallback records so the complete interaction can be reviewed before the old configuration/database arrives. Replace them through ViewModel state with:

- Dashboard summary endpoint/Room query
- Customer and subscription search
- Consolidated ledger aggregation
- Monthly installment rows
- Pending report with area/chit/agent filters
- Collection transaction command
- Receipt generation and sharing result

Navigation and reusable UI components do not need to change during this mapping.

## 8. Acceptance checklist

- [ ] Home totals equal the legacy desktop totals for the same date/branch.
- [ ] Customer consolidated Collection, Settlement, Balance and Delivery match legacy ledger.
- [ ] Monthly installment rows match the expanded desktop ledger.
- [ ] Pending/paid/excess-paid definitions are confirmed with the business owner.
- [ ] Area, Chit and Agent filters return the same rows as the legacy report.
- [ ] Cash/UPI/Bank collection creates exactly one transaction and receipt.
- [ ] Offline collection sync is idempotent and does not create duplicates.
- [ ] Tamil and English labels fit without truncating financial values.
- [ ] Call, WhatsApp, receipt sharing, backup, restore and export work on a physical device.

