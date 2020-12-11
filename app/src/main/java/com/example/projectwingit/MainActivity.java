package com.example.projectwingit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.projectwingit.io.LambdaRequests;
import com.example.projectwingit.io.LambdaResponse;
import com.example.projectwingit.io.UserInfo;
import com.example.projectwingit.utils.WingitUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private DrawerLayout dl;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UserInfo.APP_CONTEXT = getApplicationContext();

        String passwordHash = WingitUtils.hashPassword("TestPassword!1");
        LambdaResponse response = LambdaRequests.login("wingit_testing_account_verified", passwordHash);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < 100; i++){
            for (int j = 0; j < 100; j++){
                bitmap.setPixel(i, j, Color.argb(1, 1, 0, 0));
            }
        }
        LambdaResponse getURL = LambdaRequests.


        bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(bottomNavMethod);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new HomeFragment()).commit();

        toolbar = findViewById(R.id.myToolBar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        dl = findViewById(R.id.DrawerLayout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, dl, toolbar, R.string.Open, R.string.Close);
        dl.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (dl.isDrawerOpen(GravityCompat.START)) {
            dl.closeDrawer(GravityCompat.START);
        }
        else if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }
        else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.topnavigation, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.account_button_top:
                Intent intent = new Intent(this, UserAccount.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener bottomNavMethod = new
            BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment fragment = null;
                    switch (item.getItemId()) {
                        case R.id.home:
                            fragment = new HomeFragment();
                            toolbar.setTitle(R.string.home_title_toolbar);
                            break;
                        case R.id.search:
                            fragment = new SearchFragment();
                            toolbar.setTitle(R.string.search_title_toolbar);
                            break;
                        case R.id.favorites:
                            // checks if user is logged in
                            if (UserInfo.CURRENT_USER.isLoggedIn()) {
                                fragment = new FavoritesFragment();
                                toolbar.setTitle(R.string.favorites_title_toolbar);
                            }
                            else {
                                //TODO transition to login page
                            }
                            break;

                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit();

                    return true;
                }
            };

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.hamburger_home:
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new HomeFragment()).commit();
                toolbar.setTitle(R.string.home_title_toolbar);
                break;
            case R.id.hamburger_create_recipe:
                if (UserInfo.CURRENT_USER.isLoggedIn()) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, new CreateRecipe()).commit();
                    toolbar.setTitle(R.string.create_recipe_title_toolbar);
                } else {
                    Intent intent = new Intent(this, UserAccount.class);
                    String create_recipe_protected = "Log in to create a recipe!";
                    intent.putExtra("protected_destination", create_recipe_protected);
                    startActivity(intent);
                }
                break;
            case R.id.hamburger_local_venues:
			    String url = "https://www.google.com/maps/search/?api=1&query=wings";
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(webIntent);
                break;
            case R.id.hamburger_account_options:
                Intent intent = new Intent(this, UserAccount.class);
                startActivity(intent);
                break;
        }
        dl.closeDrawer(GravityCompat.START);
        return true;
    }
}