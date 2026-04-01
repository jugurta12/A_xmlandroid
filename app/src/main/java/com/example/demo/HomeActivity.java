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
    private BarAdapter barAdapter; // Utilisation du BarAdapter au lieu de UtilisateurAdapter
    private SharedPreferences prefs;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialisation DB
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();

        // Récupération Session
        prefs = getSharedPreferences("mon_app", Context.MODE_PRIVATE);
        userId = prefs.getInt("utilisateur_id", -1);
        String utilisateur = prefs.getString("utilisateur_connecte", "Anonyme");

        // Liaison UI
        TextView welcome = findViewById(R.id.welcome_text);
        Button logoutBtn = findViewById(R.id.logout_btn);
        Button profilBtn = findViewById(R.id.profil_btn);
        Button addBarBtn = findViewById(R.id.add_bar_btn);
        RecyclerView recyclerViewBars = findViewById(R.id.recycler_bars);

        welcome.setText("Bienvenue " + utilisateur);

        // --- ACTIONS ---

        // Aller à la page d'ajout de bar
        addBarBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AjoutBarActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        // Aller au profil
        profilBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfilActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        // Déconnexion
        logoutBtn.setOnClickListener(view -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // --- CONFIGURATION DE LA LISTE DES BARS ---

        // On initialise l'adapter avec une action de suppression
        // Dans le onCreate de HomeActivity.java
        barAdapter = new BarAdapter(bar -> {
            // On prépare le message avec le commentaire
            String messageInfo = "Adresse : " + bar.adresse + "\n\n" +
                    "Commentaire : " + bar.commentaire;

            new AlertDialog.Builder(HomeActivity.this)
                    .setTitle(bar.nom) // Le titre est le nom du bar
                    .setMessage(messageInfo) // On affiche l'adresse et le commentaire ici
                    .setPositiveButton("Supprimer", (d, w) -> {
                        new Thread(() -> {
                            db.barDao().supprimer(bar);
                            chargerMesBars();
                        }).start();
                    })
                    .setNegativeButton("Fermer", null) // "Non" devient "Fermer"
                    .show();
        });

        recyclerViewBars.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBars.setAdapter(barAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir le pseudo si changé dans le profil
        String utilisateurMaj = prefs.getString("utilisateur_connecte", "Anonyme");
        TextView welcome = findViewById(R.id.welcome_text);
        welcome.setText("Bienvenue " + utilisateurMaj);

        // On charge les bars de l'utilisateur à chaque fois qu'on revient sur l'écran
        chargerMesBars();
    }

    private void chargerMesBars() {
        new Thread(() -> {
            // Récupère uniquement les bars de l'utilisateur connecté
            List<Bar> mesBars = db.barDao().getBarsParUtilisateur(userId);
            runOnUiThread(() -> {
                barAdapter.setData(mesBars);
            });
        }).start();
    }
}