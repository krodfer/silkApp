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

public class FilaViewModel extends ViewModel {

    private final MutableLiveData<List<Pessoa>> _filaPessoas = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Pessoa>> getFilaPessoas() {
        return _filaPessoas;
    }

    private final MutableLiveData<String> _selectedPessoaId = new MutableLiveData<>();
    public LiveData<String> getSelectedPessoaId() {
        return _selectedPessoaId;
    }

    private final MutableLiveData<Integer> _selectionColor = new MutableLiveData<>();
    public LiveData<Integer> getSelectionColor() {
        return _selectionColor;
    }

    private final MutableLiveData<Pessoa> _navigateToProfile = new MutableLiveData<>();
    public LiveData<Pessoa> getNavigateToProfile() {
        return _navigateToProfile;
    }

    public FilaViewModel() {
        Log.d("FilaViewModel", "Construtor chamado. Criando lista inicial.");
        List<Pessoa> initialList = new ArrayList<>();

        initialList.add(new Pessoa("Teste"));

        _filaPessoas.setValue(initialList);
    }

    public void selectPessoa(Pessoa pessoa) {
        String currentSelectedId = _selectedPessoaId.getValue();
        String newId = pessoa.getId();

        if (Objects.equals(currentSelectedId, newId)) {
            _navigateToProfile.setValue(pessoa);
        } else {
            _selectedPessoaId.setValue(newId);
            _selectionColor.setValue(generateRandomPastelColor());
        }
    }

    private int generateRandomPastelColor() {
        Random random = new Random();
        final float hue = random.nextFloat() * 360;
        final float saturation = 0.5f;
        final float lightness = 0.9f;
        return Color.HSVToColor(new float[]{hue, saturation, lightness});
    }

    public void onProfileNavigated() {
        _navigateToProfile.setValue(null);
    }

    public void addPessoa(Pessoa pessoa) {
        List<Pessoa> currentList = _filaPessoas.getValue() != null ? _filaPessoas.getValue() : new ArrayList<>();
        ArrayList<Pessoa> newList = new ArrayList<>(currentList);
        newList.add(pessoa);
        _filaPessoas.setValue(newList);
    }

    public void removerPessoa(Pessoa pessoa) {
        List<Pessoa> currentList = _filaPessoas.getValue();
        if (currentList != null) {
            ArrayList<Pessoa> newList = new ArrayList<>(currentList);
            if (newList.remove(pessoa)) {
                _filaPessoas.setValue(newList);
                if(Objects.equals(pessoa.getId(), _selectedPessoaId.getValue())){
                    _selectedPessoaId.setValue(null);
                }
            }
        }
    }
}