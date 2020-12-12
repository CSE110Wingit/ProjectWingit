package com.example.projectwingit;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.text.TextUtils;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.projectwingit.io.LambdaRequests;
import com.example.projectwingit.io.LambdaResponse;

import org.w3c.dom.Text;


public class RegisterFragment extends Fragment implements OnClickListener {

    View rootView;
    Dialog errorDialog;
    TextView errorText;
    EditText userText;
    EditText emailText;
    EditText passText;
    Button signUpText;
    Button errorButton;
    CheckBox checkBoxContainsNutAllergy;
    CheckBox checkBoxGlutenAllergy;
    boolean containsNutAllergy;
    boolean containsGlutenAllergy;
    Spinner spicinessSelect;
    int spiciness;

    public RegisterFragment() {
        // Required empty public constructor
    }

//    public static RegisterFragment newInstance(String param1, String param2) {
//        RegisterFragment fragment = new RegisterFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Intent intent = new Intent(getActivity(), MainActivity.class);
//        startActivity(intent);
//    }
    @Override
    public void onClick(View view) {
////        Intent intent = new Intent(getActivity(), MainActivity.class);
////        startActivity(intent);
//          FragmentTransaction ft = getFragmentManager().beginTransaction();
//          ft.replace(R.id.container_user_account, new LoginFragment());
//          ft.commit();
//        userRegister();
    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        getActivity().setContentView(R.layout.fragment_register);
//        userText = userText.findViewById(R.id.userReg);
//        emailText = emailText.findViewById(R.id.emailReg);
//        passText = passText.findViewById(R.id.passReg);
//        signUpText = signUpText.findViewById(R.id.regSignUp);
//
//        signUpText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                userRegister();
//            }
//        });
//    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView back_Login = getActivity().findViewById(R.id.tv_Footer);
//        back_Login.setOnClickListener((View.OnClickListener) this);

        signUpText = (Button) view.findViewById(R.id.regSignUp);
        userText = (EditText) view.findViewById(R.id.userReg);
        emailText = (EditText) view.findViewById(R.id.emailReg);
        passText = (EditText) view.findViewById(R.id.passReg);


        back_Login.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container_user_account, new LoginFragment());
                ft.commit();
            }
        });

        signUpText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                userRegister(v);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    private void userRegister(View v) {
        String username, email, password, hashPass;
        username = userText.getText().toString();
        email = emailText.getText().toString();
        password = passText.getText().toString();
        hashPass = com.example.projectwingit.utils.WingitUtils.hashPassword(password);
        if (password.length() <= 8) {
            LambdaResponse registration = LambdaRequests.createAccount(username, email, hashPass, true, true, 3);

            signUpText.post(new Runnable() {
                @Override
                public void run() {
                    errorDialog = new Dialog(getActivity());
                    errorDialog.setContentView(R.layout.error_dialog);
                    errorText = (TextView) errorDialog.findViewById(R.id.error_dialog_text1);
                    errorText.setText(registration.getResponseInfo());
                    errorDialog.show();
                    LambdaRequests.logout();
                    errorButton = (Button) errorDialog.findViewById(R.id.error_dialog_button);
                    errorButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            errorDialog.cancel();
                        }
                    });
                    if (!registration.isError()) {
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        startActivity(intent);
                    }
                }

            });


//        if (TextUtils.isEmpty(username)) {
//            Toast.makeText(getActivity().getApplicationContext(), "Enter a username", Toast.LENGTH_LONG).show();
//        }
//        if (TextUtils.isEmpty(email)) {
//            Toast.makeText(getActivity().getApplicationContext(), "Enter an email", Toast.LENGTH_LONG).show();
//        }
//        if (TextUtils.isEmpty(password)) {
//            Toast.makeText(getActivity().getApplicationContext(), "Enter a password", Toast.LENGTH_LONG).show();
//        }

        } else {
            errorDialog = new Dialog(getActivity());
            errorDialog.setContentView(R.layout.error_dialog);
            errorText = (TextView) errorDialog.findViewById(R.id.error_dialog_text1);
            errorText.setText("Password must be a minimum of 8 characters");
            errorDialog.show();
            errorButton = (Button) errorDialog.findViewById(R.id.error_dialog_button);
            errorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    errorDialog.cancel();
                }
            });
        }
    }

    public boolean setUpContainsNutsCheckbox(View v) {
        checkBoxContainsNutAllergy = (CheckBox) v.findViewById(R.id.nutAllergy);
        containsNutAllergy = false;
        checkBoxContainsNutAllergy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                containsNutAllergy = isChecked;
            }
        });
        return containsNutAllergy;
    }

    public boolean setUpGlutenFreeCheckbox(View v) {
        checkBoxGlutenAllergy = (CheckBox) v.findViewById(R.id.glutenAllergy);
        containsGlutenAllergy = false;
        checkBoxGlutenAllergy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                containsGlutenAllergy = isChecked;
            }
        });
        return containsGlutenAllergy;
    }

    public int spicinessSelected(View v) {
        // Spiciness level selection.
        spicinessSelect = (Spinner) v.findViewById(R.id.spicinessSelector);
        ArrayAdapter<CharSequence> spicinessLevelAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.spiciness_level_array, android.R.layout.simple_spinner_item);
        spicinessLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spicinessSelect.setAdapter(spicinessLevelAdapter);

        spiciness = -1;
        spicinessSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, int id) {
                spiciness = id;
            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not sure if I need to implement this method.
                ;
            }
        });
        return spiciness;
    }

}