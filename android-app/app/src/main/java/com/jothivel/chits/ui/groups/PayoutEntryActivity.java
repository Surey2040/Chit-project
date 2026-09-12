package com.jothivel.chits.ui.groups;

import android.os.Bundle;
import android.widget.Toast;

import com.jothivel.chits.ui.base.BaseActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.jothivel.chits.R;

public class PayoutEntryActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payout_entry);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());

        TextInputEditText etPayoutAmount = findViewById(R.id.etPayoutAmount);
        MaterialButton btnUploadProof = findViewById(R.id.btnUploadProof);
        MaterialButton btnSavePayout = findViewById(R.id.btnSavePayout);

        btnUploadProof.setOnClickListener(v -> {
            Toast.makeText(this, "Camera intent will open here", Toast.LENGTH_SHORT).show();
        });

        btnSavePayout.setOnClickListener(v -> {
            String amount = etPayoutAmount.getText().toString();
            if (amount.isEmpty()) {
                Toast.makeText(this, "Please enter payout amount", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Payout Recorded Offline", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
