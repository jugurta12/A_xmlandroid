package com.example.demo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérification de session au démarrage
        prefs = getSharedPreferences("mon_app", Context.MODE_PRIVATE);
        int userId = prefs.getInt("utilisateur_id", -1);
        if (userId != -1) {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "app_database"
        ).build();

        Button validBtn = findViewById(R.id.valid_btn);
        EditText emailInput = findViewById(R.id.email_input);
        EditText passwordInput = findViewById(R.id.password_input);
        TextView error = findViewById(R.id.error);
        TextView goInscription = findViewById(R.id.go_inscription);

        goInscription.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, InscriptionActivity.class);
            startActivity(intent);
        });

        validBtn.setOnClickListener(view -> {
            String email = emailInput.getText().toString();
            String pass = passwordInput.getText().toString();

            if (email.isEmpty() || pass.isEmpty()) {
                error.setText("Veuillez remplir tous les champs");
                error.setVisibility(View.VISIBLE);
                return;
            }

            new Thread(() -> {
                User user = db.utilisateurDao().verifierLogin(email, pass);

                runOnUiThread(() -> {
                    if (user != null) {
                        error.setVisibility(View.INVISIBLE);

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("utilisateur_connecte", user.nom);
                        editor.putString("utilisateur_email", user.email);
                        editor.putInt("utilisateur_id", user.id);
                        editor.apply();

                        Toast.makeText(MainActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        error.setText("Email ou mot de passe incorrect");
                        error.setVisibility(View.VISIBLE);
                    }
                });
            }).start();
        });
    }
}
