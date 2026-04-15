package com.example.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class BarDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private MapView map;
    private int barId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Config OSM (doit être fait avant setContentView)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_bar_detail);

        // Activation de la flèche de retour dans la barre en haut
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Détails du Bar");
        }

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();

        // Récupérer l'ID passé par HomeActivity
        barId = getIntent().getIntExtra("BAR_ID", -1);

        TextView tvNom = findViewById(R.id.detail_nom);
        TextView tvAdresse = findViewById(R.id.detail_adresse);
        TextView tvAmbiance = findViewById(R.id.detail_ambiances);
        TextView tvComm = findViewById(R.id.detail_commentaire);
        RatingBar rb = findViewById(R.id.detail_rating);
        map = findViewById(R.id.detail_map);
        Button btnDelete = findViewById(R.id.btn_delete_bar);

        // 2. CHARGEMENT DES DONNÉES
        new Thread(() -> {
            Bar bar = db.barDao().getBarById(barId);
            if (bar != null) {
                runOnUiThread(() -> {
                    tvNom.setText(bar.nom);
                    tvAdresse.setText(bar.adresse);
                    tvAmbiance.setText("Ambiances : " + (bar.ambiances != null ? bar.ambiances : "N/A"));
                    tvComm.setText(bar.commentaire != null && !bar.commentaire.isEmpty() ? bar.commentaire : "Aucun commentaire.");
                    rb.setRating(bar.note);

                    // Configuration de la carte
                    map.setMultiTouchControls(true);
                    if (bar.latitude != 0 && bar.longitude != 0) {
                        GeoPoint point = new GeoPoint(bar.latitude, bar.longitude);
                        map.getController().setZoom(17.0);
                        map.getController().setCenter(point);

                        Marker m = new Marker(map);
                        m.setPosition(point);
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        m.setTitle(bar.nom);
                        map.getOverlays().add(m);
                    }
                });
            }
        }).start();

        // 3. LOGIQUE DE SUPPRESSION AVEC CONFIRMATION
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Supprimer ce bar ?")
                    .setMessage("Es-tu sûr de vouloir retirer ce bar de tes favoris ?")
                    .setPositiveButton("Oui, supprimer", (dialog, which) -> {
                        new Thread(() -> {
                            Bar barASupprimer = db.barDao().getBarById(barId);
                            if (barASupprimer != null) {
                                db.barDao().supprimer(barASupprimer);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Bar supprimé", Toast.LENGTH_SHORT).show();
                                    finish(); // Retour automatique à HomeActivity
                                });
                            }
                        }).start();
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });
    }

    // Gestion de la flèche de retour en haut
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // --- Gestion du cycle de vie de la Map (Important pour la mémoire) ---
    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}