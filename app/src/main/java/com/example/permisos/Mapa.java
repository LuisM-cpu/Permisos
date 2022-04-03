package com.example.permisos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class Mapa extends AppCompatActivity {

    private MapView map;
    private IMapController mapController;
    private GeoPoint ubicacion;
    private FusedLocationProviderClient mFusedLocationClient;
    private Marker marcador;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_mapa);
        ubicacion = new GeoPoint(0.0,0.0);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        solicitarPermisoAlmacenamiento.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        map = findViewById(R.id.osmMap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        marcador = new Marker(map);
        marcador.setTitle("Ubicacion Actual");
        marcador.setIcon(getResources().getDrawable(R.drawable.location,getTheme()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        mapController = map.getController();
        mapController.setZoom(18.0);
        mapController.setCenter(ubicacion);
        marcador.setPosition(ubicacion);
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    ActivityResultLauncher<String> solicitarPermisoUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onActivityResult(Boolean result) {
                    if(result){
                        mFusedLocationClient.getLastLocation().addOnSuccessListener(ubicacionObtenida);
                    }else{
                        if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
                        {
                            Toast.makeText(Mapa.this, "No se ha otorgado el permiso para la ubicacion.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private final OnSuccessListener<Location> ubicacionObtenida = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
            if (location != null) {
                ubicacion = new GeoPoint(location.getLatitude(),location.getLongitude());
                mapController = map.getController();
                mapController.setZoom(18.0);
                mapController.setCenter(ubicacion);
                map.getOverlays().add(marcador);
                marcador.setPosition(ubicacion);
            }
        }
    };

    ActivityResultLauncher<String> solicitarPermisoAlmacenamiento = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result){
                        solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }else{
                        if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        {
                            Toast.makeText(Mapa.this, "No se otorgó el permiso para el almacenamiento.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

}