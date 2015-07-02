package com.example.wang.myapplication;

import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;

import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Array;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private Intent intent;


    ExifInterface mExif;


    float[] LatLng= null;
    Uri uri=null;
    String image =null;
    String title = null;
    String date=null;


    float[][] LatLngs =null;
    ArrayList<Uri> uris_content = null;
    String[] images = null;
    String[] titles = null;
    String[] dates=null;

    int num=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);




        MapFragment mapFragment;
        mapFragment = MapFragment.newInstance();
        mapFragment.getMapAsync(this);
        
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.map, mapFragment);
        fragmentTransaction.commit();

        GetSharedData();

        intent=getIntent();

        String GetInUri = intent.getStringExtra("GetInUris");
        if(GetInUri!=null){
            Pattern p=Pattern.compile("[,]");
            uris_content =new ArrayList<>(p.split(GetInUri).length);
            for(int j=0;j<p.split(GetInUri).length;j++){
                uris_content.add(Uri.parse(p.split(GetInUri)[j]));
            }

            int i=0;
            if(uris_content != null){

                LatLngs = new float[uris_content.size()][2];
                images = new String[uris_content.size()];
                titles= new String[uris_content.size()];
                dates= new String[uris_content.size()];
                for(Uri uris : uris_content){
                    images[i]= GetAbsolutePath(uris);
                    Cursor returnCursor =
                            getContentResolver().query(uris, null, null, null, null);
                    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    returnCursor.moveToFirst();

                    titles[i]=returnCursor.getString(nameIndex);
                    try{
                        mExif = new ExifInterface(images[i]);
                        mExif.getLatLong(LatLngs[i]);
                        dates[i]= mExif.getAttribute(ExifInterface.TAG_DATETIME);
                        i++;

                    }catch (IOException e){
                        e.printStackTrace();
                    }

                }
            }
        }


    }


    private void GetSharedData() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();


        //single item selected
        if (Intent.ACTION_SEND.equals(action) && type != null) {

            if (type.startsWith("image/")) {
                LatLng = new float[2];
                uri=handleSendImage(intent);

                if(uri !=null){
                    image = GetAbsolutePath(uri);

                    Cursor returnCursor =
                            getContentResolver().query(uri, null, null, null, null);
                    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    returnCursor.moveToFirst();
                    title=returnCursor.getString(nameIndex);
                }
                try {
                    mExif = new ExifInterface(image);
                    mExif.getLatLong(LatLng);
                       date= mExif.getAttribute(ExifInterface.TAG_DATETIME);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else
        //multiple items selected
        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {

                uris_content=handleSendMultipleImages(intent);
                int i=0;
                if(uris_content != null){

                    LatLngs = new float[uris_content.size()][2];
                    images = new String[uris_content.size()];
                    titles= new String[uris_content.size()];
                    dates= new String[uris_content.size()];
                    for(Uri uris : uris_content){
                        images[i]= GetAbsolutePath(uris);
                        Cursor returnCursor =
                                getContentResolver().query(uris, null, null, null, null);
                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        returnCursor.moveToFirst();

                        titles[i]=returnCursor.getString(nameIndex);
                        try{
                            mExif = new ExifInterface(images[i]);
                            mExif.getLatLong(LatLngs[i]);
                            dates[i]= mExif.getAttribute(ExifInterface.TAG_DATETIME);
                            i++;

                        }catch (IOException e){
                            e.printStackTrace();
                        }

                    }
                }



            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setUpMapIfNeeded();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {


        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // Setting a custom info window adapter for the google map
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker arg0) {

                // Getting view from the layout file info_window_layout
                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);

                // Getting the position from the marker
                LatLng latLng = arg0.getPosition();

                TextView Name = (TextView) v.findViewById(R.id.title);
                TextView Date = (TextView) v.findViewById(R.id.date);

                // Getting reference to the TextView to set latitude
                TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);

                // Getting reference to the TextView to set longitude
                TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);

                // Getting reference to the ImageView
                ImageView imageView = (ImageView)v.findViewById(R.id.image);

                // Setting the latitude
                tvLat.setText("Latitude:" + latLng.latitude);

                // Setting the longitude
                tvLng.setText("Longitude:" + latLng.longitude);

                Name.setText("Name:"+title);
                Date.setText("Date:"+date);
                //Setting the image
                imageView.setImageURI(uri);
                // Returning the view containing InfoWindow contents
                return v;

            }
        });

        if(LatLng != null) {

            if(LatLng[0]!=0&&LatLng[1]!=0){
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(LatLng[0], LatLng[1]))
                        .title(LatLng[0] + " " + LatLng[1]));
            }

        }
        if(LatLngs !=null){
           for(num=0;num<uris_content.size();num++){


                // Setting a custom info window adapter for the google map
               googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                   // Use default InfoWindow frame
                   @Override
                   public View getInfoWindow(Marker arg0) {
                       return null;
                   }

                   // Defines the contents of the InfoWindow
                   @Override
                   public View getInfoContents(Marker arg0) {

                       // Getting view from the layout file info_window_layout
                       View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);

                       // Getting the position from the marker
                       LatLng latLng = arg0.getPosition();
                       String snippet = arg0.getSnippet();
                       Pattern p = Pattern.compile("[,]");
                       String post = p.split(snippet)[0];
                       String name = p.split(snippet)[1];
                       String date = p.split(snippet)[2];


                       TextView Name = (TextView) v.findViewById(R.id.title);
                       TextView Date = (TextView) v.findViewById(R.id.date);
                       // Getting reference to the TextView to set latitude
                       TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);

                       // Getting reference to the TextView to set longitude
                       TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);

                       // Getting reference to the ImageView
                       ImageView imageView = (ImageView) v.findViewById(R.id.image);

                       // Setting the latitude
                       tvLat.setText("Latitude:" + latLng.latitude);

                       // Setting the longitude
                       tvLng.setText("Longitude:" + latLng.longitude);
                       Name.setText("Name:"+name);
                       Date.setText("Date:"+date);

                       if (uris_content != null) {
                           imageView.setImageBitmap(BitmapFactory.decodeFile(post));
                           Log.d("tag", snippet);
                       }
                       // Returning the view containing InfoWindow contents
                       return v;

                   }
               });
               if(LatLngs[num][0]!=0&&LatLngs[num][1]!=0){
                   googleMap.addMarker(new MarkerOptions()
                           .position(new LatLng(LatLngs[num][0], LatLngs[num][1]))
                           .title(LatLngs[num][0] + " " + LatLngs[num][1]).snippet(GetAbsolutePath(uris_content.get(num))+","+titles[num]+","+dates[num]));
               }

           }
        }
    }

    Uri handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            // Update UI to reflect image being shared
            return imageUri;
        }
        return  imageUri;
    }
    ArrayList handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
            return imageUris;
        }
        return imageUris;
    }
    public String GetAbsolutePath(Uri imageUri) {
        Cursor cursor = getContentResolver().query(imageUri, new String[]{
                MediaStore.Images.Media.DATA}, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String absolutePath = cursor.getString(column_index);
        cursor.close();
        return absolutePath;
    }
}
