package com.example.demo;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface BarDAO {
    @Insert
    void inserer(Bar bar);

    @Query("SELECT * FROM bars WHERE utilisateurId = :uId")
    List<Bar> getBarsParUtilisateur(int uId);

    @Delete
    void supprimer(Bar bar);
}