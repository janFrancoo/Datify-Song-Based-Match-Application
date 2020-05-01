package com.janfranco.datifysongbasedmatchapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

public class MessageListRecyclerAdapter extends RecyclerView.Adapter<MessageListRecyclerAdapter.PostHolder> {

    private ArrayList<ChatMessage> messages;
    private String currentUsername;

    private Context context;

    private static Typeface metropolisLight;
    private static Typeface metropolisExtraLightItalic;
    // private static Typeface metropolisExtraBold;

    MessageListRecyclerAdapter(Context context, String currentUsername, ArrayList<ChatMessage> messages) {
        this.context = context;
        metropolisLight = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-Light.otf");
        metropolisExtraLightItalic = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-ExtraLightItalic.otf");
        // metropolisExtraBold = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-ExtraBold.otf");
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

        if (!message.getImgUrl().equals("")) {
            if(message.getImgUrl().substring(0, 7).equals("content")) {
                Uri uri = Uri.parse(message.getImgUrl());
                try {
                    holder.image.setImageBitmap(getBitmapFromUri(uri));
                    holder.image.getLayoutParams().height = 500;
                    holder.image.getLayoutParams().width = 500;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Picasso.get().load(message.getImgUrl()).into(holder.image);
                holder.image.getLayoutParams().height = 500;
                holder.image.getLayoutParams().width = 500;
            }
        } else {
            holder.image.getLayoutParams().width = 0;
            holder.image.getLayoutParams().height = 0;
        }

        if (holder.messageCheck != null && message.isRead())
            holder.messageCheck.setImageResource(R.drawable.doubletick);
        else if (holder.messageCheck != null && message.isTransmitted())
            holder.messageCheck.setImageResource(R.drawable.tick);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class PostHolder extends RecyclerView.ViewHolder {

        ImageView image, messageCheck;
        TextView message, date;

        PostHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.messageListMessage);
            image = itemView.findViewById(R.id.messageListImage);
            date = itemView.findViewById(R.id.messageListDate);
            messageCheck = itemView.findViewById(R.id.messageCheck);
            message.setTypeface(metropolisLight);
            date.setTypeface(metropolisExtraLightItalic);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        assert parcelFileDescriptor != null;
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

}
