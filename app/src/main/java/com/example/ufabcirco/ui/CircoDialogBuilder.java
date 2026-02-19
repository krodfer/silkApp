package com.example.ufabcirco.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import com.example.ufabcirco.R;

public class CircoDialogBuilder {
    public static AlertDialog.Builder create(Context context, String titleText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);

        TextView title = new TextView(context);
        title.setText(titleText);
        title.setPadding(0, 54, 0, 20);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#56114b"));

        try {
            Typeface pacifico = ResourcesCompat.getFont(context, R.font.pacifico);
            title.setTypeface(pacifico);
        } catch (Exception e) {
            title.setTypeface(null, Typeface.BOLD);
        }

        builder.setCustomTitle(title);
        return builder;
    }

    public static void fixColors(androidx.appcompat.app.AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            dialog.getWindow().getDecorView().findViewById(android.R.id.content)
                    .setBackgroundColor(android.graphics.Color.parseColor("#f7ece0"));
        }

        if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE) != null)
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#56114b"));

        if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE) != null)
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#56114b"));
    }
}