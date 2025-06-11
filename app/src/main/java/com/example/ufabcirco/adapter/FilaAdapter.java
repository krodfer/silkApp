// app/src/main/java/com/example/ufabcirco/adapter/FilaAdapter.java
package com.example.ufabcirco.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.model.Pessoa;
import com.google.android.flexbox.FlexboxLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FilaAdapter extends RecyclerView.Adapter<FilaAdapter.FilaViewHolder> {

    private List<Pessoa> personList = new ArrayList<>();
    private String selectedPessoaId = null;
    private int selectionColor = Color.TRANSPARENT;

    private final Consumer<Pessoa> onItemClickListener;
    private final Consumer<Pessoa> onRemoveClickListener;

    public FilaAdapter(Consumer<Pessoa> onItemClickListener, Consumer<Pessoa> onRemoveClickListener) {
        this.onItemClickListener = onItemClickListener;
        this.onRemoveClickListener = onRemoveClickListener;
    }

    public void setFilaPessoas(List<Pessoa> personList) {
        this.personList = personList != null ? personList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectionState(String id, Integer color) {
        this.selectedPessoaId = id;
        this.selectionColor = (color != null) ? color : Color.TRANSPARENT;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FilaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pessoa, parent, false);
        return new FilaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilaViewHolder holder, int position) {
        if (personList.isEmpty()) return;
        Pessoa pessoa = personList.get(position % personList.size());
        boolean isCurrentlySelected = pessoa.getId().equals(selectedPessoaId);
        holder.bind(pessoa, onItemClickListener, onRemoveClickListener,
                isCurrentlySelected, selectionColor);
    }

    @Override
    public int getItemCount() {
        return personList.isEmpty() ? 0 : Integer.MAX_VALUE;
    }

    class FilaViewHolder extends RecyclerView.ViewHolder {
        private final TextView textNome;
        private final FlexboxLayout flexboxMoves;
        private final Button buttonRemover;
        private final Context context;

        public FilaViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            textNome = itemView.findViewById(R.id.text_view_nome);
            flexboxMoves = itemView.findViewById(R.id.flexbox_moves);
            buttonRemover = itemView.findViewById(R.id.button_remover);
        }

        public void bind(final Pessoa pessoa, final Consumer<Pessoa> onItemClickListener, final Consumer<Pessoa> onRemoveClickListener, boolean isSelected, int color) {
            textNome.setText(pessoa.getNome());

            if (isSelected) {
                GradientDrawable background = new GradientDrawable();
                background.setShape(GradientDrawable.RECTANGLE);
                float cornerRadiusInPixels = itemView.getResources().getDisplayMetrics().density * 8;
                background.setCornerRadius(cornerRadiusInPixels);
                background.setColor(color);
                itemView.setBackground(background);
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT);
            }

            flexboxMoves.removeAllViews();
            Map<String, Integer> moveStatusMap = pessoa.getMoveStatus();
            if (moveStatusMap != null) {
                for (Map.Entry<String, Integer> entry : moveStatusMap.entrySet()) {
                    int status = entry.getValue();
                    if (status == 1 || status == 2) {
                        TextView chip = new TextView(context);
                        chip.setText(entry.getKey());
                        chip.setTextColor(Color.BLACK);
                        chip.setPadding(16, 8, 16, 8);

                        GradientDrawable chipBackground = new GradientDrawable();
                        chipBackground.setShape(GradientDrawable.RECTANGLE);
                        chipBackground.setCornerRadius(30f);
                        chipBackground.setStroke(4, Color.BLACK);
                        chipBackground.setColor(status == 1 ? Color.YELLOW : Color.GREEN);

                        chip.setBackground(chipBackground);

                        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                                FlexboxLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(8,8,8,8);
                        chip.setLayoutParams(params);
                        flexboxMoves.addView(chip);
                    }
                }
            }

            buttonRemover.setOnClickListener(v -> onRemoveClickListener.accept(pessoa));
            itemView.setOnClickListener(v -> onItemClickListener.accept(pessoa));
        }
    }
}