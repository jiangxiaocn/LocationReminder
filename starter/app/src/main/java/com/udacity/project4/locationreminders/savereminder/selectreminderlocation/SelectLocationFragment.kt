package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Transformations.map
import androidx.navigation.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.it_reminder.*
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private val REQUEST_LOCATION_PERMISSION = 1
    private val TAG = SelectLocationFragment::class.java.simpleName
    private lateinit var pointOfInterest: PointOfInterest
    private lateinit var randomPosition : LatLng


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//        TODO: add the map setup implementation
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Add a marker in Sydney and move the camera
        val latitude = 59.244220
        val longitude = 18.088052
        val homeLatLng = LatLng(latitude,longitude)
        val zoomLevel = 18f
        map.uiSettings.isZoomControlsEnabled = true

        map.addMarker(MarkerOptions().position(homeLatLng).title("Marker in Sweden"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng,zoomLevel))
        setMapLongClick(map)
        setLocationClick(map)
        enableMyLocation()
        setMapStyle(map)
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        binding.saveButton.setOnClickListener {
            if (this::pointOfInterest.isInitialized) {
                _viewModel.latitude.value = pointOfInterest.latLng.latitude
                _viewModel.longitude.value = pointOfInterest.latLng.longitude
                _viewModel.reminderSelectedLocationStr.value = pointOfInterest.name
                _viewModel.selectedPOI.value = pointOfInterest
                view!!.findNavController().navigate(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
            } else {
                if (this::randomPosition.isInitialized) {
                    _viewModel.latitude.value = randomPosition.latitude
                    _viewModel.longitude.value = randomPosition.longitude
                    _viewModel.reminderSelectedLocationStr.value = getString(R.string.dropped_pin)
                    view!!.findNavController().navigate(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
                } else {
                    Toast.makeText(context, "Please select a location", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
    private fun setMapLongClick(map:GoogleMap){
        map.setOnMapClickListener {latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(MarkerOptions()
                .position(latLng)
                .title(getString(R.string.dropped_pin))
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            randomPosition=latLng
        }
    }

    private fun setLocationClick(map:GoogleMap){
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions().position(poi.latLng)
                    .title(poi.name)
            )
            pointOfInterest = poi
            poiMarker.showInfoWindow()

            map.addCircle(
                    CircleOptions()
                            .center(poi.latLng)
                            .radius(200.0)
                            .strokeColor(Color.argb(255,255,0,0))
                            .fillColor(Color.argb(64,0,0,255)).strokeWidth(4F)

            )
        }
    }
    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        }catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }
    private fun isPermissionGranted() : Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED
    }
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true)
                return
            }

        }
        else {
           requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }else{
                Toast.makeText(context, "Location permission was not granted. Please add permission in setitngs", Toast.LENGTH_LONG).show()
            }
        }
    }


}
