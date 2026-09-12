package com.jothivel.chits.data.repository;

import android.app.Application;

import com.jothivel.chits.data.remote.ApiClient;
import com.jothivel.chits.data.remote.ApiService;
import com.jothivel.chits.utils.TokenManager;
import com.jothivel.chits.data.local.AppDatabase;
import com.jothivel.chits.data.local.dao.InstallmentDao;
import com.jothivel.chits.data.local.dao.PaymentDao;
import com.jothivel.chits.data.local.entity.InstallmentEntity;
import com.jothivel.chits.data.local.entity.PaymentEntity;
import com.jothivel.chits.data.local.dao.ActivityLogDao;
import com.jothivel.chits.data.local.entity.ActivityLogEntity;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class PaymentRepository {

    private ApiService apiService;
    private PaymentDao paymentDao;
    private InstallmentDao installmentDao;
    private ActivityLogDao activityLogDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PaymentRepository(Application application) {
        TokenManager tokenManager = new TokenManager(application);
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);
        AppDatabase db = AppDatabase.getDatabase(application);
        paymentDao = db.paymentDao();
        installmentDao = db.installmentDao();
        activityLogDao = db.activityLogDao();
    }

    public void recordPayment(String memberId, String groupId, int amountPaid, String mode, final PaymentCallback callback) {
        String receiptNo = "RCT-" + System.currentTimeMillis();
        long paidAt = System.currentTimeMillis();
        
        executor.execute(() -> {
            try {
                List<InstallmentEntity> installments = installmentDao.getInstallmentsForGroupSync(groupId);
                
                int remainingAmount = amountPaid;

                for (InstallmentEntity inst : installments) {
                    if (remainingAmount <= 0) break;
                    
                    int kasaru = inst.kasaruAmount != null ? inst.kasaruAmount : 0;
                    int totalDueForInst = inst.baseAmount - kasaru;
                    
                    List<PaymentEntity> existingPayments = paymentDao.getPaymentsForInstallmentSync(memberId, groupId, String.valueOf(inst.installmentNo));
                    int alreadyPaid = existingPayments.stream().mapToInt(p -> (int)p.amountPaid).sum();
                    
                    int pendingDue = totalDueForInst - alreadyPaid;
                    
                    if (pendingDue > 0) {
                        int amountToAllocate = Math.min(remainingAmount, pendingDue);
                        
                        PaymentEntity entity = new PaymentEntity();
                        entity.id = java.util.UUID.randomUUID().toString();
                        entity.memberId = memberId;
                        entity.groupId = groupId;
                        entity.installmentId = String.valueOf(inst.installmentNo);
                        entity.amountPaid = amountToAllocate;
                        entity.mode = mode;
                        entity.receiptNo = receiptNo;
                        entity.paidAt = paidAt;
                        entity.status = (amountToAllocate >= pendingDue) ? "PAID" : "PARTIAL";
                        
                        paymentDao.insertPayment(entity);
                        
                        remainingAmount -= amountToAllocate;
                    }
                }
                
                // If there's still remaining amount, apply it as ADVANCE to the last installment
                if (remainingAmount > 0 && !installments.isEmpty()) {
                    InstallmentEntity lastInst = installments.get(installments.size() - 1);
                    PaymentEntity entity = new PaymentEntity();
                    entity.id = java.util.UUID.randomUUID().toString();
                    entity.memberId = memberId;
                    entity.groupId = groupId;
                    entity.installmentId = String.valueOf(lastInst.installmentNo);
                    entity.amountPaid = remainingAmount;
                    entity.mode = mode;
                    entity.receiptNo = receiptNo;
                    entity.paidAt = paidAt;
                    entity.status = "ADVANCE";
                    paymentDao.insertPayment(entity);
                }
                
                ActivityLogEntity log = new ActivityLogEntity(
                        java.util.UUID.randomUUID().toString(),
                        "PAYMENT_RECORDED",
                        "Payment Collected",
                        "A payment of ₹" + (amountPaid / 100) + " was collected from member.",
                        System.currentTimeMillis()
                );
                activityLogDao.insertLog(log);

                mainHandler.post(() -> callback.onSuccess(receiptNo));
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("Failed to save payment: " + e.getMessage()));
            }
        });
    }
    
    public LiveData<List<PaymentEntity>> getPaymentsByMember(String memberId) {
        return paymentDao.getPaymentsByMember(memberId);
    }

    public interface PaymentCallback {
        void onSuccess(String receiptNo);
        void onError(String message);
    }
}
