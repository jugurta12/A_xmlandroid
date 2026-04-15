package com.example.demo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private AppDatabase db;
    private BarAdapter barAdapter;
    private SharedPreferences prefs;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 1. Initialisation de la Base de Données
        // Ajout de fallbackToDestructiveMigration pour éviter les crashs de changement de version
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();

        // 2. Récupération de la session utilisateur
        prefs = getSharedPreferences("mon_app", Context.MODE_PRIVATE);
        userId = prefs.getInt("utilisateur_id", -1);
        String utilisateurNom = prefs.getString("utilisateur_connecte", "Anonyme");

        // 3. Liaison des composants UI
        TextView welcome = findViewById(R.id.welcome_text);
        Button logoutBtn = findViewById(R.id.logout_btn);
        Button profilBtn = findViewById(R.id.profil_btn);
        Button addBarBtn = findViewById(R.id.add_bar_btn);
        RecyclerView recyclerViewBars = findViewById(R.id.recycler_bars);

        welcome.setText("Bienvenue " + utilisateurNom);

        // --- ACTIONS DES BOUTONS ---

        addBarBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AjoutBarActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        profilBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfilActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        logoutBtn.setOnClickListener(view -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // --- CONFIGURATION DE LA LISTE DES BARS (AVEC LA NOUVELLE PAGE) ---

        // On remplace le popup par l'ouverture de BarDetailActivity
        barAdapter = new BarAdapter(bar -> {
            Intent intent = new Intent(HomeActivity.this, BarDetailActivity.class);
            // On passe l'ID du bar pour que la page de détails puisse le charger
            intent.putExtra("BAR_ID", bar.id);
            startActivity(intent);
        });

        recyclerViewBars.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBars.setAdapter(barAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String utilisateurMaj = prefs.getString("utilisateur_connecte", "Anonyme");
        TextView welcome = findViewById(R.id.welcome_text);
        welcome.setText("Bienvenue " + utilisateurMaj);

        // Recharger la liste des bars
        chargerMesBars();
    }

    private void chargerMesBars() {
        new Thread(() -> {
            List<Bar> mesBars = db.barDao().getBarsParUtilisateur(userId);
            runOnUiThread(() -> {
                if (barAdapter != null) {
                    barAdapter.setData(mesBars);
                }
            });
        }).start();
    }
}