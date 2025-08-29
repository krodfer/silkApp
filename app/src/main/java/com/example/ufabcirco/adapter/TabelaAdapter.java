package com.example.ufabcirco.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.model.Movimento;
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.ui.custom.OutlineTextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import android.widget.Toast;

public class TabelaAdapter extends RecyclerView.Adapter<TabelaAdapter.TabelaViewHolder> {

    private List<Movimento> moveList;
    private List<Pessoa> pessoaList;
    private final OnMoveClickListener cellClickListener;

    public interface OnMoveClickListener {
        void onMoveClick(String pessoaId, String moveName);
    }

    public TabelaAdapter(List<Pessoa> initialPersonList, List<Movimento> initialMoveList, OnMoveClickListener cellClickListener) {
        this.pessoaList = new ArrayList<>(initialPersonList);
        this.moveList = new ArrayList<>(initialMoveList);
        this.cellClickListener = cellClickListener;
        setHasStableIds(true);
    }

    public void updatePersonList(List<Pessoa> newPersonList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PessoaDiffCallback(this.pessoaList, newPersonList));
        this.pessoaList.clear();
        this.pessoaList.addAll(newPersonList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateMoveList(List<Movimento> newMoveList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MovimentoDiffCallback(this.moveList, newMoveList));
        this.moveList.clear();
        this.moveList.addAll(newMoveList);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < moveList.size()) {
            return moveList.get(position).getNome().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    @NonNull
    @Override
    public TabelaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tabela_row, parent, false);
        return new TabelaViewHolder(view, cellClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull TabelaViewHolder holder, int position) {
        if (moveList == null || position >= moveList.size()) {
            return;
        }
        Movimento move = moveList.get(position);

        holder.movimentoTextView.setText(move.getNome());
        holder.dificuldadeTextView.setText(String.format("%.2f", move.getMediaDificuldade()));

        holder.statusCellsContainer.removeAllViews();

        Context context = holder.itemView.getContext();
        int cellWidth = dpToPx(80, context);

        for (Pessoa pessoa : pessoaList) {
            OutlineTextView cellTextView = new OutlineTextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cellWidth, dpToPx(40, context));
            params.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
            cellTextView.setLayoutParams(params);

            int status = pessoa.getMoveStatus().getOrDefault(move.getNome(), 0);
            setupCellView(cellTextView, status);

            cellTextView.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Pressione e segure para mudar o status.", Toast.LENGTH_SHORT).show();
            });

            cellTextView.setOnLongClickListener(v -> {
                if (cellClickListener != null) {
                    cellClickListener.onMoveClick(pessoa.getId(), move.getNome());
                    return true;
                }
                return false;
            });

            holder.statusCellsContainer.addView(cellTextView);
        }
    }

    @Override
    public int getItemCount() {
        return moveList.size();
    }

    public static class TabelaViewHolder extends RecyclerView.ViewHolder {
        private final OutlineTextView movimentoTextView;
        private final TextView dificuldadeTextView;
        private final LinearLayout statusCellsContainer;
        private final OnMoveClickListener cellClickListener;

        public TabelaViewHolder(@NonNull View itemView, OnMoveClickListener cellClickListener) {
            super(itemView);
            this.cellClickListener = cellClickListener;
            movimentoTextView = itemView.findViewById(R.id.text_view_move_letter);
            dificuldadeTextView = itemView.findViewById(R.id.text_view_dificuldade);
            statusCellsContainer = itemView.findViewById(R.id.status_cells_container);
        }
    }

    private int dpToPx(int dp, Context context) {
        if (context == null) return dp;
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private void setupCellView(OutlineTextView cell, int status) {
        Context context = cell.getContext();
        GradientDrawable cellBackground = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.cell_border).mutate();

        switch (status) {
            case 1: cellBackground.setColor(Color.YELLOW); cell.setText("Já fez"); break;
            case 2: cellBackground.setColor(Color.parseColor("#45aaf7")); cell.setText("Aprendeu"); break;
            case 3: cellBackground.setColor(Color.parseColor("#fa5f5f")); cell.setText("Não consegue"); break;
            default: cellBackground.setColor(Color.WHITE); cell.setText(""); break;
        }
        cell.setBackground(cellBackground);
    }

    class PessoaDiffCallback extends DiffUtil.Callback {
        private final List<Pessoa> oldList;
        private final List<Pessoa> newList;

        public PessoaDiffCallback(List<Pessoa> oldList, List<Pessoa> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    class MovimentoDiffCallback extends DiffUtil.Callback {
        private final List<Movimento> oldList;
        private final List<Movimento> newList;

        public MovimentoDiffCallback(List<Movimento> oldList, List<Movimento> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(oldList.get(oldItemPosition).getNome(), newList.get(newItemPosition).getNome());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}