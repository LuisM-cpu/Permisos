package com.example.permisos;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ImageButton botonCamara, botonContactos, botonMapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        botonCamara = findViewById(R.id.botonCamara);
        botonContactos = findViewById(R.id.botonContactos);
        botonMapa = findViewById(R.id.botonMapa);

        botonCamara.setOnClickListener(abrirCamara);
        botonContactos.setOnClickListener(verContactos);
        botonMapa.setOnClickListener(abrirMapa);
    }

    private View.OnClickListener abrirCamara = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(view.getContext(),Camara.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener verContactos = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(view.getContext(),Contactos.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener abrirMapa = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(view.getContext(), Mapa.class);
            startActivity(intent);
        }
    };
}