package com.example.projectwingit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;


public class RecipeListRecyclerViewAdapter extends RecyclerView.Adapter<RecipeListRecyclerViewAdapter.ViewHolder> {

    private ArrayList<String> mRecipeImages = new ArrayList<>();
    private ArrayList<String> mRecipeTitles = new ArrayList<>();
    private ArrayList<String> mRecipeCategories = new ArrayList<>();
    private ArrayList<String> mRecipeDescriptions = new ArrayList<>();
    private ArrayList<Boolean> mIsFavorited = new ArrayList<>();
    private Context mContext;
    private OnRecipeListener monRecipeListener;

    public RecipeListRecyclerViewAdapter(ArrayList<String> mRecipeImages, ArrayList<String> mRecipeTitles,
                                         ArrayList<String> mRecipeCategories, ArrayList<String> mRecipeDescriptions,
                                         Context mContext, OnRecipeListener onRecipeListener, ArrayList<Boolean> mIsFavorited) {
        this.mRecipeImages = mRecipeImages;
        this.mRecipeTitles = mRecipeTitles;
        this.mRecipeCategories = mRecipeCategories;
        this.mRecipeDescriptions = mRecipeDescriptions;
        this.mContext = mContext;
        this.monRecipeListener = onRecipeListener;
        this.mIsFavorited = mIsFavorited;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_card_layout, parent, false);
        ViewHolder holder = new ViewHolder(view, monRecipeListener);
        holder.setIsRecyclable(false);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(mContext).asBitmap().load(mRecipeImages.get(position)).into(holder.recipeImage);

        holder.recipeTitle.setText(mRecipeTitles.get(position));
        holder.recipeCategory.setText(mRecipeCategories.get(position));
        holder.recipeDescription.setText(mRecipeDescriptions.get(position));

        if(mIsFavorited.get(position)) {
            holder.favoriteButton.setText("In Favorites");
            holder.favoriteButton.setIconResource(R.drawable.ic_recipe_in_favorites);
            holder.favoriteButton.setVisibility(View.VISIBLE);
        }

        holder.recipeCard.setOnClickListener(holder);
        };

    @Override
    public int getItemCount() {
        return mRecipeTitles.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView recipeImage;
        TextView recipeTitle;
        TextView recipeCategory;
        TextView recipeDescription;
        OnRecipeListener onRecipeListener;
        MaterialCardView recipeCard;
        MaterialButton favoriteButton;

        public ViewHolder(@NonNull View itemView, OnRecipeListener onRecipeListener) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            recipeTitle = itemView.findViewById(R.id.recipe_title);
            recipeCategory = itemView.findViewById(R.id.recipe_category);
            recipeDescription = itemView.findViewById(R.id.recipe_description);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
            this.recipeCard = itemView.findViewById(R.id.recipe_card_id);
            this.onRecipeListener = onRecipeListener;
            
            //itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onRecipeListener.onRecipeClick(getAdapterPosition());
        }
    }

    public interface OnRecipeListener {
        void onRecipeClick(int position);
    }

}
