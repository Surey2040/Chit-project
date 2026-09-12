package com.jothivel.chits.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.jothivel.chits.data.local.dao.GroupDao;
import com.jothivel.chits.data.local.dao.MemberDao;
import com.jothivel.chits.data.local.entity.ChitGroupEntity;
import com.jothivel.chits.data.local.entity.MemberEntity;
import com.jothivel.chits.data.local.entity.InstallmentEntity;

import com.jothivel.chits.data.local.dao.PaymentDao;
import com.jothivel.chits.data.local.entity.PaymentEntity;
import com.jothivel.chits.data.local.entity.ActivityLogEntity;
import com.jothivel.chits.data.local.dao.ActivityLogDao;
import com.jothivel.chits.data.local.entity.ChitMembershipEntity;
import com.jothivel.chits.data.local.entity.CollectionReceiptEntity;
import com.jothivel.chits.data.local.dao.MembershipDao;
import com.jothivel.chits.data.local.dao.CollectionReceiptDao;
import com.jothivel.chits.data.local.entity.FinancialTransactionEntity;
import com.jothivel.chits.data.local.dao.FinancialTransactionDao;

@Database(entities = {ChitGroupEntity.class, MemberEntity.class, InstallmentEntity.class, PaymentEntity.class, ActivityLogEntity.class, ChitMembershipEntity.class, CollectionReceiptEntity.class, FinancialTransactionEntity.class}, version = 13, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    public abstract GroupDao groupDao();
    public abstract MemberDao memberDao();
    public abstract PaymentDao paymentDao();
    public abstract com.jothivel.chits.data.local.dao.InstallmentDao installmentDao();
    public abstract ActivityLogDao activityLogDao();
    public abstract MembershipDao membershipDao();
    public abstract CollectionReceiptDao collectionReceiptDao();
    public abstract FinancialTransactionDao financialTransactionDao();
    
    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE payments ADD COLUMN groupId TEXT");
        }
    };

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE chit_groups ADD COLUMN name TEXT");
        }
    };

    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `activity_logs` (`id` TEXT NOT NULL, `actionType` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        }
    };

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `chit_memberships` (`id` TEXT NOT NULL, `memberId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `ticketNo` TEXT, `installmentAmountPaise` INTEGER NOT NULL, `joiningDate` TEXT, `dueDate` TEXT, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_chit_memberships_memberId_groupId` ON `chit_memberships` (`memberId`, `groupId`)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_chit_memberships_groupId_ticketNo` ON `chit_memberships` (`groupId`, `ticketNo`)");
            database.execSQL("INSERT OR IGNORE INTO `chit_memberships` (`id`,`memberId`,`groupId`,`ticketNo`,`installmentAmountPaise`,`joiningDate`,`dueDate`,`isActive`) SELECT `id` || ':' || `selectedChitId`, `id`, `selectedChitId`, `ticketNo`, CAST(COALESCE(NULLIF(`installmentAmount`,''),'0') AS INTEGER) * 100, `joiningDate`, `dueDate`, `isActive` FROM `members` WHERE `selectedChitId` IS NOT NULL AND `selectedChitId` != ''");
            database.execSQL("CREATE TABLE IF NOT EXISTS `collection_receipts` (`id` TEXT NOT NULL, `requestId` TEXT NOT NULL, `receiptNo` TEXT NOT NULL, `memberId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `amountPaidPaise` INTEGER NOT NULL, `mode` TEXT NOT NULL, `referenceNo` TEXT, `notes` TEXT, `businessDate` TEXT NOT NULL, `paidAt` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_collection_receipts_requestId` ON `collection_receipts` (`requestId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_receipts_memberId_groupId_paidAt` ON `collection_receipts` (`memberId`, `groupId`, `paidAt`)");
        }
    };

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `financial_transactions` (`id` TEXT NOT NULL, `requestId` TEXT NOT NULL, `type` TEXT NOT NULL, `memberId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `amountPaise` INTEGER NOT NULL, `mode` TEXT NOT NULL, `referenceNo` TEXT, `notes` TEXT, `occurredAt` INTEGER NOT NULL, `status` TEXT NOT NULL, `reversalReason` TEXT, `reversedAt` INTEGER, PRIMARY KEY(`id`))");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_transactions_requestId` ON `financial_transactions` (`requestId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_transactions_memberId_groupId_occurredAt` ON `financial_transactions` (`memberId`, `groupId`, `occurredAt`)");
        }
    };

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_memberId_groupId_installmentId` ON `payments` (`memberId`,`groupId`,`installmentId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_paidAt` ON `payments` (`paidAt`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_groupId_installmentNo` ON `installments` (`groupId`,`installmentNo`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_receipts_businessDate_status` ON `collection_receipts` (`businessDate`,`status`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_chit_memberships_groupId_isActive` ON `chit_memberships` (`groupId`,`isActive`)");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE payments ADD COLUMN collectedBy TEXT");
            database.execSQL("ALTER TABLE payments ADD COLUMN collectedByAgentId TEXT");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "jothivel_chits_database")
                            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /** Close the current Room connection and allow a restored database to reopen cleanly. */
    public static synchronized void closeAndReset() {
        if (INSTANCE != null) {
            if (INSTANCE.isOpen()) {
                INSTANCE.close();
            }
            INSTANCE = null;
        }
    }
}
