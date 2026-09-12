package com.jothivel.chits.ui.collections;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.jothivel.chits.data.local.entity.PaymentEntity;
import com.jothivel.chits.data.repository.PaymentRepository;

import java.util.List;

public class PaymentViewModel extends AndroidViewModel {
    private PaymentRepository paymentRepository;

    public PaymentViewModel(@NonNull Application application) {
        super(application);
        paymentRepository = new PaymentRepository(application);
    }

    public void recordPayment(String memberId, String groupId, int amountPaid, String mode, PaymentRepository.PaymentCallback callback) {
        paymentRepository.recordPayment(memberId, groupId, amountPaid, mode, callback);
    }

    public LiveData<List<PaymentEntity>> getPaymentsByMember(String memberId) {
        return paymentRepository.getPaymentsByMember(memberId);
    }
}
