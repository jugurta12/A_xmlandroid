package com.example.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri; // Import indispensable pour gérer les images
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

public class ProfilActivity extends AppCompatActivity {

    private AppDatabase db;
    private int userId;
    private String currentPhotoPath = "";
    private ImageView profileImg;

    // Outil pour ouvrir la galerie et récupérer l'image
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // DEMANDER LA PERMISSION PERSISTANTE
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    try {
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }

                    currentPhotoPath = uri.toString();
                    profileImg.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil); //

        // Initialisation de la base de données
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database").build();

        // Récupération de l'ID passé par HomeActivity
        userId = getIntent().getIntExtra("USER_ID", -1);

        EditText editPseudo = findViewById(R.id.edit_pseudo);
        EditText editDob = findViewById(R.id.edit_dob);
        Button saveBtn = findViewById(R.id.save_btn);
        profileImg = findViewById(R.id.profil_image); //

        // Clic sur l'image pour ouvrir la galerie
        profileImg.setOnClickListener(v -> mGetContent.launch("image/*"));

        // 1. CHARGER LES DONNÉES AU DÉMARRAGE (Texte + Photo)
        new Thread(() -> {
            User user = db.utilisateurDao().getUserById(userId); //
            if (user != null) {
                runOnUiThread(() -> {
                    editPseudo.setText(user.nom);
                    editDob.setText(user.dateNaissance);

                    // Si l'utilisateur a déjà une photo, on l'affiche
                    if (user.imagePath != null && !user.imagePath.isEmpty()) {
                        currentPhotoPath = user.imagePath;
                        profileImg.setImageURI(Uri.parse(user.imagePath));
                    }
                });
            }
        }).start();

        // 2. SAUVEGARDER LES MODIFICATIONS
        saveBtn.setOnClickListener(v -> {
            String newPseudo = editPseudo.getText().toString().trim();
            String newDob = editDob.getText().toString().trim();

            if (newPseudo.isEmpty() || newDob.isEmpty()) {
                Toast.makeText(this, "Champs vides interdits", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                User user = db.utilisateurDao().getUserById(userId); //
                if (user != null) {
                    user.nom = newPseudo;
                    user.dateNaissance = newDob;
                    user.imagePath = currentPhotoPath; // Enregistre le chemin de la nouvelle photo

                    db.utilisateurDao().modifier(user); //

                    // Mise à jour de la session pour l'accueil
                    SharedPreferences.Editor editor = getSharedPreferences("mon_app", MODE_PRIVATE).edit();
                    editor.putString("utilisateur_connecte", newPseudo);
                    editor.apply();

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Profil mis à jour !", Toast.LENGTH_SHORT).show();
                        finish(); // Retourne à HomeActivity
                    });
                }
            }).start();
        });
    }
}