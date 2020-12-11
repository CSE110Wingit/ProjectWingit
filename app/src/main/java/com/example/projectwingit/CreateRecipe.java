package com.example.projectwingit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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

import com.example.projectwingit.io.LambdaRequests;
import com.example.projectwingit.io.LambdaResponse;
import com.example.projectwingit.io.UserInfo;
import com.example.projectwingit.utils.WingitLambdaConstants;

import org.json.JSONException;
import org.json.JSONObject;

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
    protected static final int UPLOAD_IMAGE = 1;

    protected ImageView wingImageView;
    protected Button buttonUploadImage;
    protected Button buttonRemoveImage;
    protected Uri imageURI;
    protected String imageURL;
    protected Bitmap imageBitmap;

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

    protected CheckBox checkBoxIsPrivate;
    protected boolean isPrivate;

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

    // methods with runOnUiThread: navigateToRecipePage
    private void navigateToRecipePage(int recipeId) {
        Runnable r;
        r = new Runnable() {
            @Override
            public void run() {
                showToast("Recipe successfully created! Recipe ID " + recipeId);
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.container, new RecipePageFragment(recipeId)).commit();
            }
        };
        getActivity().runOnUiThread(r);
    }

    private void notifyCreateRecipeError(boolean isServerError, String errMsg) {
        Runnable r;
        StringBuilder toastMsg = new StringBuilder();
        if (isServerError) {
            toastMsg.append("Server error: ");
        } else {
            toastMsg.append("Client error: ");
        }
        toastMsg.append(errMsg + " Please contact app developers to resolve this issue.");
        r = new Runnable() {
            @Override
            public void run() {
                showToast(toastMsg.toString());
            }
        };
        getActivity().runOnUiThread(r);
    }

    private class ProcessCreateRecipeResponseThread extends Thread {
        LambdaResponse response;
        public ProcessCreateRecipeResponseThread(LambdaResponse presponse) {
            response = presponse;
        }

        public void run() {
            // Wait until a response comes back from the lambda api.
            while (response.isRunning());

            // Check for errors
            if (response.isClientError()) {
                Log.e(tag, "Client error: " + response.getErrorMessage());
                Log.i(tag, response.getResponseInfo());
                notifyCreateRecipeError(false, response.getErrorMessage());
            } else if (response.isServerError()) {
                Log.e(tag, "Server error: " + response.getErrorMessage());
                Log.i(tag, response.getResponseInfo());
                notifyCreateRecipeError(true, response.getErrorMessage());
            } else {
                // Call navigateToRecipePage here with the correct recipe id.
                // FIXME: replace -1 with the recipe's id.
                Log.i(tag, "Success: " + response.getErrorMessage());
                Log.i(tag, response.getResponseInfo());
                JSONObject responseJSON = response.getResponseJSON();
                try {
                    int recipeID = responseJSON.getInt(WingitLambdaConstants.RECIPE_ID_STR);
                    navigateToRecipePage(recipeID);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setUpSubmitButton() {
        // Submit button
        buttonSubmitRecipe = (Button) rootView.findViewById(R.id.ButtonSubmitRecipe);
        buttonSubmitRecipe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Get the inputted recipe title.
                String recipeTitle = inputRecipeName.getText().toString().trim();

                CharSequence errMsg = null;

                // Check for required fields.
                if (recipeTitle.length() == 0) {
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

                // Ingredient array to post.
                String[] recipeIngredients = mIngredientList.toArray(new String[0]);

                // Recipe description to post. Optional.
                String recipeDescription = inputRecipeDescription.getText().toString().trim();
                if (recipeDescription.length() == 0) {
                    recipeDescription = "No description available.";
                }

                // Tutorial. One big string separated by newlines.
                String recipeTutorial;
                StringBuilder recipeTutorialBuilder = new StringBuilder();
                for (String step : mRecipeStepList) {
                    recipeTutorialBuilder.append(step + "\n");
                }
                recipeTutorial = recipeTutorialBuilder.toString().trim();
                String[] logRecipeTutorial = recipeTutorial.split("\n");
                for (String instruction: logRecipeTutorial) {
                    Log.i(tag, instruction);
                }

//                Log.i(tag, "Recipe name: " + recipeTitle);
//                Log.i(tag, "Recipe description: " + recipeDescription);
//                Log.i(tag, "Recipe ingredients: " + recipeIngredients.toString());
//                Log.i(tag, "Tutorial: " + recipeTutorial);
//                Log.i(tag, "Contains nuts " + containsNuts);
//                Log.i(tag, "Is gluten free " + isGlutenFree);
//                Log.i(tag, "Spiciness level " + spicinessLevel);
//                Log.i(tag, "Is private " + isPrivate);
//                Log.i(tag, "Current user " + UserInfo.CURRENT_USER.getUsername());


                LambdaResponse createRecipeResponse = LambdaRequests.createRecipe(recipeTitle,
                        recipeIngredients, recipeDescription, recipeTutorial, containsNuts,
                        isGlutenFree, (int) spicinessLevel, isPrivate, imageBitmap);

                new ProcessCreateRecipeResponseThread(createRecipeResponse).run();

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

    private void setUpIsPrivateCheckbox() {
        checkBoxIsPrivate = (CheckBox) rootView.findViewById(R.id.IsPrivateCheckbox);
        isPrivate = false;
        checkBoxIsPrivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isPrivate = isChecked;
            }
        });
    }

    public void setUpImageUpload() {
        // Only existing image upload so far. Still figuring how to take photos. (delegate to camera)

        buttonUploadImage = rootView.findViewById(R.id.ButtonUploadImage);
        buttonRemoveImage = rootView.findViewById(R.id.ButtonRemoveImage);
        wingImageView = rootView.findViewById(R.id.WingImageView);
        imageURI = null;
        imageURL = null;
        imageBitmap = null;

        buttonUploadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent uploadImageIntent = new Intent(Intent.ACTION_GET_CONTENT);

                // Only alow jpg and png types.
                String[] imageMimeTypes = {"image/jpeg", "image/jpg", "image/png"};
                uploadImageIntent.setType(imageMimeTypes.length == 1 ? imageMimeTypes[0] : "*/*");

                if (imageMimeTypes.length > 0) {
                    uploadImageIntent.putExtra(Intent.EXTRA_MIME_TYPES, imageMimeTypes);
                }
                if (uploadImageIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(uploadImageIntent, UPLOAD_IMAGE);
                } else {
                    Log.i(tag, "No activity for image upload found on this device.");
                }
            }

        });

        buttonRemoveImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                imageURI = null;
                imageBitmap = null;
                wingImageView.setImageResource(android.R.color.transparent);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPLOAD_IMAGE && resultCode == Activity.RESULT_OK) {
            imageURI = data.getData();
            try {
                if (Build.VERSION.SDK_INT < 28) {
                    imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageURI);
                    wingImageView.setImageBitmap(imageBitmap);
                } else {
                    ImageDecoder.Source source = ImageDecoder.createSource(getActivity().getContentResolver(), imageURI);
                    imageBitmap = ImageDecoder.decodeBitmap(source);
                    wingImageView.setImageBitmap(imageBitmap);
                }
            } catch (IOException e) {
                showToast("Unable to load image uri.");
            }
        }

    }

    private void showToast(CharSequence msg) {
        Toast toast;
        toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();
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
        setUpIsPrivateCheckbox();
        setUpImageUpload();

        return rootView;
    }
}