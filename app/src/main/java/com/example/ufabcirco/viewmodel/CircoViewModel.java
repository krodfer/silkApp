// app/src/main/java/com/example/ufabcirco/viewmodel/CircoViewModel.java
package com.example.ufabcirco.viewmodel;

import android.graphics.Color;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.ufabcirco.model.Pessoa;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class CircoViewModel extends ViewModel {

    private static final String TAG = "CircoViewModel";

    private final MutableLiveData<List<Pessoa>> _masterList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Pessoa>> getMasterList() { return _masterList; }

    private final MutableLiveData<List<Pessoa>> _queueList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Pessoa>> getQueueList() { return _queueList; }

    private final MutableLiveData<String> _selectedPessoaId = new MutableLiveData<>();
    public LiveData<String> getSelectedPessoaId() { return _selectedPessoaId; }

    private final MutableLiveData<Integer> _selectionColor = new MutableLiveData<>();
    public LiveData<Integer> getSelectionColor() { return _selectionColor; }

    private final MutableLiveData<Pessoa> _navigateToProfile = new MutableLiveData<>();
    public LiveData<Pessoa> getNavigateToProfile() { return _navigateToProfile; }

    public CircoViewModel() {
        Log.d(TAG, "Construtor CircoViewModel chamado. Criando lista inicial.");
        List<Pessoa> initialPeople = new ArrayList<>();
        Pessoa kaique = new Pessoa("Kaique Ferreira");

        //kaique.getMoveStatus().put("A", 2);

        initialPeople.add(kaique);
        _masterList.setValue(initialPeople);

        List<Pessoa> initialQueue = new ArrayList<>();
        initialQueue.add(kaique);
        _queueList.setValue(initialQueue);
    }

    public void cycleMoveStatus(Pessoa pessoa, String move) {
        if (pessoa == null || move == null) {
            Log.e(TAG, "Tentativa de cycleMoveStatus com pessoa ou movimento nulo.");
            return;
        }

        List<Pessoa> currentMasterList = _masterList.getValue();
        if (currentMasterList == null) {
            Log.e(TAG, "MasterList nula ao tentar cycleMoveStatus.");
            return;
        }

        boolean personFoundAndUpdate = false;
        for (Pessoa p : currentMasterList) {
            if (p.getId().equals(pessoa.getId())) {
                Map<String, Integer> statusMap = p.getMoveStatus();
                int currentStatus = statusMap.getOrDefault(move, 0);
                int nextStatus = (currentStatus + 1) % 4;
                statusMap.put(move, nextStatus);
                personFoundAndUpdate = true;
                break;
            }
        }
        if (personFoundAndUpdate) {
            _masterList.setValue(new ArrayList<>(currentMasterList));
            Log.d(TAG, "Status do movimento '" + move + "' para '" + pessoa.getNome() + "' atualizado para " + pessoa.getMoveStatus().get(move));
        } else {
            Log.w(TAG, "Pessoa não encontrada na masterList para cycleMoveStatus: " + pessoa.getNome());
        }
    }

    public void importMasterList(List<Pessoa> importedList) {
        if (importedList != null) {
            Log.d(TAG, "ViewModel: importMasterList chamado com " + importedList.size() + " pessoas.");
            _masterList.setValue(new ArrayList<>(importedList));
            _queueList.setValue(new ArrayList<>());
            _selectedPessoaId.setValue(null);
            _selectionColor.setValue(Color.TRANSPARENT);
            Log.d(TAG, "ViewModel: MasterList atualizada com " + importedList.size() + " pessoas. Fila e seleção limpas.");
        } else {
            Log.w(TAG, "ViewModel: Tentativa de importar lista nula para masterList. Definindo como vazia.");
            _masterList.setValue(new ArrayList<>());
            _queueList.setValue(new ArrayList<>());
            _selectedPessoaId.setValue(null);
            _selectionColor.setValue(Color.TRANSPARENT);
        }
    }

    public String addPersonToQueue(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            Log.w(TAG, "Tentativa de adicionar pessoa com nome inválido/vazio à fila.");
            return "INVALID_NAME";
        }

        String trimmedNome = nome.trim();
        List<Pessoa> currentMasterList = _masterList.getValue() != null ? new ArrayList<>(_masterList.getValue()) : new ArrayList<>();
        List<Pessoa> currentQueueList = _queueList.getValue() != null ? new ArrayList<>(_queueList.getValue()) : new ArrayList<>();

        for (Pessoa p : currentQueueList) {
            if (p.getNome().equalsIgnoreCase(trimmedNome)) {
                Log.d(TAG, trimmedNome + " já está na fila.");
                return "DUPLICATE_IN_QUEUE";
            }
        }

        Pessoa personToAddToQueue = null;
        boolean foundInMaster = false;
        int masterListIndex = -1;

        for (int i = 0; i < currentMasterList.size(); i++) {
            if (currentMasterList.get(i).getNome().equalsIgnoreCase(trimmedNome)) {
                personToAddToQueue = currentMasterList.get(i);
                foundInMaster = true;
                masterListIndex = i;
                break;
            }
        }

        String resultMessage;
        if (!foundInMaster) {
            personToAddToQueue = new Pessoa(trimmedNome);
            currentMasterList.add(0, personToAddToQueue);
            _masterList.setValue(currentMasterList);
            resultMessage = "CREATED_NEW";
            Log.d(TAG, trimmedNome + " criado na tabela (no início).");
        } else {
            if (masterListIndex > 0) {
                Pessoa p = currentMasterList.remove(masterListIndex);
                currentMasterList.add(0, p);
                _masterList.setValue(currentMasterList);
            }
            resultMessage = "EXISTED_IN_MASTER";
            Log.d(TAG, trimmedNome + " já existia na tabela (e foi movido para o início se aplicável).");
        }

        currentQueueList.add(personToAddToQueue);
        _queueList.setValue(currentQueueList);
        Log.d(TAG, trimmedNome + " adicionado à fila.");

        return resultMessage;
    }

    public void removePersonFromQueue(Pessoa pessoa) {
        List<Pessoa> currentQueueList = _queueList.getValue();
        if (currentQueueList != null && pessoa != null) {
            ArrayList<Pessoa> newList = new ArrayList<>(currentQueueList);
            if (newList.remove(pessoa)) {
                _queueList.setValue(newList);
                if(Objects.equals(pessoa.getId(), _selectedPessoaId.getValue())){
                    _selectedPessoaId.setValue(null);
                    _selectionColor.setValue(Color.TRANSPARENT);
                }
            }
        }
    }

    public void selectPessoa(Pessoa pessoa) {
        if (pessoa == null) {
            _selectedPessoaId.setValue(null);
            _selectionColor.setValue(Color.TRANSPARENT);
            return;
        }
        String currentSelectedId = _selectedPessoaId.getValue();
        String newId = pessoa.getId();

        if (Objects.equals(currentSelectedId, newId)) {
            _navigateToProfile.setValue(pessoa);
        } else {
            _selectedPessoaId.setValue(newId);
            _selectionColor.setValue(generateRandomPastelColor());
        }
    }

    public void onProfileNavigated() { _navigateToProfile.setValue(null); }

    private int generateRandomPastelColor() {
        Random random = new Random();
        final float hue = random.nextFloat() * 360;
        final float saturation = 0.5f;
        final float lightness = 0.9f;
        return Color.HSVToColor(new float[]{hue, saturation, lightness});
    }
}