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

public class BlockedListRecyclerAdapter extends RecyclerView.Adapter<BlockedListRecyclerAdapter.BlockHolder> {

    private ArrayList<Block> blocks;

    private static Typeface metropolisLight;
    private static Typeface metropolisExtraLightItalic;

    BlockedListRecyclerAdapter(Context context, ArrayList<Block> blocks) {
        metropolisLight = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-Light.otf");
        metropolisExtraLightItalic = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-ExtraLightItalic.otf");
        this.blocks = blocks;
    }

    @NonNull
    @Override
    public BlockHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.blocklist_row, parent, false);
        return new BlockHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull BlockHolder holder, int position) {
        Block block = blocks.get(position);

        holder.username.setText(block.getUsername());
        holder.reason.setText(block.getReason());
        holder.date.setText(DateFormat.format(
                Constants.DATE_MESSAGE,
                block.getCreateDate() * 1000L
        ).toString());
        Picasso.get().load(block.getAvatarUrl()).into(holder.avatar);
    }

    @Override
    public int getItemCount() {
        return blocks.size();
    }

    static class BlockHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView username, reason, date;

        BlockHolder(@NonNull View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.blockListAvatar);
            username = itemView.findViewById(R.id.blockListUsername);
            reason = itemView.findViewById(R.id.blockListReason);
            date = itemView.findViewById(R.id.blockListDate);

            username.setTypeface(metropolisLight);
            reason.setTypeface(metropolisLight);
            date.setTypeface(metropolisExtraLightItalic);
        }
    }

}
