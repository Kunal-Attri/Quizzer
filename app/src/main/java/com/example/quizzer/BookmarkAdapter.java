package com.example.quizzer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.viewholder> {

    public List<QuestionModel> list;
    public BookmarkAdapter(List<QuestionModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_item, parent, false);
        return new viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewholder holder, int position) {
        holder.setData(list.get(position).getQuestion(), list.get(position).getCorrectAns(), position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class viewholder extends RecyclerView.ViewHolder{

            private TextView question, answer;
            private ImageButton deleteBtn;

        public viewholder(@NonNull View itemView) {
                super(itemView);

            question = itemView.findViewById(R.id.question);
            answer = itemView.findViewById(R.id.answer);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }

        private void setData(String question, String answer, final int position) {
            this.question.setText(question);
            this.answer.setText(answer);

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final int deletedIndex = getAdapterPosition();
                    final QuestionModel deletedQuestion = list.get(getAdapterPosition());

                    list.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());

                    Snackbar snackbar = Snackbar
                            .make(itemView, "Question removed!", Snackbar.LENGTH_LONG);
                    snackbar.setAction("UNDO", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            // undo is selected, restore the deleted item
                            list.add(deletedIndex, deletedQuestion);
                            notifyItemInserted(deletedIndex);
                        }
                    });
                    snackbar.setActionTextColor(Color.LTGRAY);
                    snackbar.show();
                }
            });

        }
    }

}
