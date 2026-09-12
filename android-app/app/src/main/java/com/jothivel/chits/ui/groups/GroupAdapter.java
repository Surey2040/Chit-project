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
import com.jothivel.chits.data.local.entity.ChitGroupEntity;
import com.jothivel.chits.utils.CurrencyUtils;

public class GroupAdapter extends ListAdapter<ChitGroupEntity, GroupAdapter.GroupViewHolder> {

    private static final DiffUtil.ItemCallback<ChitGroupEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChitGroupEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChitGroupEntity oldItem, @NonNull ChitGroupEntity newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChitGroupEntity oldItem, @NonNull ChitGroupEntity newItem) {
                    return oldItem.id.equals(newItem.id)
                            && oldItem.chitValue == newItem.chitValue
                            && safeEquals(oldItem.status, newItem.status)
                            && oldItem.durationMonths == newItem.durationMonths;
                }

                private boolean safeEquals(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ChitGroupEntity group);
    }

    public GroupAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setGroups(java.util.List<ChitGroupEntity> groups) {
        submitList(groups);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        ChitGroupEntity currentGroup = getItem(position);
        holder.tvRegisterNo.setText(currentGroup.registerNo);
        holder.tvStatus.setText(currentGroup.status);
        holder.tvChitValue.setText(CurrencyUtils.formatInr(currentGroup.chitValue / 100));
        holder.tvDuration.setText(currentGroup.durationMonths + " Months");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentGroup);
            }
        });
    }

    @Override
    public int getItemCount() {
        return getCurrentList().size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        private TextView tvRegisterNo;
        private TextView tvStatus;
        private TextView tvChitValue;
        private TextView tvDuration;

        public GroupViewHolder(View itemView) {
            super(itemView);
            tvRegisterNo = itemView.findViewById(R.id.tvRegisterNo);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvChitValue = itemView.findViewById(R.id.tvChitValue);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }
    }
}
