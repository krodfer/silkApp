package com.example.ufabcirco.viewmodel;

import android.graphics.Color;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.ufabcirco.model.Pessoa;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

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
        initialPeople.add(new Pessoa("Kaique Ferreira"));
        _masterList.setValue(initialPeople);

        List<Pessoa> initialQueue = new ArrayList<>();
        initialQueue.add(initialPeople.get(0));
        _queueList.setValue(initialQueue);
    }

    public void cycleMoveStatus(Pessoa pessoa, String move) {
        if (pessoa == null || move == null) {
            return;
        }
        List<Pessoa> currentMasterList = _masterList.getValue();
        if (currentMasterList == null) {
            return;
        }
        for (Pessoa p : currentMasterList) {
            if (p.getId().equals(pessoa.getId())) {
                int currentStatus = p.getMoveStatus().getOrDefault(move, 0);
                int nextStatus = (currentStatus + 1) % 4;
                p.getMoveStatus().put(move, nextStatus);
                break;
            }
        }
        _masterList.setValue(new ArrayList<>(currentMasterList));
    }

    public void importMasterList(List<Pessoa> importedList) {
        if (importedList != null) {
            _masterList.setValue(new ArrayList<>(importedList));
            _queueList.setValue(new ArrayList<>());
            _selectedPessoaId.setValue(null);
            _selectionColor.setValue(Color.TRANSPARENT);
        } else {
            _masterList.setValue(new ArrayList<>());
            _queueList.setValue(new ArrayList<>());
            _selectedPessoaId.setValue(null);
            _selectionColor.setValue(Color.TRANSPARENT);
        }
    }

    public List<Pessoa> findPeopleInMasterList(String name) {
        List<Pessoa> master = _masterList.getValue();
        if (name == null || name.trim().isEmpty() || master == null) {
            return new ArrayList<>();
        }
        String trimmedSearchName = name.trim();
        return master.stream()
                .filter(person -> {
                    String[] nameParts = person.getNome().split("\\s+");
                    if (nameParts.length == 0) {
                        return false;
                    }

                    String firstName = nameParts[0];
                    if (firstName.equalsIgnoreCase(trimmedSearchName)) {
                        return true;
                    }

                    if (nameParts.length > 1) {
                        String lastName = nameParts[nameParts.length - 1];
                        if (lastName.equalsIgnoreCase(trimmedSearchName)) {
                            return true;
                        }
                    }

                    return false;
                })
                .collect(Collectors.toList());
    }

    public String createNewPersonAndAddToQueue(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "INVALID";
        }
        String finalName = name.trim();
        List<Pessoa> currentMasterList = _masterList.getValue() != null ? new ArrayList<>(_masterList.getValue()) : new ArrayList<>();

        boolean alreadyExists = currentMasterList.stream().anyMatch(p -> p.getNome().equalsIgnoreCase(finalName));
        if (alreadyExists) {
            return "DUPLICATE_MASTER";
        }

        Pessoa newPerson = new Pessoa(finalName);
        currentMasterList.add(0, newPerson);
        _masterList.setValue(currentMasterList);

        List<Pessoa> currentQueueList = _queueList.getValue() != null ? new ArrayList<>(_queueList.getValue()) : new ArrayList<>();
        currentQueueList.add(newPerson);
        _queueList.setValue(currentQueueList);

        return "SUCCESS";
    }

    public String addPersonToQueue(Pessoa personToAdd) {
        if (personToAdd == null) {
            return "INVALID";
        }

        List<Pessoa> currentQueueList = _queueList.getValue() != null ? new ArrayList<>(_queueList.getValue()) : new ArrayList<>();
        boolean alreadyInQueue = currentQueueList.stream().anyMatch(p -> p.getId().equals(personToAdd.getId()));

        if (alreadyInQueue) {
            return "DUPLICATE_QUEUE";
        }

        List<Pessoa> currentMasterList = _masterList.getValue() != null ? new ArrayList<>(_masterList.getValue()) : new ArrayList<>();
        int masterListIndex = -1;
        for(int i = 0; i < currentMasterList.size(); i++){
            if(currentMasterList.get(i).getId().equals(personToAdd.getId())){
                masterListIndex = i;
                break;
            }
        }

        if (masterListIndex > 0) {
            Pessoa p = currentMasterList.remove(masterListIndex);
            currentMasterList.add(0, p);
            _masterList.setValue(currentMasterList);
        }

        currentQueueList.add(personToAdd);
        _queueList.setValue(currentQueueList);
        return "SUCCESS";
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