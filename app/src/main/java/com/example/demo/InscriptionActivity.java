package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.firebase.firestore.FirebaseFirestore;

public class InscriptionActivity extends AppCompatActivity {

    private AppDatabase db;
    private FirebaseFirestore dbFirebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inscription);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration().build();

        dbFirebase = FirebaseFirestore.getInstance();

        Button inscriptionBtn = findViewById(R.id.inscription_btn);
        EditText emailInput = findViewById(R.id.inscription_email);
        EditText pseudoInput = findViewById(R.id.inscription_pseudo);
        EditText dobInput = findViewById(R.id.inscription_dob);
        EditText passwordInput = findViewById(R.id.inscription_password);
        EditText confirmInput = findViewById(R.id.inscription_confirm);
        TextView error = findViewById(R.id.inscription_error);
        TextView goLogin = findViewById(R.id.go_login);

        goLogin.setOnClickListener(view -> finish());

        inscriptionBtn.setOnClickListener(view -> {
            String email = emailInput.getText().toString().trim();
            String pseudo = pseudoInput.getText().toString().trim();
            String dob = dobInput.getText().toString().trim();
            String pass = passwordInput.getText().toString();
            String confirm = confirmInput.getText().toString();

            if (email.isEmpty() || pseudo.isEmpty() || dob.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                showError(error, "Veuillez remplir tous les champs");
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(error, "Format d'email invalide");
                return;
            }

            if (!dob.matches("^[0-9]{2}/[0-9]{2}/[0-9]{4}$")) {
                showError(error, "Date invalide (Format: JJ/MM/AAAA)");
                return;
            }

            if (!pass.equals(confirm)) {
                showError(error, "Les mots de passe ne correspondent pas");
                return;
            }

            error.setVisibility(View.INVISIBLE);

            // 1. Vérifier si l'email existe localement
            new Thread(() -> {
                User existant = db.utilisateurDao().verifierEmailExiste(email);
                runOnUiThread(() -> {
                    if (existant != null) {
                        showError(error, "Cet email est déjà utilisé");
                    } else {
                        // 2. Vérifier si le pseudo est unique sur Firebase
                        verifierPseudoUniqueSurFirebase(email, pseudo, dob, pass, error);
                    }
                });
            }).start();
        });
    }

    private void verifierPseudoUniqueSurFirebase(String email, String pseudo, String dob, String pass, TextView error) {
        dbFirebase.collection("users")
                .whereEqualTo("nom", pseudo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            showError(error, "Ce pseudo est déjà pris");
                        } else {
                            creerNouvelUtilisateur(email, pseudo, dob, pass);
                        }
                    } else {
                        Toast.makeText(this, "Erreur serveur", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void creerNouvelUtilisateur(String email, String pseudo, String dob, String pass) {
        new Thread(() -> {
            // --- CRÉATION DE L'UTILISATEUR AVEC MOT DE PASSE HACHÉ ---
            User newUser = new User();
            newUser.email = email;
            newUser.nom = pseudo;
            newUser.dateNaissance = dob;

            // On utilise HashUtil pour hacher le mot de passe avant stockage
            newUser.password = HashUtil.hashPassword(pass);

            // Sauvegarde locale (Room)
            db.utilisateurDao().inserer(newUser);

            // Sauvegarde Cloud (Firebase)
            dbFirebase.collection("users")
                    .document(email)
                    .set(newUser)
                    .addOnSuccessListener(aVoid -> {
                        runOnUiThread(() -> {
                            Toast.makeText(InscriptionActivity.this, "Inscription réussie !", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(InscriptionActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    })
                    .addOnFailureListener(e -> Log.e("Firebase", "Erreur : " + e.getMessage()));
        }).start();
    }

    private void showError(TextView errorTv, String message) {
        errorTv.setText(message);
        errorTv.setVisibility(View.VISIBLE);
    }
}