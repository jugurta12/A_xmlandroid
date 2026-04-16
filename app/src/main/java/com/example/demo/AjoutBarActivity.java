package com.example.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat; // CORRECTION ICI : L'import correct
import androidx.room.Room;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AjoutBarActivity extends AppCompatActivity {
    private AppDatabase db;
    private FirebaseFirestore dbFirebase; // Firebase
    private int userId;
    private String userEmail;

    private MapView mapPreview;
    private EditText editAdresse;

    // CORRECTION ICI : Utilisation de SwitchCompat
    private SwitchCompat switchPrivate;

    private double latSelectionnee = 0;
    private double lonSelectionnee = 0;
    private Handler handler = new Handler();
    private Runnable runnable;
    private boolean isSelectionByClick = false;

    private List<String> barPhotosPaths = new ArrayList<>();

    private final ActivityResultLauncher<String> getBarPhoto = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        barPhotosPaths.add(uri.toString());
                        Toast.makeText(this, "Photo ajoutée (" + barPhotosPaths.size() + ")", Toast.LENGTH_SHORT).show();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_ajout_bar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ajouter un bar");
        }

        // 1. Initialisation Bases de données
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();
        dbFirebase = FirebaseFirestore.getInstance();

        // 2. Récupération infos utilisateur
        userId = getIntent().getIntExtra("USER_ID", -1);
        SharedPreferences prefs = getSharedPreferences("mon_app", Context.MODE_PRIVATE);
        userEmail = prefs.getString("utilisateur_email", "");

        // 3. Liaison UI
        EditText editNom = findViewById(R.id.nom_bar);
        editAdresse = findViewById(R.id.adresse_bar);
        EditText editComm = findViewById(R.id.comm_bar);
        RatingBar ratingBar = findViewById(R.id.rating_bar);
        ChipGroup chipGroupAmbiance = findViewById(R.id.chip_group_ambiance);

        // CORRECTION ICI : Liaison avec le bon type
        switchPrivate = findViewById(R.id.switch_private);

        Button btnSave = findViewById(R.id.save_bar_btn);
        Button btnAddPhoto = findViewById(R.id.btn_add_photo);

        mapPreview = findViewById(R.id.map_preview);
        mapPreview.setMultiTouchControls(true);
        mapPreview.getController().setZoom(15.0);
        mapPreview.getController().setCenter(new GeoPoint(48.8566, 2.3522));

        // --- RECHERCHE ADRESSE ---
        editAdresse.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (runnable != null) handler.removeCallbacks(runnable);
            }
            @Override public void afterTextChanged(Editable s) {
                if (isSelectionByClick) { isSelectionByClick = false; return; }
                String query = s.toString().trim();
                if (query.length() > 6) {
                    runnable = () -> chercherAdressesLive(query);
                    handler.postDelayed(runnable, 1000);
                }
            }
        });

        btnAddPhoto.setOnClickListener(v -> getBarPhoto.launch("image/*"));

        // --- BOUTON ENREGISTRER ---
        btnSave.setOnClickListener(v -> {
            String nom = editNom.getText().toString().trim();
            if (nom.isEmpty()) {
                Toast.makeText(this, "Le nom est obligatoire", Toast.LENGTH_SHORT).show();
                return;
            }

            // Récupération de l'état du Switch (Privé ou Public)
            boolean isPrivate = switchPrivate.isChecked();

            List<Integer> ids = chipGroupAmbiance.getCheckedChipIds();
            List<String> selectedAmbiances = new ArrayList<>();
            for (Integer id : ids) {
                Chip chip = findViewById(id);
                selectedAmbiances.add(chip.getText().toString());
            }

            new Thread(() -> {
                // Création de l'objet Bar
                Bar nouveauBar = new Bar();
                nouveauBar.nom = nom;
                nouveauBar.adresse = editAdresse.getText().toString();
                nouveauBar.commentaire = editComm.getText().toString();
                nouveauBar.note = ratingBar.getRating();
                nouveauBar.utilisateurId = userId;

                // Champs cruciaux pour le Cloud et le partage
                nouveauBar.userEmail = userEmail;
                nouveauBar.isPrivate = isPrivate;

                nouveauBar.ambiances = TextUtils.join(", ", selectedAmbiances);
                nouveauBar.photosPaths = TextUtils.join(",", barPhotosPaths);
                nouveauBar.latitude = latSelectionnee;
                nouveauBar.longitude = lonSelectionnee;

                // 1. Sauvegarde Locale (Room)
                db.barDao().inserer(nouveauBar);

                // 2. Sauvegarde Cloud (Firebase)
                dbFirebase.collection("bars")
                        .add(nouveauBar)
                        .addOnSuccessListener(doc -> Log.d("Firebase", "Bar synchronisé avec succès !"))
                        .addOnFailureListener(e -> Log.e("Firebase", "Erreur de synchronisation", e));

                runOnUiThread(() -> {
                    Toast.makeText(this, isPrivate ? "Spot ajouté en privé" : "Spot ajouté et partagé !", Toast.LENGTH_SHORT).show();
                    finish(); // Retour à l'écran précédent
                });
            }).start();
        });
    }

    private void chercherAdressesLive(String query) {
        new Thread(() -> {
            try {
                String urlStr = "https://nominatim.openstreetmap.org/search?q="
                        + URLEncoder.encode(query, "UTF-8")
                        + "&format=json&addressdetails=1&limit=5";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "MonAppBarDemo");

                InputStream in = conn.getInputStream();
                Scanner s = new Scanner(in).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";

                JSONArray jsonArray = new JSONArray(result);

                if (jsonArray.length() > 0) {
                    runOnUiThread(() -> {
                        try {
                            String[] suggestions = new String[jsonArray.length()];
                            for (int i = 0; i < jsonArray.length(); i++) {
                                suggestions[i] = jsonArray.getJSONObject(i).getString("display_name");
                            }

                            new AlertDialog.Builder(this)
                                    .setTitle("Choisissez l'adresse exact :")
                                    .setItems(suggestions, (dialog, which) -> {
                                        try {
                                            JSONObject obj = jsonArray.getJSONObject(which);
                                            latSelectionnee = obj.getDouble("lat");
                                            lonSelectionnee = obj.getDouble("lon");
                                            isSelectionByClick = true;
                                            editAdresse.setText(suggestions[which]);

                                            GeoPoint point = new GeoPoint(latSelectionnee, lonSelectionnee);
                                            mapPreview.getController().animateTo(point);
                                            mapPreview.getController().setZoom(18.0);
                                            mapPreview.getOverlays().clear();
                                            Marker m = new Marker(mapPreview);
                                            m.setPosition(point);
                                            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                            mapPreview.getOverlays().add(m);
                                            mapPreview.invalidate();
                                        } catch (Exception e) { e.printStackTrace(); }
                                    }).show();
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @Override protected void onResume() { super.onResume(); mapPreview.onResume(); }
    @Override protected void onPause() { super.onPause(); mapPreview.onPause(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}