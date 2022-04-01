package com.example.permisos;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class Contactos extends AppCompatActivity {

    String[] mProjection;
    Cursor mCursor;
    ContactsAdapter mContactsAdapter;
    ListView listaContactos;
    Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactos);
        listaContactos = (ListView) findViewById(R.id.listaContactos);
        mProjection = new String[]{
                ContactsContract.Profile._ID,
                ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
        };
        mContactsAdapter = new ContactsAdapter(this, null, 0);
        listaContactos.setAdapter(mContactsAdapter);
        snackbar = Snackbar.make(findViewById(R.id.layoutcontactos),"Se necesita el permiso para poder leer y mostrar sus contactos",Snackbar.LENGTH_SHORT);
        snackbar.setAction("Otorgar permiso",Otorgar);
        if(shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS))
        {
            snackbar.show();
        }else{
            solicitarPermisoContactos.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    ActivityResultLauncher<String> solicitarPermisoContactos = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result == true){
                    mCursor=getContentResolver().query(
                            ContactsContract.Contacts.CONTENT_URI,
                            mProjection, null, null, null);
                    mContactsAdapter.changeCursor(mCursor);
                }else{
                    Toast.makeText(Contactos.this, "No se otorgó el permiso para leer los contactos.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    private View.OnClickListener Otorgar = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            solicitarPermisoContactos.launch(Manifest.permission.READ_CONTACTS);
        }
    };
}