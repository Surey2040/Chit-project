package com.jothivel.chits.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "members")
public class MemberEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String phone;
    public String photoUrl; // Nullable
    public String nomineeName; // Nullable
    public String nomineePhone; // Nullable
    public String role; // Role of the member
    public boolean isActive;

    // Additional fields for the 5-step form
    public String dob;
    public String gender;
    
    // Address
    public String addressLine;
    public String city;
    public String state;
    public String pincode;

    // KYC (Encrypted/Stored safely)
    public String aadhaarNoEncrypted;
    public String panNo;
    public String aadhaarDocumentPath; // Local URI path
    public String panDocumentPath; // Local URI path

    // Chit linkage (MVP storage, ideally in a separate join table but adding here for simplicity per the UI)
    public String selectedChitId;
    public String ticketNo;
    public String installmentAmount;
    public String joiningDate;
    public String dueDate;

    // Nominee extras
    public String nomineeRelationship;
}

