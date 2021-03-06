package com.example.worldgreen.Reports;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.worldgreen.DataModel.Event;
import com.example.worldgreen.DataModel.Report;
import com.example.worldgreen.Donate.DonateActivity;
import com.example.worldgreen.Events.AllEventActivity;
import com.example.worldgreen.Events.CreateEventActivity;
import com.example.worldgreen.Events.MyEventActivity;
import com.example.worldgreen.FirebaseManager.EventCallback;
import com.example.worldgreen.FirebaseManager.FirebaseManager;
import com.example.worldgreen.MainActivity;
import com.example.worldgreen.R;
import com.example.worldgreen.Users.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DetailReportActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static final String TAG = "DetailReportActivity";
    private LinearLayout gallery;
    private LayoutInflater layoutInflater;
    Report report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_report);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        report = (Report) getIntent().getSerializableExtra("report");
        Log.d(TAG, "onCreate: photo size" + report.getPhotos().size());

        gallery = findViewById(R.id.photo_gallery);
        layoutInflater = LayoutInflater.from(this);

        setupButtons();
        displayData();
    }

    private void setupButtons() {
        final FirebaseManager manager = new FirebaseManager();
        Button createEvent = findViewById(R.id.test_create_event);
        createEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(DetailReportActivity.this, CreateEventActivity.class);
                i.putExtra("report", report);
                startActivity(i);
            }
        });
        ImageButton goToMapButton = findViewById(R.id.show_in_map);
        goToMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriString = "geo:" + report.getLatitude() + "," + report.getLongitude();
                Log.d(TAG, "onClick: uriString: " + uriString);
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(uriString));
                startActivity(intent);
            }
        });
    }

    private void displayData() {
        for (byte[] photo: report.getPhotos()) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            addPhoto(bitmap);
        }

        TextView titleTextView = findViewById(R.id.report_detail_title);
        TextView description = findViewById(R.id.description);
        TextView accessibility = findViewById(R.id.accessibility);
        TextView addressTextView = findViewById(R.id.address);
        TextView position = findViewById(R.id.gps_coordinates);
        TextView size = findViewById(R.id.size);
        ImageView accessImg = findViewById(R.id.detail_access_img);
        ImageView sizeImg = findViewById(R.id.detail_size_img);


        String address = getAddress(report.getLatitude(), report.getLongitude());
        DecimalFormat df = new DecimalFormat("##.####");
        String latStr = df.format(report.getLatitude());
        String lonStr = df.format(report.getLongitude());

        description.setText(report.getDescription());
        titleTextView.setText(report.getTitle());
        position.setText(latStr + ", " + lonStr);

        if (report.isAccessibleByCar()) {
            accessImg.setBackgroundResource(R.drawable.ic_acess_car);
            accessibility.setText("Accessible by car");
        } else {
            accessImg.setBackgroundResource(R.drawable.ic_acess_walk);
            accessibility.setText("Not accessible by car");
        }


        if (address.length() > 0 ) {
            addressTextView.setText(address);
        } else {
            addressTextView.setText("Unknown address.");
        }

        if (report.getSize().toLowerCase().equals("bag")) {
            Log.d(TAG, "displayData: set as bag");
            sizeImg.setBackgroundResource(R.drawable.ic_size_sack);
            size.setText(report.getSize() + " should be enough");
        } else if (report.getSize().toLowerCase().equals("wheelbarrow")) {
            Log.d(TAG, "displayData: set as wheelbarrow");
            sizeImg.setBackgroundResource(R.drawable.ic_size_wheelbarrow);
            size.setText(report.getSize() + " needed.");
        } else if (report.getSize().toLowerCase().equals("car")) {
            Log.d(TAG, "displayData: set as car");
            sizeImg.setBackgroundResource(R.drawable.ic_acess_car);
            size.setText(report.getSize() + " needed.");
        }

    }

    private String getAddress(double lat, double lon) {
        String cityName = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(lat,lon,10);
            if (addresses.size() > 0) {
                for (Address address : addresses) {
                    if (address.getLocality() != null && address.getLocality().length() > 0) {
                        cityName = address.getLocality();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void addPhoto(Bitmap photo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        View newView = layoutInflater.inflate(R.layout.create_report_item, gallery, false);
        ImageView imageView = newView.findViewById(R.id.create_report_item_imageView);
        imageView.setImageBitmap(photo);
        gallery.addView(newView);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_create_report) {
            startActivity(new Intent(this,CreateReportActivity.class));
        } else if (id == R.id.nav_view_all_report) {
            startActivity(new Intent(this, AllReportActivity.class));
        } else if (id == R.id.nav_view_all_event) {
            startActivity(new Intent(this, AllEventActivity.class));
        } else if (id == R.id.nav_donate) {
            startActivity(new Intent(this, DonateActivity.class));
        } else if (id == R.id.nav_my_event) {
            startActivity(new Intent(this, MyEventActivity.class).putExtra("participating", false));
        } else if (id == R.id.nav_participate_event) {
            startActivity(new Intent(this, MyEventActivity.class).putExtra("participating", true));
        } else if (id == R.id.nav_my_report) {
            startActivity(new Intent(this, MyReportActivity.class));
        } else if (id == R.id.nav_sign_out) {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Sign out Successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else if (id == R.id.nav_home) {
            startActivity(new Intent(this, MainActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}