package com.example.ufabcirco.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.ufabcirco.R;
import com.example.ufabcirco.model.Movimento;
import com.example.ufabcirco.viewmodel.CircoViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovimentoDetailFragment extends DialogFragment {
    private Movimento movimento;
    private int currentMediaIndex = 0;
    private List<String> allMedia = new ArrayList<>();
    private ProgressBar loadingSpinner;

    public static MovimentoDetailFragment newInstance(Movimento m) {
        MovimentoDetailFragment fragment = new MovimentoDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("movimento", m);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movimento_detail, container, false);
        movimento = (Movimento) getArguments().getSerializable("movimento");
        loadingSpinner = view.findViewById(R.id.progress_bar_loading);

        setupHeader(view);
        setupMedia(view);
        setupSections(view);

        return view;
    }

    private void setupHeader(View v) {
        TextView title = v.findViewById(R.id.text_view_movimento_title);
        TextView tipoTv = v.findViewById(R.id.text_view_movimento_tipo);
        RatingBar ratingBar = v.findViewById(R.id.rating_bar_difficulty);
        TextView label = v.findViewById(R.id.text_view_difficulty_label);

        title.setText(movimento.getNome());
        String tipoStr;
        switch(movimento.getTipo()) {
            case 0: tipoStr = "Subida"; break;
            case 1: tipoStr = "Trava"; break;
            case 2: tipoStr = "Figura"; break;
            case 3: tipoStr = "Giro"; break;
            case 4: tipoStr = "Queda"; break;
            default: tipoStr = "Outro"; break;
        }
        tipoTv.setText(tipoStr);

        float diff = (float) movimento.getMediaDificuldade();
        ratingBar.setRating(diff);

        if (diff < 1.0) label.setText("Básico");
        else if (diff < 2.0) label.setText("Fácil");
        else if (diff < 3.0) label.setText("Médio");
        else if (diff < 4.0) label.setText("Difícil");
        else label.setText("Impossível");
    }

    private void setupMedia(View v) {
        ImageView imageView = v.findViewById(R.id.image_view_detail);
        if (movimento.getFotos() != null) allMedia.addAll(movimento.getFotos());
        //if (movimento.getVideos() != null) allMedia.addAll(movimento.getVideos());

        if (allMedia.isEmpty()) {
            loadingSpinner.setVisibility(View.GONE);
            return;
        }

        displayMedia(imageView);

        imageView.setOnClickListener(view -> {
            currentMediaIndex = (currentMediaIndex + 1) % allMedia.size();
            displayMedia(imageView);
        });
    }

    private void displayMedia(ImageView iv) {
        loadingSpinner.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(allMedia.get(currentMediaIndex))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        loadingSpinner.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        loadingSpinner.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(iv);
    }

    private void setupSections(View v) {
        LinearLayout btnContainer = v.findViewById(R.id.section_buttons_container);
        TextView contentTv = v.findViewById(R.id.text_view_content);
        String rawText = movimento.getTexto();
        String[] sections = {"Montagem", "Desmontagem", "Dicas"};
        List<Button> buttonList = new ArrayList<>();

        Typeface quicksandFont = ResourcesCompat.getFont(getContext(), R.font.quicksand);

        for (int i = 0; i < sections.length; i++) {
            String sectionName = sections[i];
            Pattern pattern = Pattern.compile("\\$" + sectionName + "\\$(.*?)(?=\\$|$)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(rawText);

            if (matcher.find()) {
                String content = matcher.group(1).trim();

                Button btn = new Button(getContext());
                buttonList.add(btn);
                btn.setText(sectionName);

                btn.setBackgroundResource(R.drawable.bg_button_layout);
                btn.setTextColor(Color.parseColor("#56114b"));
                btn.setTypeface(quicksandFont, Typeface.BOLD);
                btn.setAllCaps(false);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);


                params.setMargins(10, 100, 20, 40);
                btn.setLayoutParams(params);

                btn.setOnClickListener(view -> updateSectionText(content, contentTv, buttonList, btn));

                btnContainer.addView(btn);

                if (contentTv.getText().length() == 0) {
                    updateSectionText(content, contentTv, buttonList, btn);
                }
            }
        }
    }

    private void updateSectionText(String rawSectionText, TextView tv, List<Button> btnList, Button btn) {
        (btnList.get(0)).setBackgroundResource(R.drawable.bg_button_layout);
        if (btnList.size() >= 2) {
            (btnList.get(1)).setBackgroundResource(R.drawable.bg_button_layout);
        }
        if (btnList.size() == 3) {
            (btnList.get(2)).setBackgroundResource(R.drawable.bg_button_layout);
        }

        btn.setBackgroundResource(R.drawable.bg_button_inverted);

        SpannableStringBuilder finalBuilder = new SpannableStringBuilder();

        String[] lines = rawSectionText.split("\n");
        int count = 1;

        for (String line : lines) {
            if (line.trim().isEmpty()){
                continue;
            }

            String prefix = count + ") ";
            SpannableStringBuilder processedLine = processLinksInText(line.trim());
            processedLine.insert(0, prefix);
            processedLine.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#56114b")),
                    0,
                    prefix.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            finalBuilder.append(processedLine).append("\n\n");
            count++;
        }

        tv.setText(finalBuilder);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private SpannableStringBuilder processLinksInText(String text) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        Pattern pattern = Pattern.compile("%([^%]+)%");
        Matcher matcher = pattern.matcher(text);
        int offset = 0;
        while (matcher.find()) {
            String targetMove = matcher.group(1);
            int start = matcher.start() - offset;
            int end = matcher.end() - offset;
            ssb.delete(start, start + 1);
            ssb.delete(end - 2, end - 1);
            offset += 2;
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) { navigateToMove(targetMove); }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.parseColor("#56114b"));
                    ds.setUnderlineText(true);
                    ds.setFakeBoldText(true);
                    ds.setFakeBoldText(true);
                }
            };
            ssb.setSpan(clickableSpan, start, end - 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }

    private void navigateToMove(String name) {
        CircoViewModel viewModel = new ViewModelProvider(requireActivity()).get(CircoViewModel.class);
        List<Movimento> moves = viewModel.getMoveList().getValue();
        if (moves != null) {
            for (Movimento m : moves) {
                if (m.getNome().equalsIgnoreCase(name)) {
                    this.dismiss();
                    MovimentoDetailFragment.newInstance(m).show(getParentFragmentManager(), "MovimentoDetailFragment");
                    return;
                }
            }
        }
        Toast.makeText(getContext(), "Movimento não encontrado: " + name, Toast.LENGTH_SHORT).show();
    }
}