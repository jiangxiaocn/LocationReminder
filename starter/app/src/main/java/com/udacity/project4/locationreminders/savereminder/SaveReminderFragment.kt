package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var reminderDataItem: ReminderDataItem
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencingRequest :GeofencingRequest
    private lateinit var contxt: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contxt = context
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            reminderDataItem = ReminderDataItem(title, description, location, latitude, longitude)

                checkPermissionsAndStartGeofencing()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }
    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(contxt,
                            Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            contxt, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }
    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")
        requestPermissions(permissionsArray,resultCode)
    }
    /*
     * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
     * the background permission as well.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            // Permission denied.
            Snackbar.make(
                binding.fragmentSaveReminder,
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }
    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                        startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON,null,0,0,0,null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.fragmentSaveReminder,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofence()
            }
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(contxt, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(contxt, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        if (_viewModel.validateEnteredData(reminderDataItem)) {
            val currentGeofenceData = reminderDataItem

            // Build the Geofence Object
            val geofence = Geofence.Builder()
                // Set the request ID, string to identify the geofence.
                .setRequestId(currentGeofenceData.id)
                // Set the circular region of this geofence.
                .setCircularRegion(
                    currentGeofenceData.latitude!!,
                    currentGeofenceData.longitude!!,
                    GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
                )
                // Set the expiration duration of the geofence. This geofence gets
                // automatically removed after this period of time.
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            // Build the geofence request
            val geofencingRequest = GeofencingRequest.Builder()
                // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
                // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
                // is already inside that geofence.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

                // Add the geofences to be monitored by geofencing service.
                .addGeofence(geofence)
                .build()


            // First, remove any existing geofences that use our pending intent
                // Regardless of success/failure of the removal, add the new geofence

                    // Add the new geofence request with the new geofence
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                        addOnSuccessListener {
                            // Geofences added.
                            Toast.makeText(
                                contxt, R.string.geofences_added,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            Log.d(TAG, "Location added!!!")
                            // Tell the viewmodel that we've reached the end of the game and
                            _viewModel.validateAndSaveReminder(reminderDataItem)
                        }
                        addOnFailureListener {
                            // Failed to add geofences.
                            Toast.makeText(
                                contxt, R.string.geofences_not_added,
                                Toast.LENGTH_SHORT
                            ).show()
                            if ((it.message != null)) {
                                Log.w(TAG, it.message!!)
                            }
                        }
                    }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val TAG = "SaveReminderFragment"
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29