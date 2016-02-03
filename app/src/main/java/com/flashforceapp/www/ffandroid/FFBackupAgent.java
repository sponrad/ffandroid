package com.flashforceapp.www.ffandroid;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Created by conradframe on 2/3/16.
 */
public class FFBackupAgent extends BackupAgentHelper {
    // The name of the SharedPreferences file
    String PREFS = getString(R.string.userpref);

    // A key to uniquely identify the set of backup data
    String PREFS_BACKUP_KEY = getString(R.string.prefkey);

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper =
                new SharedPreferencesBackupHelper(this, PREFS);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
