// app/src/main/java/com/example/ufabcirco/ui/PlaceholderFragment.java
package com.example.ufabcirco.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ufabcirco.R;

public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_TEXT = "section_text";

    public static PlaceholderFragment newInstance(String sectionText) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SECTION_TEXT, sectionText);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        TextView textView = view.findViewById(R.id.section_label);
        if (getArguments() != null) {
            textView.setText(getArguments().getString(ARG_SECTION_TEXT));
        }
        return view;
    }
}