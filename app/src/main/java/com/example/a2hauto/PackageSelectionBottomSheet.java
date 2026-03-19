package com.example.a2hauto;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.FeeCommission;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class PackageSelectionBottomSheet {

    private PackageSelectionBottomSheet() {
    }

    public interface OnPackageSelectedListener {
        void onPackageSelected(String packageId);
    }

    public static void show(Context context, OnPackageSelectedListener listener) {
        if (context == null) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_select_package, null, false);
        dialog.setContentView(view);

        RecyclerView rvUserPackages = view.findViewById(R.id.rvUserPackages);
        View layoutEmptyPackages = view.findViewById(R.id.layoutEmptyPackages);
        MaterialButton btnBuyNow = view.findViewById(R.id.btnBuyNow);

        rvUserPackages.setLayoutManager(new LinearLayoutManager(context));
        UserPackageAdapter adapter = new UserPackageAdapter(new ArrayList<>(), selected -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onPackageSelected(selected.getFeeId());
            }
        });
        rvUserPackages.setAdapter(adapter);

        btnBuyNow.setOnClickListener(v -> {
            context.startActivity(new Intent(context, PlanActivity.class));
            dialog.dismiss();
        });

        AuthSessionManager authSessionManager = new AuthSessionManager(context);
        String token = authSessionManager.getAuthToken();
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        apiService.getUserPackages("Bearer " + token).enqueue(new Callback<ApiResponse<List<FeeCommission>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FeeCommission>>> call,
                                   @NonNull Response<ApiResponse<List<FeeCommission>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    rvUserPackages.setVisibility(View.GONE);
                    layoutEmptyPackages.setVisibility(View.VISIBLE);
                    return;
                }

                List<FeeCommission> userPackages = response.body().getData();
                if (userPackages == null || userPackages.isEmpty()) {
                    rvUserPackages.setVisibility(View.GONE);
                    layoutEmptyPackages.setVisibility(View.VISIBLE);
                } else {
                    layoutEmptyPackages.setVisibility(View.GONE);
                    rvUserPackages.setVisibility(View.VISIBLE);
                    adapter.submitList(userPackages);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<FeeCommission>>> call, @NonNull Throwable t) {
                rvUserPackages.setVisibility(View.GONE);
                layoutEmptyPackages.setVisibility(View.VISIBLE);
                Toast.makeText(context, "Không thể tải danh sách gói", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private static final class UserPackageAdapter extends RecyclerView.Adapter<UserPackageAdapter.PackageViewHolder> {

        private final List<FeeCommission> packages;
        private final OnPackageClickListener onPackageClickListener;

        interface OnPackageClickListener {
            void onPackageClick(FeeCommission item);
        }

        UserPackageAdapter(List<FeeCommission> packages, OnPackageClickListener onPackageClickListener) {
            this.packages = packages;
            this.onPackageClickListener = onPackageClickListener;
        }

        void submitList(List<FeeCommission> newData) {
            packages.clear();
            if (newData != null) {
                packages.addAll(newData);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new PackageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
            FeeCommission item = packages.get(position);
            holder.tvName.setText(item.getFeeName());
            holder.tvDetail.setText(String.format(
                    Locale.getDefault(),
                    "%s | %,.0f VNĐ | %d ngày",
                    item.getDescription() == null ? "Gói đăng tin" : item.getDescription(),
                    item.getAmount(),
                    item.getPackageDurationDays()
            ));
            holder.itemView.setOnClickListener(v -> onPackageClickListener.onPackageClick(item));
        }

        @Override
        public int getItemCount() {
            return packages.size();
        }

        static final class PackageViewHolder extends RecyclerView.ViewHolder {

            private final TextView tvName;
            private final TextView tvDetail;

            PackageViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(android.R.id.text1);
                tvDetail = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}