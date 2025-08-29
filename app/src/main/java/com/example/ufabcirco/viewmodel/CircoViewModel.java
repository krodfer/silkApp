package com.example.ufabcirco.viewmodel;

import android.graphics.Color;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.ufabcirco.model.Movimento;
import com.example.ufabcirco.model.Pessoa;
import com.example.ufabcirco.model.Post;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class CircoViewModel extends ViewModel {

    private static final String TAG = "CircoViewModel";
    private final Set<String> instructorNames = new HashSet<>();
    private final Map<String, Pessoa> pessoaMap = new HashMap<>();

    private final MutableLiveData<List<Pessoa>> _masterList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Pessoa>> getMasterList() { return _masterList; }

    private final MutableLiveData<List<Movimento>> _moveList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Movimento>> getMoveList() { return _moveList; }

    private final MutableLiveData<List<Pessoa>> _queueList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Pessoa>> getQueueList() { return _queueList; }

    private final MutableLiveData<String> _selectedPessoaId = new MutableLiveData<>();
    public LiveData<String> getSelectedPessoaId() { return _selectedPessoaId; }

    private final MutableLiveData<Integer> _selectionColor = new MutableLiveData<>();
    public LiveData<Integer> getSelectionColor() { return _selectionColor; }

    private final MutableLiveData<Pessoa> _navigateToProfile = new MutableLiveData<>();
    public LiveData<Pessoa> getNavigateToProfile() { return _navigateToProfile; }

    private final MutableLiveData<Boolean> _localModificationEvent = new MutableLiveData<>();
    public LiveData<Boolean> getLocalModificationEvent() { return _localModificationEvent; }

    private final MutableLiveData<List<Post>> _allPosts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Post>> _filteredPosts = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Post>> getFilteredPosts() { return _filteredPosts; }

    private final MutableLiveData<String> _galleryFilter = new MutableLiveData<>(null);
    public LiveData<String> getGalleryFilter() { return _galleryFilter; }

    private List<Post> shuffledPosts = new ArrayList<>();
    private int shuffledPostsIndex = 0;

    public CircoViewModel() {
        Log.d(TAG, "Construtor CircoViewModel chamado. Criando lista inicial.");

        instructorNames.addAll(Arrays.asList(
                "Kaique Ferreira", "Sandy Netto", "Lucas Mendes", "Karin Yanagi",
                "Amanda Andrade", "Yasmin Batista", "Gabriel Ross", "Gabs Ross", "Fernando Militani",
                "Dany Serrano", "Catarina Movio", "Gabriel Sgarbi", "M. Julio", "Giovanna Geloneze",
                "Carla Gomes", "Sagai Yami", "Bibi Souza"
        ));

        List<Pessoa> initialQueue = new ArrayList<>();
        _queueList.setValue(initialQueue);

        List<Post> hardcodedPosts = new ArrayList<>();
        List<String> tagsExemplo = Arrays.asList("video", "porto", "chaveDeCintura");

        hardcodedPosts.add(new Post("https://drive.google.com/uc?export=download&id=1dqBpuYc1BFXQrLTRzrtu5ZGjPkU7Anfo", tagsExemplo, "Chave de Cintura & Giro", 1, 3));
        hardcodedPosts.add(new Post("https://drive.google.com/uc?export=download&id=1WQOQXr8GKkYKxEw3i56bv_cHoYVncrSo", tagsExemplo, "Escorpião rei & Espacate", 2, 2));
        hardcodedPosts.add(new Post("https://drive.google.com/uc?export=download&id=1aTg7Btom4daXQVAX1OkfSoFdOYu2B3d5", tagsExemplo, "Subida Crochê & Chave de Cintura", 1, 3));
        hardcodedPosts.add(new Post("https://drive.google.com/uc?export=download&id=1lFgABHtdGxEYQ61WIhAst3RMjX0mE1OV", tagsExemplo, "Espacate", 3, 1));
        hardcodedPosts.add(new Post("https://drive.google.com/uc?export=download&id=1FF1aTfXK0L-tQDCyunBz27q6h4IWiFX_", tagsExemplo, "Super homem", 1, 3));

        _allPosts.setValue(hardcodedPosts);
        shuffleAndFilterPosts(null);
    }

    public void cycleMoveStatus(String pessoaId, String move) {
        if (pessoaId == null || move == null) {
            return;
        }

        Pessoa pessoaToUpdate = pessoaMap.get(pessoaId);
        if (pessoaToUpdate == null) {
            return;
        }

        int currentStatus = pessoaToUpdate.getMoveStatus().getOrDefault(move, 0);
        int nextStatus;

        switch (currentStatus) {
            case 0:
                nextStatus = 1;
                break;
            case 1:
                nextStatus = 2;
                break;
            case 2:
                nextStatus = 3;
                break;
            case 3:
                nextStatus = 0;
                break;
            default:
                nextStatus = 0;
                break;
        }

        pessoaToUpdate.getMoveStatus().put(move, nextStatus);
        List<Pessoa> updatedMasterList = new ArrayList<>(Objects.requireNonNull(getMasterList().getValue()));
        for(int i=0; i < updatedMasterList.size(); i++) {
            if(updatedMasterList.get(i).getId().equals(pessoaId)) {
                updatedMasterList.set(i, pessoaToUpdate);
                break;
            }
        }
        _masterList.setValue(updatedMasterList);
        notifyLocalModification();
    }

    public boolean isInstructor(String personName) {
        return instructorNames.contains(personName);
    }

    public void setMoveList(List<Movimento> moves) {
        if (!Objects.equals(_moveList.getValue(), moves)) {
            _moveList.setValue(moves);
        }
    }

    public void loadFromLocalFile() {
        if (_masterList.getValue() != null) {
            _masterList.setValue(new ArrayList<>(_masterList.getValue()));
        }
    }

    public void setMoveStatus(String pessoaId, String move, int newStatus) {
        if (pessoaId == null || move == null) {
            return;
        }

        Pessoa pessoaToUpdate = pessoaMap.get(pessoaId);
        if (pessoaToUpdate == null) {
            return;
        }

        pessoaToUpdate.getMoveStatus().put(move, newStatus);
        List<Pessoa> updatedMasterList = new ArrayList<>(Objects.requireNonNull(getMasterList().getValue()));
        for(int i=0; i < updatedMasterList.size(); i++) {
            if(updatedMasterList.get(i).getId().equals(pessoaId)) {
                updatedMasterList.set(i, pessoaToUpdate);
                break;
            }
        }
        _masterList.setValue(updatedMasterList);
        notifyLocalModification();
    }

    public void importMasterList(List<Pessoa> importedList) {
        if (importedList != null) {
            pessoaMap.clear();
            for (Pessoa p : importedList) {
                pessoaMap.put(p.getId(), p);
            }

            if (!Objects.equals(_masterList.getValue(), importedList)) {
                _masterList.setValue(new ArrayList<>(importedList));

                List<Pessoa> currentQueue = _queueList.getValue();
                if (currentQueue != null) {
                    List<Pessoa> updatedQueue = new ArrayList<>();
                    for (Pessoa queuedPerson : currentQueue) {
                        Pessoa updatedPerson = importedList.stream()
                                .filter(p -> p.getId().equals(queuedPerson.getId()))
                                .findFirst()
                                .orElse(queuedPerson);
                        updatedQueue.add(updatedPerson);
                    }
                    _queueList.setValue(updatedQueue);
                }

                String selectedId = _selectedPessoaId.getValue();
                if (selectedId != null && (currentQueue == null || currentQueue.stream().noneMatch(p -> p.getId().equals(selectedId)))) {
                    _selectedPessoaId.setValue(null);
                    _selectionColor.setValue(Color.TRANSPARENT);
                }
            }
        } else {
            pessoaMap.clear();
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
        pessoaMap.put(newPerson.getId(), newPerson);

        List<Pessoa> currentQueueList = _queueList.getValue() != null ? new ArrayList<>(_queueList.getValue()) : new ArrayList<>();
        currentQueueList.add(newPerson);
        _queueList.setValue(currentQueueList);
        notifyLocalModification();

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
        for (int i = 0; i < currentMasterList.size(); i++) {
            if (currentMasterList.get(i).getId().equals(personToAdd.getId())) {
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
        notifyLocalModification();

        return "SUCCESS";
    }

    public void updateMoveStatus(String pessoaId, String moveName, int newStatus) {
        List<Pessoa> currentList = _masterList.getValue();
        if (currentList != null) {
            int pessoaIndex = -1;
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(pessoaId)) {
                    pessoaIndex = i;
                    break;
                }
            }

            if (pessoaIndex != -1) {
                Pessoa pessoaToUpdate = currentList.get(pessoaIndex);
                Map<String, Integer> statusMap = pessoaToUpdate.getMoveStatus();
                statusMap.put(moveName, newStatus);
                _masterList.setValue(currentList);
            }
        }
    }

    public void removePersonFromQueue(Pessoa pessoa) {
        List<Pessoa> currentQueueList = _queueList.getValue();
        if (currentQueueList != null && pessoa != null) {
            ArrayList<Pessoa> newList = new ArrayList<>(currentQueueList);
            if (newList.remove(pessoa)) {
                _queueList.setValue(newList);
                if (Objects.equals(pessoa.getId(), _selectedPessoaId.getValue())) {
                    _selectedPessoaId.setValue(null);
                    _selectionColor.setValue(Color.TRANSPARENT);
                }
                notifyLocalModification();
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

    private void notifyLocalModification() {
        _localModificationEvent.setValue(true);
    }

    public void setAllPosts(List<Post> allPosts) {
        _allPosts.setValue(allPosts);
        shuffleAndFilterPosts(null);
    }

    public void setGalleryFilter(String filter) {
        _galleryFilter.setValue(filter);
        shuffleAndFilterPosts(filter);
    }

    private void shuffleAndFilterPosts(String filter) {
        List<Post> postsToFilter = _allPosts.getValue() != null ? new ArrayList<>(_allPosts.getValue()) : new ArrayList<>();

        if (filter != null) {
            postsToFilter = postsToFilter.stream()
                    .filter(p -> p.getTags().contains(filter) || p.getMovimentoNome().equals(filter))
                    .collect(Collectors.toList());
        }

        shuffledPosts = new ArrayList<>(postsToFilter);
        Collections.shuffle(shuffledPosts);
        shuffledPostsIndex = 0;

        _filteredPosts.setValue(new ArrayList<>());
        loadMorePosts();
    }

    public void loadMorePosts() {
        if (shuffledPosts.isEmpty()) {
            return;
        }

        int chunkSize = 5;
        List<Post> currentFilteredPosts = _filteredPosts.getValue() != null ? new ArrayList<>(_filteredPosts.getValue()) : new ArrayList<>();

        if (shuffledPostsIndex >= shuffledPosts.size()) {
            Collections.shuffle(shuffledPosts);
            shuffledPostsIndex = 0;
        }

        for (int i = 0; i < chunkSize && shuffledPostsIndex < shuffledPosts.size(); i++) {
            currentFilteredPosts.add(shuffledPosts.get(shuffledPostsIndex));
            shuffledPostsIndex++;
        }

        _filteredPosts.setValue(currentFilteredPosts);
    }
}