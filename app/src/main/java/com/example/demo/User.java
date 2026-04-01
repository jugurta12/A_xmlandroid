package com.example.demo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "utilisateurs")
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Stocke le pseudo de l'utilisateur
    public String nom;

    public String email;

    public String password;

    // Champ pour la date de naissance
    @ColumnInfo(name = "date_naissance")
    public String dateNaissance;

    // NOUVEAU : Champ pour stocker le chemin de la photo de profil
    @ColumnInfo(name = "image_path")
    public String imagePath;
}