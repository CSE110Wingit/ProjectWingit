package com.example.projectwingit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecipeStepListAdapter extends RecyclerView.Adapter<RecipeStepListAdapter.RecipeStepViewHolder> {

    private final ArrayList<String> mRecipeStepList;

    class RecipeStepViewHolder extends RecyclerView.ViewHolder {

        private final TextView recipeStep;
        private final Button buttonDeleteStep;
        public RecipeStepViewHolder(View v) {
            super(v);
            recipeStep = (TextView) v.findViewById(R.id.RecipeStep);
            buttonDeleteStep = (Button) v.findViewById(R.id.ButtonDeleteRecipeStep);


            // OnClick handler: delete ingredient
            buttonDeleteStep.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    mRecipeStepList.remove(position);
                    notifyDataSetChanged();

                }
            });
        }

        public TextView getTextView() {
            return recipeStep;
        }

    }

    public RecipeStepListAdapter(ArrayList<String> recipeStepList) {
        mRecipeStepList = recipeStepList;
    }

    @NonNull
    @Override
    public RecipeStepListAdapter.RecipeStepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_step_list_item, parent, false);

        return new RecipeStepListAdapter.RecipeStepViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeStepListAdapter.RecipeStepViewHolder holder, int position) {
        holder.getTextView().setText(Integer.toString(position + 1) + ". " + mRecipeStepList.get(position));
    }

    @Override
    public int getItemCount() {
        return mRecipeStepList.size();
    }
}

