package com.example.ufabcirco.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.model.Movimento;
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.ui.custom.OutlineTextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TabelaAdapter extends RecyclerView.Adapter<TabelaAdapter.TabelaViewHolder> {

    private List<Pessoa> personList;
    private List<Movimento> moveList;
    private final OnMoveClickListener cellClickListener;
    private static final String TAG = "TabelaAdapter";
    private final RowScrollNotifier rowScrollNotifierCallback;
    private int lastKnownScrollX = 0;

    public interface OnMoveClickListener { void onMoveClick(Pessoa pessoa, String moveName); }
    public interface RowScrollNotifier {
        void onRowScrolled(int scrollX, RecyclerView.ViewHolder originatedFromViewHolder);
    }

    public TabelaAdapter(List<Pessoa> initialPersonList, List<Movimento> initialMoveList, OnMoveClickListener cellClickListener,
                         RowScrollNotifier rowScrollNotifierCallback) {
        this.personList = new ArrayList<>(initialPersonList);
        this.moveList = new ArrayList<>(initialMoveList);
        this.cellClickListener = cellClickListener;
        this.rowScrollNotifierCallback = rowScrollNotifierCallback;
    }

    public void updatePersonList(List<Pessoa> newPersonList) {
        this.personList.clear();
        this.personList.addAll(newPersonList);
        notifyDataSetChanged();
    }

    public void updateMoveList(List<Movimento> newMoveList) {
        this.moveList.clear();
        this.moveList.addAll(newMoveList);
        notifyDataSetChanged();
    }

    public void setHorizontalScrollPosition(int scrollX) {
        this.lastKnownScrollX = scrollX;
    }

    public void syncAllRowsToScroll(int scrollX, RecyclerView recyclerView, TabelaViewHolder excludedViewHolder) {
        if (recyclerView == null) return;
        setHorizontalScrollPosition(scrollX);
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            TabelaViewHolder vh = (TabelaViewHolder) recyclerView.getChildViewHolder(child);
            if (vh != null && vh != excludedViewHolder) {
                vh.syncScrollProgrammatically(scrollX);
            }
        }
    }

    @NonNull
    @Override
    public TabelaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tabela_row, parent, false);
        return new TabelaViewHolder(view, rowScrollNotifierCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull TabelaViewHolder holder, int position) {
        if (moveList == null || position >= moveList.size()) {
            return;
        }
        Movimento move = moveList.get(position);
        holder.bind(move, personList, cellClickListener);
        holder.syncScrollProgrammatically(lastKnownScrollX);
    }

    @Override
    public int getItemCount() {
        return moveList.size();
    }

    public static class TabelaViewHolder extends RecyclerView.ViewHolder {
        private final OutlineTextView moveLetter;
        private final LinearLayout statusCellsContainer;
        private final Context context;
        private final HorizontalScrollView statusCellsScrollView;
        private boolean isProgrammaticScroll = false;
        private final RowScrollNotifier rowScrollNotifier;
        private final View.OnScrollChangeListener scrollListener;

        public TabelaViewHolder(@NonNull View itemView, RowScrollNotifier rowScrollNotifier) {
            super(itemView);
            context = itemView.getContext();
            this.rowScrollNotifier = rowScrollNotifier;
            moveLetter = itemView.findViewById(R.id.text_view_move_letter);
            statusCellsContainer = itemView.findViewById(R.id.status_cells_container);
            statusCellsScrollView = itemView.findViewById(R.id.status_cells_scroll_view);

            this.scrollListener = (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isProgrammaticScroll && this.rowScrollNotifier != null) {
                    this.rowScrollNotifier.onRowScrolled(scrollX, this);
                }
                isProgrammaticScroll = false;
            };
            statusCellsScrollView.setOnScrollChangeListener(scrollListener);
        }

        public void bind(Movimento move, List<Pessoa> currentPersonList, OnMoveClickListener cellListener) {
            moveLetter.setText(move.getNome());
            statusCellsContainer.removeAllViews();

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

            for (Pessoa pessoa : currentPersonList) {
                TextView cell = (TextView) LayoutInflater.from(context).inflate(R.layout.item_move_cell, statusCellsContainer, false);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(120, context), dpToPx(40, context));
                params.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
                cell.setLayoutParams(params);

                int status = 0;
                if (pessoa.getMoveStatus() != null) {
                    status = pessoa.getMoveStatus().getOrDefault(move.getNome(), 0);
                }

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
                    case 3: cellBackground.setColor(Color.parseColor("#fa5f5f")); cell.setText("Não sabe"); break;
                    default: cellBackground.setColor(Color.WHITE); cell.setText(""); break;
                }
                cell.setBackground(cellBackground);
                cell.setOnClickListener(v -> cellListener.onMoveClick(pessoa, move.getNome()));
                statusCellsContainer.addView(cell);
            }
        }

        public void syncScrollProgrammatically(int scrollX) {
            isProgrammaticScroll = true;
            statusCellsScrollView.scrollTo(scrollX, 0);
        }

        private int dpToPx(int dp, Context context) {
            if (context == null) return dp;
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }
    }
}