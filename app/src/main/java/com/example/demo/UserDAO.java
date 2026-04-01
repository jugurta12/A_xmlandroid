package com.example.demo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update; // Ajout de l'import pour la mise à jour

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

    // --- NOUVELLES MÉTHODES POUR LE PROFIL ---

    // Permet de récupérer un utilisateur précis via son ID unique
    @Query("SELECT * FROM utilisateurs WHERE id = :id LIMIT 1")
    User getUserById(int id);

    // Permet de sauvegarder les modifications (pseudo, date de naissance, etc.)
    @Update
    void modifier(User user);
}