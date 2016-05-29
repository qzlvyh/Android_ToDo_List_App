package com.imaginat.androidtodolist;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.imaginat.androidtodolist.businessModels.ToDoListItemManager;
import com.imaginat.androidtodolist.customlayouts.ActionListFragment;
import com.imaginat.androidtodolist.customlayouts.AddListFragment;
import com.imaginat.androidtodolist.customlayouts.MainListFragment;
import com.imaginat.androidtodolist.customlayouts.ToDoListOptionsFragment;
import com.imaginat.androidtodolist.google.Constants;
import com.imaginat.androidtodolist.google.LocationUpdateService;

//import com.imaginat.androidtodolist.google.GoogleAPIClientManager;

public class MainActivity extends AppCompatActivity
        implements ActionListFragment.IChangeActionBarTitle,
        com.imaginat.androidtodolist.google.LocationServices.ILocationServiceClient,
        ToDoListOptionsFragment.IGeoOptions,
        ResultCallback<Status> {

    private static final String TAG = MainActivity.class.getName();

    private static final int REQUEST_FINE_LOCATION = 0;
    private static final int REQUEST_LOCATION = 12;


    //for reference to service
    LocationUpdateService mLocationUpdateService;
    boolean mLocationUpdateServiceBound;
    MyServiceConnection mServiceConnection;

    private SharedPreferences mSharedPreferences;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.lists_of_lists_dropdown, menu);


        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_new_list:
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.my_frame, new AddListFragment());
                ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                ft.addToBackStack(null);
                ft.commit();
                return true;
            case R.id.testStartService:
                Log.d(TAG, "startService selected");
                Intent startServiceIntent = new Intent(MainActivity.this, LocationUpdateService.class);
                startService(startServiceIntent);
                return true;
            case R.id.testStopService:
                Log.d(TAG, "stopService selected");
                Intent stopServiceIntent = new Intent(MainActivity.this, LocationUpdateService.class);
                stopService(stopServiceIntent);
                return true;
            case R.id.testIsServiceRunning:
                isServiceRunning();
                return true;
            case 100:
                android.support.v4.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.my_frame);
                ActionListFragment alf = (ActionListFragment) f;
                alf.toggleEdit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //shared preferences
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        //UI Stuff
        getSupportActionBar().setTitle("Main");


        //Settinig up the initial fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        MainListFragment fragment = new MainListFragment();
        fragment.setIGeoOptions(this);
        fragmentTransaction.add(R.id.my_frame, fragment);
        fragmentTransaction.commit();

        //For Search Bar
        handleIntent(getIntent());

        //Permissions for Location services
        loadPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_FINE_LOCATION);



        //Bind to service (if service is running)
        if (isServiceRunning() == false) {
            //look in shared preference to see if anybody needs it, if it does start it up
            int totalNoOfSharedPreferences = mSharedPreferences.getInt(Constants.GEO_ALARM_COUNT, -1);
            ToDoListItemManager listItemManager = ToDoListItemManager.getInstance(this);
            int totalNoInDatabase = listItemManager.getTotalActiveGeoAlarms();
            Log.d(TAG,"totalInShared: "+totalNoOfSharedPreferences+" totalDatabase"+totalNoInDatabase);
            if (totalNoInDatabase != totalNoOfSharedPreferences) {
                //reset shared preferences
                SharedPreferences.Editor ed = mSharedPreferences.edit();
                ed.putInt(Constants.GEO_ALARM_COUNT, totalNoInDatabase);
                ed.commit();
            }
            int totalNoOfActiveGeoAlarms = totalNoInDatabase;
            if (totalNoOfActiveGeoAlarms > 0) {
                //start up the service
                Intent startUpServiceIntent = new Intent(this, LocationUpdateService.class);
                startService(startUpServiceIntent);
                mServiceConnection=new MyServiceConnection();
                bindService(startUpServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

            }
        } else {
            //service is already up and running, now bind if not already bound
            if (mLocationUpdateServiceBound == false) {
                Intent boundIntent = new Intent(this, LocationUpdateService.class);
                mServiceConnection=new MyServiceConnection();
                bindService(boundIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        }

    }

    @Override
    protected void onDestroy() {
        if (isServiceRunning()) {
            //look in shared preference to see if anybody needs it, if not, stop iti
            int totalNoOfActiveGeoAlarms = mSharedPreferences.getInt(Constants.GEO_ALARM_COUNT, -1);
            Log.d(TAG,"onDestroy totaNoOfActiveAlarms: "+totalNoOfActiveGeoAlarms);
            if (totalNoOfActiveGeoAlarms < 1) {
                //start up the service
                Intent stopServiceIntent = new Intent(this, LocationUpdateService.class);
                stopService(stopServiceIntent);

            }
            if (mLocationUpdateServiceBound) {
                unbindService(mServiceConnection);
            }
            super.onDestroy();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mLocationServices.stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //  if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
        //mLocationServices.startLocationUpdates();
        // }
    }

    @Override
    protected void onStart() {

        // mGoogleApiClient.connect();

        super.onStart();
    }

    @Override
    protected void onStop() {

        // mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Toast.makeText(MainActivity.this, "Searching for " + query, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpdateTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(title);

        //actionBar.setBackgroundDrawable(new ColorDrawable(0xff00DDED));
        //actionBar.setDisplayShowTitleEnabled(false);
        //actionBar.setDisplayShowTitleEnabled(true);
    }


    public void onConnectedToGoogleAPIClient() {
//        Log.d(TAG, "MainActivity onConnectedtoGoogleAPIClient");
//
//        loadPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_FINE_LOCATION);
//        //mLocationServices.createLocationRequest();
//        mRequestingLocationUpdates = true;
//
//        if (mRequestingLocationUpdates) {
//            try {
//                //mLocationServices.startLocationUpdates();
//            } catch (SecurityException ex) {
//                ex.printStackTrace();
//            }
//        } else {
//
//        }


    }


    @Override
    public void displayDialogBasedOnStatus(Status status) throws IntentSender.SendIntentException {
        status.startResolutionForResult(
                MainActivity.this,
                REQUEST_LOCATION);
    }


    private void loadPermissions(String perm, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, requestCode);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

//    @Override
//    public void getAddressFromLocation() {
//        //Log.d(TAG, "inside getAddressFromLocatin");
//        //Location lastLocation = mLocationServices.getLocation();
//        //GeoCoder.startIntentService(this,lastLocation,mAddressResultReceiver);
//
//    }
//
//
//    @Override
//    public void removeGeoFence(String alarmID) {
//        removeGeofence(alarmID);
//    }
//
//    @Override
//    public void testButton(PendingIntent pi) {
//        //mLocationServices.populateGeofenceList();
//        addGeofences("THIS IS A TEST","1","1",getGeofencePendingIntent("test","1","1"));
//
//    }


    public void removeGeofence(String alarmID) {

    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent(String theText, String listID, String reminderID) {
        // Reuse the PendingIntent if we already have it.
//        if (mGeofencePendingIntent != null) {
//            return mGeofencePendingIntent;
//        }

/*
        Intent intent = new Intent(getApplicationContext(), GeofenceTransitionsIntentService.class);
        intent.putExtra(Constants.THE_TEXT,theText);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
*/

//        Intent intent = new Intent(this, AlarmReceiver.class);//GeofenceReceiver.class);
//        intent.setAction("com.imaginat.androidtodolist.LOCATiON_RECEIVED");
//        intent.putExtra(Constants.THE_TEXT,theText);
//
//        int maxSize=5;
//        if(theText.length()<maxSize){
//            maxSize = theText.length()-1;
//        }
//        String substring = theText.substring(0, maxSize);
//        String flag= substring + "_L" + listID + "I" + reminderID+"GEO";
//        int strlen = flag.length();
//        int hash = 7;
//        for (int i = 0; i < strlen; i++) {
//            hash = hash * 31 + flag.charAt(i);
//        }


//        return PendingIntent.getBroadcast(getApplicationContext(),hash,intent,0);

        //Intent myIntent = new Intent(getContext(), AlarmReceiver.class);
        //pendingIntent = PendingIntent.getBroadcast(getContext(), ToDoListOptionsFragment.this.createAlarmTag(CALENDAR),
        //        myIntent, 0);
        return null;
    }

    public void addGeofences(String theText, String listID, String reminderID, PendingIntent pi) {

    }


    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    @Override
    public void onResult(@NonNull Status status) {
     /*   if (status.isSuccess()) {
            //Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.apply();
//
//            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
//            // geofences enables the Add Geofences button.
//            setButtonsEnabledState();

            if (mGeofencesAdded) {
                Log.d(TAG, "geofence added");
                Toast.makeText(MainActivity.this, "GEO FENCE ADDED", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "geofence removed");
                Toast.makeText(MainActivity.this, "GEO FENCE REMOVED", Toast.LENGTH_SHORT).show();
            }



        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }*/
    }

    //===================CODE TO LINK TO SERVICE============================


    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            //Log.d(TAG, "CHECKING " + service.service.getClassName());
            if ("com.imaginat.androidtodolist.google.LocationUpdateService".equalsIgnoreCase(service.service.getClassName())) {
                Toast.makeText(this, "LocationUpdateService is RUNNING", Toast.LENGTH_LONG).show();
                Log.d(TAG, "LocationUpdateService is currently running");
                return true;
            }
        }
        Toast.makeText(this, "LocationUpdateService is NOT RUNNING", Toast.LENGTH_LONG).show();
        Log.d(TAG, "LocationUpdateService is NOT currently running");
        return false;
    }

    //Methods for binding to service
    //these methods let you start the service if it wasn't started onCreate of Main Activity of app
    //it will end the service on destroy if required, but not while app is in use

    @Override
    public LocationUpdateService getServiceReference() {
        if (mLocationUpdateServiceBound) {
            return mLocationUpdateService;
        }
        //check if number of geoFenceAlarms warrants system to start
        int totalNoOfActiveGeoAlarms = mSharedPreferences.getInt(Constants.GEO_ALARM_COUNT, -1);
        if (totalNoOfActiveGeoAlarms > 0 && mLocationUpdateServiceBound == false) {
            //bind it here
            Intent bindingIntent = new Intent(this, LocationUpdateService.class);
            mServiceConnection=new MyServiceConnection();
            bindService(bindingIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            mLocationUpdateServiceBound = true;
            return mLocationUpdateService;
        }
        return null;
    }

    @Override
    public void requestStartOfLocationUpdateService() {
        //add to count of alarm using it
        int currentTotal = mSharedPreferences.getInt(Constants.GEO_ALARM_COUNT, 0);
        currentTotal++;
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(Constants.GEO_ALARM_COUNT, currentTotal);
        editor.commit();
        if (isServiceRunning() == false) {
            //start the service
            Intent startServiceIntent = new Intent(this, LocationUpdateService.class);
            startService(startServiceIntent);
        }

    }

    @Override
    public void requestStopOfLocationUpdateService() {
        //reduce the number of geoFence kept in shared preferences
        //onDestroy will stop service if count is less than 1
        int currentTotal = mSharedPreferences.getInt(Constants.GEO_ALARM_COUNT, 0);
        currentTotal--;
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(Constants.GEO_ALARM_COUNT, currentTotal);
    }


    private class MyServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdateService.MyLocationUpdateServiceBinder myBinder = (LocationUpdateService.MyLocationUpdateServiceBinder) service;
            mLocationUpdateService = myBinder.getService();
            mLocationUpdateServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocationUpdateServiceBound = false;
        }
    }

}
