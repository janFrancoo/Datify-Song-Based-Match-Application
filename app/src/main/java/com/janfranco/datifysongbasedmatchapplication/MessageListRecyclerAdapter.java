package com.janfranco.datifysongbasedmatchapplication;

import android.annotation.SuppressLint;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        View view;
        if (viewType == 1) {
            view = layoutInflater.inflate(R.layout.messagelist_row_2, parent, false);
        } else {
            view = layoutInflater.inflate(R.layout.messagelist_row, parent, false);
        }
        return new PostHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSender().equals(currentUsername))
            return 1;
        else
            return 0;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PostHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.message.setText(message.getMessage());
        holder.date.setText(DateFormat.format(
                Constants.DATE_MESSAGE,
                message.getSendDate() * 1000L
        ).toString());
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
