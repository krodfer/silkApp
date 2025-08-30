package com.example.ufabcirco.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.PopupWindow;
import android.widget.ProgressBar;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TabelaFragment extends Fragment {

    private static final String SPREADSHEET_ID = "17g23jX5Su4rlUKW5Htq9pZRboW_5GxJieoYDlcTJe_w";
    private static final String API_KEY = "AIzaSyC2Af7CSAT3Aees4gg1PMB3NmTPhdwVxUA";
    private static final String MOVES_RANGE = "Moves!A1:BZ100";
    private static final String LINKS_RANGE = "Links!A1:BZ10";
    private static final String TAG = "TabelaFragment";

    private CircoViewModel circoViewModel;
    private LinearLayout mainTableContainer;
    private LinearLayout fixedMoveListContainer;
    private LinearLayout headerNamesContainer;
    private HorizontalScrollView mainHorizontalScrollView;
    private ScrollView fixedMoveColumnScrollView;
    private ScrollView mainTableScrollView;
    private FloatingActionButton fabExport, fabImport;
    private HorizontalScrollView headerNamesScrollView;
    private LinearLayout difficultyColumnContainer;
    private LinearLayout dataColumnsContainer;
    private ProgressBar progressBar;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tabela, container, false);
        this.context = requireContext();

        fixedMoveListContainer = view.findViewById(R.id.fixed_move_list_container);
        headerNamesContainer = view.findViewById(R.id.header_names_container);
        mainTableContainer = view.findViewById(R.id.main_table_container);
        mainHorizontalScrollView = view.findViewById(R.id.main_horizontal_scrollview);
        fixedMoveColumnScrollView = view.findViewById(R.id.fixed_move_column_scroll_view);
        mainTableScrollView = view.findViewById(R.id.main_table_scroll_view);
        headerNamesScrollView = view.findViewById(R.id.header_names_scroll_view);
        difficultyColumnContainer = view.findViewById(R.id.difficulty_column_container);
        dataColumnsContainer = view.findViewById(R.id.data_columns_container);
        progressBar = view.findViewById(R.id.progress_bar);

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
            updateTable(currentPessoaList, currentMoveList);
        });

        circoViewModel.getMoveList().observe(getViewLifecycleOwner(), moves -> {
            currentMoveList = moves;
            updateTable(currentPessoaList, currentMoveList);
        });

        fixedMoveColumnScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            mainTableScrollView.scrollTo(scrollX, scrollY);
        });

        mainTableScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            fixedMoveColumnScrollView.scrollTo(scrollX, scrollY);
        });

        if (headerNamesScrollView != null) {
            mainHorizontalScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                headerNamesScrollView.scrollTo(scrollX, 0);
            });
        }

        handler.post(syncRunnable);

        return view;
    }

    private void updateTable(List<Pessoa> pessoaList, List<Movimento> moveList) {
        if (pessoaList == null || moveList == null || pessoaList.isEmpty() || moveList.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.GONE);

        int savedScrollX = mainHorizontalScrollView.getScrollX();
        int savedScrollY = mainTableScrollView.getScrollY();

        fixedMoveListContainer.removeAllViews();
        difficultyColumnContainer.removeAllViews();
        dataColumnsContainer.removeAllViews();


        for (Movimento move : moveList) {
            OutlineTextView moveNameCell = new OutlineTextView(context);
            LinearLayout.LayoutParams moveNameParams = new LinearLayout.LayoutParams(dpToPx(200, context), dpToPx(45, context));
            moveNameParams.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
            moveNameCell.setLayoutParams(moveNameParams);
            moveNameCell.setGravity(Gravity.CENTER);
            moveNameCell.setPadding(dpToPx(4, context), dpToPx(4, context), dpToPx(4, context), dpToPx(4, context));
            moveNameCell.setBackgroundColor(Color.WHITE);
            moveNameCell.setText(move.getNome());
            int textColor = Color.WHITE;
            switch(move.getTipo()){
                case 0: textColor = Color.parseColor("#00FF00"); break;
                case 1: textColor = Color.parseColor("#c9ffc9"); break;
                case 2: textColor = Color.parseColor("#f037a6"); break;
                case 3: textColor = Color.parseColor("#FED8B1"); break;
                case 4: textColor = Color.parseColor("#ff7700"); break;
            }
            moveNameCell.setTextColor(textColor);
            moveNameCell.setOutlineColor(Color.BLACK);
            moveNameCell.setOutlineWidth(4.0f);

            moveNameCell.setOnClickListener(v -> {
                MovimentoDetailFragment.newInstance(move).show(getParentFragmentManager(), "MovimentoDetailFragment");
            });

            fixedMoveListContainer.addView(moveNameCell);

            TextView difficultyValueCell = new TextView(context);
            LinearLayout.LayoutParams difficultyValueParams = new LinearLayout.LayoutParams(dpToPx(80, context), dpToPx(45, context));
            difficultyValueParams.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
            difficultyValueCell.setLayoutParams(difficultyValueParams);
            difficultyValueCell.setGravity(Gravity.CENTER);
            difficultyValueCell.setPadding(dpToPx(4, context), dpToPx(4, context), dpToPx(4, context), dpToPx(4, context));
            difficultyValueCell.setBackgroundColor(Color.WHITE);
            difficultyValueCell.setText(String.format("⭐ %.2f", move.getMediaDificuldade()));
            difficultyValueCell.setTextColor(Color.BLACK);

            difficultyValueCell.setOnClickListener(v -> showStarRatingMenu(v, move.getNome()));

            difficultyColumnContainer.addView(difficultyValueCell);


            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);

            for (Pessoa pessoa : pessoaList) {
                TextView cell = new TextView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(150, context), dpToPx(45, context));
                params.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setTextColor(Color.BLACK);
                cell.setBackgroundResource(R.drawable.cell_border);

                int status = pessoa.getMoveStatus().getOrDefault(move.getNome(), 0);
                updateCellAppearance(cell, status);

                cell.setOnClickListener(v -> {
                    circoViewModel.cycleMoveStatus(pessoa.getId(), move.getNome());
                });

                cell.setOnLongClickListener(v -> {
                    showStatusMenu(v, pessoa.getId(), move.getNome());
                    return true;
                });

                rowLayout.addView(cell);
            }
            dataColumnsContainer.addView(rowLayout);
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

    private void showStatusMenu(View view, String pessoaId, String moveName) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.custom_status_menu, null);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        popupView.findViewById(R.id.btn_white).setOnClickListener(v -> {
            circoViewModel.setMoveStatus(pessoaId, moveName, 0);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.btn_yellow).setOnClickListener(v -> {
            circoViewModel.setMoveStatus(pessoaId, moveName, 1);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.btn_blue).setOnClickListener(v -> {
            circoViewModel.setMoveStatus(pessoaId, moveName, 2);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.btn_red).setOnClickListener(v -> {
            circoViewModel.setMoveStatus(pessoaId, moveName, 3);
            popupWindow.dismiss();
        });

        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        popupWindow.showAsDropDown(view, -35, -view.getHeight() - 5);
    }

    private void showStarRatingMenu(View view, String moveName) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.star_rating_menu, null);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        popupView.findViewById(R.id.btn_star_1).setOnClickListener(v -> {
            circoViewModel.addDificuldade(moveName, 1);
            popupWindow.dismiss();
        });
        popupView.findViewById(R.id.btn_star_2).setOnClickListener(v -> {
            circoViewModel.addDificuldade(moveName, 2);
            popupWindow.dismiss();
        });
        popupView.findViewById(R.id.btn_star_3).setOnClickListener(v -> {
            circoViewModel.addDificuldade(moveName, 3);
            popupWindow.dismiss();
        });
        popupView.findViewById(R.id.btn_star_4).setOnClickListener(v -> {
            circoViewModel.addDificuldade(moveName, 4);
            popupWindow.dismiss();
        });
        popupView.findViewById(R.id.btn_star_5).setOnClickListener(v -> {
            circoViewModel.addDificuldade(moveName, 5);
            popupWindow.dismiss();
        });

        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.showAsDropDown(view, 0, 0);
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
                    writeToGoogleSheet(SPREADSHEET_ID, MOVES_RANGE, dataToWrite);
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
                String movesUrlString = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + "/values/" + MOVES_RANGE + "?key=" + API_KEY;
                String linksUrlString = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + "/values/" + LINKS_RANGE + "?key=" + API_KEY;

                URL movesUrl = new URL(movesUrlString);
                urlConnection = (HttpURLConnection) movesUrl.openConnection();
                JSONObject movesJson = readUrlContent(urlConnection);
                urlConnection.disconnect();

                URL linksUrl = new URL(linksUrlString);
                urlConnection = (HttpURLConnection) linksUrl.openConnection();
                JSONObject linksJson = readUrlContent(urlConnection);
                urlConnection.disconnect();

                JSONArray movesValues = movesJson.getJSONArray("values");
                JSONArray linksValues = linksJson.getJSONArray("values");

                List<Pessoa> importedPeople = new ArrayList<>();
                Map<String, Movimento> tempMovesMap = new HashMap<>();

                Map<String, Map<String, String>> linksData = new HashMap<>();
                if (linksValues.length() > 0) {
                    JSONArray headerRow = linksValues.getJSONArray(0);
                    for (int j = 1; j < headerRow.length(); j++) {
                        String moveName = headerRow.getString(j);
                        Map<String, String> moveData = new HashMap<>();
                        for (int i = 1; i < linksValues.length(); i++) {
                            JSONArray row = linksValues.getJSONArray(i);
                            if (row.length() > 0 && row.length() > j) {
                                String header = row.getString(0);
                                String value = row.getString(j);
                                moveData.put(header, value);
                            }
                        }
                        linksData.put(moveName, moveData);
                    }
                }

                JSONArray moveNamesArr = movesValues.getJSONArray(0);
                JSONArray typesArr = movesValues.getJSONArray(1);
                JSONArray difficultiesArr = movesValues.getJSONArray(2);

                for (int i = 1; i < moveNamesArr.length(); i++) {
                    String name = moveNamesArr.getString(i);
                    int type = typesArr.getInt(i);

                    List<Integer> dificuldadesList = new ArrayList<>();
                    if (difficultiesArr.length() > i) {
                        String dificuldadesString = difficultiesArr.getString(i);
                        if (dificuldadesString != null && !dificuldadesString.trim().isEmpty()) {
                            String cleanedString = dificuldadesString.substring(1, dificuldadesString.length() - 1);
                            if (!cleanedString.isEmpty()) {
                                String[] valores = cleanedString.split(",");
                                for (String valor : valores) {
                                    try {
                                        dificuldadesList.add(Integer.parseInt(valor.trim()));
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Erro ao converter valor de dificuldade: " + valor, e);
                                    }
                                }
                            }
                        }
                    }

                    List<String> fotos = new ArrayList<>();
                    String texto = "";
                    List<String> variantes = new ArrayList<>();

                    if (linksData.containsKey(name)) {
                        Map<String, String> moveData = linksData.get(name);
                        String fotosString = moveData.getOrDefault("Foto!", "[]");
                        if (fotosString.startsWith("[") && fotosString.endsWith("]")) {
                            String cleaned = fotosString.substring(1, fotosString.length() - 1);
                            if (!cleaned.isEmpty()) {
                                fotos = new ArrayList<>(Arrays.asList(cleaned.split(",")));
                            }
                        }
                        String textoString = moveData.getOrDefault("Text!", "");
                        if (textoString != null) {
                            texto = textoString;
                        }
                        String variantesString = moveData.getOrDefault("Variantes!", "[]");
                        if (variantesString.startsWith("[") && variantesString.endsWith("]")) {
                            String cleaned = variantesString.substring(1, variantesString.length() - 1);
                            if (!cleaned.isEmpty()) {
                                variantes = new ArrayList<>(Arrays.asList(cleaned.split(",")));
                            }
                        }
                    }
                    tempMovesMap.put(name, new Movimento(name, type, dificuldadesList, fotos, texto, variantes));
                }

                List<Movimento> sortedMoves = new ArrayList<>(tempMovesMap.values());
                sortedMoves.sort(Comparator.comparingInt(Movimento::getTipo).reversed()
                        .thenComparing(Comparator.comparingDouble(Movimento::getMediaDificuldade).reversed()));

                for (int j = 3; j < movesValues.length(); j++) {
                    JSONArray personValues = movesValues.getJSONArray(j);
                    String personName = personValues.getString(0);
                    if (personName.isEmpty()) continue;

                    Pessoa person = new Pessoa(personName);
                    for (int i = 1; i < moveNamesArr.length(); i++) {
                        String moveName = moveNamesArr.getString(i);
                        if (personValues.length() > i) {
                            int status = personValues.getInt(i);
                            person.getMoveStatus().put(moveName, status);
                        }
                    }
                    importedPeople.add(person);
                }

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

    private JSONObject readUrlContent(HttpURLConnection urlConnection) throws Exception {
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
            return new JSONObject(responseJson.toString());
        } else {
            throw new Exception("Erro na requisição: " + responseCode);
        }
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
            difficultiesRow.add(move.getDificuldades().toString());
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

        TextView difficultyHeader = new TextView(context);
        LinearLayout.LayoutParams difficultyParams = new LinearLayout.LayoutParams(dpToPx(80, context), dpToPx(45, context));
        difficultyParams.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
        difficultyHeader.setLayoutParams(difficultyParams);
        difficultyHeader.setGravity(Gravity.CENTER);
        difficultyHeader.setBackgroundColor(Color.WHITE);
        difficultyHeader.setText("⭐");
        difficultyHeader.setTextSize(24);
        difficultyHeader.setTextColor(Color.BLACK);
        headerNamesContainer.addView(difficultyHeader);


        for (Pessoa person : personList) {
            TextView headerCell = new TextView(context);
            LinearLayout.LayoutParams personParams = new LinearLayout.LayoutParams(dpToPx(150, context), dpToPx(45, context));
            personParams.setMargins(dpToPx(1, context), dpToPx(1, context), dpToPx(1, context), dpToPx(1, context));
            headerCell.setLayoutParams(personParams);
            headerCell.setGravity(Gravity.CENTER);
            headerCell.setBackgroundColor(Color.WHITE);
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
}