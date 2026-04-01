package com.example.demo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "bars")
public class Bar {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int utilisateurId; // Pour savoir à quel user appartient le bar
    public String nom;
    public String adresse;
    public float note; // Pour les étoiles
    public String commentaire;
}