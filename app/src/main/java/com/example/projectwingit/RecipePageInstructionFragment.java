package com.example.projectwingit;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.bumptech.glide.Glide;
import com.example.projectwingit.io.LambdaResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

    private String tutorialString;
    private String[] arrayTutorial = null;
    private int arraySize;
    private int recipeID;
    private Button nextButton;
    private Button prevButton;
    private Button finishButton;

    public RecipePageInstructionFragment() {
        // Required empty public constructor
    }

    public RecipePageInstructionFragment(String tutorialString, int recipeID) {
        this.tutorialString = tutorialString;
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

        // Set button visibility
        nextButton = (Button) v.findViewById((R.id.nextButton));
        nextButton.setVisibility(View.GONE);

        prevButton = (Button) v.findViewById((R.id.prevButton));
        prevButton.setVisibility(View.GONE);

        finishButton = (Button) v.findViewById((R.id.finishButton));
        finishButton.setVisibility(View.VISIBLE);

        // Set text using an array
        TextView instructionsText = v.findViewById(R.id.englargedinstructions);
        arrayTutorial = tutorialString.split("\n");

        arraySize = arrayTutorial.length;
        for(int i = 0; i < arraySize; i++) {
            instructionsText.append(arrayTutorial[i]);
            instructionsText.append("\n\n");
        }

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
                getFragmentManager().beginTransaction().replace(R.id.container, new RecipePageFragment(recipeID)).commit();
            }
        });

        // Swap between step by step view and infinite scroll view
        Switch toggle = (Switch) v.findViewById(R.id.switchView);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Step by step view
                    getFragmentManager().beginTransaction().replace(R.id.container, new RecipeInstructionSingleviewFragment(tutorialString, recipeID)).commit();
                } else {
                    //Shouldn't be able to reach this case but will "reload" the page if it does
                    getFragmentManager().beginTransaction().replace(R.id.container, new RecipePageInstructionFragment(tutorialString, recipeID)).commit();
                }
            }
        });

        // Inflate the layout for this fragment
        return v;
    }

}