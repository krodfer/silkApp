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
        holder.bind(move, pessoaList);
    }

    @Override
    public int getItemCount() {
        return moveList.size();
    }

    public static class TabelaViewHolder extends RecyclerView.ViewHolder {
        private final OutlineTextView moveLetter;
        private final TextView movimentoTextView;
        private final LinearLayout statusCellsContainer;
        private final Context context;
        private final OnMoveClickListener cellClickListener;

        public TabelaViewHolder(@NonNull View itemView, OnMoveClickListener cellClickListener) {
            super(itemView);
            context = itemView.getContext();
            this.cellClickListener = cellClickListener;
            moveLetter = itemView.findViewById(R.id.text_view_move_letter);
            movimentoTextView = itemView.findViewById(R.id.text_view_move_letter);
            statusCellsContainer = itemView.findViewById(R.id.status_cells_container);
        }

        public void bind(Movimento move, List<Pessoa> currentPersonList) {
            moveLetter.setText(move.getNome());

            int numPerson = currentPersonList.size();
            int currentCellCount = statusCellsContainer.getChildCount();

            while (currentCellCount < numPerson) {
                TextView newCell = (TextView) LayoutInflater.from(context).inflate(R.layout.item_move_cell, statusCellsContainer, false);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(120, context), dpToPx(40, context));
                params.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
                newCell.setLayoutParams(params);
                statusCellsContainer.addView(newCell);
                currentCellCount++;
            }

            while (currentCellCount > numPerson) {
                statusCellsContainer.removeViewAt(currentCellCount - 1);
                currentCellCount--;
            }

            int textColor = Color.BLACK;
            switch(move.getTipo()){
                case 0: textColor = Color.parseColor("#00FF00"); break;
                case 1: textColor = Color.parseColor("#c9ffc9"); break;
                case 2: textColor = Color.parseColor("#f037a6"); break;
                case 3: textColor = Color.parseColor("#FED8B1"); break;
                case 4: textColor = Color.parseColor("#ff7700"); break;
            }

            moveLetter.setTextColor(textColor);
            moveLetter.setOutlineColor(Color.BLACK);
            moveLetter.setOutlineWidth(4.0f);

            if (currentPersonList == null) return;

            for (int i = 0; i < numPerson; i++) {
                Pessoa pessoa = currentPersonList.get(i);
                TextView cell = (TextView) statusCellsContainer.getChildAt(i);
                int status = pessoa.getMoveStatus().getOrDefault(move.getNome(), 0);

                GradientDrawable cellBackground;
                try {
                    cellBackground = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.cell_border).mutate();
                } catch (Exception e) {
                    cellBackground = new GradientDrawable();
                    cellBackground.setShape(GradientDrawable.RECTANGLE);
                    cellBackground.setStroke(dpToPx(1, context), Color.LTGRAY);
                }

                switch (status) {
                    case 1: cellBackground.setColor(Color.YELLOW); cell.setText("Já fez"); break;
                    case 2: cellBackground.setColor(Color.parseColor("#45aaf7")); cell.setText("Aprendeu"); break;
                    case 3: cellBackground.setColor(Color.parseColor("#fa5f5f")); cell.setText("Não consegue"); break;
                    default: cellBackground.setColor(Color.WHITE); cell.setText(""); break;
                }
                cell.setBackground(cellBackground);

                cell.setOnClickListener(v -> {
                    if (cellClickListener != null) {
                        cellClickListener.onMoveClick(pessoa.getId(), move.getNome());
                    }
                });
            }
        }

        private int dpToPx(int dp, Context context) {
            if (context == null) return dp;
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }
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