package com.example.ufabcirco.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.ufabcirco.R;
import com.example.ufabcirco.model.Movimento;

import java.util.List;

public class MovimentoDetailFragment extends DialogFragment {

    private static final String ARG_MOVIMENTO = "movimento";
    private Movimento movimento;
    private int currentImageIndex = 0;

    public static MovimentoDetailFragment newInstance(Movimento movimento) {
        MovimentoDetailFragment fragment = new MovimentoDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MOVIMENTO, movimento);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            movimento = (Movimento) getArguments().getSerializable(ARG_MOVIMENTO);
        }
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.DialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movimento_detail, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int width = (int) (displayMetrics.widthPixels * 0.9);
                int height = (int) (displayMetrics.heightPixels * 0.9);
                window.setLayout(width, height);
                window.setGravity(Gravity.CENTER);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleTextView = view.findViewById(R.id.text_view_movimento_title);
        TextView variantesTextView = view.findViewById(R.id.text_view_movimento_variantes);
        ImageView imageView = view.findViewById(R.id.image_view_movimento);
        TextView textTextView = view.findViewById(R.id.text_view_movimento_text);
        TextView tipoTextView = view.findViewById(R.id.text_view_movimento_tipo);

        if (movimento != null) {
            titleTextView.setText(movimento.getNome());

            String tipo;
            int tipoColor = Color.BLACK;
            switch (movimento.getTipo()) {
                case 0: tipo = "Figura"; tipoColor = Color.parseColor("#FF69B4"); break;
                case 1: tipo = "Trava"; tipoColor = Color.parseColor("#90EE90"); break;
                case 2: tipo = "Subida"; tipoColor = Color.parseColor("#00FF00"); break;
                case 3: tipo = "Giro"; tipoColor = Color.parseColor("#FED8B1"); break;
                case 4: tipo = "Queda"; tipoColor = Color.parseColor("#ff7700"); break;
                default: tipo = "Desconhecido";
            }
            tipoTextView.setText(tipo);
            tipoTextView.setTextColor(tipoColor);

            if (movimento.getVariantes() != null && !movimento.getVariantes().isEmpty()) {
                variantesTextView.setText(String.join(", ", movimento.getVariantes()));
                variantesTextView.setVisibility(View.VISIBLE);
            } else {
                variantesTextView.setVisibility(View.GONE);
            }

            if (movimento.getTexto() != null && !movimento.getTexto().isEmpty()) {
                textTextView.setText(movimento.getTexto());
                textTextView.setVisibility(View.VISIBLE);
            } else {
                textTextView.setVisibility(View.GONE);
            }

            List<String> fotos = movimento.getFotos();
            if (fotos != null && !fotos.isEmpty()) {
                updateImage(imageView, fotos);
                imageView.setVisibility(View.VISIBLE);
                imageView.setOnClickListener(v -> {
                    currentImageIndex = (currentImageIndex + 1) % fotos.size();
                    updateImage(imageView, fotos);
                });
            } else {
                imageView.setVisibility(View.GONE);
            }
        }
    }

    private void updateImage(ImageView imageView, List<String> fotos) {
        if (fotos != null && !fotos.isEmpty() && getContext() != null) {
            String url = fotos.get(currentImageIndex).trim();
            if (!url.isEmpty()) {
                Glide.with(this)
                        .load(url)
                        .into(imageView);
            } else {
                imageView.setImageDrawable(null);
                imageView.setVisibility(View.GONE);
            }
        } else {
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.GONE);
        }
    }
}