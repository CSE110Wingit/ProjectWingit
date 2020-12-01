package com.example.projectwingit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.projectwingit.utils.LoginInfo;
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
        LoginInfo.APP_CONTEXT = getApplicationContext();

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
        if (dl.isDrawerOpen(GravityCompat.START)) {
            dl.closeDrawer(GravityCompat.START);
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
                            fragment = new SearchFragment_Ingredients();
                            toolbar.setTitle(R.string.search_title_toolbar);
                            break;
                        case R.id.favorites:
                            fragment = new FavoritesFragment();
                            toolbar.setTitle(R.string.favorites_title_toolbar);
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();

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
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new CreateRecipe()).commit();
                toolbar.setTitle(R.string.create_recipe_title_toolbar);
                break;
            case R.id.hamburger_local_venues:
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new LocalVenues()).commit();
                toolbar.setTitle(R.string.local_venues_title_toolbar);
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