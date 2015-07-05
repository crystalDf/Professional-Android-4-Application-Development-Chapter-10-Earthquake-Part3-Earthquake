package com.star.earthquake;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;

public class EarthquakeDialog extends DialogFragment {

    private static String DIALOG_STRING = "DIALOG_STRING";

    public static EarthquakeDialog newInstance(Context context, Quake quake) {

        EarthquakeDialog earthquakeDialog = new EarthquakeDialog();

        Bundle args = new Bundle();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String dateString = simpleDateFormat.format(quake.getDate());

        String quakeText = dateString + "\n" +
                "Magnitude " + quake.getMagnitude() + "\n" +
                quake.getDetails() + "\n" +
                quake.getLink();

        args.putString(DIALOG_STRING, quakeText);
        earthquakeDialog.setArguments(args);

        return earthquakeDialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.quake_details, container, false);

        String title = getArguments().getString(DIALOG_STRING);

        TextView textView = (TextView) view.findViewById(R.id.quakeDetailsTextView);

        textView.setText(title);

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Earthquake Details");

        return dialog;
    }
}
