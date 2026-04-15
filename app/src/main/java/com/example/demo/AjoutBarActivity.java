package com.example.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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
    private int userId;

    // Variables UI et Map
    private MapView mapPreview;
    private EditText editAdresse;
    private double latSelectionnee = 0;
    private double lonSelectionnee = 0;
    private Handler handler = new Handler();
    private Runnable runnable;

    // Le drapeau pour bloquer la recherche automatique après un choix
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

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();

        userId = getIntent().getIntExtra("USER_ID", -1);

        EditText editNom = findViewById(R.id.nom_bar);
        editAdresse = findViewById(R.id.adresse_bar);
        EditText editComm = findViewById(R.id.comm_bar);
        RatingBar ratingBar = findViewById(R.id.rating_bar);
        ChipGroup chipGroupAmbiance = findViewById(R.id.chip_group_ambiance);
        Button btnSave = findViewById(R.id.save_bar_btn);
        Button btnAddPhoto = findViewById(R.id.btn_add_photo);

        mapPreview = findViewById(R.id.map_preview);
        mapPreview.setMultiTouchControls(true);
        mapPreview.getController().setZoom(15.0);
        mapPreview.getController().setCenter(new GeoPoint(48.8566, 2.3522));

        // --- LOGIQUE LIVE SEARCH ---
        editAdresse.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (runnable != null) handler.removeCallbacks(runnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // SI ON VIENT DE CLIQUER SUR UNE SUGGESTION, ON S'ARRÊTE LÀ
                if (isSelectionByClick) {
                    isSelectionByClick = false;
                    return;
                }

                String query = s.toString().trim();
                if (query.length() > 6) {
                    runnable = () -> chercherAdressesLive(query);
                    handler.postDelayed(runnable, 1000);
                }
            }
        });

        btnAddPhoto.setOnClickListener(v -> getBarPhoto.launch("image/*"));

        for (int i = 0; i < chipGroupAmbiance.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupAmbiance.getChildAt(i);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && chipGroupAmbiance.getCheckedChipIds().size() > 3) {
                    buttonView.setChecked(false);
                    Toast.makeText(this, "3 ambiances maximum !", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnSave.setOnClickListener(v -> {
            String nom = editNom.getText().toString().trim();
            if (nom.isEmpty()) {
                Toast.makeText(this, "Le nom est obligatoire", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Integer> ids = chipGroupAmbiance.getCheckedChipIds();
            List<String> selectedAmbiances = new ArrayList<>();
            for (Integer id : ids) {
                Chip chip = findViewById(id);
                selectedAmbiances.add(chip.getText().toString());
            }

            new Thread(() -> {
                Bar nouveauBar = new Bar();
                nouveauBar.nom = nom;
                nouveauBar.adresse = editAdresse.getText().toString();
                nouveauBar.commentaire = editComm.getText().toString();
                nouveauBar.note = ratingBar.getRating();
                nouveauBar.utilisateurId = userId;
                nouveauBar.ambiances = TextUtils.join(", ", selectedAmbiances);
                nouveauBar.photosPaths = TextUtils.join(",", barPhotosPaths);
                nouveauBar.latitude = latSelectionnee;
                nouveauBar.longitude = lonSelectionnee;

                db.barDao().inserer(nouveauBar);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Bar ajouté !", Toast.LENGTH_SHORT).show();
                    finish();
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
                                    .setTitle("Choisissez l'adresse :")
                                    .setItems(suggestions, (dialog, which) -> {
                                        try {
                                            JSONObject obj = jsonArray.getJSONObject(which);
                                            latSelectionnee = obj.getDouble("lat");
                                            lonSelectionnee = obj.getDouble("lon");

                                            // ON ACTIVE LE DRAPEAU AVANT LE SETTEXT
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapPreview.onPause();
    }
}