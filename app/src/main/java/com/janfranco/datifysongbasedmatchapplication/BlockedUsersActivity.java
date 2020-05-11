package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class BlockedUsersActivity extends AppCompatActivity {

    private ArrayList<Block> blockedList;
    private BlockedListRecyclerAdapter adapter;

    private FirebaseFirestore db;
    private CurrentUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        init();
    }

    private void init() {
        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        TextView blockedListInfo = findViewById(R.id.blockedListInfo);
        blockedListInfo.setTypeface(metropolisLight);
        blockedListInfo.setTextColor(Constants.DARK_PURPLE);

        db = FirebaseFirestore.getInstance();
        currentUser = CurrentUser.getInstance();

        blockedList = currentUser.getUser().getBlockedMails();
        RecyclerView recyclerView = findViewById(R.id.blockedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BlockedListRecyclerAdapter(getApplicationContext(), blockedList);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        recyclerView.addOnItemTouchListener(
            new RecyclerItemClickListener(getApplicationContext(), recyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) { }

                    @Override public void onLongItemClick(View view, int position) {
                        Block block = blockedList.get(position);
                        db.collection("userDetail").document(currentUser.getUser().geteMail())
                                .update("blockedMails", FieldValue.arrayRemove(block),
                                        "matches", FieldValue.arrayUnion(block.getMail()))
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        currentUser.getUser().getBlockedMails().remove(block);
                                        currentUser.getUser().getMatches().add(block.getMail());
                                        blockedList.remove(block);
                                        adapter.notifyDataSetChanged();
                                        finish();
                                    }
                                });
                    }
                })
        );
    }

}
