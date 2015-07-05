package com.star.earthquake;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class EarthquakeAddActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_earthquake_add);

        Button button = (Button) findViewById(R.id.add_button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(EarthquakeAddActivity.this,
                        EarthquakeUpdateService.class);

                EditText detailsEditText = (EditText) findViewById(R.id.details_edit_text);
                EditText latitudeEditText = (EditText) findViewById(R.id.latitude_edit_text);
                EditText longitudeEditText = (EditText) findViewById(R.id.longitude_edit_text);
                EditText magnitudeEditText = (EditText) findViewById(R.id.magnitude_edit_text);
                EditText linkEditText = (EditText) findViewById(R.id.link_edit_text);

                String details = detailsEditText.getText().toString();
                if (details.equals("")) {
                    details = "Dominican Republic region";
                }

                double latitude = Double.parseDouble(latitudeEditText.getText().toString());
                double longitude = Double.parseDouble(longitudeEditText.getText().toString());

                Location location = new Location("Location");
                location.setLatitude(latitude);
                location.setLongitude(longitude);

                double magnitude = Double.parseDouble(magnitudeEditText.getText().toString());

                String link = linkEditText.getText().toString();
                if (link.equals("")) {
                    link = "http://earthquake.usgs.gov/earthquakes/" +
                            "recenteqsww/Quakes/pr15185000.php";
                }

                Bundle args = new Bundle();

                args.putLong("QuakeDate", System.currentTimeMillis());
                args.putString("Details", details);
                args.putParcelable("Location", location);
                args.putDouble("Magnitude", magnitude);
                args.putString("Link", link);

                intent.putExtras(args);

                startService(intent);

                finish();
            }
        });
    }
}
