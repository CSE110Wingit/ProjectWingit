package com.example.projectwingit;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.projectwingit.io.LambdaRequests;
import com.example.projectwingit.io.LambdaResponse;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link passwordChangeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class passwordChangeFragment extends Fragment {

    Dialog errorDialog;
    TextView errorText;
    EditText currentEmail;
    EditText currentPass;
    EditText newPass;
    Button cp_Button;
    Button errorButton;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public passwordChangeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment passwordChangeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static passwordChangeFragment newInstance(String param1, String param2) {
        passwordChangeFragment fragment = new passwordChangeFragment();
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
        return inflater.inflate(R.layout.fragment_password_change, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentEmail = view.findViewById(R.id.cp_Email);
        currentPass = view.findViewById(R.id.cp_currentPass);
        newPass = view.findViewById(R.id.cp_newPass);
        cp_Button = view.findViewById(R.id.change_password_button);

        cp_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

    }

    private void changePassword() {
        String email, currentPassword, newPassword, hashCurrentPass, hashNewPass;
        email = currentEmail.getText().toString();
        currentPassword = currentPass.getText().toString();
        newPassword = newPass.getText().toString();

        hashCurrentPass = com.example.projectwingit.utils.WingitUtils.hashPassword(currentPassword);
        hashNewPass = com.example.projectwingit.utils.WingitUtils.hashPassword(newPassword);

        LambdaResponse changePassword = LambdaRequests.changePassword(email, hashCurrentPass, hashNewPass);

        cp_Button.post(new Runnable() {
            @Override
            public void run() {
                errorDialog = new Dialog(getActivity());
                errorDialog.setContentView(R.layout.error_dialog);
                errorText = (TextView)errorDialog.findViewById(R.id.error_dialog_text1);
                errorText.setText(changePassword.getResponseInfo());
                errorDialog.show();
                errorButton = (Button)errorDialog.findViewById(R.id.error_dialog_button);
                if (!changePassword.isError()) {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                }
//                Toast.makeText(getActivity().getApplicationContext(), LoginInfo.CURRENT_LOGIN.username, Toast.LENGTH_LONG).show();
                errorButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        errorDialog.cancel();
                    }
                });
            }
        });

    }
}