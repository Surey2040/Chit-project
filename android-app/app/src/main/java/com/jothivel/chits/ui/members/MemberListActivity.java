package com.jothivel.chits.ui.members;

import android.os.Bundle;

import com.jothivel.chits.ui.base.BaseActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jothivel.chits.R;

public class MemberListActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());

        androidx.recyclerview.widget.RecyclerView rvMembers = findViewById(R.id.rvMembers);
        rvMembers.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        MemberAdapter adapter = new MemberAdapter();
        rvMembers.setAdapter(adapter);

        MemberViewModel memberViewModel = new androidx.lifecycle.ViewModelProvider(this).get(MemberViewModel.class);
        
        String groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId != null) {
            topAppBar.setTitle("Group Members");
            memberViewModel.getMembersByChitId(groupId).observe(this, adapter::setMembers);
        } else {
            memberViewModel.getAllMembers().observe(this, adapter::setMembers);
        }

        FloatingActionButton fabAddMember = findViewById(R.id.fabAddMember);
        fabAddMember.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, AddMemberActivity.class));
        });
    }
}
