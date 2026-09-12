package com.jothivel.chits.ui.groups;

import android.os.Bundle;

import com.jothivel.chits.ui.base.BaseActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.jothivel.chits.R;
import com.jothivel.chits.ui.components.AppBottomNavigationBarKt;

public class GroupListActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);
        
        topAppBar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView and ViewModel
        androidx.recyclerview.widget.RecyclerView rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        GroupAdapter adapter = new GroupAdapter(group -> {
            android.content.Intent intent = new android.content.Intent(this, GroupDetailActivity.class);
            intent.putExtra("GROUP_ID", group.id);
            startActivity(intent);
        });
        rvGroups.setAdapter(adapter);

        GroupViewModel groupViewModel = new androidx.lifecycle.ViewModelProvider(this).get(GroupViewModel.class);
        groupViewModel.getAllGroups().observe(this, adapter::setGroups);

        androidx.compose.ui.platform.ComposeView bottomNav = findViewById(R.id.bottomNavComposeView);
        AppBottomNavigationBarKt.setupAppBottomNavigation(bottomNav, "Chits");
    }
}
