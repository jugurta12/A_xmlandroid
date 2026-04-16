package com.example.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private List<String> friendsList = new ArrayList<>();
    private OnFriendClickListener listener;

    // Interface pour gérer le clic sur un ami
    public interface OnFriendClickListener {
        void onFriendClick(String friendEmail);
    }

    public FriendsAdapter(OnFriendClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<String> friends) {
        this.friendsList = friends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        String email = friendsList.get(position);
        holder.emailTv.setText(email);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFriendClick(email);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView emailTv;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTv = itemView.findViewById(R.id.tv_friend_email);
        }
    }
}