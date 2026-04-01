package com.example.demo;

import androidx.room.Database;
import androidx.room.RoomDatabase;

// On passe à la version 2 car on a ajouté une table
@Database(entities = {User.class, Bar.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDAO utilisateurDao();
    public abstract BarDAO barDao(); // Ajoute cette ligne
}
