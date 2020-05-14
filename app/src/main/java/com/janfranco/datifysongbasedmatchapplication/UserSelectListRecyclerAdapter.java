package com.janfranco.datifysongbasedmatchapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class UserSelectListRecyclerAdapter extends RecyclerView.Adapter<UserSelectListRecyclerAdapter.UserHolder> {

    private ArrayList<User> users;
    private static Typeface metropolisLight;

    UserSelectListRecyclerAdapter(Context context, ArrayList<User> users) {
        metropolisLight = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-Light.otf");
        this.users = users;
    }

    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.select_user_row, parent, false);
        return new UserHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull UserHolder holder, int position) {
        User user = users.get(position);
        if (!user.getAvatarUrl().equals("default"))
            Picasso.get().load(user.getAvatarUrl()).into(holder.avatar);
        holder.username.setText(user.getUsername());
        holder.bio.setText(user.getBio());
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView username, bio;

        UserHolder(@NonNull View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.userSelectListAvatar);
            username = itemView.findViewById(R.id.userSelectListUsername);
            bio = itemView.findViewById(R.id.userSelectListBio);

            username.setTypeface(metropolisLight);
            bio.setTypeface(metropolisLight);
        }
    }

}
