package com.example.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.firebase.firestore.FirebaseFirestore;

public class ProfilActivity extends AppCompatActivity {

    private AppDatabase db;
    private int userId;
    private String currentPhotoPath = "";
    private ImageView profileImg;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
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
        setContentView(R.layout.activity_profil);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database").build();
        userId = getIntent().getIntExtra("USER_ID", -1);

        EditText editEmail = findViewById(R.id.edit_email);
        EditText editPseudo = findViewById(R.id.edit_pseudo);
        EditText editDob = findViewById(R.id.edit_dob);
        EditText editOldPass = findViewById(R.id.edit_old_password);
        EditText editNewPass = findViewById(R.id.edit_new_password);
        Button saveBtn = findViewById(R.id.save_btn);
        profileImg = findViewById(R.id.profil_image);

        editEmail.setEnabled(false);
        profileImg.setOnClickListener(v -> mGetContent.launch("image/*"));

        new Thread(() -> {
            User user = db.utilisateurDao().getUserById(userId);
            if (user != null) {
                runOnUiThread(() -> {
                    editEmail.setText(user.email);
                    editPseudo.setText(user.nom);
                    editDob.setText(user.dateNaissance);
                    if (user.imagePath != null && !user.imagePath.isEmpty()) {
                        currentPhotoPath = user.imagePath;
                        profileImg.setImageURI(Uri.parse(user.imagePath));
                    }
                });
            }
        }).start();

        saveBtn.setOnClickListener(v -> {
            String newPseudo = editPseudo.getText().toString().trim();
            String newDob = editDob.getText().toString().trim();
            String oldPassInput = editOldPass.getText().toString().trim();
            String newPassInput = editNewPass.getText().toString().trim();

            if (newPseudo.isEmpty() || newDob.isEmpty()) {
                Toast.makeText(this, "Pseudo et Date de naissance obligatoires", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newDob.matches("^[0-9]{2}/[0-9]{2}/[0-9]{4}$")) {
                Toast.makeText(this, "Format date invalide (JJ/MM/AAAA)", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                User user = db.utilisateurDao().getUserById(userId);
                if (user != null) {

                    // --- LOGIQUE CHANGEMENT MOT DE PASSE AVEC HACHAGE ---
                    if (!oldPassInput.isEmpty() || !newPassInput.isEmpty()) {

                        // 1. On hache l'ancien mot de passe saisi pour comparer avec la base
                        String oldPassHached = HashUtil.hashPassword(oldPassInput);

                        if (!user.password.equals(oldPassHached)) {
                            runOnUiThread(() -> Toast.makeText(this, "Ancien mot de passe incorrect", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        if (newPassInput.length() < 4) {
                            runOnUiThread(() -> Toast.makeText(this, "Nouveau mot de passe trop court", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        // 2. On hache le NOUVEAU mot de passe avant de l'enregistrer
                        user.password = HashUtil.hashPassword(newPassInput);

                        // Note : Comme newUser est envoyé à Firebase via set() ou update(),
                        // le mot de passe haché sera aussi mis à jour sur le Cloud si tu envoies l'objet complet.
                        // Ici, on met à jour Room. Pour Firebase, on ne met à jour que les infos de profil (nom, etc.)
                    }

                    user.nom = newPseudo;
                    user.dateNaissance = newDob;
                    user.imagePath = currentPhotoPath;

                    db.utilisateurDao().modifier(user);

                    FirebaseFirestore dbFirebase = FirebaseFirestore.getInstance();
                    dbFirebase.collection("users")
                            .document(user.email)
                            .update(
                                    "nom", newPseudo,
                                    "dateNaissance", newDob,
                                    "imagePath", currentPhotoPath,
                                    "password", user.password // On synchronise aussi le nouveau haché sur Firebase
                            )
                            .addOnSuccessListener(aVoid -> Log.d("Firebase", "Profil synchronisé"))
                            .addOnFailureListener(e -> Log.e("Firebase", "Erreur synchro", e));

                    SharedPreferences.Editor editor = getSharedPreferences("mon_app", MODE_PRIVATE).edit();
                    editor.putString("utilisateur_connecte", newPseudo);
                    editor.apply();

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Profil mis à jour !", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }).start();
        });
    }
}