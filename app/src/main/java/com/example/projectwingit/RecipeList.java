package com.example.projectwingit;

import android.os.Bundle;

import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.example.projectwingit.io.LambdaRequests;
import com.example.projectwingit.io.LambdaResponse;
import com.example.projectwingit.utils.LoginInfo;
import com.example.projectwingit.utils.WingitUtils;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RecipeList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecipeList extends Fragment implements RecipeListRecyclerViewAdapter.OnRecipeListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ArrayList<String> mRecipeImageUrls = new ArrayList<>();
    private ArrayList<String> mRecipeTitles = new ArrayList<>();
    private ArrayList<String> mRecipeCategories = new ArrayList<>();
    private ArrayList<String> mRecipeDescriptions = new ArrayList<>();

    public RecipeList() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RecipeList.
     */
    // TODO: Rename and change types and number of parameters
    public static RecipeList newInstance(String param1, String param2) {
        RecipeList fragment = new RecipeList();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recipe_list, container, false);

        initImageBitmaps(v);

        // Inflate the layout for this fragment
        return v;
    }


    private void initImageBitmaps(View v){
        mRecipeImageUrls.add("https://preview.redd.it/vs72r3qmvw161.jpg?width=960&crop=smart&auto=webp&s=069b95767b62037912016943bf95c635fd31e108");
        mRecipeImageUrls.add("https://external-preview.redd.it/TlyTd28U32ciIa4jdWBNCVsVxIbvKtdzlTVrSkp9Qb8.jpg?width=960&crop=smart&auto=webp&s=24f30acc7b93f4b459f03227727d7b536c679e1c");
        mRecipeImageUrls.add("https://preview.redd.it/0l8f7bh0dt061.jpg?width=960&crop=smart&auto=webp&s=3a9503b91f7f3862cecddebcdd60a6dd583d184b");

        mRecipeTitles.add("Recipe 1");
        mRecipeTitles.add("Recipe 2");
        mRecipeTitles.add("Recipe 3");

        mRecipeCategories.add("Category 1");
        mRecipeCategories.add("Category 2");
        mRecipeCategories.add("Category 3");

        mRecipeDescriptions.add("Description 1");
        mRecipeDescriptions.add("Description 2");
        mRecipeDescriptions.add("Description 3");

        initRecyclerView(v);
    }

    private void initRecyclerView(View v) {
        RecyclerView recyclerView = (RecyclerView)  v.findViewById(R.id.recycler_view_container);
        RecipeListRecyclerViewAdapter adapter = new RecipeListRecyclerViewAdapter(mRecipeImageUrls, mRecipeTitles, mRecipeCategories,mRecipeDescriptions, getContext(), this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onRecipeClick(int position) {
        LoginInfo.setCurrentLogin("JustWingit","cse110wingit@gmail.com", WingitUtils.hashPassword("wingit!1"));
        LambdaResponse recipe = LambdaRequests.getRecipe(1);
        while(recipe.isRunning()){
            System.out.println("Running!");
        }
        JSONObject ofRecipe = recipe.getResponseJSON();
        System.out.println(ofRecipe);
        String recipeName = "";
        try {
            recipeName = ofRecipe.getString("recipe_title");
        } catch (JSONException e) {
            recipeName = "Null";
        }

        // TODO eventually we will pass in an entire recipe object here but for now it is just the title
        Fragment recipeFragment = new RecipePageFragment(recipeName);
        getFragmentManager().beginTransaction().replace(R.id.container, recipeFragment).commit();
    }
}