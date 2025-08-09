package com.example.ufabcirco.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.ufabcirco.BuildConfig;
import com.example.ufabcirco.R;
import com.example.ufabcirco.model.Movimento;
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.ui.custom.OutlineTextView;
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
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class TabelaFragment extends Fragment {

    private static final String SPREADSHEET_ID = "17g23jX5Su4rlUKW5Htq9pZRboW_5GxJieoYDlcTJe_w";
    private static final String API_KEY = "AIzaSyC2Af7CSAT3Aees4gg1PMB3NmTPhdwVxUA";
    private static final String RANGE = "Moves!A1:BZ100";
    private static final String TAG = "TabelaFragment";

    private CircoViewModel circoViewModel;
    private LinearLayout mainTableContainer;
    private LinearLayout fixedMoveListContainer;
    private LinearLayout headerNamesContainer;
    private HorizontalScrollView mainHorizontalScrollView;
    private ScrollView fixedColumnScrollView;
    private ScrollView mainTableScrollView;
    private FloatingActionButton fabExport, fabImport;
    private HorizontalScrollView headerNamesScrollView;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context context;

    private boolean isSyncing = false;
    private long lastLocalModificationTime = 0;
    private long lastRemoteSyncTime = 0;
    private final int SYNC_INTERVAL_MS = 3000;
    private final long SYNC_DEBOUNCE_MS = 1000;

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncData();
            handler.postDelayed(this, SYNC_INTERVAL_MS);
        }
    };

    private final Runnable debounceSyncRunnable = () -> {
        if (!isSyncing && isOnline() && lastLocalModificationTime > lastRemoteSyncTime) {
            Log.d(TAG, "Debounce concluído. Sincronizando dados locais para remoto.");
            syncData();
        }
    };

    private List<Pessoa> currentPessoaList = new ArrayList<>();
    private List<Movimento> currentMoveList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tabela, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.context = requireContext();

        fixedMoveListContainer = view.findViewById(R.id.fixed_move_list_container);
        headerNamesContainer = view.findViewById(R.id.header_names_container);
        mainTableContainer = view.findViewById(R.id.main_table_container);
        mainHorizontalScrollView = view.findViewById(R.id.main_horizontal_scrollview);
        fixedColumnScrollView = view.findViewById(R.id.fixed_column_scroll_view);
        mainTableScrollView = view.findViewById(R.id.main_table_scroll_view);
        headerNamesScrollView = view.findViewById(R.id.header_names_scroll_view);

        fabExport = view.findViewById(R.id.fab_export_csv);
        fabImport = view.findViewById(R.id.fab_import_csv);
        fabExport.setVisibility(View.GONE);
        fabImport.setVisibility(View.GONE);

        circoViewModel = new ViewModelProvider(requireActivity()).get(CircoViewModel.class);

        circoViewModel.getLocalModificationEvent().observe(getViewLifecycleOwner(), isModified -> {
            if (Boolean.TRUE.equals(isModified)) {
                lastLocalModificationTime = System.currentTimeMillis();
                Log.d(TAG, "Notificação de modificação local recebida. Agendando sincronização com debounce.");
                handler.removeCallbacks(debounceSyncRunnable);
                handler.postDelayed(debounceSyncRunnable, SYNC_DEBOUNCE_MS);
            }
        });

        circoViewModel.getMasterList().observe(getViewLifecycleOwner(), pessoas -> {
            currentPessoaList = pessoas;
            updateHeaders(pessoas);
            updateTable();
        });

        circoViewModel.getMoveList().observe(getViewLifecycleOwner(), moves -> {
            currentMoveList = moves;
            updateTable();
        });

        fixedColumnScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            mainTableScrollView.scrollTo(scrollX, scrollY);
        });
        mainTableScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            fixedColumnScrollView.scrollTo(scrollX, scrollY);
        });

        if (headerNamesScrollView != null) {
            mainHorizontalScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                headerNamesScrollView.scrollTo(scrollX, 0);
            });
        }

        handler.post(syncRunnable);
    }

    private void updateTable() {
        if (currentPessoaList == null || currentMoveList == null) return;

        int savedScrollX = mainHorizontalScrollView.getScrollX();
        int savedScrollY = mainTableScrollView.getScrollY();

        mainTableContainer.removeAllViews();
        fixedMoveListContainer.removeAllViews();

        for (Movimento move : currentMoveList) {
            OutlineTextView moveLetter = new OutlineTextView(context);
            LinearLayout.LayoutParams moveParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40, context));
            moveParams.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context)); // Adicionado margem
            moveLetter.setLayoutParams(moveParams);
            moveLetter.setGravity(Gravity.CENTER);
            moveLetter.setPadding(dpToPx(4, context), dpToPx(12, context), dpToPx(4, context), dpToPx(12, context));
            moveLetter.setBackgroundResource(R.drawable.cell_border);
            moveLetter.setText(move.getNome());
            moveLetter.setBackgroundColor(Color.parseColor("#F0F0F0")); // Cor de fundo da célula

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
            fixedMoveListContainer.addView(moveLetter);

            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);

            for (Pessoa pessoa : currentPessoaList) {
                TextView cell = new TextView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(150, context), dpToPx(45, context));
                params.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context)); // Adicionado margem
                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setTextColor(Color.BLACK);
                cell.setBackgroundResource(R.drawable.cell_border);

                int status = pessoa.getMoveStatus().getOrDefault(move.getNome(), 0);
                updateCellAppearance(cell, status);

                cell.setOnClickListener(v -> {
                    int currentStatus = pessoa.getMoveStatus().getOrDefault(move.getNome(), 0);

                    if (currentStatus >= 4){
                        currentStatus = 0;
                    }

                    pessoa.getMoveStatus().put(move.getNome(), currentStatus);
                    updateCellAppearance(cell, currentStatus);

                    if (circoViewModel != null) {
                        circoViewModel.cycleMoveStatus(pessoa.getId(), move.getNome());
                    }
                });

                rowLayout.addView(cell);
            }
            mainTableContainer.addView(rowLayout);
        }

        mainHorizontalScrollView.post(() -> mainHorizontalScrollView.scrollTo(savedScrollX, 0));
        mainTableScrollView.post(() -> mainTableScrollView.scrollTo(0, savedScrollY));
    }

    private void updateCellAppearance(TextView cell, int status) {
        GradientDrawable cellBackground = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.cell_border).mutate();

        switch (status) {
            case 1: cellBackground.setColor(Color.YELLOW); cell.setText("Já fez"); break;
            case 2: cellBackground.setColor(Color.parseColor("#45aaf7")); cell.setText("Aprendeu"); break;
            case 3: cellBackground.setColor(Color.parseColor("#fa5f5f")); cell.setText("Não consegue"); break;
            default: cellBackground.setColor(Color.WHITE); cell.setText(""); break;
        }
        cell.setBackground(cellBackground);
    }

    private void syncData() {
        if (isSyncing || !isOnline()) {
            return;
        }
        isSyncing = true;
        executor.execute(() -> {
            try {
                if (lastLocalModificationTime > lastRemoteSyncTime) {
                    Log.d(TAG, "Mudança local detectada, enviando para o remoto...");
                    List<List<Object>> dataToWrite = prepareDataForExport();
                    writeToGoogleSheet(SPREADSHEET_ID, RANGE, dataToWrite);
                } else {
                    Log.d(TAG, "Sincronizando do remoto...");
                    readGoogleSheet();
                }
            } finally {
                isSyncing = false;
            }
        });
    }

    private boolean isOnline() {
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void readGoogleSheet() {
        if (context == null) return;
        executor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                String urlString = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + "/values/" + RANGE + "?key=" + API_KEY;
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

                    List<Pessoa> currentPeople = circoViewModel.getMasterList().getValue();
                    List<Movimento> currentMoves = circoViewModel.getMoveList().getValue();

                    if (!isDataEqual(currentPeople, importedPeople) || !isMovesEqual(currentMoves, sortedMoves)) {
                        handler.post(() -> {
                            if (isAdded()) {
                                circoViewModel.setMoveList(sortedMoves);
                                circoViewModel.importMasterList(importedPeople);
                                lastRemoteSyncTime = System.currentTimeMillis();
                            }
                        });
                    } else {
                        lastRemoteSyncTime = System.currentTimeMillis();
                        Log.d(TAG, "Dados remotos e locais são idênticos. Nenhuma atualização.");
                    }
                } else {
                    Log.e(TAG, "Erro na requisição: " + responseCode);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Erro ao ler a planilha", t);
                handler.post(() -> {
                    if (isAdded()) {
                        Toast.makeText(context, "Erro ao sincronizar: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    private boolean isDataEqual(List<Pessoa> list1, List<Pessoa> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null || list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isMovesEqual(List<Movimento> list1, List<Movimento> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null || list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        return true;
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
            Log.e(TAG, "A chave da conta de serviço está vazia.");
            throw new Exception("A chave da conta de serviço está vazia.");
        }
        GoogleCredential credential = GoogleCredential.fromStream(new ByteArrayInputStream(jsonKey.getBytes()))
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName("UFABCirco Android App")
                .build();
    }

    private void writeToGoogleSheet(String spreadsheetId, String range, List<List<Object>> dataToWrite) {
        if (context == null) return;
        executor.execute(() -> {
            try {
                Sheets sheetsService = getSheetsService();
                ValueRange body = new ValueRange().setValues(dataToWrite);
                sheetsService.spreadsheets().values().update(spreadsheetId, range, body)
                        .setValueInputOption("RAW")
                        .execute();
                handler.post(() -> {
                    if (isAdded()) {
                        lastRemoteSyncTime = System.currentTimeMillis();
                        lastLocalModificationTime = lastRemoteSyncTime;
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Erro ao escrever na planilha", e);
                handler.post(() -> {
                    if (isAdded()) {
                        Toast.makeText(context, "Erro ao sincronizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void updateHeaders(List<Pessoa> personList) {
        if (headerNamesContainer == null || context == null || personList == null) return;

        headerNamesContainer.removeAllViews();
        if (context == null || personList == null) return;

        for (Pessoa person : personList) {
            TextView headerCell = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(150, context), dpToPx(60, context));
            params.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
            headerCell.setLayoutParams(params);
            headerCell.setGravity(Gravity.CENTER);
            headerCell.setPadding(dpToPx(4, context), dpToPx(12, context), dpToPx(4, context), dpToPx(12, context));
            headerCell.setBackgroundResource(R.drawable.cell_border);
            headerCell.setText(person.getNome());

            if (circoViewModel.isInstructor(person.getNome())) {
                headerCell.setTextColor(Color.parseColor("#800080"));
                headerCell.setShadowLayer(2.5f, 0, 0, Color.BLACK);
            } else {
                headerCell.setTextColor(Color.BLACK);
            }

            headerCell.setOnClickListener(v -> showProfileDialog(person));
            headerNamesContainer.addView(headerCell);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(syncRunnable);
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