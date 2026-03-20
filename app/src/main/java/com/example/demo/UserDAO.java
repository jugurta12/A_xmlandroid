package com.example.demo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDAO {

    @Insert
    void inserer(User user);

    @Query("SELECT * FROM utilisateurs WHERE email = :email AND password = :password")
    User verifierLogin(String email, String password);

    @Query("SELECT * FROM utilisateurs WHERE email = :email")
    User verifierEmailExiste(String email);

    @Query("SELECT * FROM utilisateurs")
    List<User> getTousLesUtilisateurs();

    @Query("DELETE FROM utilisateurs WHERE id = :id")
    void supprimerUtilisateur(int id);
}
