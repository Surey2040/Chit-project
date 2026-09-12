package com.jothivel.chits.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.jothivel.chits.R;
import com.jothivel.chits.data.local.entity.InstallmentEntity;
import com.jothivel.chits.utils.CurrencyUtils;

public class InstallmentAdapter extends ListAdapter<InstallmentEntity, InstallmentAdapter.InstallmentViewHolder> {

    private static final DiffUtil.ItemCallback<InstallmentEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<InstallmentEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull InstallmentEntity oldItem, @NonNull InstallmentEntity newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull InstallmentEntity oldItem, @NonNull InstallmentEntity newItem) {
                    return oldItem.id.equals(newItem.id)
                            && oldItem.installmentNo == newItem.installmentNo
                            && oldItem.baseAmount == newItem.baseAmount
                            && safeEquals(oldItem.kasaruAmount, newItem.kasaruAmount)
                            && safeEquals(oldItem.payoutAmount, newItem.payoutAmount);
                }

                private boolean safeEquals(Object a, Object b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    public InstallmentAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setInstallments(java.util.List<InstallmentEntity> installments) {
        submitList(installments);
    }

    @NonNull
    @Override
    public InstallmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_installment, parent, false);
        return new InstallmentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InstallmentViewHolder holder, int position) {
        InstallmentEntity inst = getItem(position);
        holder.tvInstallmentNo.setText(String.valueOf(inst.installmentNo));
        holder.tvBaseAmount.setText(CurrencyUtils.formatInr(inst.baseAmount / 100));

        if (inst.kasaruAmount != null) {
            holder.tvKasaru.setText(CurrencyUtils.formatInr(inst.kasaruAmount / 100));
        } else {
            holder.tvKasaru.setText("-");
        }

        if (inst.payoutAmount != null) {
            holder.tvPayout.setText(CurrencyUtils.formatInr(inst.payoutAmount / 100));
        } else {
            holder.tvPayout.setText("-");
        }
    }

    static class InstallmentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvInstallmentNo;
        private TextView tvBaseAmount;
        private TextView tvKasaru;
        private TextView tvPayout;

        public InstallmentViewHolder(View itemView) {
            super(itemView);
            tvInstallmentNo = itemView.findViewById(R.id.tvInstallmentNo);
            tvBaseAmount = itemView.findViewById(R.id.tvBaseAmount);
            tvKasaru = itemView.findViewById(R.id.tvKasaru);
            tvPayout = itemView.findViewById(R.id.tvPayout);
        }
    }
}
