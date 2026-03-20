package com.example.a2hauto.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.R;
import com.example.a2hauto.model.FeeCommission;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {
    private List<FeeCommission> plans;
    private final OnPlanClickListener listener;

    public interface OnPlanClickListener {
        void onPlanClick(FeeCommission plan);
    }

    public PlanAdapter(List<FeeCommission> plans, OnPlanClickListener listener) {
        this.plans = plans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        FeeCommission plan = plans.get(position);

        holder.tvPlanName.setText(plan.getFeeName());
        holder.tvPlanSubtitle.setText(plan.getDescription());
        holder.tvPlanPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", plan.getAmount()));
        holder.tvFeatureOne.setText("Tối đa " + plan.getMaxListings() + " tin đăng");
        holder.tvFeatureTwo.setText("Thời hạn sử dụng " + plan.getPackageDurationDays() + " ngày");

        holder.btnChoosePlan.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlanClick(plan);
            }
        });
    }

    @Override
    public int getItemCount() {
        return plans == null ? 0 : plans.size();
    }

    public void setPlans(List<FeeCommission> plans) {
        this.plans = plans;
        notifyDataSetChanged();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlanName;
        TextView tvPlanSubtitle;
        TextView tvPlanPrice;
        TextView tvFeatureOne;
        TextView tvFeatureTwo;
        MaterialButton btnChoosePlan;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlanName = itemView.findViewById(R.id.tvPlanName);
            tvPlanSubtitle = itemView.findViewById(R.id.tvPlanSubtitle);
            tvPlanPrice = itemView.findViewById(R.id.tvPlanPrice);
            tvFeatureOne = itemView.findViewById(R.id.tvFeatureOne);
            tvFeatureTwo = itemView.findViewById(R.id.tvFeatureTwo);
            btnChoosePlan = itemView.findViewById(R.id.btnChoosePlan);
        }
    }
}
