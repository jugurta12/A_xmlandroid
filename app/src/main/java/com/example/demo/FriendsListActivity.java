package com.example.demo;

import android.content.Intent; // Ajout de l'import
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FriendsListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FriendsAdapter adapter;
    private FirebaseFirestore db;
    private String monEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        monEmail = getSharedPreferences("mon_app", MODE_PRIVATE).getString("utilisateur_email", "");

        findViewById(R.id.btn_back_friends).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rv_friends);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // MISE À JOUR : On ouvre maintenant le profil de l'ami au clic
        adapter = new FriendsAdapter(amiEmail -> {
            Intent intent = new Intent(FriendsListActivity.this, FriendProfilActivity.class);
            // On envoie l'email pour que la page sache quel profil charger
            intent.putExtra("FRIEND_EMAIL", amiEmail);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        chargerAmis();
    }

    private void chargerAmis() {
        // On cherche les demandes acceptées où JE suis impliqué
        db.collection("friend_requests")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    List<String> mesAmis = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String sender = doc.getString("senderEmail");
                            String receiver = doc.getString("receiverEmail");

                            if (monEmail.equals(sender)) {
                                mesAmis.add(receiver);
                            } else if (monEmail.equals(receiver)) {
                                mesAmis.add(sender);
                            }
                        }
                    }
                    adapter.setData(mesAmis);
                });
    }
}