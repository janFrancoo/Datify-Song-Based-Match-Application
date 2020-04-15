package com.janfranco.datifysongbasedmatchapplication;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ChatListRecyclerAdapter extends RecyclerView.Adapter<ChatListRecyclerAdapter.PostHolder> {

    private ArrayList<Chat> chats;
    private String currentUsername;

    ChatListRecyclerAdapter(ArrayList<Chat> chats, String currentUsername) {
        this.chats = chats;
        this.currentUsername = currentUsername;
    }

    @NonNull
    @Override
    public PostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.chatlist_row, parent, false);
        return new PostHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PostHolder holder, int position) {
        Chat chat = chats.get(position);
        if (chat.getUsername1().equals(currentUsername)) {
            Picasso.get().load(chat.getAvatar2()).into(holder.avatar);
            holder.username.setText(chat.getUsername2());
        } else {
            Picasso.get().load(chat.getAvatar1()).into(holder.avatar);
            holder.username.setText(chat.getUsername1());
        }
        holder.message.setText(chat.getLastMessage());
        holder.date.setText("15.04.20 22:05");
        // ToDo: Add date!
    }

    

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class PostHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView username, message, date;

        PostHolder(@NonNull View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.chatListAvatar);
            username = itemView.findViewById(R.id.chatListUsername);
            message = itemView.findViewById(R.id.chatListMessage);
            date = itemView.findViewById(R.id.chatListDate);
        }
    }

}
