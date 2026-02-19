package com.example.ufabcirco.ui;

import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
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
import androidx.recyclerview.widget.ItemTouchHelper;

import com.example.ufabcirco.R;
import com.example.ufabcirco.adapter.FilaAdapter;
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.viewmodel.CircoViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return makeMovementFlags(0, 0);

                List<Pessoa> lista = filaAdapter.getPersonList();
                Pessoa pessoaNoViewHolder = lista.get(position % lista.size());

                if (pessoaNoViewHolder.getId().equals(filaAdapter.getSelectedPessoaId())) {
                    int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                    int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                    return makeMovementFlags(dragFlags, swipeFlags);
                }

                return makeMovementFlags(0, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();

                filaAdapter.swapItems(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                List<Pessoa> atual = filaAdapter.getPersonList();
                if (!atual.isEmpty()) {
                    Pessoa pessoaParaRemover = atual.get(position % atual.size());
                    circoViewModel.removePersonFromQueue(pessoaParaRemover);
                    Toast.makeText(getContext(), pessoaParaRemover.getNome() + " removido(a)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                circoViewModel.updateQueueOrder(filaAdapter.getPersonList());
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerViewFila);

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
        if (pessoa != null && getParentFragmentManager() != null) {
            ProfileMenuFragment.newInstance(pessoa).show(getParentFragmentManager(), "ProfileMenu");
        }
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
        if (getContext() == null){
            return;
        }

        AlertDialog.Builder builder = CircoDialogBuilder.create(requireContext(), "Adicionar pessoa à fila");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_pessoa, null);
        final EditText input = dialogView.findViewById(R.id.edit_text_nome_pessoa);

        builder.setView(dialogView);
        builder.setPositiveButton("Adicionar", (dialog, which) -> {
            String partialName = input.getText().toString().trim();
            if (!partialName.isEmpty()) {
                List<Pessoa> matches = circoViewModel.findPeopleInMasterList(partialName);
                if (matches.isEmpty()) {
                    showCreateNewPersonDialog(partialName);
                } else if (matches.size() == 1) {
                    addPersonToQueue(matches.get(0));
                } else {
                    showMultipleMatchesDialog(matches);
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        CircoDialogBuilder.fixColors(dialog);
    }

    private void showCreateNewPersonDialog(String partialName) {
        if (getContext() == null) {
            return;
        }

        AlertDialog.Builder builder = CircoDialogBuilder.create(requireContext(), "Pessoa não encontrada");
        builder.setMessage("Deseja criar um novo cadastro para '" + partialName + "'?");

        builder.setPositiveButton("Sim, Criar", (dialog, which) -> {
            circoViewModel.createNewPersonAndAddToQueue(partialName);
        });

        builder.setNegativeButton("Não", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        CircoDialogBuilder.fixColors(dialog);
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
            case "DUPLICATE_QUEUE":
                Toast.makeText(getContext(), pessoa.getNome() + " já está na fila.", Toast.LENGTH_SHORT).show();
                break;
            case "INVALID":
                Toast.makeText(getContext(), "Erro: Pessoa inválida.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}