package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendProfilActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private BarAdapter adapter;
    private String friendEmail;
    private static final String TAG = "DEBUG_FRIEND_PROFIL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profil);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 1. Récupérer l'email (on vérifie qu'il n'est pas nul)
        friendEmail = getIntent().getStringExtra("FRIEND_EMAIL");
        Log.d(TAG, "Tentative de chargement du profil pour : " + friendEmail);

        if (friendEmail == null || friendEmail.isEmpty()) {
            Toast.makeText(this, "Erreur : Email de l'ami introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Initialisation des vues
        TextView tvName = findViewById(R.id.friend_name_title);
        TextView tvCount = findViewById(R.id.friend_spots_count);
        ImageButton btnBack = findViewById(R.id.btn_back_friend_profil);
        RecyclerView rv = findViewById(R.id.rv_friend_bars);

        db = FirebaseFirestore.getInstance();

        btnBack.setOnClickListener(v -> finish());

        // 3. Configuration du RecyclerView
        rv.setLayoutManager(new LinearLayoutManager(this));

        // On réutilise ton BarAdapter avec un clic vers les détails
        adapter = new BarAdapter(bar -> {
            Intent intent = new Intent(this, BarDetailActivity.class);
            // On signale que c'est un bar d'ami pour pas chercher dans Room
            intent.putExtra("IS_FRIEND_BAR", true);
            intent.putExtra("BAR_NOM", bar.nom); // On utilise le nom pour la recherche Firebase
            intent.putExtra("FRIEND_EMAIL", friendEmail);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        // 4. Charger les données
        chargerInfosAmi(tvName);
        chargerSpotsAmi(tvCount);
    }

    private void chargerInfosAmi(TextView tvName) {
        db.collection("users")
                .document(friendEmail) // Si tu utilises l'email comme ID de document à l'inscription
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvName.setText(documentSnapshot.getString("nom"));
                    } else {
                        // Si le document n'a pas l'email comme ID, on cherche par champ
                        db.collection("users").whereEqualTo("email", friendEmail).get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        tvName.setText(queryDocumentSnapshots.getDocuments().get(0).getString("nom"));
                                    }
                                });
                    }
                });
    }

    private void chargerSpotsAmi(TextView tvCount) {
        Log.d(TAG, "Requête Firebase pour les bars de : " + friendEmail);

        // On cherche les bars de cet utilisateur qui ne sont PAS privés
        db.collection("bars")
                .whereEqualTo("userEmail", friendEmail)
                .whereEqualTo("isPrivate", false) // Filtre de confidentialité
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Bar> list = new ArrayList<>();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Aucun bar trouvé sur Firebase pour cet email.");
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Bar bar = doc.toObject(Bar.class);
                        if (bar != null) {
                            list.add(bar);
                            Log.d(TAG, "Bar ajouté à la liste : " + bar.nom);
                        }
                    }

                    adapter.setData(list);
                    tvCount.setText(list.size() + " spots publics visités");
                    Log.d(TAG, "Affichage de " + list.size() + " bars.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de la récupération des bars", e);
                    Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                });
    }
}