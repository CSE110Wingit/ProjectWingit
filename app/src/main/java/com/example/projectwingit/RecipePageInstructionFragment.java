package com.example.projectwingit;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.projectwingit.io.LambdaResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Scanner;

import static com.example.projectwingit.io.LambdaRequests.getRecipe;
import static com.example.projectwingit.utils.WingitLambdaConstants.GLUTEN_FREE_STR;
import static com.example.projectwingit.utils.WingitLambdaConstants.NUT_ALLERGY_STR;
import static com.example.projectwingit.utils.WingitLambdaConstants.RECIPE_DESCRIPTION_STR;
import static com.example.projectwingit.utils.WingitLambdaConstants.RECIPE_INGREDIENTS_STR;
import static com.example.projectwingit.utils.WingitLambdaConstants.RECIPE_PICTURE_STR;
import static com.example.projectwingit.utils.WingitLambdaConstants.RECIPE_TITLE_STR;
import static com.example.projectwingit.utils.WingitLambdaConstants.RECIPE_TUTORIAL_STR;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RecipePageInstructionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecipePageInstructionFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private int recipeID;

    public RecipePageInstructionFragment() {
        // Required empty public constructor
    }

    public RecipePageInstructionFragment(int recipeID) {
        this.recipeID = recipeID;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RecipePageInstructionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RecipePageInstructionFragment newInstance(String param1, String param2) {
        RecipePageInstructionFragment fragment = new RecipePageInstructionFragment();
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

        View v = inflater.inflate(R.layout.fragment_recipe_page_instruction, container, false);

        LambdaResponse recipeLambdaResponse = getRecipe(recipeID);
        while (recipeLambdaResponse.isRunning()) {}

        JSONObject recipeObject = recipeLambdaResponse.getResponseJSON();
        while (recipeLambdaResponse.isRunning()) {}

        TextView instructionsText = v.findViewById(R.id.englargedinstructions);

        String newInstruct = "";
        try {
            String oldInstruct = recipeObject.getString(RECIPE_TUTORIAL_STR);
            newInstruct = oldInstruct.replaceAll("\n", "\n\n");
            instructionsText.setText(newInstruct);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*try {
            instructionsText.setText(recipeObject.getString(RECIPE_TUTORIAL_STR));
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        // Inflate the layout for this fragment
        return v;
    }
}