// app/src/main/java/com/example/ufabcirco/ui/TabelaFragment.java
package com.example.ufabcirco.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.viewmodel.CircoViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TabelaFragment extends Fragment implements TabelaAdapter.RowScrollNotifier {

    private static final String TAG = "TabelaFragment";
    private CircoViewModel circoViewModel;
    private RecyclerView recyclerViewTabela;
    private TabelaAdapter tabelaAdapter;
    private LinearLayout headerNamesContainer;
    private HorizontalScrollView headerNamesScrollView;
    private List<String> moveList;
    private FloatingActionButton fabExport, fabImport;

    private ActivityResultLauncher<Intent> exportCsvLauncher;
    private ActivityResultLauncher<Intent> importCsvLauncher;

    private boolean isHeaderUserScrolling = false;
    private boolean isRowUserScrolling = false;

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

        moveList = new ArrayList<>();

        moveList.add("Queda Maluca");
        moveList.add("Uma e Meia");
        moveList.add("Duas e Meia");
        moveList.add("Mata Rim");
        moveList.add("Queda 360");
        moveList.add("Queda Tic-Tac");
        moveList.add("Axila");
        moveList.add("Bala");
        moveList.add("Suicidio");
        moveList.add("Pião");
        moveList.add("Calcinha");
        moveList.add("Queda Portô");
        moveList.add("Queda Infinita");
        moveList.add("Queda Chave de Cintura");
        moveList.add("Queda Secretária");
        moveList.add("Queda Casulo");
        moveList.add("Cambalhota");

        moveList.add("Abertura do Escorpião");
        moveList.add("Escorpião Rei");
        moveList.add("Escorpião");

        moveList.add("Super Homem");
        moveList.add("Abertura do Arqueiro");
        moveList.add("Homem Aranha");
        moveList.add("Forca");
        moveList.add("Portô");
        moveList.add("X nas Costas");
        moveList.add("Arqueiro");

        moveList.add("Avião");
        moveList.add("Borboleta");
        moveList.add("Grega");
        moveList.add("Cupido");
        moveList.add("Chave de Cintura");

        moveList.add("Querubim");
        moveList.add("Pegasus");
        moveList.add("Árvore de Natal");
        moveList.add("Dunut");
        moveList.add("Estrela do Mar");
        moveList.add("Joelho Pendurado");
        moveList.add("Flamingo");
        moveList.add("Secretária");
        moveList.add("Arabesco");
        moveList.add("Arqueado");
        moveList.add("Aviãozinho"); //meia lua
        moveList.add("Caixão");
        moveList.add("Morcego");
        moveList.add("Ninja");
        moveList.add("Homem na Lua");
        moveList.add("Tesourinha");
        moveList.add("Triângulo");
        moveList.add("Losango");
        moveList.add("Varal");
        moveList.add("Casulo");
        moveList.add("Espacate");
        moveList.add("Chave de Pé");

        moveList.add("Bandeira");
        moveList.add("Cristo");
        moveList.add("Meio Cristo");
        moveList.add("Lua Crescente");
        moveList.add("Lua Minguante");
        moveList.add("Sereia");
        moveList.add("Vela");

        moveList.add("Subida Francesa");
        moveList.add("Subida Crochê");
        moveList.add("Subida Secretária");
        moveList.add("Subida Aranha"); //macaco
        moveList.add("Subida Escadinha");
        moveList.add("Subida Bombeiro");
        moveList.add("Subida de Braço");
        moveList.add("Subida Russa");
        moveList.add("Subida Bailarina");

        headerNamesContainer = view.findViewById(R.id.header_names_container);
        headerNamesScrollView = view.findViewById(R.id.header_names_scroll_view);
        recyclerViewTabela = view.findViewById(R.id.recycler_view_tabela);
        recyclerViewTabela.setLayoutManager(new LinearLayoutManager(getContext()));

        fabExport = view.findViewById(R.id.fab_export_csv);
        fabImport = view.findViewById(R.id.fab_import_csv);

        tabelaAdapter = new TabelaAdapter(new ArrayList<>(), moveList,
                (pessoa, move) -> {
                    if (circoViewModel != null) {
                        circoViewModel.cycleMoveStatus(pessoa, move);
                    }
                },
                this
        );
        recyclerViewTabela.setAdapter(tabelaAdapter);

        circoViewModel = new ViewModelProvider(requireActivity()).get(CircoViewModel.class);

        circoViewModel.getMasterList().observe(getViewLifecycleOwner(), pessoas -> {
            Log.d(TAG, "Observer da MasterList disparado com " + (pessoas != null ? pessoas.size() : "null") + " pessoas.");
            if (pessoas != null) {
                updateHeaders(pessoas);
                tabelaAdapter.updatePersonList(pessoas);

                if (headerNamesScrollView != null && recyclerViewTabela != null) {
                    recyclerViewTabela.post(() -> {
                        if(tabelaAdapter != null) {
                            tabelaAdapter.syncAllRowsToScroll(headerNamesScrollView.getScrollX(), recyclerViewTabela, null);
                        }
                    });
                }
            }
        });

        setupScrollSync();

        fabExport.setOnClickListener(v -> createCsvFile());
        fabImport.setOnClickListener(v -> {
            Log.d(TAG, "Botão Importar CSV clicado!");
            openCsvFile();
        });
    }

    private void setupScrollSync() {
        if (headerNamesScrollView == null) {
            Log.e(TAG, "headerNamesScrollView é nulo em setupScrollSync");
            return;
        }
        headerNamesScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (isRowUserScrolling) {
                return;
            }

            isHeaderUserScrolling = true;
            if (tabelaAdapter != null) {
                tabelaAdapter.syncAllRowsToScroll(scrollX, recyclerViewTabela, null);
            }
            isHeaderUserScrolling = false;
        });
    }

    @Override
    public void onRowScrolled(int scrollX, RecyclerView.ViewHolder originatedFromViewHolder) {
        if (isHeaderUserScrolling) {
            return;
        }
        isRowUserScrolling = true;
        if (headerNamesScrollView != null) {
            headerNamesScrollView.scrollTo(scrollX, 0);
        }
        if (tabelaAdapter != null && recyclerViewTabela != null && originatedFromViewHolder instanceof TabelaAdapter.TabelaViewHolder) {
            tabelaAdapter.syncAllRowsToScroll(scrollX, recyclerViewTabela, (TabelaAdapter.TabelaViewHolder) originatedFromViewHolder);
        }
        isRowUserScrolling = false;
    }


    private void setupActivityResultLaunchers() {
        exportCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Resultado do seletor de arquivos de exportação recebido. ResultCode: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Log.d(TAG, "Exportando para URI: " + uri.toString());
                            writeCsvToUri(uri);
                        } else {
                            Log.e(TAG, "URI de exportação nula.");
                            if(getContext() != null) Toast.makeText(getContext(), "Falha ao obter caminho do arquivo.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Exportação cancelada ou falhou.");
                    }
                });

        importCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Resultado do seletor de arquivos de importação recebido. ResultCode: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Log.d(TAG, "Importando da URI: " + uri.toString());
                            readCsvFromUri(uri);
                        } else {
                            Log.e(TAG, "URI de importação nula.");
                            if(getContext() != null) Toast.makeText(getContext(), "Nenhum arquivo selecionado.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Importação cancelada ou falhou.");
                        if(getContext() != null && result.getResultCode() != Activity.RESULT_CANCELED) {
                            Toast.makeText(getContext(), "Falha ao selecionar arquivo.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createCsvFile() {
        Log.d(TAG, "Método createCsvFile() chamado.");
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "tabela_tecido.csv");
        try {
            Log.d(TAG, "Lançando intent de criação de CSV...");
            exportCsvLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao lançar intent de criação de CSV", e);
            if(getContext() != null) Toast.makeText(getContext(), "Não foi possível iniciar a exportação. Verifique se há um app gerenciador de arquivos.", Toast.LENGTH_LONG).show();
        }
    }

    private void openCsvFile() {
        Log.d(TAG, "Método openCsvFile() chamado.");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/csv", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        try {
            Log.d(TAG, "Lançando intent de importação...");
            importCsvLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao lançar intent de abertura de CSV", e);
            if(getContext() != null) Toast.makeText(getContext(), "Não foi possível iniciar a importação. Verifique se há um app gerenciador de arquivos.", Toast.LENGTH_LONG).show();
        }
    }

    private void writeCsvToUri(Uri uri) {
        Log.d(TAG, "Iniciando writeCsvToUri...");
        if (getContext() == null) {
            Log.e(TAG, "Contexto nulo em writeCsvToUri");
            return;
        }
        try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

            List<Pessoa> personList = circoViewModel.getMasterList().getValue();
            if (personList == null || personList.isEmpty()) {
                Toast.makeText(getContext(), "Não há dados para exportar.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Nenhum dado para exportar.");
                return;
            }

            StringBuilder headerRow = new StringBuilder();
            headerRow.append("\"Nome\"");
            for (String move : moveList) {
                headerRow.append(",\"").append(move.replace("\"", "\"\"")).append("\"");
            }
            writer.write(headerRow.toString() + "\n");
            Log.d(TAG, "Cabeçalho CSV: " + headerRow.toString());

            for (Pessoa person : personList) {
                StringBuilder dataRow = new StringBuilder();
                dataRow.append("\"").append(person.getNome().replace("\"", "\"\"")).append("\"");
                Map<String, Integer> statusMap = person.getMoveStatus();
                for (String move : moveList) {
                    dataRow.append(",").append(statusMap.getOrDefault(move, 0));
                }
                writer.write(dataRow.toString() + "\n");
            }
            writer.flush();
            Toast.makeText(getContext(), "Tabela exportada com sucesso!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Tabela exportada para: " + uri.toString());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao escrever CSV na URI: " + uri.toString(), e);
            Toast.makeText(getContext(), "Erro ao exportar tabela: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void readCsvFromUri(Uri uri) {
        Log.d(TAG, "Iniciando readCsvFromUri...");
        if (getContext() == null) {
            Log.e(TAG, "Contexto nulo em readCsvFromUri");
            return;
        }
        List<Pessoa> importedPeople = new ArrayList<>();
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            String[] fileHeaders = null;
            int lineCount = 0;
            Log.d(TAG, "Iniciando leitura do arquivo CSV.");

            while ((line = reader.readLine()) != null) {
                lineCount++;
                Log.d(TAG, "Lendo linha " + lineCount + ": " + line);
                if (line.trim().isEmpty()) {
                    Log.d(TAG, "Linha " + lineCount + " está vazia, pulando.");
                    continue;
                }

                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                Log.d(TAG, "Linha " + lineCount + " dividida em " + values.length + " valores.");

                if (lineCount == 1) {
                    fileHeaders = new String[values.length];
                    for(int i=0; i<values.length; i++) {
                        fileHeaders[i] = values[i].replace("\"", "").trim();
                        Log.d(TAG, "Cabeçalho " + i + ": '" + fileHeaders[i] + "'");
                    }
                    if (fileHeaders.length == 0 || !fileHeaders[0].equalsIgnoreCase("Nome")) {
                        Log.e(TAG, "Formato de CSV inválido: primeira coluna do cabeçalho não é 'Nome'. Encontrado: " + (fileHeaders.length > 0 ? fileHeaders[0] : "NENHUM"));
                        if(getContext() != null) Toast.makeText(getContext(), "CSV inválido: primeira coluna deve ser 'Nome'.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Log.d(TAG, "Cabeçalho processado com sucesso.");
                    continue;
                }

                if (fileHeaders == null) {
                    Log.e(TAG, "Cabeçalho do CSV não foi processado antes dos dados.");
                    if(getContext() != null) Toast.makeText(getContext(), "Cabeçalho do CSV não encontrado ou inválido.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (values.length == 0 || values[0].trim().isEmpty()) {
                    Log.d(TAG, "Linha " + lineCount + " sem nome, pulando.");
                    continue;
                }

                String personName = values[0].replace("\"", "").trim();
                Log.d(TAG, "Processando pessoa: " + personName);
                Pessoa person = new Pessoa(personName);
                Map<String, Integer> moveStatusMap = new java.util.HashMap<>();
                for (int i = 1; i < values.length && i < fileHeaders.length; i++) {
                    String moveHeader = fileHeaders[i];
                    if (moveHeader.isEmpty()) {
                        Log.d(TAG, "Cabeçalho de movimento vazio na coluna " + i + ", pulando.");
                        continue;
                    }
                    try {
                        String statusValueStr = values[i].replace("\"", "").trim();
                        Log.d(TAG, "Para " + personName + ", Movimento '" + moveHeader + "', Status lido: '" + statusValueStr + "'");
                        int status = 0;
                        if(!statusValueStr.isEmpty()){
                            status = Integer.parseInt(statusValueStr);
                        }
                        moveStatusMap.put(moveHeader, status);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Valor de status não numérico para " + personName + " no movimento " + moveHeader + ": '" + values[i] + "'. Usando 0.");
                        moveStatusMap.put(moveHeader, 0);
                    }
                }
                person.setMoveStatus(moveStatusMap);
                importedPeople.add(person);
                Log.d(TAG, "Pessoa '" + personName + "' adicionada à lista de importação com " + moveStatusMap.size() + " status de movimentos.");
            }

            Log.d(TAG, "Leitura do CSV finalizada. Total de linhas lidas: " + lineCount + ". Pessoas importadas: " + importedPeople.size());
            if (importedPeople.isEmpty() && lineCount <= 1) {
                Log.d(TAG, "Nenhuma pessoa válida encontrada no CSV.");
                if(getContext() != null) Toast.makeText(getContext(), "Arquivo CSV vazio ou sem dados de pessoas válidos.", Toast.LENGTH_LONG).show();
                return;
            }

            circoViewModel.importMasterList(importedPeople);
            if(getContext() != null) Toast.makeText(getContext(), "Tabela importada com sucesso!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Erro crítico ao ler CSV da URI: " + uri.toString(), e);
            if(getContext() != null) Toast.makeText(getContext(), "Erro ao importar arquivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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