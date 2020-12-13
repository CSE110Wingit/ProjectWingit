package com.example.projectwingit;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.projectwingit.io.UserInfo;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Button searchRecipeButton;
    EditText searchText;
    String searchRecipeText;

    public SearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Settings.
     */
    // TODO: Rename and change types and number of parameters
    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
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
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        // rating button characteristics
        searchRecipeButton = (Button) v.findViewById(R.id.search_wing_recipe);
        searchText = v.findViewById(R.id.search_bar_text);

        searchRecipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchRecipeText = searchText.getText().toString();
                RecipeList rl = new RecipeList();
                if(UserInfo.CURRENT_USER.isLoggedIn()){

                    Boolean containsNuts;
                    Boolean glutenFree;
                    if(UserInfo.CURRENT_USER.getNutAllergy()){
                        containsNuts = false;
                    }
                    else{
                        containsNuts = null;
                    }
                    if(UserInfo.CURRENT_USER.getGlutenFree()){
                        glutenFree = true;
                    }
                    else{
                        glutenFree = null;
                    }
                    rl.typeResults(searchRecipeText, containsNuts,
                            glutenFree,
                            UserInfo.CURRENT_USER.getSpicinessLevel(), null,
                            Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
                }
                else{
                    rl.typeResults(searchRecipeText, null, null, 3, null,Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
                }
                getFragmentManager().beginTransaction().replace(R.id.container, rl).addToBackStack(null).commit();
            }
        });


        // Inflate the layout for this fragment
        return v;
    }
}