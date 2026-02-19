package com.example.ufabcirco.viewmodel;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.adapter.FilaAdapter;
import com.example.ufabcirco.model.Pessoa;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class FilaFragment extends Fragment {

    private static final String TAG = "FilaFragment";
    private CircoViewModel circoViewModel;
    private FilaAdapter filaAdapter;
    private RecyclerView recyclerViewFila;
    private TextView textViewEmptyFila;
    private FloatingActionButton fabAddPessoa;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fila, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        circoViewModel = new ViewModelProvider(requireActivity()).get(CircoViewModel.class);

        recyclerViewFila = view.findViewById(R.id.recycler_view_fila);
        textViewEmptyFila = view.findViewById(R.id.text_view_empty_fila);
        fabAddPessoa = view.findViewById(R.id.fab_add_pessoa);

        filaAdapter = new FilaAdapter(
                pessoa -> circoViewModel.selectPessoa(pessoa),
                pessoa -> circoViewModel.removePersonFromQueue(pessoa)
        );

        recyclerViewFila.setAdapter(filaAdapter);
        recyclerViewFila.setLayoutManager(new LinearLayoutManager(getContext()));

        setupObservers();

        fabAddPessoa.setOnClickListener(v -> showAddPersonDialog());
    }

    private void setupObservers() {
        circoViewModel.getQueueList().observe(getViewLifecycleOwner(), filaDePessoas -> {
            Log.d(TAG, "Observer da QueueList disparado. " + (filaDePessoas == null ? "Lista nula." : "Pessoas na fila: " + filaDePessoas.size()));
            filaAdapter.setFilaPessoas(filaDePessoas);
            updateUI(filaDePessoas);

            if (filaDePessoas != null && !filaDePessoas.isEmpty()) {
                int currentAdapterItemCount = filaAdapter.getItemCount();
                if (currentAdapterItemCount > filaDePessoas.size()) {
                    int startPosition = Integer.MAX_VALUE / 2;
                    int offset = startPosition % filaDePessoas.size();
                    recyclerViewFila.scrollToPosition(startPosition - offset);
                    Log.d(TAG, "Rolagem infinita: Posicionando em " + (startPosition - offset));
                }
            }
        });

        circoViewModel.getSelectedPessoaId().observe(getViewLifecycleOwner(), selectedId -> {
            Log.d(TAG, "SelectedPessoaId mudou para: " + selectedId);
            filaAdapter.setSelectionState(selectedId, circoViewModel.getSelectionColor().getValue());
        });

        circoViewModel.getSelectionColor().observe(getViewLifecycleOwner(), color -> {
            Log.d(TAG, "SelectionColor mudou para: " + color);
            filaAdapter.setSelectionState(circoViewModel.getSelectedPessoaId().getValue(), color);
        });

        circoViewModel.getNavigateToProfile().observe(getViewLifecycleOwner(), pessoa -> {
            if (pessoa != null) {
                Log.d(TAG, "Navegando para o perfil de: " + pessoa.getNome());
                showProfileDialog(pessoa);
                circoViewModel.onProfileNavigated();
            }
        });
    }

    private void showProfileDialog(Pessoa pessoa) {
        if(getContext() == null || pessoa == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Perfil de " + pessoa.getNome())
                .setMessage(formatMoveStatusForDialog(pessoa.getMoveStatus()))
                .setPositiveButton("Fechar", null)
                .show();
    }

    private String formatMoveStatusForDialog(Map<String, Integer> moveStatus) {
        if (moveStatus == null || moveStatus.isEmpty()) {
            return "Nenhum movimento registrado.";
        }
        StringBuilder sb = new StringBuilder();
        int relevantMovesCount = 0;

        List<Map.Entry<String, Integer>> sortedMoves = new ArrayList<>(moveStatus.entrySet());
        Collections.sort(sortedMoves, Comparator.comparing(Map.Entry::getKey));

        for (Map.Entry<String, Integer> entry : sortedMoves) {
            String statusText = "";
            int statusValue = entry.getValue() != null ? entry.getValue() : 0;

            switch (statusValue) {
                case 1: statusText = " (Já fez)"; break;
                case 2: statusText = " (Aprendeu)"; break;
                case 3: statusText = " (Não sabe)"; break;
            }

            if (statusValue != 0) {
                if (relevantMovesCount == 0) {
                    sb.append("Movimentos:\n");
                }
                sb.append("- ").append(entry.getKey()).append(statusText).append("\n");
                relevantMovesCount++;
            }
        }

        if (relevantMovesCount == 0) {
            return "Nenhum movimento com status relevante.";
        }
        return sb.toString().trim();
    }

    private void updateUI(List<Pessoa> pessoas) {
        if (pessoas == null || pessoas.isEmpty()) {
            Log.d(TAG, "updateUI: A fila está vazia. ESCONDENDO o RecyclerView.");
            textViewEmptyFila.setText("A fila está vazia.");
            textViewEmptyFila.setVisibility(View.VISIBLE);
            recyclerViewFila.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "updateUI: A fila contém itens. MOSTRANDO o RecyclerView.");
            textViewEmptyFila.setVisibility(View.GONE);
            recyclerViewFila.setVisibility(View.VISIBLE);
        }
    }

    private void showAddPersonDialog() {
        if(getContext() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Adicionar Pessoa à Fila");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Digite parte do nome");
        builder.setView(input);

        builder.setPositiveButton("Adicionar", (dialog, which) -> {
            String partialName = input.getText().toString().trim();
            if (partialName.isEmpty()) {
                Toast.makeText(getContext(), "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Pessoa> matches = circoViewModel.findPeopleInMasterList(partialName);

            if (matches.isEmpty()) {
                Toast.makeText(getContext(), "Ninguém encontrado na tabela com esse nome.", Toast.LENGTH_SHORT).show();
            } else if (matches.size() == 1) {
                addPersonToQueue(matches.get(0));
            } else {
                showMultipleMatchesDialog(matches);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showMultipleMatchesDialog(List<Pessoa> matches) {
        if(getContext() == null) {
            return;
        }
        CharSequence[] names = matches.stream().map(Pessoa::getNome).toArray(CharSequence[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Múltiplos resultados. Escolha um:")
                .setItems(names, (dialog, which) -> {
                    Pessoa selectedPerson = matches.get(which);
                    addPersonToQueue(selectedPerson);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void addPersonToQueue(Pessoa pessoa) {
        String result = circoViewModel.addPersonToQueue(pessoa);
        if (getContext() == null) {
            return;
        }

        switch (result) {
            case "SUCCESS":
                Toast.makeText(getContext(), pessoa.getNome() + " foi adicionado(a) à fila.", Toast.LENGTH_SHORT).show();
                break;
            case "DUPLICATE":
                Toast.makeText(getContext(), pessoa.getNome() + " já está na fila.", Toast.LENGTH_SHORT).show();
                break;
            case "INVALID":
                Toast.makeText(getContext(), "Erro: Pessoa inválida.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}