package com.example.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private AppDatabase db;
    private UtilisateurAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "app_database"
        ).build();

        SharedPreferences prefs = getSharedPreferences("mon_app", Context.MODE_PRIVATE);
        String utilisateur = prefs.getString("utilisateur_connecte", "Anonyme");
        String email = prefs.getString("utilisateur_email", "");

        TextView welcome = findViewById(R.id.welcome_text);
        Button logoutBtn = findViewById(R.id.logout_btn);
        Button profilBtn = findViewById(R.id.profil_btn);
        RecyclerView recyclerView = findViewById(R.id.recycler_utilisateurs);

        welcome.setText("Bienvenue " + utilisateur);

        logoutBtn.setOnClickListener(view -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear(); 
            editor.apply();

            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        profilBtn.setOnClickListener(view -> {
            new AlertDialog.Builder(HomeActivity.this)
                    .setTitle("Mon profil")
                    .setMessage("Nom : " + utilisateur + "\nEmail : " + email)
                    .setPositiveButton("OK", null)
                    .show();
        });

        adapter = new UtilisateurAdapter(this, null, user -> {
            new AlertDialog.Builder(HomeActivity.this)
                    .setTitle("Supprimer")
                    .setMessage("Supprimer " + user.nom + " ?")
                    .setPositiveButton("Oui", (d, w) -> {
                        new Thread(() -> {
                            db.utilisateurDao().supprimerUtilisateur(user.id);
                            List<User> updated = db.utilisateurDao().getTousLesUtilisateurs();
                            runOnUiThread(() -> {
                                adapter.setData(updated);
                                Toast.makeText(HomeActivity.this, user.nom + " supprimé", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("Non", null)
                    .show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        chargerUtilisateurs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        chargerUtilisateurs();
    }

    private void chargerUtilisateurs() {
        new Thread(() -> {
            List<User> users = db.utilisateurDao().getTousLesUtilisateurs();
            runOnUiThread(() -> adapter.setData(users));
        }).start();
    }
}
