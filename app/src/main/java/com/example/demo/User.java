package com.example.demo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "utilisateurs")
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nom;
    public String email;
    public String password;
}
