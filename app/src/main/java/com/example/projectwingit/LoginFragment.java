package com.example.projectwingit;


import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.projectwingit.utils.LoginInfo;


//import com.example.projectwingit.LoginResult;
//import com.example.projectwingit.LoginViewModel;
//import com.example.projectwingit.LoginViewModelFactory;
import com.example.projectwingit.R;
import com.example.projectwingit.RegisterFragment;
//import com.example.projectwingit.forgot_user_Fragment;
//import com.example.projectwingit.forgot_pass_Fragment;
import com.example.projectwingit.io.LambdaRequests;
import com.example.projectwingit.io.LambdaResponse;
//import com.example.projectwingit.ui.login.LoggedInUserView;

public class LoginFragment extends Fragment {

//    private LoginViewModel loginViewModel;
    Dialog errorDialog;
    TextView errorText;
    EditText usernameEditText;
    EditText passwordEditText;
    Button loginButton;
    Button errorButton;
    ProgressBar loadingProgressBar;
    TextView forgot_User;
    TextView forgot_Pass;
    TextView create_Account;

    @Override
    public void onCreate(Bundle SavedInstanceState) {
        super.onCreate(SavedInstanceState);

    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);
//        usernameEditText = v.findViewById(R.id.username);
//        passwordEditText = v.findViewById(R.id.password);
//        loginButton = (Button)v.findViewById(R.id.login);
//        loadingProgressBar = v.findViewById(R.id.loading);
//        forgot_User = v.findViewById(R.id.forgot_User);
//        forgot_Pass = v.findViewById(R.id.forgot_Pass);
//        create_Account = v.findViewById(R.id.create_Account);
//
//        loginButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        usernameEditText = view.findViewById(R.id.username);
        passwordEditText = view.findViewById(R.id.password);
        loginButton = view.findViewById(R.id.login);
        loadingProgressBar = view.findViewById(R.id.loading);
        forgot_User = view.findViewById(R.id.forgot_User);
        forgot_Pass = view.findViewById(R.id.forgot_Pass);
        create_Account = view.findViewById(R.id.create_Account);


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loadingProgressBar.setVisibility(View.VISIBLE);
//                Toast.makeText(getActivity(), "Enter username", Toast.LENGTH_LONG).show();
//
//                Toast.makeText(getActivity().getApplicationContext(), "Enter password", Toast.LENGTH_LONG).show();

                userLogin(v);

            }
        });

        forgot_Pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container_user_account, new forgot_pass_Fragment());
                ft.commit();
            }
        });

        forgot_User.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container_user_account, new forgot_user_Fragment());
                ft.commit();
            }
        });

        create_Account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container_user_account, new RegisterFragment());
                ft.commit();
            }
        });

    }
    private void userLogin(View v) {
        String username, password;
        username = usernameEditText.getText().toString();
        password = passwordEditText.getText().toString();
        String hashPass = com.example.projectwingit.utils.WingitUtils.hashPassword(password);

        LambdaResponse userLog = LambdaRequests.login(username, hashPass);
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(getActivity().getApplicationContext(), "Enter username", Toast.LENGTH_LONG).show();
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getActivity().getApplicationContext(), "Enter password", Toast.LENGTH_LONG).show();
        }

        loginButton.post(new Runnable() {
            @Override
            public void run() {
                errorDialog = new Dialog(getActivity());
                errorDialog.setContentView(R.layout.error_dialog);
                errorText = (TextView)errorDialog.findViewById(R.id.error_dialog_text1);
                errorText.setText(userLog.getResponseInfo());
                errorDialog.show();
                errorButton = (Button)errorDialog.findViewById(R.id.error_dialog_button);
                if (!userLog.isError()) {
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

//    private void updateUiWithUser(LoggedInUserView model) {
//        String welcome = getString(R.string.welcome) + model.getDisplayName();
//        // TODO : initiate successful logged in experience
//        if (getContext() != null && getContext().getApplicationContext() != null) {
//            Toast.makeText(getContext().getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
//        }
//    }

//    private void showLoginFailed(@StringRes Integer errorString) {
//        if (getContext() != null && getContext().getApplicationContext() != null) {
//            Toast.makeText(
//                    getContext().getApplicationContext(),
//                    errorString,
//                    Toast.LENGTH_LONG).show();
//        }
//    }
}