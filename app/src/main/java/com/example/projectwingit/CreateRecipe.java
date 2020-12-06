package com.example.projectwingit;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CreateRecipe#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreateRecipe extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String tag = "CREATE RECIPE FRAGMENT";
    protected static final int GET_FROM_GALLERY = 1;

    protected ImageView wingImageView;
    protected Button buttonUploadPhoto;
    protected Button buttonRemovePhoto;
    protected Uri imageUri;

    protected View rootView;
    protected RecyclerView mRecyclerView;
    protected IngredientListAdapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ArrayList<String> mIngredientList;

    protected RecyclerView recipeStepsRecyclerView;
    protected RecipeStepListAdapter recipeStepListAdapter;
    protected RecyclerView.LayoutManager recipeStepListLayoutManager;
    protected ArrayList<String> mRecipeStepList;


    protected EditText inputRecipeName;
    protected EditText inputRecipeDescription;
    protected Button buttonAddIngredient;
    protected EditText inputIngredientName;
    protected EditText inputIngredientQuantity;
    protected EditText inputIngredientUnit;

    protected EditText inputRecipeStep;
    protected Button buttonAddRecipeStep;

    protected CheckBox checkBoxContainsNuts;
    protected boolean containsNuts;
    protected CheckBox checkBoxGlutenFree;
    protected boolean isGlutenFree;

    protected Spinner spicinessLevelSpinner;
    protected long spicinessLevel;

    protected Button buttonSubmitRecipe;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CreateRecipe() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CreateRecipe.
     */
    // TODO: Rename and change types and number of parameters
    public static CreateRecipe newInstance(String param1, String param2) {
        CreateRecipe fragment = new CreateRecipe();
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

        mIngredientList = new ArrayList<>();
        mRecipeStepList = new ArrayList<>();
    }

    public void setUpIngredientList() {
        // Recycler view: ingredient list
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.IngredientRecyclerView);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new IngredientListAdapter(mIngredientList);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setNestedScrollingEnabled(false);

        // Set up ingredient input.
        inputIngredientName = (EditText) rootView.findViewById(R.id.InputIngredientName);
        inputIngredientQuantity = (EditText) rootView.findViewById(R.id.InputIngredientQuantity);
        inputIngredientUnit = (EditText) rootView.findViewById(R.id.InputIngredientUnit);
        buttonAddIngredient = (Button) rootView.findViewById(R.id.ButtonAddIngredient);

        // OnClick listener for button to add ingredients.
        buttonAddIngredient.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String userIngredientName = inputIngredientName.getText().toString().trim();
                String userIngredientQuantity = inputIngredientQuantity.getText().toString().trim();
                String userIngredientUnit = inputIngredientUnit.getText().toString().trim();

                CharSequence errMsg = null;

                StringBuilder newIngredient = new StringBuilder();

                // Ingredient name must not be empty.
                if (userIngredientName.length() == 0) {
                    errMsg = "New ingredient name cannot be empty.";
                }

                // There cannot be a unit without a quantity.
                if (userIngredientUnit.length() > 0 && userIngredientQuantity.length() == 0) {
                    errMsg = "Ingredient unit needs a quantity.";
                }

                if (errMsg != null) {
                    showToast(errMsg);
                    return;
                }

                if (userIngredientQuantity.length() > 0) {
                    newIngredient.append(userIngredientQuantity + " ");
                    if (userIngredientUnit.length() > 0) newIngredient.append(userIngredientUnit + " ");
                }

                newIngredient.append(userIngredientName);

                mIngredientList.add(newIngredient.toString());
                mAdapter.notifyDataSetChanged();
                inputIngredientName.setText("");
                inputIngredientQuantity.setText("");
                inputIngredientUnit.setText("");

            }
        });
    }

    public void setUpInstructionList() {
        // Recycler view: recipe step list
        recipeStepsRecyclerView = (RecyclerView) rootView.findViewById(R.id.RecipeStepsRecyclerView);
        recipeStepListLayoutManager = new LinearLayoutManager(getActivity());
        recipeStepListAdapter = new RecipeStepListAdapter(mRecipeStepList);
        recipeStepsRecyclerView.setLayoutManager(recipeStepListLayoutManager);
        recipeStepsRecyclerView.setAdapter(recipeStepListAdapter);
        recipeStepsRecyclerView.setNestedScrollingEnabled(false);

        // INPUT: recipe steps
        inputRecipeStep = (EditText) rootView.findViewById(R.id.InputRecipeStep);
        buttonAddRecipeStep = (Button) rootView.findViewById(R.id.ButtonAddRecipeStep);

        // OnClick listener for button to add steps.
        buttonAddRecipeStep.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String userRecipeStep = inputRecipeStep.getText().toString().trim();

                CharSequence errMsg = null;

                // Cannot be empty.
                if (userRecipeStep.length() == 0) {
                    errMsg = "New recipe instruction cannot be empty.";
                    showToast(errMsg);
                    return;
                }

                mRecipeStepList.add(userRecipeStep);
                recipeStepListAdapter.notifyDataSetChanged();
                inputRecipeStep.setText("");
            }
        });
    }

    public void setUpSpicinessSpinner() {
        // Spiciness level selection.
        spicinessLevelSpinner = (Spinner) rootView.findViewById(R.id.SpicinessLevelSpinner);
        ArrayAdapter<CharSequence> spicinessLevelAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.spiciness_level_array, android.R.layout.simple_spinner_item);
        spicinessLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spicinessLevelSpinner.setAdapter(spicinessLevelAdapter);

        spicinessLevel = -1;
        spicinessLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spicinessLevel = id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not sure if I need to implement this method.
                ;
            }
        });
    }

    public void setUpSubmitButton() {
        // Submit button
        buttonSubmitRecipe = (Button) rootView.findViewById(R.id.ButtonSubmitRecipe);
        buttonSubmitRecipe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String recipeName = inputRecipeName.getText().toString().trim();
                String recipeDescription = inputRecipeDescription.getText().toString().trim();

                CharSequence errMsg = null;

                if (recipeName.length() == 0) {
                    errMsg = "Recipe name cannot be empty.";
                } else if (mIngredientList.size() == 0) {
                    errMsg = "The recipe needs at least one ingredient.";
                } else if (mRecipeStepList.size() == 0) {
                    errMsg = "The recipe needs at least one instruction/step.";
                }

                if (errMsg != null) {
                    showToast(errMsg);
                    return;
                }

                // Submission here. Need to use LambdaRequests.
                // For now: just log current recipe state.
                Log.i(tag, "Recipe name: " + recipeName);
                Log.i(tag, "Recipe description: " + recipeDescription);
                if (imageUri != null) {
                    Log.i(tag, "Image uri " + imageUri.toString());
                } else {
                    Log.i(tag, "No image selected");
                }
                for (String ingredient: mIngredientList) {
                    Log.i(tag, "Ingredient: " + ingredient);
                }
                for (String recipeStep: mRecipeStepList) {
                    Log.i(tag, "Instruction: " + recipeStep);
                }
                Log.i(tag, "Contains nuts " + containsNuts);
                Log.i(tag, "Is gluten free " + isGlutenFree);
                Log.i(tag, "Spiciness level " + spicinessLevel);
            }
        });
    }

    public void setUpContainsNutsCheckbox() {
        checkBoxContainsNuts = rootView.findViewById(R.id.ContainsNutsCheckbox);
        containsNuts = false;
        checkBoxContainsNuts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                containsNuts = isChecked;
            }
        });
    }

    public void setUpGlutenFreeCheckbox() {
        checkBoxGlutenFree = rootView.findViewById(R.id.GlutenFreeCheckbox);
        isGlutenFree = false;
        checkBoxGlutenFree.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isGlutenFree = isChecked;
            }
        });
    }

    public void setUpPhotoUpload() {
        // Only photo upload so far. Still figuring how to take photos. (delegate to camera)

        buttonUploadPhoto = rootView.findViewById(R.id.ButtonUploadPhoto);
        buttonRemovePhoto = rootView.findViewById(R.id.ButtonRemovePhoto);
        wingImageView = rootView.findViewById(R.id.WingImageView);
        imageUri = null;

        buttonUploadPhoto.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent uploadFromGalleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                if (uploadFromGalleryIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(uploadFromGalleryIntent, GET_FROM_GALLERY);
                } else {
                    Log.i(tag, "No upload from gallery activity available.");
                }
            }

        });

        buttonRemovePhoto.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                imageUri = null;
                wingImageView.setImageResource(android.R.color.transparent);
            }
        });
    }

    private void showToast(CharSequence msg) {
        Toast toast;
        toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                wingImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                showToast("Unable to load image uri.");
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_create_recipe, container, false);

        // Set up recipe name.
        inputRecipeName = (EditText) rootView.findViewById(R.id.InputRecipeName);

        // Set up recipe description.
        inputRecipeDescription = (EditText) rootView.findViewById(R.id.InputRecipeDescription);

        setUpIngredientList();
        setUpInstructionList();
        setUpContainsNutsCheckbox();
        setUpGlutenFreeCheckbox();
        setUpSpicinessSpinner();
        setUpSubmitButton();
        setUpPhotoUpload();

        return rootView;
    }
}