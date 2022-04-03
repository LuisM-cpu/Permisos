package com.example.permisos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Date;

public class Mapa extends AppCompatActivity {

    private MapView map;
    private IMapController mapController;
    private GeoPoint ultimaUbicacion;
    private Location nuevaUbicacion;
    private FusedLocationProviderClient mFusedLocationClient;
    private Marker marcador;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private boolean settingsOK;
    JSONArray localizaciones;
    private double radio;
    SensorManager sensorManager;
    Sensor lightSensor;
    SensorEventListener lightSensorListener;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_mapa);
        ultimaUbicacion = new GeoPoint(0.0,0.0);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        solicitarPermisoAlmacenamiento.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        settingsOK = false;
        radio = 6378.1;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        lightSensorListener = lecturaSensor;
        localizaciones = new JSONArray();
        mLocationRequest = createLocationRequest();
        mLocationCallback = callbackUbicacion;
        map = findViewById(R.id.osmMap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        marcador = new Marker(map);
        marcador.setTitle("Ubicacion Actual");
        marcador.setIcon(getResources().getDrawable(R.drawable.location,getTheme()));
        checkLocationSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        startLocationUpdates();
        mapController = map.getController();
        mapController.setZoom(18.0);
        mapController.setCenter(ultimaUbicacion);
        marcador.setPosition(ultimaUbicacion);
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        stopLocationUpdates();
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
                ultimaUbicacion = new GeoPoint(location.getLatitude(),location.getLongitude());
                mapController = map.getController();
                mapController.setZoom(18.0);
                mapController.setCenter(ultimaUbicacion);
                map.getOverlays().add(marcador);
                marcador.setPosition(ultimaUbicacion);
            }
        }
    };

    ActivityResultLauncher<String> solicitarPermisoAlmacenamiento = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result){
                        solicitarPermisoUbicacion.launch(Manifest.permission_group.LOCATION);
                    }else{
                        if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        {
                            Toast.makeText(Mapa.this, "No se otorgó el permiso para el almacenamiento.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    ActivityResultLauncher<IntentSenderRequest> getLocationSettings =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            Toast.makeText(Mapa.this, result.getResultCode(), Toast.LENGTH_SHORT).show();
                            if(result.getResultCode() == RESULT_OK){
                                settingsOK = true;
                                startLocationUpdates();
                            }else{
                                Toast.makeText(Mapa.this, "GPS Apagado", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    private void checkLocationSettings(){
        LocationSettingsRequest.Builder builder = new
                LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                settingsOK = true;
                startLocationUpdates();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(((ApiException) e).getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED){
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    IntentSenderRequest isr = new IntentSenderRequest.Builder(resolvable.getResolution()).build();
                    getLocationSettings.launch(isr);
                }else {
                    Toast.makeText(Mapa.this, "No hay GPS", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private LocationRequest createLocationRequest(){
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    private void stopLocationUpdates(){
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private LocationCallback callbackUbicacion = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            nuevaUbicacion = locationResult.getLastLocation();
            if(nuevaUbicacion!=null)
            {
                if(distance(ultimaUbicacion.getLatitude(),ultimaUbicacion.getLongitude(),
                        nuevaUbicacion.getLatitude(),nuevaUbicacion.getLongitude()) >= 30)
                {
                    writeJSONObject();
                    Toast.makeText(Mapa.this, "La diferencia es mayor a 30 metreos.", Toast.LENGTH_SHORT).show();
                }
                ultimaUbicacion = new GeoPoint(nuevaUbicacion.getLatitude(),nuevaUbicacion.getLongitude());
                mapController = map.getController();
                mapController.setZoom(18.0);
                mapController.setCenter(ultimaUbicacion);
                map.getOverlays().add(marcador);
                marcador.setPosition(ultimaUbicacion);
            }
        }
    };

    public JSONObject toJSON () {
        JSONObject obj = new JSONObject();
        try {
            obj.put("latitud", nuevaUbicacion.getLatitude());
            obj.put("longitud", nuevaUbicacion.getLongitude());
            obj.put("fecha", new Date(System.currentTimeMillis()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private void writeJSONObject(){
        localizaciones.put(toJSON());
        Writer output = null;
        String filename= "ubicaciones.json";
        try {
            File file = new File(getBaseContext().getExternalFilesDir(null), filename);
            output = new BufferedWriter(new FileWriter(file));
            output.write(localizaciones.toString());
            output.close();
            Toast.makeText(getApplicationContext(), "Ubicacion Guardada",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double distance(double lat1, double long1, double lat2, double long2) {
        double latDistance = Math.toRadians(lat1 - lat2);
        double lngDistance = Math.toRadians(long1 - long2);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double result = radio * c;
        return Math.round(result*100.0)/100.0;
    }

    private SensorEventListener lecturaSensor = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(map != null)
            {
                if(sensorEvent.values[0]<1800)
                {
                    map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

}