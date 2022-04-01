package com.example.permisos;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.permisos.databinding.ActivityMapaBinding;
import com.google.android.gms.tasks.OnSuccessListener;

public class Mapa extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapaBinding binding;
    private FusedLocationProviderClient mFusedLocationClient;
    private double latitud;
    private double longitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Si se otorgó el permiso.
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, ubicacionObtenida);
        }else{
            //Si no se otorgó el permiso.
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Implementar luego
            } else {
                solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Add a marker in Sydney and move the camera
        LatLng ubicacion = new LatLng(latitud, longitud);
        mMap.addMarker(new MarkerOptions().position(ubicacion).title("Ubicacion Actual"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ubicacion));
    }

    ActivityResultLauncher<String> solicitarPermisoUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result == true){
                        //Permiso concedido
                                }else{
                        Toast.makeText(Mapa.this, "No se otorgó el permiso para la ubicacion.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private OnSuccessListener<Location> ubicacionObtenida = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
            Log.i("Ubicacion", "Ubicacion obtenida.");
            if (location != null) {
                longitud = location.getLongitude();
                latitud = location.getLatitude();
            }
        }
    };
}