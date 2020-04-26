package com.janfranco.datifysongbasedmatchapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateFormat;
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

    private static Typeface metropolisLight;
    private static Typeface metropolisExtraLightItalic;

    ChatListRecyclerAdapter(Context context, ArrayList<Chat> chats, String currentUsername) {
        metropolisLight = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-Light.otf");
        metropolisExtraLightItalic = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-ExtraLightItalic.otf");
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
            if (!chat.getAvatar2().matches("default"))
                Picasso.get().load(chat.getAvatar2()).into(holder.avatar);
            holder.username.setText(chat.getUsername2());
        } else {
            if (!chat.getAvatar1().matches("default"))
                Picasso.get().load(chat.getAvatar1()).into(holder.avatar);
            holder.username.setText(chat.getUsername1());
        }
        holder.message.setText(chat.getLastMessage());
        holder.date.setText(DateFormat.format(
                Constants.DATE_MESSAGE,
                chat.getLastMessageDate() * 1000L
        ).toString());
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

            message.setTypeface(metropolisLight);
            username.setTypeface(metropolisLight);
            date.setTypeface(metropolisExtraLightItalic);
        }
    }

}
