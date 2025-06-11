package com.example.ufabcirco.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
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
import com.example.ufabcirco.model.Pessoa;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TabelaAdapter extends RecyclerView.Adapter<TabelaAdapter.TabelaViewHolder> {

    private List<Pessoa> personList;
    private final List<String> moveList;
    private final OnMoveClickListener cellClickListener;
    private static final String TAG = "TabelaAdapter";
    private final RowScrollNotifier rowScrollNotifierCallback;
    private int lastKnownScrollX = 0;

    public interface OnMoveClickListener { void onMoveClick(Pessoa pessoa, String move); }
    public interface RowScrollNotifier {
        void onRowScrolled(int scrollX, RecyclerView.ViewHolder originatedFromViewHolder);
    }

    public TabelaAdapter(List<Pessoa> initialPersonList, List<String> moveList, OnMoveClickListener cellClickListener,
                         RowScrollNotifier rowScrollNotifierCallback) {
        this.personList = new ArrayList<>(initialPersonList);
        this.moveList = moveList;
        this.cellClickListener = cellClickListener;
        this.rowScrollNotifierCallback = rowScrollNotifierCallback;
    }

    public void updatePersonList(List<Pessoa> newPersonList) {
        this.personList.clear();
        this.personList.addAll(newPersonList);
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
            Log.e(TAG, "Tentativa de acessar move fora dos limites: " + position);
            return;
        }
        String move = moveList.get(position);
        holder.bind(move, personList, cellClickListener);
        holder.syncScrollProgrammatically(lastKnownScrollX);
    }

    @Override
    public int getItemCount() {
        return moveList.size();
    }

    public static class TabelaViewHolder extends RecyclerView.ViewHolder {
        private final TextView moveLetter;
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

        public void bind(String move, List<Pessoa> currentPersonList, OnMoveClickListener cellListener) {
            moveLetter.setText(move);
            statusCellsContainer.removeAllViews();

            if (currentPersonList == null) return;

            for (Pessoa pessoa : currentPersonList) {
                TextView cell = (TextView) LayoutInflater.from(context).inflate(R.layout.item_move_cell, statusCellsContainer, false);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(120, context), dpToPx(40, context));
                params.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
                cell.setLayoutParams(params);

                int status = 0;
                if (pessoa.getMoveStatus() != null) {
                    status = pessoa.getMoveStatus().getOrDefault(move, 0);
                }

                GradientDrawable background;
                try {
                    background = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.cell_border).mutate();
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao obter drawable cell_border. Criando um novo.", e);
                    background = new GradientDrawable();
                    background.setShape(GradientDrawable.RECTANGLE);
                    background.setStroke(dpToPx(1, context), Color.LTGRAY);
                }
                if(background == null) {
                    background = new GradientDrawable();
                    background.setShape(GradientDrawable.RECTANGLE);
                    background.setStroke(dpToPx(1, context), Color.LTGRAY);
                }


                switch (status) {
                    case 1: background.setColor(Color.YELLOW); cell.setText("Já fez"); break;
                    case 2: background.setColor(Color.GREEN); cell.setText("Aprendeu"); break;
                    case 3: background.setColor(Color.RED); cell.setText("Não sabe"); break;
                    default: background.setColor(Color.WHITE); cell.setText(""); break;
                }
                cell.setBackground(background);
                cell.setOnClickListener(v -> cellListener.onMoveClick(pessoa, move));
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