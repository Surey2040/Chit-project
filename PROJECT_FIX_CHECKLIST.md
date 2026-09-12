# Jothi Vel Chits - Project Fix Checklist

இந்த document project analysis-ல் கண்டுபிடிக்கப்பட்ட மாற்றங்கள் மற்றும் pending வேலைகளை track செய்ய பயன்படுத்த வேண்டும்.

## P0 - Build மற்றும் Runtime Blockers

- [ ] `AndroidManifest.xml`-ல் activity declarations நடுவில் உள்ள literal `` `n `` text-ஐ நீக்க வேண்டும்.
- [ ] `ApiService.getGroups()` return type மற்றும் `GroupRepository` எதிர்பார்க்கும் `List<ChitGroupEntity>` type-ஐ ஒரே மாதிரியாக மாற்ற வேண்டும்.
- [ ] `ApiService`-ல் missing `getMembers()` endpoint declaration சேர்க்க வேண்டும்.
- [ ] `MemberRepository` API response type மற்றும் backend `/members` response format match ஆகிறதா உறுதி செய்ய வேண்டும்.
- [ ] `backend/src/routes/payments.js`-ல் பயன்படுத்தப்படும் `User` model-ஐ import செய்ய வேண்டும்.
- [ ] Gradle wrapper (`gradlew`, `gradlew.bat`, wrapper JAR) repository-ல் சேர்க்க வேண்டும்.
- [ ] Android debug build ஓட்டி எல்லா Java/Kotlin compilation errors-ஐ சரி செய்ய வேண்டும்.
- [ ] `backend/package.json`-ல் உண்மையான test command configure செய்ய வேண்டும்.

## P0 - Authentication மற்றும் Security

- [ ] Android `AuthRepository`-ல் உள்ள local demo PIN login-ஐ real `/auth/login` API call-ஆக மாற்ற வேண்டும்.
- [ ] `demo_access_token` மற்றும் `demo_refresh_token` பயன்பாட்டை முழுவதும் நீக்க வேண்டும்.
- [ ] Access token expire ஆனால் `/auth/refresh-token` மூலம் refresh செய்து request retry செய்ய OkHttp authenticator சேர்க்க வேண்டும்.
- [ ] PIN, access token, refresh token ஆகியவற்றை plain `SharedPreferences`-க்கு பதிலாக encrypted storage-ல் வைக்க வேண்டும்.
- [ ] Public `/auth/seed-admin` endpoint-ஐ production code-லிருந்து நீக்க வேண்டும் அல்லது development environment-க்கு மட்டும் restrict செய்ய வேண்டும்.
- [ ] Default admin username/password-ஐ நீக்கி secure one-time admin provisioning flow உருவாக்க வேண்டும்.
- [ ] Hard-coded fallback JWT secret-ஐ நீக்க வேண்டும்; `JWT_SECRET` இல்லாவிட்டால் server startup fail ஆக வேண்டும்.
- [ ] Access token மற்றும் refresh token-க்கு தனித்தனி secrets/keys பயன்படுத்த வேண்டும்.
- [ ] Refresh-token rotation, revocation மற்றும் logout invalidation implement செய்ய வேண்டும்.
- [ ] Login endpoint-க்கு rate limiting / brute-force protection சேர்க்க வேண்டும்.
- [ ] CORS allowed origins-ஐ production domains-க்கு மட்டும் restrict செய்ய வேண்டும்.
- [ ] Android-ல் `usesCleartextTraffic="true"` நீக்கி HTTPS மட்டும் பயன்படுத்த வேண்டும்.
- [ ] Hard-coded LAN `BASE_URL`-ஐ build configuration/environment அடிப்படையில் மாற்ற வேண்டும்.
- [ ] Release build-ல் OkHttp `BODY` logging disable செய்ய வேண்டும்.
- [ ] Financial/member data backup policy முடிவு செய்து தேவையில்லையெனில் `allowBackup="false"` அமைக்க வேண்டும்.

## P0 - Financial Data Integrity

- [ ] Auction finalize, installment update, member win update மற்றும் payment dues generation அனைத்தையும் ஒரே database transaction-ல் செய்ய வேண்டும்.
- [ ] Auction winner அந்த group-ன் active subscriber என்பதை validate செய்ய வேண்டும்.
- [ ] ஏற்கெனவே auction வென்ற member மறுபடியும் winner ஆகாமல் validate செய்ய வேண்டும்.
- [ ] Auction-ல் கணக்கிடப்படும் `companyCommission` value-ஐ installment-ல் save செய்ய வேண்டும்.
- [ ] Auction code உருவாக்கும் `paymentsToCreate` rows-ஐ உண்மையில் database-ல் insert செய்ய வேண்டும்.
- [ ] `DUE` payment rows-க்கு `paidAt` nullable ஆக model/schema மாற்ற வேண்டும்.
- [ ] Partial payment-க்கு balance collection/update flow implement செய்ய வேண்டும்; இரண்டாவது payment-ஐ முழுமையாக reject செய்யக்கூடாது.
- [ ] Payment creation + PDF generation + notification failure handling-ஐ பிரிக்க வேண்டும்; payment save ஆன பிறகு notification fail ஆனால் API தவறாக total failure காட்டக்கூடாது.
- [ ] Duplicate payment race condition தவிர்க்க `(installmentId, memberId)` database unique constraint சேர்க்க வேண்டும்.
- [ ] `(groupId, slotNo)` database unique constraint சேர்க்க வேண்டும்.
- [ ] Business rule தேவைப்பட்டால் `(groupId, memberId)` uniqueness/allowed slot count constraint சேர்க்க வேண்டும்.
- [ ] எல்லா money inputs integer paise, positive, safe integer range என்று validate செய்ய வேண்டும்.
- [ ] `chitValue` duration-ஆல் முழுமையாக divide ஆகாதபோது rounding/remainder business rule define செய்ய வேண்டும்.

## P1 - Backend API மற்றும் Validation

- [ ] Group create/update endpoints-ல் required fields, types, positive ranges மற்றும் valid dates validate செய்ய வேண்டும்.
- [ ] Group member add செய்யும் முன் group exists என்பதை check செய்ய வேண்டும்.
- [ ] Member exists மற்றும் role `MEMBER` என்பதை subscription creation முன்னால் check செய்ய வேண்டும்.
- [ ] Group status `DRAFT` இல்லாதபோது member/slot changes allow செய்யலாமா என்ற rule enforce செய்ய வேண்டும்.
- [ ] Auction நடத்தும் முன் group `ACTIVE` என்பதை check செய்ய வேண்டும்.
- [ ] Auction installments சரியான chronological order-ல் மட்டுமே finalize செய்ய validation சேர்க்க வேண்டும்.
- [ ] Winning bid upper limit மற்றும் commission percentage allowed range validate செய்ய வேண்டும்.
- [ ] Payment member அந்த installment group-ல் subscribed என்பதை validate செய்ய வேண்டும்.
- [ ] Payment mode மற்றும் UPI/bank reference requirements validate செய்ய வேண்டும்.
- [ ] `/payments?groupId=` filter-ஐ உண்மையில் implement செய்ய வேண்டும்.
- [ ] Role-based data scope சேர்க்க வேண்டும்: member தன்னுடைய data மட்டும்; agent assigned groups/members மட்டும் பார்க்க வேண்டும்.
- [ ] `/groups/:id/members` response-ல் `passwordHash` எப்போதும் expose ஆகாதபடி exclude செய்ய வேண்டும்.
- [ ] Update endpoints-ல் mass assignment தவிர்த்து allowed fields மட்டும் accept செய்ய வேண்டும்.
- [ ] Consistent error response format எல்லா endpoints-லும் பயன்படுத்த வேண்டும்.
- [ ] Request validation library மற்றும் centralized error middleware சேர்க்க வேண்டும்.
- [ ] Pagination, sorting மற்றும் safe query limits list endpoints-க்கு சேர்க்க வேண்டும்.
- [ ] SQLite-இலிருந்து production database migration strategy முடிவு செய்ய வேண்டும்.

## P1 - Android App Architecture மற்றும் Data Flow

- [ ] Retrofit API models-க்கு raw `Map<String, Object>` பதிலாக typed request/response DTOs உருவாக்க வேண்டும்.
- [ ] Backend JSON field names மற்றும் Room entity fields ஒரே contract-க்கு map செய்ய வேண்டும்.
- [ ] Repository operations-க்கு consistent result/loading/error state பயன்படுத்த வேண்டும்.
- [ ] Java/Kotlin mixed code-ல் duplicate screen/activity flows இருக்கிறதா audit செய்து ஒரே navigation flow வைத்திருக்க வேண்டும்.
- [ ] Main/login navigation மற்றும் persisted session validation சரி செய்ய வேண்டும்.
- [ ] App launch-ல் stored access token validity check மற்றும் refresh flow இயக்க வேண்டும்.
- [ ] Logout-ல் tokens, sensitive cached state மற்றும் scheduled sync state சரியாக clear செய்ய வேண்டும்.
- [ ] UI thread-ல் database/network work நடக்காததை உறுதி செய்ய வேண்டும்.
- [ ] Retrofit/server errors பயனருக்கு Tamil/English friendly messages-ஆக map செய்ய வேண்டும்.
- [ ] Release configuration-ல் debug-only code/logs இல்லாததை உறுதி செய்ய வேண்டும்.

## P1 - Offline-First Sync

- [ ] Current mock `SyncWorker` sleep logic-ஐ real sync implementation-ஆக மாற்ற வேண்டும்.
- [ ] Local changes-க்கு `syncStatus`, `updatedAt`, client operation ID போன்ற metadata சேர்க்க வேண்டும்.
- [ ] Pending local writes-ஐ server-க்கு idempotently upload செய்ய வேண்டும்.
- [ ] Server updates-ஐ incremental cursor/timestamp மூலம் download செய்ய வேண்டும்.
- [ ] Successful upload பிறகே local row synced என்று mark செய்ய வேண்டும்.
- [ ] Network/server failure-க்கு retry மற்றும் permanent validation failure-க்கு user-visible state சேர்க்க வேண்டும்.
- [ ] Duplicate sync requests தவிர்க்க stable idempotency keys பயன்படுத்த வேண்டும்.
- [ ] Same record local/server இரண்டிலும் மாறினால் conflict-resolution rule define செய்ய வேண்டும்.
- [ ] Financial records/locked auctions-க்கு silent last-write-wins பயன்படுத்தக்கூடாது.
- [ ] Sync status, last synced time மற்றும் failed item count UI-ல் காட்ட வேண்டும்.

## P1 - Database மற்றும் Migrations

- [ ] Sequelize model changes-க்கு versioned migrations பயன்படுத்த வேண்டும்; startup `sequelize.sync()` மட்டும் நம்பக்கூடாது.
- [ ] Existing SQLite data மீது migrations test செய்ய வேண்டும்.
- [ ] Room versions 1-5 இலிருந்து current version வருவதற்கான migration path இருக்கிறதா verify செய்ய வேண்டும்.
- [ ] Room schema export enable செய்து migration schemas version control-ல் வைக்க வேண்டும்.
- [ ] Foreign keys மற்றும் delete/update behavior (`RESTRICT`, `CASCADE`) தெளிவாக define செய்ய வேண்டும்.
- [ ] Database indexes group, installment, member, payment report queries-க்கு சேர்க்க வேண்டும்.
- [ ] Repository root/backend folder-ல் real customer database commit ஆகாதபடி `.gitignore` மற்றும் deployment storage configure செய்ய வேண்டும்.

## P2 - Reports, Receipts மற்றும் Notifications

- [ ] Dashboard/report calculations seeded realistic data-க்கு verify செய்ய வேண்டும்.
- [ ] Daily tally, dues, collections, P&L மற்றும் settlement reports-க்கு automated tests எழுத வேண்டும்.
- [ ] Report date filters/timezone handling consistent ஆக மாற்ற வேண்டும்.
- [ ] PDF receipts secure storage-ல் உருவாக்கி authenticated download endpoint வழங்க வேண்டும்.
- [ ] Mock hard-coded receipt domain-ஐ configured public URL-ஆக மாற்ற வேண்டும்.
- [ ] WhatsApp/SMS mock service-க்கு பதிலாக provider integration அல்லது Android share intent flow முடிக்க வேண்டும்.
- [ ] Notification delivery status, retry மற்றும் failure audit log சேர்க்க வேண்டும்.
- [ ] Receipt numbering uniqueness மற்றும் financial-year numbering rule define செய்ய வேண்டும்.

## P2 - UI/UX மற்றும் Localization

- [ ] எல்லா visible strings-யும் resources-க்கு move செய்து English/Tamil translations complete செய்ய வேண்டும்.
- [ ] தற்போது encoding corruption உள்ள Tamil comments/text/resources இருக்கிறதா scan செய்து UTF-8 normalize செய்ய வேண்டும்.
- [ ] Currency display எப்போதும் paise-to-rupee formatter வழியாக வர வேண்டும்.
- [ ] Loading, empty, offline, error மற்றும் retry states எல்லா screens-க்கும் சேர்க்க வேண்டும்.
- [ ] Form validation messages field அருகில் காட்ட வேண்டும்.
- [ ] Accessibility labels, touch target sizes, contrast மற்றும் font scaling test செய்ய வேண்டும்.
- [ ] Small phone, tablet, landscape மற்றும் Tamil long-text layouts test செய்ய வேண்டும்.
- [ ] Destructive/financial finalization actions-க்கு confirmation மற்றும் summary screen சேர்க்க வேண்டும்.

## P1 - Testing மற்றும் Release Readiness

- [ ] Kasaru edge cases: zero bid, commission boundary, rounding remainder, large values ஆகியவற்றுக்கு unit tests சேர்க்க வேண்டும்.
- [ ] Auth, roles, groups, subscriptions, auctions, payments மற்றும் payouts backend integration tests எழுத வேண்டும்.
- [ ] Concurrent auction/payment requests-க்கு tests எழுத வேண்டும்.
- [ ] Room DAO மற்றும் migration tests எழுத வேண்டும்.
- [ ] Android repository tests mock API responses உடன் எழுத வேண்டும்.
- [ ] Login-to-payment முக்கிய user flow instrumentation/UI test உருவாக்க வேண்டும்.
- [ ] Debug மற்றும் release APK builds CI-ல் run செய்ய வேண்டும்.
- [ ] Lint, unit tests மற்றும் backend tests CI checks-ஆக configure செய்ய வேண்டும்.
- [ ] Release signing, ProGuard/R8 மற்றும் secret management configure செய்ய வேண்டும்.
- [ ] Restore/backup, offline recovery மற்றும் app upgrade scenarios manually verify செய்ய வேண்டும்.

## Definition of Done

ஒரு feature complete என்று mark செய்ய முன்:

- [ ] Android debug மற்றும் release build இரண்டும் pass ஆக வேண்டும்.
- [ ] Backend automated tests pass ஆக வேண்டும்.
- [ ] Android unit/Room tests pass ஆக வேண்டும்.
- [ ] English மற்றும் Tamil UI இரண்டிலும் verify செய்திருக்க வேண்டும்.
- [ ] Online, offline மற்றும் reconnect flows test செய்திருக்க வேண்டும்.
- [ ] Role/access-control checks verify செய்திருக்க வேண்டும்.
- [ ] Financial calculations மற்றும் database side effects verify செய்திருக்க வேண்டும்.
- [ ] Sensitive data logs/responses-ல் leak ஆகவில்லை என்பதை verify செய்திருக்க வேண்டும்.

