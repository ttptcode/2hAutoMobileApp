package com.example.a2hauto;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {

    private WebView webViewPayment;
    private ProgressBar progressBar;
    private boolean isPaymentProcessed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        webViewPayment = findViewById(R.id.webViewPayment);
        progressBar = findViewById(R.id.progressBarPayment);

        // Check if this is a callback intent from VNPay
        Uri deepLink = getIntent().getData();
        if (deepLink != null && "yourapp".equals(deepLink.getScheme())) {
            handlePaymentCallback(deepLink);
            return;
        }

        // Lấy payment URL từ Intent
        String paymentUrl = getIntent().getStringExtra("paymentUrl");

        if (paymentUrl == null || paymentUrl.isEmpty()) {
            Toast.makeText(this, "Không có URL thanh toán", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupWebView();
        webViewPayment.loadUrl(paymentUrl);
    }

    private void handlePaymentCallback(Uri deepLink) {
        // Lấy parameters từ callback URL
        String status = deepLink.getQueryParameter("status");
        String paymentId = deepLink.getQueryParameter("paymentId");
        String transactionNo = deepLink.getQueryParameter("transactionNo");
        String amount = deepLink.getQueryParameter("amount");
        String errorMessage = deepLink.getQueryParameter("message");

        android.util.Log.d("PaymentActivity", "Callback received - Status: " + status + ", PaymentId: " + paymentId + ", Error: " + errorMessage);

        try {
            // Check if payment was successful
            if ("00".equals(status) || "0".equals(status)) {
                // ✅ Thanh toán thành công
                Toast.makeText(this, "✓ Thanh toán thành công!", Toast.LENGTH_LONG).show();
                
                // Save payment info
                savePaymentInfo(paymentId, amount, "SUCCESS", transactionNo);
                
                setResult(RESULT_OK);
                isPaymentProcessed = true;
                
                // Delay để user thấy toast message rồi mới close
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    this::finish,
                    1500
                );
            } else if ("ERROR".equals(status)) {
                String displayMsg = errorMessage != null && !errorMessage.isEmpty()
                    ? "Lỗi: " + errorMessage 
                    : "Lỗi xử lý thanh toán";
                    
                Toast.makeText(this, " " + displayMsg, Toast.LENGTH_LONG).show();
                savePaymentInfo(paymentId, amount, "ERROR: " + errorMessage, transactionNo);
                setResult(RESULT_CANCELED);
                isPaymentProcessed = true;
                
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    this::finish,
                    2000
                );
            } else if ("INVALID_SIGNATURE".equals(status)) {
                // ❌ Invalid signature - security issue
                Toast.makeText(this, " Xác thực giao dịch thất bại", Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                isPaymentProcessed = true;
                
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    this::finish,
                    2000
                );
            } else {
                String displayMsg = "Thanh toán thành công";
                
                switch (status) {
                    case "99":
                        displayMsg = "Giao dịch bị hủy";
                        break;
                    case "01":
                        displayMsg = "Ngân hàng từ chối";
                        break;
                    case "02":
                        displayMsg = "Thẻ bị khóa";
                        break;
                    case "24":
                        displayMsg = "Giao dịch thất bại";
                        break;
                    case "FAILED":
                        displayMsg = "Xử lý giao dịch thất bại";
                        break;
                }
                
                Toast.makeText(this, "✗ " + displayMsg, Toast.LENGTH_LONG).show();
                savePaymentInfo(paymentId, amount, "FAILED: " + status, transactionNo);
                setResult(RESULT_CANCELED);
                isPaymentProcessed = true;
                
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    this::finish,
                    1500
                );
            }
        } catch (Exception e) {
            android.util.Log.e("PaymentActivity", "Error handling callback", e);
            Toast.makeText(this, "Lỗi xử lý kết quả: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void savePaymentInfo(String paymentId, String amount, String status, String transactionNo) {
        // Optional: Save to SharedPreferences
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("payment_info", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_payment_id", paymentId != null ? paymentId : "");
            editor.putString("last_payment_amount", amount != null ? amount : "");
            editor.putString("last_payment_status", status);
            editor.putString("last_transaction_no", transactionNo != null ? transactionNo : "");
            editor.putLong("last_payment_time", System.currentTimeMillis());
            editor.apply();
            
            android.util.Log.d("PaymentActivity", "Payment info saved: " + paymentId + " - " + status);
        } catch (Exception e) {
            android.util.Log.e("PaymentActivity", "Error saving payment info", e);
        }
    }

    private void setupWebView() {
        // Cho phép JavaScript
        webViewPayment.getSettings().setJavaScriptEnabled(true);
        webViewPayment.getSettings().setDomStorageEnabled(true);
        webViewPayment.getSettings().setDatabaseEnabled(true);

        // Xử lý loading
        webViewPayment.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
                if (newProgress < 100) {
                    progressBar.setVisibility(android.view.View.VISIBLE);
                } else {
                    progressBar.setVisibility(android.view.View.GONE);
                }
            }
        });

        // Xử lý navigation
        webViewPayment.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Kiểm tra nếu là custom scheme callback từ backend
                if (url.startsWith("yourapp://payment-callback")) {
                    android.util.Log.d("PaymentActivity", "Deep link detected: " + url);
                    handlePaymentCallback(Uri.parse(url));
                    return true;
                }
                
                // Kiểm tra nếu là return URL từ VNPay (old fallback)
                if (url.contains("vnpay_response") || url.contains("return_url")) {
                    handlePaymentResponse(url);
                    return true;
                }
                
                // Cho phép các URL khác được load bình thường
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(android.view.View.GONE);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Toast.makeText(PaymentActivity.this, "Lỗi tải trang: " + description, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(android.view.View.GONE);
            }
        });
    }

    private void handlePaymentResponse(String url) {
        // Parse URL để lấy transaction status (old fallback method)
        try {
            // Kiểm tra các parameter: vnp_ResponseCode, vnp_TransactionStatus
            if (url.contains("vnp_ResponseCode=00") || url.contains("vnp_TransactionStatus=0")) {
                // Thanh toán thành công
                Toast.makeText(this, "✓ Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
            } else {
                // Thanh toán thất bại
                Toast.makeText(this, "✗ Thanh toán thất bại hoặc bị hủy", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
        }

        // Quay lại màn hình trước
        isPaymentProcessed = true;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            this::finish,
            1000
        );
    }

    @Override
    public void onBackPressed() {
        if (webViewPayment.canGoBack()) {
            webViewPayment.goBack();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}







