package com.example.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class BarDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private MapView map;
    private TextView tvNom, tvAdresse, tvAmbiance, tvComm;
    private RatingBar rb;
    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Configuration OSM
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_bar_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Détails du spot");
        }

        // Liaisons UI
        tvNom = findViewById(R.id.detail_nom);
        tvAdresse = findViewById(R.id.detail_adresse);
        tvAmbiance = findViewById(R.id.detail_ambiances);
        tvComm = findViewById(R.id.detail_commentaire);
        rb = findViewById(R.id.detail_rating);
        map = findViewById(R.id.detail_map);
        btnDelete = findViewById(R.id.btn_delete_bar);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();

        // --- DÉTERMINATION DE LA SOURCE ---
        boolean isFriendBar = getIntent().getBooleanExtra("IS_FRIEND_BAR", false);

        if (isFriendBar) {
            // Cas A : C'est le bar d'un ami (Firebase)
            String barNom = getIntent().getStringExtra("BAR_NOM");
            String friendEmail = getIntent().getStringExtra("FRIEND_EMAIL");

            btnDelete.setVisibility(View.GONE); // On ne peut pas supprimer le bar d'un ami
            chargerDepuisFirebase(barNom, friendEmail);
        } else {
            // Cas B : C'est ton bar (Room)
            int barId = getIntent().getIntExtra("BAR_ID", -1);
            chargerDepuisRoom(barId);

            // Logique de suppression uniquement pour tes propres bars
            btnDelete.setOnClickListener(v -> confirmerSuppression(barId));
        }
    }

    private void chargerDepuisRoom(int id) {
        new Thread(() -> {
            Bar bar = db.barDao().getBarById(id);
            if (bar != null) {
                runOnUiThread(() -> afficherInfosBar(bar));
            }
        }).start();
    }

    private void chargerDepuisFirebase(String nom, String email) {
        FirebaseFirestore.getInstance().collection("bars")
                .whereEqualTo("userEmail", email)
                .whereEqualTo("nom", nom)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Bar bar = queryDocumentSnapshots.getDocuments().get(0).toObject(Bar.class);
                        if (bar != null) {
                            afficherInfosBar(bar);
                        }
                    } else {
                        Toast.makeText(this, "Impossible de charger les détails Cloud", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_SHORT).show());
    }

    private void afficherInfosBar(Bar bar) {
        tvNom.setText(bar.nom);
        tvAdresse.setText(bar.adresse);
        tvAmbiance.setText("Ambiances : " + (bar.ambiances != null ? bar.ambiances : "N/A"));
        tvComm.setText(bar.commentaire != null && !bar.commentaire.isEmpty() ? bar.commentaire : "Aucun commentaire.");
        rb.setRating(bar.note);

        map.setMultiTouchControls(true);
        if (bar.latitude != 0 && bar.longitude != 0) {
            GeoPoint point = new GeoPoint(bar.latitude, bar.longitude);
            map.getController().setZoom(17.0);
            map.getController().setCenter(point);

            map.getOverlays().clear();
            Marker m = new Marker(map);
            m.setPosition(point);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setTitle(bar.nom);
            map.getOverlays().add(m);
            map.invalidate();
        }
    }

    private void confirmerSuppression(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer ce bar ?")
                .setMessage("Es-tu sûr de vouloir retirer ce bar de tes favoris ?")
                .setPositiveButton("Oui, supprimer", (dialog, which) -> {
                    new Thread(() -> {
                        Bar barASupprimer = db.barDao().getBarById(id);
                        if (barASupprimer != null) {
                            db.barDao().supprimer(barASupprimer);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Bar supprimé", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
    @Override protected void onResume() { super.onResume(); map.onResume(); }
    @Override protected void onPause() { super.onPause(); map.onPause(); }
}