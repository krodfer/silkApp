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

import com.example.ufabcirco.BuildConfig;
import com.example.ufabcirco.R;
import com.example.ufabcirco.adapter.TabelaAdapter;
import com.example.ufabcirco.model.Movimento;
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.viewmodel.CircoViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TabelaFragment extends Fragment implements TabelaAdapter.RowScrollNotifier {
    private static final String SPREADSHEET_ID = "17g23jX5Su4rlUKW5Htq9pZRboW_5GxJieoYDlcTJe_w";
    private static final String API_KEY = "AIzaSyC2Af7CSAT3Aees4gg1PMB3NmTPhdwVxUA"; // Substitua por sua chave de API real
    private static final String RANGE = "Moves!A1:Z";
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
    private Context context;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
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

        fabImport.setOnClickListener(v -> readGoogleSheet());
        fabExport.setOnClickListener(v -> {
            List<List<Object>> dataToWrite = prepareDataForExport();
            writeToGoogleSheet(SPREADSHEET_ID, RANGE, dataToWrite);
        });

        readGoogleSheet();
    }

    private void readGoogleSheet() {
        if (context == null) return;
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Sincronizando com a planilha... Por favor, aguarde.");
        progressDialog.setCancelable(false);
        progressDialog.show();
        executor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                String urlString = "https://sheets.googleapis.com/v4/spreadsheets/"
                        + SPREADSHEET_ID + "/values/" + RANGE + "?key=" + API_KEY;
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = urlConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder responseJson = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseJson.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(responseJson.toString());
                    JSONArray values = jsonObject.getJSONArray("values");

                    List<Pessoa> importedPeople = new ArrayList<>();
                    List<Movimento> tempMoves = new ArrayList<>();
                    List<String> moveNames = new ArrayList<>();

                    JSONArray moveNamesArr = values.getJSONArray(0);
                    for (int i = 1; i < moveNamesArr.length(); i++) {
                        moveNames.add(moveNamesArr.getString(i));
                    }

                    JSONArray typesArr = values.getJSONArray(1);
                    JSONArray difficultiesArr = values.getJSONArray(2);
                    for (int i = 0; i < moveNames.size(); i++) {
                        String name = moveNames.get(i);
                        int type = typesArr.getInt(i + 1);
                        int difficulty = difficultiesArr.getInt(i + 1);
                        tempMoves.add(new Movimento(name, type, difficulty));
                    }

                    for (int j = 3; j < values.length(); j++) {
                        JSONArray personValues = values.getJSONArray(j);
                        String personName = personValues.getString(0);
                        if (personName.isEmpty()) continue;

                        Pessoa person = new Pessoa(personName);
                        for (int i = 0; i < moveNames.size(); i++) {
                            if (personValues.length() > i + 1) {
                                int status = personValues.getInt(i + 1);
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
                        Toast.makeText(context, "Planilha sincronizada com sucesso!", Toast.LENGTH_SHORT).show();
                    });

                } else {
                    throw new IOException("Erro na requisição: " + responseCode);
                }
            } catch (Throwable t) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Erro ao sincronizar: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
                Log.e(TAG, "Erro ao ler a planilha", t);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    private List<List<Object>> prepareDataForExport() {
        List<List<Object>> dataToWrite = new ArrayList<>();
        List<Pessoa> personList = circoViewModel.getMasterList().getValue();
        List<Movimento> moveList = circoViewModel.getMoveList().getValue();
        if (personList == null || moveList == null || moveList.isEmpty()) {
            return dataToWrite;
        }

        List<Object> moveNamesRow = new ArrayList<>();
        moveNamesRow.add("Movimento");
        for (Movimento move : moveList) {
            moveNamesRow.add(move.getNome());
        }
        dataToWrite.add(moveNamesRow);

        List<Object> typesRow = new ArrayList<>();
        typesRow.add("Tipo!");
        for (Movimento move : moveList) {
            typesRow.add(move.getTipo());
        }
        dataToWrite.add(typesRow);

        List<Object> difficultiesRow = new ArrayList<>();
        difficultiesRow.add("Dificuldade!");
        for (Movimento move : moveList) {
            difficultiesRow.add(move.getDificuldade());
        }
        dataToWrite.add(difficultiesRow);

        for (Pessoa person : personList) {
            List<Object> personRow = new ArrayList<>();
            personRow.add(person.getNome());
            for (Movimento move : moveList) {
                int status = person.getMoveStatus().getOrDefault(move.getNome(), 0);
                personRow.add(status);
            }
            dataToWrite.add(personRow);
        }
        return dataToWrite;
    }

    private Sheets getSheetsService() throws Exception {
        String jsonKey = BuildConfig.GOOGLE_SHEETS_SERVICE_KEY;
        if (jsonKey.isEmpty()) {
            throw new Exception("Chave da conta de serviço não encontrada.");
        }
        GoogleCredential credential = GoogleCredential.fromStream(new ByteArrayInputStream(jsonKey.getBytes()))
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName("UFABCirco Android App")
                .build();
    }

    private void writeToGoogleSheet(String spreadsheetId, String range, List<List<Object>> dataToWrite) {
        if (context == null) return;
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Atualizando planilha... Por favor, aguarde.");
        progressDialog.setCancelable(false);
        progressDialog.show();
        executor.execute(() -> {
            try {
                Sheets sheetsService = getSheetsService();
                ValueRange body = new ValueRange().setValues(dataToWrite);
                sheetsService.spreadsheets().values().update(spreadsheetId, range, body)
                        .setValueInputOption("RAW")
                        .execute();
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Planilha atualizada com sucesso!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Erro ao escrever na planilha: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                Log.e(TAG, "Erro ao escrever na planilha", e);
            }
        });
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

    private void updateHeaders(List<Pessoa> personList) {
        headerNamesContainer.removeAllViews();
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
        if (context == null || pessoa == null) return;
        if (getParentFragmentManager() != null) {
            ProfileMenuFragment.newInstance(pessoa).show(getParentFragmentManager(), "ProfileMenu");
        }
    }

    private int dpToPx(int dp, Context contextParam) {
        if (contextParam == null) {
            return dp;
        }
        return (int) (dp * contextParam.getResources().getDisplayMetrics().density);
    }

    public static TabelaFragment newInstance() {
        return new TabelaFragment();
    }
}