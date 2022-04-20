package com.example.permisos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.permisos.databinding.ActivityMapaBinding;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

public class Mapa extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapaBinding binding;
    private FusedLocationProviderClient mFusedLocationClient;
    private LatLng ubicacion;
    private MarkerOptions marcador;
    private MarkerOptions marcaBusqueda;
    private MarkerOptions marcaToque;
    private boolean settingsOK;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location nuevaUbicacion;
    private JSONArray localizaciones;
    private double radio;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener lightSensorListener;
    private EditText busqueda;
    private String direccion;
    private Geocoder mGeocoder;
    private double lowerLeftLatitude;
    private double lowerLeftLongitude;
    private double upperRightLatitude;
    private double upperRigthLongitude;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        marcador = new MarkerOptions();
        marcaBusqueda = new MarkerOptions();
        marcaToque = new MarkerOptions();
        ubicacion = new LatLng(0,0);
        settingsOK = false;
        radio = 6378.1;
        localizaciones = new JSONArray();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        lightSensorListener = lecturaSensor;
        lowerLeftLatitude = 1.396967;
        lowerLeftLongitude= -78.903968;
        upperRightLatitude= 11.983639;
        upperRigthLongitude= -71.869905;
        mGeocoder = new Geocoder(getBaseContext());
        busqueda = findViewById(R.id.busqueda);
        //busqueda.setOnEditorActionListener(buscar);

        binding = ActivityMapaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        mFusedLocationClient.getLastLocation().addOnSuccessListener(this,ubicacionObtenida);
        mLocationRequest = createLocationRequest();
        mLocationCallback = callbackUbicacion;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        checkLocationSettings();
        sensorManager.registerListener(lightSensorListener,lightSensor,SensorManager.SENSOR_DELAY_NORMAL);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //mMap.setOnMapLongClickListener(click);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        marcador.position(ubicacion).title("Ubicacion Actual");
        mMap.addMarker(marcador);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ubicacion));
    }


    ActivityResultLauncher<String> solicitarPermisoUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onActivityResult(Boolean result) {
                    if(result == true){
                        mFusedLocationClient.getLastLocation().addOnSuccessListener(Mapa.this,ubicacionObtenida);
                                }else{
                        Toast.makeText(Mapa.this, "No se ha otorgado el permiso para la ubicacion.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private OnSuccessListener<Location> ubicacionObtenida = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
            if (location != null) {
                ubicacion = new LatLng(location.getLatitude(),location.getLongitude());
                onMapReady(mMap);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
        sensorManager.registerListener(lightSensorListener,lightSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        sensorManager.unregisterListener(lightSensorListener);
    }

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

    @SuppressLint("MissingPermission")
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
                if(distance(ubicacion.latitude,ubicacion.longitude,
                        nuevaUbicacion.getLatitude(),nuevaUbicacion.getLongitude()) >= 0.03)
                {
                    writeJSONObject();
                    ubicacion = new LatLng(nuevaUbicacion.getLatitude(),nuevaUbicacion.getLongitude());
                    marcador.position(ubicacion).title("Ubicacion Actual");
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(ubicacion));
                }else{
                    ubicacion = new LatLng(nuevaUbicacion.getLatitude(),nuevaUbicacion.getLongitude());
                    marcador.position(ubicacion).title("Ubicacion Actual");
                }

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
            if(mMap != null)
            {
                if(sensorEvent.values[0]<2000)
                {
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(Mapa.this, R.raw.modo_oscuro));
                }else{
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(Mapa.this, R.raw.modo_claro));
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private TextView.OnEditorActionListener buscar = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if(i == EditorInfo.IME_ACTION_SEARCH)
            {
                if(!busqueda.getText().toString().isEmpty())
                {
                    direccion = busqueda.getText().toString();
                    try {
                        List<Address> direcciones = mGeocoder.getFromLocationName(direccion,1,lowerLeftLatitude,lowerLeftLongitude,upperRightLatitude,upperRigthLongitude);
                        if(direcciones != null && !direcciones.isEmpty())
                        {
                            Address resultado = direcciones.get(0);
                            LatLng ubiEncontrada = new LatLng(resultado.getLatitude(),resultado.getLongitude());
                            if(mMap != null){
                                mMap.clear();
                                marcaBusqueda.position(ubiEncontrada).title(resultado.getAddressLine(0));
                                mMap.addMarker(marcador);
                                mMap.addMarker(marcaBusqueda);
                                mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(ubiEncontrada));
                            }
                        }else{
                            Toast.makeText(Mapa.this, "Dirección no encontrada.", Toast.LENGTH_SHORT).show();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(Mapa.this, "La dirección está vacía.", Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        }
    };

    private GoogleMap.OnMapLongClickListener click = new GoogleMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(@NonNull LatLng latLng) {
            try {
                direccion = mGeocoder.getFromLocation(latLng.latitude,latLng.longitude,1).get(0).getAddressLine(0);
                mMap.clear();
                marcaToque.position(latLng).title(direccion);
                mMap.addMarker(marcador);
                mMap.addMarker(marcaToque);
                mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

}