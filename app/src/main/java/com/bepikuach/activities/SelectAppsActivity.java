package com.bepikuach.activities;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.bepikuach.R;
import com.bepikuach.admin.DeviceAdminReceiver;
import com.bepikuach.utils.AppInfo;
import com.bepikuach.utils.LockTaskManager;
import com.bepikuach.utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SelectAppsActivity extends AppCompatActivity {

    private PrefManager prefManager;
    private LockTaskManager lockTaskManager;
    private DevicePolicyManager dpm;
    private ComponentName admin;

    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> filteredApps = new ArrayList<>();

    // טאבים
    private TextView tabApps, tabBlocked, tabWhitelist;
    private View panelApps, panelBlocked, panelWhitelist;

    // גריד
    private GridLayout appsGrid;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_apps);

        prefManager = new PrefManager(this);
        lockTaskManager = new LockTaskManager(this);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, DeviceAdminReceiver.class);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("ניהול אפליקציות");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        allApps = getAllInstalledApps();
        filteredApps = new ArrayList<>(allApps);

        setupTabs();
        setupSearch();
        buildAppsGrid(filteredApps);
        setupBlockedLog();
        setupWhitelistPanel();
    }

    private void setupTabs() {
        tabApps = findViewById(R.id.tabApps);
        tabBlocked = findViewById(R.id.tabBlocked);
        tabWhitelist = findViewById(R.id.tabWhitelist);
        panelApps = findViewById(R.id.panelApps);
        panelBlocked = findViewById(R.id.panelBlocked);
        panelWhitelist = findViewById(R.id.panelWhitelist);

        tabApps.setOnClickListener(v -> switchTab(0));
        tabBlocked.setOnClickListener(v -> switchTab(1));
        tabWhitelist.setOnClickListener(v -> switchTab(2));
        switchTab(0);
    }

    private void switchTab(int tab) {
        panelApps.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        panelBlocked.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        panelWhitelist.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        int active = 0xFF3F51B5, inactive = 0xFFE8EAF6;
        int activeText = 0xFFFFFFFF, inactiveText = 0xFF3F51B5;

        tabApps.setBackgroundColor(tab == 0 ? active : inactive);
        tabApps.setTextColor(tab == 0 ? activeText : inactiveText);
        tabBlocked.setBackgroundColor(tab == 1 ? active : inactive);
        tabBlocked.setTextColor(tab == 1 ? activeText : inactiveText);
        tabWhitelist.setBackgroundColor(tab == 2 ? active : inactive);
        tabWhitelist.setTextColor(tab == 2 ? activeText : inactiveText);

        if (tab == 1) refreshBlockedLog();
        if (tab == 2) refreshWhitelistPanel();
    }

    private void setupSearch() {
        SearchView search = findViewById(R.id.searchApps);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String q) {
                searchQuery = q;
                filterAndRebuild();
                return true;
            }
        });
    }

    private void filterAndRebuild() {
        filteredApps = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (searchQuery.isEmpty() ||
                app.name.toLowerCase().contains(searchQuery.toLowerCase()) ||
                app.packageName.toLowerCase().contains(searchQuery.toLowerCase())) {
                filteredApps.add(app);
            }
        }
        buildAppsGrid(filteredApps);
    }

    private void buildAppsGrid(List<AppInfo> apps) {
        appsGrid = findViewById(R.id.appsGrid);
        appsGrid.removeAllViews();

        int columns = 4;
        appsGrid.setColumnCount(columns);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cellSize = screenWidth / columns;

        for (AppInfo app : apps) {
            FrameLayout cell = new FrameLayout(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            cell.setLayoutParams(params);
            cell.setPadding(8, 8, 8, 8);

            // רקע של בחירה
            if (app.isApproved) {
                cell.setBackgroundColor(0x333F51B5);
            } else {
                cell.setBackgroundColor(0x00000000);
            }

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            inner.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            // אייקון
            ImageView icon = new ImageView(this);
            int iconSize = (int) (cellSize * 0.55f);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            icon.setLayoutParams(iconParams);
            icon.setImageDrawable(app.icon);

            // שם
            TextView name = new TextView(this);
            name.setText(app.name);
            name.setTextSize(10);
            name.setTextColor(0xFF1A1A2E);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            nameParams.topMargin = 4;
            name.setLayoutParams(nameParams);

            // סימון מאושר
            if (app.isApproved) {
                TextView check = new TextView(this);
                check.setText("✓");
                check.setTextColor(0xFF3F51B5);
                check.setTextSize(14);
                check.setGravity(Gravity.CENTER);
                inner.addView(icon);
                inner.addView(name);
                inner.addView(check);
            } else {
                inner.addView(icon);
                inner.addView(name);
            }

            cell.addView(inner);

            // לחיצה — toggle
            cell.setOnClickListener(v -> {
                app.isApproved = !app.isApproved;
                // עדכן גם allApps
                for (AppInfo a : allApps) {
                    if (a.packageName.equals(app.packageName)) {
                        a.isApproved = app.isApproved;
                        break;
                    }
                }
                // שמור מיד
                saveApproved();
                buildAppsGrid(filteredApps);
            });

            // לחיצה ארוכה — הסתר/הצג
            cell.setOnLongClickListener(v -> {
                if (!lockTaskManager.isDeviceOwner) {
                    Toast.makeText(this, "נדרש Device Owner להסתרת אפליקציות", Toast.LENGTH_SHORT).show();
                    return true;
                }
                boolean isHidden = prefManager.isAppHidden(app.packageName);
                String msg = isHidden
                    ? "הצג את " + app.name + " ב-Launcher?"
                    : "הסתר את " + app.name + " מה-Launcher?\n(האפליקציה תמשיך לפעול)";
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(isHidden ? "הצג אפליקציה" : "הסתר אפליקציה")
                    .setMessage(msg)
                    .setPositiveButton("אישור", (d, w) -> {
                        Set<String> hidden = prefManager.getHiddenApps();
                        if (isHidden) {
                            hidden.remove(app.packageName);
                            try { dpm.setApplicationHidden(admin, app.packageName, false); } catch (Exception ignored) {}
                        } else {
                            hidden.add(app.packageName);
                            try { dpm.setApplicationHidden(admin, app.packageName, true); } catch (Exception ignored) {}
                        }
                        prefManager.setHiddenApps(hidden);
                        Toast.makeText(this, isHidden ? "מוצג ✓" : "מוסתר ✓", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("ביטול", null)
                    .show();
                return true;
            });

            appsGrid.addView(cell);
        }

        // כפתור שמור בתחתית
        Button saveBtn = findViewById(R.id.btnSave);
        saveBtn.setOnClickListener(v -> {
            saveApproved();
            Toast.makeText(this, "נשמר ✓", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void saveApproved() {
        Set<String> approved = new HashSet<>();
        for (AppInfo app : allApps) {
            if (app.isApproved) approved.add(app.packageName);
        }
        prefManager.setApprovedApps(approved);
        lockTaskManager.updateApprovedPackages(approved);
    }

    // ---- טאב חסימות אחרונות ----
    private void setupBlockedLog() {
        Button clearBtn = findViewById(R.id.btnClearLog);
        clearBtn.setOnClickListener(v -> {
            prefManager.clearBlockedLog();
            refreshBlockedLog();
            Toast.makeText(this, "הלוג נוקה ✓", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshBlockedLog() {
        LinearLayout logContainer = findViewById(R.id.blockedLogContainer);
        TextView emptyMsg = findViewById(R.id.blockedLogEmpty);
        logContainer.removeAllViews();

        List<String> log = prefManager.getBlockedLog();
        if (log.isEmpty()) {
            emptyMsg.setVisibility(View.VISIBLE);
            return;
        }
        emptyMsg.setVisibility(View.GONE);
        Collections.sort(log, Collections.reverseOrder());

        PackageManager pm = getPackageManager();
        Set<String> approved = prefManager.getApprovedApps();

        for (String entry : log) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length < 2) continue;
            String pkg = parts[1];
            long timestamp = 0;
            try { timestamp = Long.parseLong(parts[0]); } catch (Exception ignored) {}

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 12, 16, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(0xFFFFFFFF);

            // אייקון
            try {
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(pm.getApplicationIcon(pkg));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(80, 80);
                lp.setMarginEnd(12);
                icon.setLayoutParams(lp);
                row.addView(icon);
            } catch (Exception ignored) {}

            // שם + זמן
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            info.setLayoutParams(infoParams);

            String appName = pkg;
            try { appName = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
            catch (Exception ignored) {}

            TextView nameView = new TextView(this);
            nameView.setText(appName);
            nameView.setTextColor(0xFF1A1A2E);
            nameView.setTextSize(14);

            TextView timeView = new TextView(this);
            if (timestamp > 0)
                timeView.setText(new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(timestamp)));
            timeView.setTextColor(0xFF9E9E9E);
            timeView.setTextSize(11);

            info.addView(nameView);
            info.addView(timeView);

            // כפתור התר
            boolean alreadyApproved = approved.contains(pkg);
            Button allowBtn = new Button(this);
            allowBtn.setText(alreadyApproved ? "מותר ✓" : "התר");
            allowBtn.setTextSize(12);
            allowBtn.setEnabled(!alreadyApproved);
            allowBtn.setBackgroundColor(alreadyApproved ? 0xFF9E9E9E : 0xFF43A047);
            allowBtn.setTextColor(0xFFFFFFFF);

            final String finalPkg = pkg;
            final String finalName = appName;
            allowBtn.setOnClickListener(v -> {
                Set<String> newApproved = prefManager.getApprovedApps();
                newApproved.add(finalPkg);
                prefManager.setApprovedApps(newApproved);
                lockTaskManager.updateApprovedPackages(newApproved);
                // עדכן גם allApps
                for (AppInfo a : allApps) {
                    if (a.packageName.equals(finalPkg)) { a.isApproved = true; break; }
                }
                allowBtn.setText("מותר ✓");
                allowBtn.setEnabled(false);
                allowBtn.setBackgroundColor(0xFF9E9E9E);
                Toast.makeText(this, finalName + " הותר ✓", Toast.LENGTH_SHORT).show();
            });

            row.addView(info);
            row.addView(allowBtn);

            // קו הפרדה
            View divider = new View(this);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divider.setLayoutParams(dp);
            divider.setBackgroundColor(0xFFC5CAE9);

            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(row);
            wrapper.addView(divider);
            logContainer.addView(wrapper);
        }
    }

    // ---- טאב רשימה לבנה ----
    private void setupWhitelistPanel() {
        refreshWhitelistPanel();
    }

    private void refreshWhitelistPanel() {
        LinearLayout container = findViewById(R.id.whitelistContainer);
        TextView emptyMsg = findViewById(R.id.whitelistEmpty);
        container.removeAllViews();

        Set<String> approved = prefManager.getApprovedApps();
        PackageManager pm = getPackageManager();

        if (approved.isEmpty()) {
            emptyMsg.setVisibility(View.VISIBLE);
            return;
        }
        emptyMsg.setVisibility(View.GONE);

        List<String> sortedApproved = new ArrayList<>(approved);
        Collections.sort(sortedApproved);

        for (String pkg : sortedApproved) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 12, 16, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(0xFFFFFFFF);

            // אייקון
            try {
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(pm.getApplicationIcon(pkg));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(80, 80);
                lp.setMarginEnd(12);
                icon.setLayoutParams(lp);
                row.addView(icon);
            } catch (Exception ignored) {}

            // שם
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            info.setLayoutParams(infoParams);

            String appName = pkg;
            try { appName = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
            catch (Exception ignored) {}

            TextView nameView = new TextView(this);
            nameView.setText(appName);
            nameView.setTextColor(0xFF1A1A2E);
            nameView.setTextSize(14);

            TextView pkgView = new TextView(this);
            pkgView.setText(pkg);
            pkgView.setTextColor(0xFF9E9E9E);
            pkgView.setTextSize(10);

            info.addView(nameView);
            info.addView(pkgView);

            // כפתור הסר היתר
            Button removeBtn = new Button(this);
            removeBtn.setText("הסר");
            removeBtn.setTextSize(12);
            removeBtn.setBackgroundColor(0xFFD32F2F);
            removeBtn.setTextColor(0xFFFFFFFF);

            final String finalPkg = pkg;
            final String finalName = appName;
            removeBtn.setOnClickListener(v -> {
                Set<String> newApproved = prefManager.getApprovedApps();
                newApproved.remove(finalPkg);
                prefManager.setApprovedApps(newApproved);
                lockTaskManager.updateApprovedPackages(newApproved);
                for (AppInfo a : allApps) {
                    if (a.packageName.equals(finalPkg)) { a.isApproved = false; break; }
                }
                Toast.makeText(this, finalName + " הוסר מהרשימה הלבנה", Toast.LENGTH_SHORT).show();
                refreshWhitelistPanel();
            });

            row.addView(info);
            row.addView(removeBtn);

            View divider = new View(this);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divider.setLayoutParams(dp);
            divider.setBackgroundColor(0xFFC5CAE9);

            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(row);
            wrapper.addView(divider);
            container.addView(wrapper);
        }
    }

    private List<AppInfo> getAllInstalledApps() {
        List<AppInfo> result = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Set<String> approved = prefManager.getApprovedApps();

        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo info : installed) {
            if (info.packageName.equals("com.bepikuach")) continue;

            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchIntent.setPackage(info.packageName);
            if (pm.queryIntentActivities(launchIntent, 0).isEmpty()) continue;

            String name;
            try { name = pm.getApplicationLabel(info).toString(); }
            catch (Exception e) { name = info.packageName; }

            Drawable icon;
            try { icon = pm.getApplicationIcon(info.packageName); }
            catch (Exception e) { icon = pm.getDefaultActivityIcon(); }

            result.add(new AppInfo(name, info.packageName, icon, approved.contains(info.packageName)));
        }
        result.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
