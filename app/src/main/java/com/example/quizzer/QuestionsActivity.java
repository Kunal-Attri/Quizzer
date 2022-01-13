package com.example.quizzer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuestionsActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private TextView question, noIndicator;
    private FloatingActionButton bookmarkBtn;
    private LinearLayout optionsContainer;
    private Button shareBtn, nextBtn;
    private int count = 0;
    private List<QuestionModel> list;
    private int position = 0;
    private int score = 0;
    private Dialog loadingDialog;
    private String setId;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private List<QuestionModel> bookmarksList;
    public static final String FILE_NAME = "QUIZZER";
    public static final String KEY_NAME = "QUESTIONS";
    private int matchedQuestionPosition;
    private int animation_duration;
    private int selected_answer_text_size;
    private String next_button_color;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_questions);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        question = findViewById(R.id.question);
        noIndicator = findViewById(R.id.no_indicator);
        bookmarkBtn = findViewById(R.id.bookmark_btn);
        optionsContainer = findViewById(R.id.options_constainer);
        shareBtn = findViewById(R.id.share_button);
        nextBtn = findViewById(R.id.next_button);
        preferences = getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
        gson = new Gson();
        list = new ArrayList<>();
        setId = getIntent().getStringExtra("setId");

        getBookmarks();

        bookmarkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (modelMatch()) {
                    bookmarksList.remove(matchedQuestionPosition);
                    bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_border));
                }else {
                    bookmarksList.add(list.get(position));
                    bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark));
                }
            }
        });

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        loadingDialog.show();
        myRef.child("configuration").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                animation_duration = Integer.parseInt(snapshot.child("animation_duration").getValue().toString());
                selected_answer_text_size = Integer.parseInt(snapshot.child("selected_answer_text_size").getValue().toString());
                next_button_color = snapshot.child("next_button_color").getValue().toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                animation_duration = 250;
                selected_answer_text_size = 17;
                next_button_color = "#8C8989";
            }
        });
        myRef.child("SETS").child(setId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String id = dataSnapshot.getKey();
                    String question = dataSnapshot.child("question").getValue().toString();
                    String a = dataSnapshot.child("optionA").getValue().toString();
                    String b = dataSnapshot.child("optionB").getValue().toString();
                    String c = dataSnapshot.child("optionC").getValue().toString();
                    String d = dataSnapshot.child("optionD").getValue().toString();
                    String correctAns = dataSnapshot.child("correctAns").getValue().toString();
                    List<String> op = new ArrayList<>();
                    op.add(a);
                    op.add(b);
                    op.add(c);
                    op.add(d);
                    Collections.shuffle(op, new Random());
                    list.add(new QuestionModel(question, op.get(0), op.get(1), op.get(2), op.get(3),
                            correctAns, id, setId));
                }
                Collections.shuffle(list, new Random());

                if (list.size() > 0) {
                    for (int i = 0; i < 4; i++) {
                        optionsContainer.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                checkAnswer((Button) view);
                            }
                        });
                    }

                    playAnim(question, 0, list.get(position).getQuestion());
                    nextBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            nextBtn.setEnabled(false);
                            nextBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D3D1D1")));
                            enableOption(true);
                            position++;
                            if (position == list.size()) {
                                Intent scoreIntent = new Intent(QuestionsActivity.this, ScoreActivity.class);
                                scoreIntent.putExtra("score", score);
                                scoreIntent.putExtra("total", list.size());
                                startActivity(scoreIntent);
                                finish();
                                return;
                            }
                            count = 0;
                            playAnim(question, 0, list.get(position).getQuestion());
                        }
                    });

                    shareBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String body = "Q. " + list.get(position).getQuestion() + "\n" +
                                    "(a)" + list.get(position).getOptionA() + "\n" +
                                    "(b)" + list.get(position).getOptionB() + "\n" +
                                    "(c)" + list.get(position).getOptionC() + "\n" +
                                    "(d)" + list.get(position).getOptionD();
                            Intent shareIntent = new Intent (Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Quizzer challenge");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, body);
                            startActivity(Intent.createChooser(shareIntent, "Share via "));
                        }
                    });
                }else {
                    finish();
                    Toast.makeText(QuestionsActivity.this, "No Questions", Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuestionsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeBookmarks();
    }

    private void playAnim(final View view, final int value, final String data) {
        view.animate().alpha(value).scaleX(value).scaleY(value).setDuration(animation_duration).setStartDelay(5).setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (value == 0 && count < 4) {
                    String option = "";
                    if (count == 0) {
                        option = list.get(position).getOptionA();
                    }else if (count == 1) {
                        option = list.get(position).getOptionB();
                    }else if (count == 2) {
                        option = list.get(position).getOptionC();
                    }else if (count == 3) {
                        option = list.get(position).getOptionD();
                    }
                    playAnim(optionsContainer.getChildAt(count), 0, option);
                    count++;
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                // data change
                if (value == 0) {
                    try {
                        ((TextView) view).setText(data);
                        noIndicator.setText(position+1+"/"+list.size());
                        if (modelMatch()) {
                            bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark));
                        }else {
                            bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_border));
                        }
                    }catch (ClassCastException ignored) {
                        ((Button) view).setText(data);
                    }
                    view.setTag(data);
                    playAnim(view, 1, data);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    private void checkAnswer(Button selectedOption) {
        enableOption(false);
        nextBtn.setEnabled(true);
        nextBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(next_button_color)));
        if (selectedOption.getText().toString().equals(list.get(position).getCorrectAns())) {
            // correct
            score++;
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            selectedOption.setTypeface(null, Typeface.BOLD_ITALIC);
            selectedOption.setTextSize(selected_answer_text_size);

        }else {
            // incorrect
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0000")));
            Button correctOption = (Button) optionsContainer.findViewWithTag(list.get(position).getCorrectAns());
            correctOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            correctOption.setTypeface(null, Typeface.BOLD_ITALIC);
            correctOption.setTextSize(selected_answer_text_size);
        }
    }

    private void enableOption(boolean enable) {
        for (int i = 0; i < 4; i++) {
            optionsContainer.getChildAt(i).setEnabled(enable);
            if (enable) {
                optionsContainer.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#989898")));
            }
        }
        Button a = (Button) optionsContainer.findViewWithTag(list.get(position).getOptionA());
        Button b = (Button) optionsContainer.findViewWithTag(list.get(position).getOptionB());
        Button c = (Button) optionsContainer.findViewWithTag(list.get(position).getOptionC());
        Button d = (Button) optionsContainer.findViewWithTag(list.get(position).getOptionD());

        a.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        d.setTypeface(Typeface.DEFAULT_BOLD);

        a.setTextSize(14);
        b.setTextSize(14);
        c.setTextSize(14);
        d.setTextSize(14);


    }

    private void getBookmarks(){
        String json = preferences.getString(KEY_NAME, "");
        Type type = new TypeToken<List<QuestionModel>>(){}.getType();
        bookmarksList = gson.fromJson(json, type);
        if (bookmarksList == null) {
            bookmarksList = new ArrayList<>();
        }
    }

    private void storeBookmarks(){
        String json = gson.toJson(bookmarksList);
        editor.putString(KEY_NAME, json);
        editor.commit();
    }

    private boolean modelMatch(){
        boolean matched = false;
        int i = 0;
        for (QuestionModel model : bookmarksList) {
            if (model.getQuestion().equals(list.get(position).getQuestion())
                && model.getCorrectAns().equals(list.get(position).getCorrectAns())
                && model.getSet().equals(setId)) {
                matched = true;
                matchedQuestionPosition = i;
            }
            i++;
        }
        return matched;
    }
}