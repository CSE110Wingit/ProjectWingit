package com.example.projectwingit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import com.example.projectwingit.io.LambdaRequests;
import com.example.projectwingit.io.LambdaResponse;
import com.example.projectwingit.utils.LoginInfo;
import com.google.android.material.navigation.NavigationView;


public class UserAccount extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView accountView;
    private DrawerLayout account_Drawer;

    private String loginUsername;
    private String loginEmail;

    // preference values for the editCharacteristics page (bools default to false, ints default to -1)
    boolean prefNutAllergy = false;
    boolean prefGlutenFree = false;
    boolean prefVegan = false;
    boolean prefVegetarian = false;
    static int prefSpiciness = -1;
    static int prefSweetness = -1;

    boolean isLoggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account);

        // checks if user is logged in
        if (LoginInfo.CURRENT_LOGIN != null) {
            loginUsername = LoginInfo.CURRENT_LOGIN.username;
            loginEmail = LoginInfo.CURRENT_LOGIN.email;
            isLoggedIn = true;
        }


        account_Drawer = findViewById(R.id.drawer_Acc);
        account_Drawer.openDrawer(GravityCompat.START);
        NavigationView accountView = findViewById(R.id.userAccountView);
        accountView.setNavigationItemSelectedListener(this);
//        getSupportFragmentManager().beginTransaction().replace(R.id.container_user_account, new LoginFragment()).commit();

        if(isLoggedIn){
            TextView greeting = accountView.getHeaderView(0).findViewById(R.id.AO_title);
            greeting.setText("Hi, " + loginUsername + "!");
            accountView.getMenu().getItem(0).getSubMenu().getItem(1).setVisible(true);
            accountView.getMenu().getItem(0).getSubMenu().getItem(2).setVisible(true);
            accountView.getMenu().getItem(0).getSubMenu().getItem(3).setVisible(true);
        }
        else {
            accountView.getMenu().getItem(0).getSubMenu().getItem(0).setVisible(true);
            accountView.getMenu().getItem(0).getSubMenu().getItem(4).setVisible(true);
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.user_account_login:
                getSupportFragmentManager().beginTransaction().replace(R.id.container_user_account, new LoginFragment()).commit();
                break;
            case R.id.change_password:
                getSupportFragmentManager().beginTransaction().replace(R.id.container_user_account, new passwordChangeFragment()).commit();
                break;
            case R.id.delete_account:
                getSupportFragmentManager().beginTransaction().replace(R.id.container_user_account, new DeleteAccountFragment()).commit();
                break;
            case R.id.logout_account:
                getSupportFragmentManager().beginTransaction().replace(R.id.container_user_account, new logOffFragment()).commit();
                break;
            case R.id.user_account_create:
                getSupportFragmentManager().beginTransaction().replace(R.id.container_user_account, new RegisterFragment()).commit();
                break;
            case R.id.user_account_characteristics:
                // only users with accounts can access TODO set to true to debug characteristics page
                if(isLoggedIn) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.container_user_account, new EditCharacteristicsFragment()).commit();
                }
                else {
                    getSupportFragmentManager().beginTransaction().replace(R.id.container_user_account, new LoginFragment()).commit();
                }
                break;

        }
        account_Drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    // checkbox listener for preference checkboxes
    // needs to be in activity and not fragment code
    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox)view).isChecked();
        switch(view.getId()) {
            case R.id.checkbox_gluten_free:
                if (checked) prefGlutenFree = true;
                else prefGlutenFree = false;
                break;

            case R.id.checkbox_nut_allergy:
                if (checked) prefNutAllergy = true;
                else prefNutAllergy = false;
                break;

            case R.id.checkbox_vegan:
                if (checked) prefVegan = true;
                else prefVegan = false;
                break;

            case R.id.checkbox_vegetarian:
                if (checked) prefVegetarian = true;
                else prefVegetarian = false;
                break;
        }
    }



    // listener for the confirm button in edit preferences that calls the lambda method to
    // actually change a user's preferences on the backend
    public void onConfirmClicked(View view){

        // TODO call lambda method to confirm current changes
        LambdaResponse recipe = LambdaRequests.editPersonalCharacteristics(loginUsername, loginEmail, prefNutAllergy, prefGlutenFree, prefSpiciness);
    }

    // resets all of the preference fields to their default values, tells the backend
    public void onResetClicked(View view){
        Button resetButton = findViewById(R.id.prefResetButton);

        prefVegetarian = false;
        prefVegan = false;
        prefGlutenFree = false;
        prefNutAllergy = false;
        prefSpiciness = -1;
        prefSweetness = -1;

        CheckBox veganCB = findViewById(R.id.checkbox_vegan);
        veganCB.setChecked(false);

        CheckBox vegetarianCB = findViewById(R.id.checkbox_vegetarian);
        vegetarianCB.setChecked(false);

        CheckBox glutenCB = findViewById(R.id.checkbox_gluten_free);
        glutenCB.setChecked(false);

        CheckBox nutCB = findViewById(R.id.checkbox_nut_allergy);
        nutCB.setChecked(false);

        SeekBar spiceSB = findViewById(R.id.spice_seekbar);
        spiceSB.setProgress(0);

        SeekBar sweetSB = findViewById(R.id.sweet_seekbar);
        sweetSB.setProgress(0);

        TextView spiceTB = findViewById(R.id.spice_textview);
        spiceTB.setText("Spice Level Preference: None");

        TextView sweetTB = findViewById(R.id.sweetness_textView);
        sweetTB.setText("Sweetness Level Preference: None");


        // TODO calls lambda method on reset
        LambdaResponse recipe = LambdaRequests.editPersonalCharacteristics(loginUsername, loginEmail, false, false, -1);
    }
}