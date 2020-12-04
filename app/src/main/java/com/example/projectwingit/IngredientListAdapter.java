package com.example.projectwingit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class IngredientListAdapter extends RecyclerView.Adapter<IngredientListAdapter.IngredientViewHolder> {

    private final ArrayList<String> mIngredientList;

    class IngredientViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;
        private final Button buttonDeleteIngredient;
        public IngredientViewHolder(View v) {
            super(v);
            textView = (TextView) v.findViewById(R.id.RecipeStep);
            buttonDeleteIngredient = (Button) v.findViewById(R.id.ButtonDeleteRecipeStep);

            // OnClick handler: delete ingredient
            buttonDeleteIngredient.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    mIngredientList.remove(position);
                    notifyDataSetChanged();

                }
            });
        }

        public TextView getTextView() {
            return textView;
        }

    }

    public IngredientListAdapter(ArrayList<String> ingredientList) {
        mIngredientList = ingredientList;
    }

    @NonNull
    @Override
    public IngredientListAdapter.IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ingredientlist_item, parent, false);

        return new IngredientListAdapter.IngredientViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientListAdapter.IngredientViewHolder holder, int position) {
        holder.getTextView().setText(mIngredientList.get(position));
    }

    @Override
    public int getItemCount() {
        return mIngredientList.size();
    }
}

