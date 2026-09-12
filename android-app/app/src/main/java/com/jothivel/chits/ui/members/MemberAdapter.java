package com.jothivel.chits.ui.members;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.jothivel.chits.R;
import com.jothivel.chits.data.local.entity.MemberEntity;

public class MemberAdapter extends ListAdapter<MemberEntity, MemberAdapter.MemberViewHolder> {

    private static final DiffUtil.ItemCallback<MemberEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MemberEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull MemberEntity oldItem, @NonNull MemberEntity newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull MemberEntity oldItem, @NonNull MemberEntity newItem) {
                    return oldItem.id.equals(newItem.id)
                            && safeEquals(oldItem.name, newItem.name)
                            && safeEquals(oldItem.phone, newItem.phone)
                            && oldItem.isActive == newItem.isActive;
                }

                private boolean safeEquals(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    public MemberAdapter() {
        super(DIFF_CALLBACK);
    }

    // Keep setMembers() for backward compatibility with existing LiveData observers
    public void setMembers(java.util.List<MemberEntity> members) {
        submitList(members);
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberEntity currentMember = getItem(position);
        holder.tvName.setText(currentMember.name);
        holder.tvPhone.setText(currentMember.phone);

        if (currentMember.isActive) {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(holder.itemView.getResources().getColor(R.color.statusPaid));
        } else {
            holder.tvStatus.setText("Blocked");
            holder.tvStatus.setTextColor(holder.itemView.getResources().getColor(R.color.statusOverdue));
        }

        // Open Member Profile on tap
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, MemberProfileActivity.class);
            intent.putExtra(MemberProfileActivity.EXTRA_MEMBER_ID, currentMember.id);
            context.startActivity(intent);
        });
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvPhone;
        private TextView tvStatus;

        public MemberViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
