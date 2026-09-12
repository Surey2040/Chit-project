package com.jothivel.chits.ui.members;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.jothivel.chits.data.local.entity.ChitGroupEntity;
import com.jothivel.chits.data.local.entity.MemberEntity;
import com.jothivel.chits.data.repository.GroupRepository;
import com.jothivel.chits.data.repository.MemberRepository;

import java.util.List;

public class MemberViewModel extends AndroidViewModel {

    private MemberRepository memberRepository;
    private GroupRepository groupRepository;

    public MemberViewModel(@NonNull Application application) {
        super(application);
        memberRepository = new MemberRepository(application);
        groupRepository = new GroupRepository(application);
    }

    public LiveData<List<MemberEntity>> getAllMembers() {
        return memberRepository.getAllMembers();
    }

    public LiveData<List<MemberEntity>> getMembersByChitId(String chitId) {
        return memberRepository.getMembersByChitId(chitId);
    }

    public LiveData<MemberEntity> getMemberById(String memberId) {
        return memberRepository.getMemberById(memberId);
    }

    public void updateMember(MemberEntity member) {
        memberRepository.updateMember(member);
    }

    /** All chit groups — used to populate the dropdown in step 3 of AddMember form */
    public LiveData<List<ChitGroupEntity>> getAllGroups() {
        return groupRepository.getAllGroups();
    }

    public void refreshMembers() {
        memberRepository.refreshMembers();
    }

    public void insertMember(MemberEntity member) {
        memberRepository.insertMember(member);
    }
}
