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

        Button inscriptionBtn = findViewById(R.id.inscription_btn);
        EditText nomInput = findViewById(R.id.inscription_nom);
        EditText emailInput = findViewById(R.id.inscription_email);
        EditText passwordInput = findViewById(R.id.inscription_password);
        EditText confirmInput = findViewById(R.id.inscription_confirm);
        TextView error = findViewById(R.id.inscription_error);
        TextView goLogin = findViewById(R.id.go_login);

        goLogin.setOnClickListener(view -> {
            finish();
        });

        inscriptionBtn.setOnClickListener(view -> {
            String nom = nomInput.getText().toString();
            String email = emailInput.getText().toString();
            String pass = passwordInput.getText().toString();
            String confirm = confirmInput.getText().toString();

            if (nom.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                error.setText("Veuillez remplir tous les champs");
                error.setVisibility(View.VISIBLE);
                return;
            }

            if (!pass.equals(confirm)) {
                error.setText("Les mots de passe ne correspondent pas");
                error.setVisibility(View.VISIBLE);
                return;
            }

            new Thread(() -> {
                User existant = db.utilisateurDao().verifierEmailExiste(email);

                runOnUiThread(() -> {
                    if (existant != null) {
                        error.setText("Cet email est déjà utilisé");
                        error.setVisibility(View.VISIBLE);
                    } else {
                        new Thread(() -> {
                            User newUser = new User();
                            newUser.nom = nom;
                            newUser.email = email;
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
