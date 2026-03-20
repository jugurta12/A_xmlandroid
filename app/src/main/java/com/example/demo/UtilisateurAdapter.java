package com.example.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UtilisateurAdapter extends RecyclerView.Adapter<UtilisateurAdapter.ViewHolder> {

    private List<User> data;
    private Context context;
    private OnDeleteListener listener;

    public interface OnDeleteListener {
        void onDelete(User user);
    }

    public UtilisateurAdapter(Context context, List<User> data, OnDeleteListener listener) {
        this.context = context;
        this.data = data;
        this.listener = listener;
    }

    public void setData(List<User> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_utilisateur, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = data.get(position);
        holder.nom.setText(user.nom);
        holder.email.setText(user.email);
        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(user));
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nom, email;
        ImageButton deleteBtn;

        ViewHolder(View itemView) {
            super(itemView);
            nom = itemView.findViewById(R.id.item_nom);
            email = itemView.findViewById(R.id.item_email);
            deleteBtn = itemView.findViewById(R.id.item_delete_btn);
        }
    }
}
