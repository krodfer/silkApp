package com.example.ufabcirco.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.adapter.GaleriaAdapter;
import com.example.ufabcirco.viewmodel.CircoViewModel;
import com.example.ufabcirco.model.Post;
import java.util.ArrayList;
import java.util.List;

public class GaleriaFragment extends Fragment {

    private static final String TAG = "GaleriaFragment";
    private CircoViewModel viewModel;
    private RecyclerView recyclerView;
    private GaleriaAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_galeria, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(CircoViewModel.class);
        recyclerView = view.findViewById(R.id.recycler_view_galeria);
        progressBar = view.findViewById(R.id.progress_bar_galeria);
        emptyTextView = view.findViewById(R.id.text_view_empty_galeria);

        adapter = new GaleriaAdapter(new ArrayList<Post>());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ( (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && firstVisibleItemPosition >= 0) {
                        viewModel.loadMorePosts();
                    }
                }
            }
        });

        setupObservers();
    }

    private void setupObservers() {
        viewModel.getFilteredPosts().observe(getViewLifecycleOwner(), posts -> {
            adapter.updatePosts(posts);
            updateUI(posts);
        });

        viewModel.getGalleryFilter().observe(getViewLifecycleOwner(), filter -> {
        });
    }

    private void updateUI(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public static GaleriaFragment newInstance() {
        return new GaleriaFragment();
    }
}