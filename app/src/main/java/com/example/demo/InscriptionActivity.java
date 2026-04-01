package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

public class InscriptionActivity extends AppCompatActivity {

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inscription);

        db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "app_database"
        ).build();

        // Récupération des nouveaux composants UI
        Button inscriptionBtn = findViewById(R.id.inscription_btn);
        EditText emailInput = findViewById(R.id.inscription_email);
        EditText pseudoInput = findViewById(R.id.inscription_pseudo); // Changé de nom à pseudo
        EditText dobInput = findViewById(R.id.inscription_dob);       // Nouveau champ Date de naissance
        EditText passwordInput = findViewById(R.id.inscription_password);
        EditText confirmInput = findViewById(R.id.inscription_confirm);
        TextView error = findViewById(R.id.inscription_error);
        TextView goLogin = findViewById(R.id.go_login);

        goLogin.setOnClickListener(view -> {
            finish();
        });

        inscriptionBtn.setOnClickListener(view -> {
            // Lecture des valeurs
            String email = emailInput.getText().toString().trim();
            String pseudo = pseudoInput.getText().toString().trim();
            String dob = dobInput.getText().toString().trim();
            String pass = passwordInput.getText().toString();
            String confirm = confirmInput.getText().toString();

            // Vérification si tous les champs sont remplis
            if (email.isEmpty() || pseudo.isEmpty() || dob.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                error.setText("Veuillez remplir tous les champs");
                error.setVisibility(View.VISIBLE);
                return;
            }

            // Vérification de la correspondance des mots de passe
            if (!pass.equals(confirm)) {
                error.setText("Les mots de passe ne correspondent pas");
                error.setVisibility(View.VISIBLE);
                return;
            }

            new Thread(() -> {
                // Vérification de l'existence de l'email
                User existant = db.utilisateurDao().verifierEmailExiste(email);

                runOnUiThread(() -> {
                    if (existant != null) {
                        error.setText("Cet email est déjà utilisé");
                        error.setVisibility(View.VISIBLE);
                    } else {
                        // Insertion du nouvel utilisateur
                        new Thread(() -> {
                            User newUser = new User();
                            newUser.email = email;
                            newUser.nom = pseudo; // On stocke le pseudo dans le champ nom (ou change le nom dans ta classe User)
                            newUser.dateNaissance = dob; // Assure-toi que ton entité User possède ce champ
                            newUser.password = pass;

                            db.utilisateurDao().inserer(newUser);

                            runOnUiThread(() -> {
                                Toast.makeText(InscriptionActivity.this, "Inscription réussie !", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(InscriptionActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        }).start();
                    }
                });
            }).start();
        });
    }
}