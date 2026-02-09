package com.example.ufabcirco.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.ufabcirco.R;
import com.example.ufabcirco.model.Movimento;
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.ui.custom.ColoredUnderlineSpan;
import com.example.ufabcirco.viewmodel.CircoViewModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProfileMenuFragment extends DialogFragment {

    private static final String ARG_PESSOA = "pessoa";
    private Pessoa pessoa;
    private CircoViewModel viewModel;

    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 0.7f;
    private int baseWidth;
    private final double aspectRatio = 5.0 / 7.0;

    public static ProfileMenuFragment newInstance(Pessoa pessoa) {
        ProfileMenuFragment fragment = new ProfileMenuFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PESSOA, pessoa);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pessoa = (Pessoa) getArguments().getSerializable(ARG_PESSOA);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(CircoViewModel.class);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        baseWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_menu, container, false);
        view.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setGravity(Gravity.CENTER);
                resizeDialog();
            }
        }
    }

    private void resizeDialog() {
        if (getDialog() == null || getDialog().getWindow() == null) {
            return;
        }

        int finalWidth = (int) (baseWidth * scaleFactor);
        int finalHeight = (int) (finalWidth * aspectRatio);

        getDialog().getWindow().setLayout(finalWidth, finalHeight);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.7f, Math.min(scaleFactor, 1.2f));
            resizeDialog();
            return true;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView nameTextView = view.findViewById(R.id.text_view_profile_name);
        TextView roleTextView = view.findViewById(R.id.text_view_profile_role);
        TextView columnOneTextView = view.findViewById(R.id.text_view_column_one);
        TextView columnTwoTextView = view.findViewById(R.id.text_view_column_two);

        if (pessoa != null) {
            nameTextView.setText(pessoa.getNome());

            if (viewModel.isInstructor(pessoa.getNome())) {
                roleTextView.setText("Instrutor");
                roleTextView.setTextColor(Color.parseColor("#800080"));
            } else {
                roleTextView.setText("Aluno");
            }

            Map<String, Integer> moveStatusMap = pessoa.getMoveStatus();
            List<Movimento> allMovesMasterList = viewModel.getMoveList().getValue();
            if (allMovesMasterList == null) allMovesMasterList = new ArrayList<>();

            List<Pair<Movimento, Integer>> allMovesWithStatus = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : moveStatusMap.entrySet()) {
                if (entry.getValue() >= 1 && entry.getValue() <= 3) {
                    Optional<Movimento> moveOpt = allMovesMasterList.stream().filter(m -> m.getNome().equals(entry.getKey())).findFirst();
                    moveOpt.ifPresent(movimento -> allMovesWithStatus.add(new Pair<>(movimento, entry.getValue())));
                }
            }

            allMovesWithStatus.sort(Comparator.comparing(p -> p.first.getNome()));

            SpannableStringBuilder colOneBuilder = new SpannableStringBuilder();
            SpannableStringBuilder colTwoBuilder = new SpannableStringBuilder();

            float iconSize = columnOneTextView.getTextSize();

            for (int i = 0; i < allMovesWithStatus.size(); i++) {
                Pair<Movimento, Integer> movePair = allMovesWithStatus.get(i);

                SpannableStringBuilder currentBuilder = (i % 2 == 0) ? colOneBuilder : colTwoBuilder;

                appendMoveWithSpans(currentBuilder, movePair, iconSize);
            }

            columnOneTextView.setText(colOneBuilder);
            columnTwoTextView.setText(colTwoBuilder);
        }
    }

    private void appendMoveWithSpans(SpannableStringBuilder builder, Pair<Movimento, Integer> movePair, float iconSize) {
        if (getContext() == null) {
            return;
        }

        Movimento move = movePair.first;
        int status = movePair.second;

        int drawableId = 0;
        switch(status) {
            case 1: drawableId = R.drawable.ic_status_cannot; break;
            case 2: drawableId = R.drawable.ic_status_learned; break;
            case 3: drawableId = R.drawable.ic_status_done; break;
        }

        int start = builder.length();
        builder.append(" ");
        int end = builder.length();

        if (drawableId != 0) {
            try {
                Drawable d = ContextCompat.getDrawable(getContext(), drawableId);
                d.setBounds(0, 0, (int) iconSize, (int) iconSize);
                ImageSpan imageSpan = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                builder.setSpan(imageSpan, start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            } catch (Exception e) {
                builder.replace(start, end, "•");
            }
        }

        start = builder.length();
        builder.append(" ").append(move.getNome()).append("\n");
        end = builder.length();

        int typeColor = Color.BLACK;
        switch(move.getTipo()){
            case 0: typeColor = Color.parseColor("#90EE90"); break;
            case 1: typeColor = Color.parseColor("#00FF00"); break;
            case 2: typeColor = Color.parseColor("#FF69B4"); break;
            case 3: typeColor = Color.parseColor("#FED8B1"); break;
            case 4: typeColor = Color.parseColor("#FFA500"); break;
        }

        float underlineHeight = 4f;
        builder.setSpan(new ColoredUnderlineSpan(typeColor, underlineHeight), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }
}