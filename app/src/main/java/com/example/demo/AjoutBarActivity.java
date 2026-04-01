package com.example.demo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

public class AjoutBarActivity extends AppCompatActivity {
    private AppDatabase db;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajout_bar);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database").build();
        userId = getIntent().getIntExtra("USER_ID", -1);

        EditText editNom = findViewById(R.id.nom_bar);
        EditText editAdresse = findViewById(R.id.adresse_bar);
        EditText editComm = findViewById(R.id.comm_bar);
        RatingBar ratingBar = findViewById(R.id.rating_bar);
        Button btnSave = findViewById(R.id.save_bar_btn);

        btnSave.setOnClickListener(v -> {
            Bar nouveauBar = new Bar();
            nouveauBar.nom = editNom.getText().toString();
            nouveauBar.adresse = editAdresse.getText().toString();
            nouveauBar.commentaire = editComm.getText().toString();
            nouveauBar.note = ratingBar.getRating();
            nouveauBar.utilisateurId = userId; // On lie le bar à l'utilisateur !

            new Thread(() -> {
                db.barDao().inserer(nouveauBar);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Bar ajouté !", Toast.LENGTH_SHORT).show();
                    finish(); // Retourne à l'accueil
                });
            }).start();
        });
    }
}