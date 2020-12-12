package com.example.projectwingit;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.projectwingit.io.UserInfo;

import org.w3c.dom.Text;

// fragment for displaying the edit characteristics page, but most of the code for changing profile
// values is located in UserAccount.java, such as the listeners for the ui components
public class EditCharacteristicsFragment extends Fragment{


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EditCharacteristicsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EditCharacteristicsFragment
     */
    // TODO: Rename and change types and number of parameters
    public static EditCharacteristicsFragment newInstance(String param1, String param2) {
        EditCharacteristicsFragment fragment = new EditCharacteristicsFragment();
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
        View v = inflater.inflate(R.layout.fragment_edit_characteristics, container, false);

        CheckBox glutenCB = v.findViewById(R.id.checkbox_gluten_free);
        glutenCB.setChecked(UserInfo.CURRENT_USER.getGlutenFree());

        CheckBox nutCB = v.findViewById(R.id.checkbox_nut_allergy);
        nutCB.setChecked(UserInfo.CURRENT_USER.getNutAllergy());

        SeekBar spiceSB = v.findViewById(R.id.spice_seekbar);
        spiceSB.setProgress(UserInfo.CURRENT_USER.getSpicinessLevel());

        TextView spiceTB = v.findViewById(R.id.spice_textview);
        spiceTB.setText("Spice Level Preference: " + UserInfo.CURRENT_USER.getSpicinessLevel());

        // setting listeners for the edit preference sweet and spicy seekbars
        spiceSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                TextView spiceTV = v.findViewById(R.id.spice_textview);
                spiceTV.setText("Spice Level Preference: " + progress);
                UserAccount.prefSpiciness = progress;
            }
        });

        return v;
    }




}
