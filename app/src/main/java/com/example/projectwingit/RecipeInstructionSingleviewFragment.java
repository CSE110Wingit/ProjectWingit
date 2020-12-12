package com.example.projectwingit;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RecipeInstructionSingleviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecipeInstructionSingleviewFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private String tutorialString;
    private String[] arrayTutorial = null;
    private int currentStep = 0;
    private int arraySize;
    private int recipeID;
    private Button nextButton;
    private Button prevButton;
    private Button finishButton;


    public RecipeInstructionSingleviewFragment() {
        // Required empty public constructor
    }

    public RecipeInstructionSingleviewFragment(String tutorialString, int recipeID) {
        this.tutorialString = tutorialString;
        this.recipeID = recipeID;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RecipeInstructionSingleviewFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RecipeInstructionSingleviewFragment newInstance(String param1, String param2) {
        RecipeInstructionSingleviewFragment fragment = new RecipeInstructionSingleviewFragment();
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
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_recipe_page_instruction, container, false);

        // Set text using an array
        TextView instructionsText = v.findViewById(R.id.englargedinstructions);
        arrayTutorial = tutorialString.split("\n");
        arraySize = arrayTutorial.length;
        instructionsText.setText(arrayTutorial[currentStep]);

        // Set button visibility
        prevButton = (Button) v.findViewById((R.id.prevButton));
        prevButton.setVisibility(View.GONE);

        finishButton = (Button) v.findViewById((R.id.finishButton));
        finishButton.setVisibility(View.GONE);

        // Next button functionality
        nextButton = (Button) v.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentStep < arraySize){
                    // Update text
                    instructionsText.setText("");
                    instructionsText.append(arrayTutorial[++currentStep]);

                    // Update buttons
                    if(currentStep > 0) {
                        prevButton.setVisibility(View.VISIBLE);
                    }
                    if(currentStep == arraySize - 1){
                        finishButton.setVisibility(View.VISIBLE);
                        nextButton.setVisibility(View.GONE);
                    }
                }
            }
        });

        // Prev button functionality
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentStep <= arraySize){
                    // Update text
                    instructionsText.setText("");
                    instructionsText.append(arrayTutorial[--currentStep]);

                    // Update buttons
                    if(currentStep == 0) {
                        prevButton.setVisibility(View.GONE);
                    }
                    if(currentStep == arraySize - 2){
                        finishButton.setVisibility(View.GONE);
                        nextButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        // Finish button functionality
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
                getFragmentManager().beginTransaction().replace(R.id.container, new RecipePageFragment(recipeID)).commit();
            }
        });

        // Swap between step by step view and infinite scroll view
        Switch toggle = (Switch) v.findViewById(R.id.switchView);
        toggle.setChecked(true);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    // Infinite scrolling view
                    getFragmentManager().beginTransaction().replace(R.id.container, new RecipePageInstructionFragment(tutorialString, recipeID)).commit();
                } else {
                    // Shouldn't be able to reach this case but will "reload" the page if it does
                    getFragmentManager().beginTransaction().replace(R.id.container, new RecipeInstructionSingleviewFragment(tutorialString, recipeID)).commit();
                }
            }
        });

        return v;
    }
}