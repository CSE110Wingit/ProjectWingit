package com.example.projectwingit;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.projectwingit.io.UserInfo;


public class CreatedRecipeList extends Fragment{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CreatedRecipeList() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FavoritesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CreatedRecipeList newInstance(String param1, String param2) {
        CreatedRecipeList fragment = new CreatedRecipeList();
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

        View v = inflater.inflate(R.layout.fragment_createdrecipes, container, false);
        if (UserInfo.CURRENT_USER.isLoggedIn()) {
            if (UserInfo.CURRENT_USER.getCreatedRecipes().length == 0) {
                TextView createdRecipeText = v.findViewById(R.id.createdrec_text);
                createdRecipeText.setHint("No created recipes. Make some!");
            }
            else {
                RecipeList myList = new RecipeList();
                myList.typeResults("", Boolean.FALSE, Boolean.FALSE, 5, null, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
                getFragmentManager().beginTransaction().replace(R.id.container, myList).commit();
            }

        }

        // Inflate the layout for this fragment
        return v;
    }
}
