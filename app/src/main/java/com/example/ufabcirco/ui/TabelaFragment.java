package com.example.ufabcirco.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.adapter.TabelaAdapter;
import com.example.ufabcirco.model.Movimento;
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.viewmodel.CircoViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TabelaFragment extends Fragment implements TabelaAdapter.RowScrollNotifier {

    private static final String TAG = "TabelaFragment";
    private CircoViewModel circoViewModel;
    private RecyclerView recyclerViewTabela;
    private TabelaAdapter tabelaAdapter;
    private LinearLayout headerNamesContainer;
    private HorizontalScrollView headerNamesScrollView;
    private FloatingActionButton fabExport, fabImport;

    private ActivityResultLauncher<Intent> exportCsvLauncher;
    private ActivityResultLauncher<Intent> importCsvLauncher;

    private boolean isSyncingScroll = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActivityResultLaunchers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tabela, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        headerNamesContainer = view.findViewById(R.id.header_names_container);
        headerNamesScrollView = view.findViewById(R.id.header_names_scroll_view);
        recyclerViewTabela = view.findViewById(R.id.recycler_view_tabela);
        recyclerViewTabela.setLayoutManager(new LinearLayoutManager(getContext()));

        fabExport = view.findViewById(R.id.fab_export_csv);
        fabImport = view.findViewById(R.id.fab_import_csv);

        tabelaAdapter = new TabelaAdapter(new ArrayList<>(), new ArrayList<>(),
                (pessoa, moveName) -> {
                    if (circoViewModel != null) {
                        circoViewModel.cycleMoveStatus(pessoa, moveName);
                    }
                },
                this
        );
        recyclerViewTabela.setAdapter(tabelaAdapter);

        circoViewModel = new ViewModelProvider(requireActivity()).get(CircoViewModel.class);

        circoViewModel.getMasterList().observe(getViewLifecycleOwner(), pessoas -> {
            if (pessoas != null) {
                updateHeaders(pessoas);
                tabelaAdapter.updatePersonList(pessoas);
            }
        });

        circoViewModel.getMoveList().observe(getViewLifecycleOwner(), moves -> {
            if (moves != null) {
                tabelaAdapter.updateMoveList(moves);
            }
        });

        setupScrollSync();

        fabExport.setOnClickListener(v -> createCsvFile());
        fabImport.setOnClickListener(v -> openCsvFile());
    }

    private void setupScrollSync() {
        if (headerNamesScrollView == null) return;
        headerNamesScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (isSyncingScroll) return;
            isSyncingScroll = true;
            if (tabelaAdapter != null) {
                tabelaAdapter.syncAllRowsToScroll(scrollX, recyclerViewTabela, null);
            }
            isSyncingScroll = false;
        });
    }

    @Override
    public void onRowScrolled(int scrollX, RecyclerView.ViewHolder originatedFromViewHolder) {
        if (isSyncingScroll) return;
        isSyncingScroll = true;
        if (headerNamesScrollView != null) {
            headerNamesScrollView.scrollTo(scrollX, 0);
        }
        if (tabelaAdapter != null && recyclerViewTabela != null && originatedFromViewHolder instanceof TabelaAdapter.TabelaViewHolder) {
            tabelaAdapter.syncAllRowsToScroll(scrollX, recyclerViewTabela, (TabelaAdapter.TabelaViewHolder) originatedFromViewHolder);
        }
        isSyncingScroll = false;
    }

    private void setupActivityResultLaunchers() {
        exportCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            writeCsvToUri(uri);
                        } else {
                            if(getContext() != null) Toast.makeText(getContext(), "Falha ao obter caminho do arquivo.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        importCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            readCsvFromUri(uri);
                        } else {
                            if(getContext() != null) Toast.makeText(getContext(), "Nenhum arquivo selecionado.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createCsvFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "ufabc_circo_tabela.csv");
        try {
            exportCsvLauncher.launch(intent);
        } catch (Exception e) {
            if(getContext() != null) Toast.makeText(getContext(), "Não foi possível iniciar a exportação.", Toast.LENGTH_LONG).show();
        }
    }

    private void openCsvFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        try {
            importCsvLauncher.launch(intent);
        } catch (Exception e) {
            if(getContext() != null) Toast.makeText(getContext(), "Não foi possível iniciar a importação.", Toast.LENGTH_LONG).show();
        }
    }

    private void writeCsvToUri(Uri uri) {
        Context context = getContext();
        if (context == null) return;

        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Exportando... Por favor, aguarde.");
        progressDialog.setCancelable(false);
        progressDialog.show();

        executor.execute(() -> {
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                 OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

                List<Pessoa> personList = circoViewModel.getMasterList().getValue();
                List<Movimento> moveList = circoViewModel.getMoveList().getValue();

                if (personList == null || moveList == null || moveList.isEmpty()) {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Não há dados para exportar.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                StringBuilder moveNamesRow = new StringBuilder();
                moveNamesRow.append("\"Movimento\"");
                for (Movimento move : moveList) {
                    moveNamesRow.append(",\"").append(move.getNome().replace("\"", "\"\"")).append("\"");
                }
                writer.write(moveNamesRow.toString() + "\n");

                StringBuilder typesRow = new StringBuilder();
                typesRow.append("\"Tipo!\"");
                for (Movimento move : moveList) {
                    typesRow.append(",").append(move.getTipo());
                }
                writer.write(typesRow.toString() + "\n");

                StringBuilder difficultiesRow = new StringBuilder();
                difficultiesRow.append("\"Dificuldade!\"");
                for (Movimento move : moveList) {
                    difficultiesRow.append(",").append(move.getDificuldade());
                }
                writer.write(difficultiesRow.toString() + "\n");

                for (Pessoa person : personList) {
                    StringBuilder personRow = new StringBuilder();
                    personRow.append("\"").append(person.getNome().replace("\"", "\"\"")).append("\"");
                    for (Movimento move : moveList) {
                        int status = person.getMoveStatus().getOrDefault(move.getNome(), 0);
                        personRow.append(",").append(status);
                    }
                    writer.write(personRow.toString() + "\n");
                }

                writer.flush();
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Tabela exportada com sucesso!", Toast.LENGTH_SHORT).show();
                });

            } catch (Throwable t) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Erro fatal ao exportar: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
                t.printStackTrace();
            }
        });
    }

    private void readCsvFromUri(Uri uri) {
        Context context = getContext();
        if (context == null) return;

        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Importando... Por favor, aguarde.");
        progressDialog.setCancelable(false);
        progressDialog.show();

        executor.execute(() -> {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                List<Pessoa> importedPeople = new ArrayList<>();
                List<Movimento> tempMoves = new ArrayList<>();
                List<String> moveNames = new ArrayList<>();

                String[] moveNamesArr = reader.readLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 1; i < moveNamesArr.length; i++) {
                    moveNames.add(moveNamesArr[i].replace("\"", "").trim());
                }

                String[] typesArr = reader.readLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                String[] difficultiesArr = reader.readLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                for (int i = 0; i < moveNames.size(); i++) {
                    String name = moveNames.get(i);
                    int type = Integer.parseInt(typesArr[i + 1].trim());
                    int difficulty = Integer.parseInt(difficultiesArr[i + 1].trim());
                    tempMoves.add(new Movimento(name, type, difficulty));
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    String personName = values[0].replace("\"", "").trim();
                    if (personName.isEmpty()) continue;

                    Pessoa person = new Pessoa(personName);
                    for (int i = 0; i < moveNames.size(); i++) {
                        if(values.length > i + 1) {
                            int status = Integer.parseInt(values[i + 1].trim());
                            person.getMoveStatus().put(moveNames.get(i), status);
                        }
                    }
                    importedPeople.add(person);
                }

                List<Movimento> sortedMoves = tempMoves.stream()
                        .sorted(Comparator.comparingInt(Movimento::getTipo).reversed()
                                .thenComparing(Comparator.comparingInt(Movimento::getDificuldade).reversed()))
                        .collect(Collectors.toList());

                handler.post(() -> {
                    progressDialog.dismiss();
                    circoViewModel.setMoveList(sortedMoves);
                    circoViewModel.importMasterList(importedPeople);
                    Toast.makeText(context, "Tabela importada com sucesso!", Toast.LENGTH_SHORT).show();
                });

            } catch (Throwable t) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Erro fatal ao importar: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
                t.printStackTrace();
            }
        });
    }

    private void updateHeaders(List<Pessoa> personList) {
        headerNamesContainer.removeAllViews();
        Context context = getContext();
        if (context == null || personList == null) return;

        for (Pessoa person : personList) {
            TextView headerCell = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(120, context), LinearLayout.LayoutParams.MATCH_PARENT);
            headerCell.setLayoutParams(params);
            headerCell.setText(person.getNome());
            headerCell.setGravity(Gravity.CENTER);
            headerCell.setPadding(dpToPx(4, context), dpToPx(12, context), dpToPx(4, context), dpToPx(12, context));
            headerCell.setBackgroundResource(R.drawable.cell_border);
            headerNamesContainer.addView(headerCell);

            if (circoViewModel.isInstructor(person.getNome())) {
                headerCell.setTextColor(Color.parseColor("#800080"));
                headerCell.setShadowLayer(2.5f, 0, 0, Color.BLACK);
            }

            headerCell.setOnClickListener(v -> showProfileDialog(person));
        }
    }

    private void showProfileDialog(Pessoa pessoa) {
        if (getContext() == null || pessoa == null) return;
        if (getParentFragmentManager() != null) {
            ProfileMenuFragment.newInstance(pessoa).show(getParentFragmentManager(), "ProfileMenu");
        }
    }

    private int dpToPx(int dp, Context contextParam) {
        Context context = contextParam;
        if (context == null) {
            context = getContext();
        }
        if (context == null) return dp;
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static TabelaFragment newInstance() {
        return new TabelaFragment();
    }
}