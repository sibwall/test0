package duress.ultimate;

import java.util.Collections;
import android.content.pm.PackageManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

	private static final String FRP_DISABLED = "frp_disabled";
	
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        ephemeral_profile_masking(context);
        disableFRP(context);			
    }

	@Override
    public void onEnabled(Context context, Intent intent) {         
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();        
    }
    
    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
    }

  
    static void disableFRP(Context context) {
           try {
		   if (context.getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean(FRP_DISABLED, false)) return;             
           DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
           if (!dpm.isDeviceOwnerApp(context.getPackageName())) return;
           ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);

           if (android.os.Build.VERSION.SDK_INT >= 30) {
                  android.app.admin.FactoryResetProtectionPolicy frpPolicy =       
                  new android.app.admin.FactoryResetProtectionPolicy.Builder()
                  .setFactoryResetProtectionAccounts(Collections.emptyList())        
                  .setFactoryResetProtectionEnabled(false)
                  .build();
            dpm.setFactoryResetProtectionPolicy(admin, frpPolicy);
                               
           } else {
                   android.os.Bundle restrictions = new android.os.Bundle();
                   restrictions.putBoolean("disableFactoryResetProtectionAdmin", true);
                   dpm.setApplicationRestrictions(admin, "com.google.android.gms", restrictions);
           }

           Intent intent = new Intent("com.google.android.gms.auth.FRP_CONFIG_CHANGED");
           intent.setPackage("com.google.android.gms");
           context.sendBroadcast(intent);

		   context.getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean(FRP_DISABLED, true).commit();           
           
           } catch (Throwable t) {}
   }


    private void ephemeral_profile_masking(Context context) {
    try {
	DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
	if (!dpm.isProfileOwnerApp(context.getPackageName())) return;           	
	ComponentName adminComponent = new ComponentName(context, MyDeviceAdminReceiver.class);			        
    android.os.UserManager userManager = (android.os.UserManager) context.getSystemService(Context.USER_SERVICE);

    if (userManager != null) {
        android.os.UserHandle myUserHandle = android.os.Process.myUserHandle();
        long userSerial = userManager.getSerialNumberForUser(myUserHandle);

        if (userSerial != 0 && dpm.isEphemeralUser(adminComponent)) {            
                PackageManager pm = context.getPackageManager();
                ComponentName entryActivity = new ComponentName(context, EntryActivity.class);
                
                pm.setComponentEnabledSetting(
                        entryActivity,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );

		dpm.setApplicationHidden(adminComponent, "com.android.settings", true);		
					
        }
    } 

    } catch (Throwable e) {}
    
    }	
}
