package com.pocketledger.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pocketledger.R;
import com.pocketledger.fragments.DashboardFragment;
import com.pocketledger.fragments.ManageFragment;
import com.pocketledger.fragments.SettingsFragment;
import com.pocketledger.fragments.SMSFragment;
import com.pocketledger.fragments.TransactionsFragment;
import com.pocketledger.models.Session;
import com.pocketledger.repositories.MealRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.SessionManager;
import com.pocketledger.workers.WorkerScheduler;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private BroadcastReceiver syncReceiver;
    private boolean isProgrammaticNavigation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            if (!isProgrammaticNavigation) {
                renderTab(item.getItemId());
            }
            return true;
        });

        FloatingActionButton fabAddTransaction = findViewById(R.id.fabAddTransaction);
        fabAddTransaction.setOnClickListener(v -> {
            startActivity(new Intent(this, EditTransactionActivity.class));
        });

        if (savedInstanceState == null) {
            renderTab(R.id.nav_dashboard);
        }
        WorkerScheduler.scheduleAll(this);
        requestNotificationPermission();
        generateMeals();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 301);
        }
    }

    private void generateMeals() {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            return;
        }
        AppExecutors.io().execute(() -> {
            new MealRepository(this).ensureDailyMeals(session.userId, DateUtils.today());
            WorkerScheduler.requestImmediateSync(this);
        });
    }

    public void showTab(int itemId) {
        isProgrammaticNavigation = true;
        bottomNav.setSelectedItemId(itemId);
        isProgrammaticNavigation = false;
    }

    private void renderTab(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_transactions) {
            fragment = new TransactionsFragment();
        } else if (itemId == R.id.nav_sms) {
            fragment = new SMSFragment();
        } else if (itemId == R.id.nav_manage) {
            fragment = new ManageFragment();
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new DashboardFragment();
            itemId = R.id.nav_dashboard;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void openTransactions() {
        renderTab(R.id.nav_transactions);
    }

    public void showMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        View root = findViewById(android.R.id.content);
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }
                if (Constants.ACTION_AUTH_EXPIRED.equals(intent.getAction())) {
                    SessionManager.get(MainActivity.this).clear();
                    startActivity(new Intent(MainActivity.this, AuthActivity.class));
                    finish();
                    return;
                }
                String message = intent.getStringExtra(Constants.EXTRA_SYNC_MESSAGE);
                boolean success = intent.getBooleanExtra(Constants.EXTRA_SYNC_SUCCESS, false);
                if (message != null && !message.trim().isEmpty()
                        && !message.equals(getString(R.string.sync_complete))) {
                    showMessage(message);
                }
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (success && fragment instanceof DashboardFragment) {
                    ((DashboardFragment) fragment).refresh();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SYNC_STATUS);
        filter.addAction(Constants.ACTION_AUTH_EXPIRED);
        ContextCompat.registerReceiver(this, syncReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        if (syncReceiver != null) {
            unregisterReceiver(syncReceiver);
            syncReceiver = null;
        }
        super.onStop();
    }
}
