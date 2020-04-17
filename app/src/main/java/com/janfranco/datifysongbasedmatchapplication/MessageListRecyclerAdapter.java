package com.janfranco.datifysongbasedmatchapplication;

import android.annotation.SuppressLint;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MessageListRecyclerAdapter extends RecyclerView.Adapter<MessageListRecyclerAdapter.PostHolder> {

    private ArrayList<ChatMessage> messages;
    private String currentUsername;

    MessageListRecyclerAdapter(String currentUsername, ArrayList<ChatMessage> messages) {
        this.currentUsername = currentUsername;
        this.messages = messages;
    }

    @NonNull
    @Override
    public PostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.messagelist_row, parent, false);
        return new PostHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PostHolder holder, int position) {
        ChatMessage message = messages.get(position);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageWrapper.getLayoutParams();

        if (message.getSender().equals(currentUsername)) {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            holder.messageWrapper.setLayoutParams(params);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            holder.messageWrapper.setLayoutParams(params);
        }
        holder.message.setText(message.getMessage());
        holder.date.setText("15.04.20 22:05");
        // ToDo: Add date!
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class PostHolder extends RecyclerView.ViewHolder {

        RelativeLayout messageWrapper;
        TextView message, date;

        PostHolder(@NonNull View itemView) {
            super(itemView);

            messageWrapper = itemView.findViewById(R.id.messageListLayout);
            message = itemView.findViewById(R.id.messageListMessage);
            date = itemView.findViewById(R.id.messageListDate);
        }
    }

}
