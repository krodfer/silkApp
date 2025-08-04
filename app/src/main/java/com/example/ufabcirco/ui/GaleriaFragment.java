package com.example.ufabcirco.ui;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.adapter.GaleriaAdapter;
import com.example.ufabcirco.model.Post;
import com.example.ufabcirco.viewmodel.CircoViewModel;
import java.util.ArrayList;

public class GaleriaFragment extends Fragment {

    private static final String TAG = "GaleriaFragment";
    private CircoViewModel viewModel;
    private RecyclerView recyclerView;
    private GaleriaAdapter adapter;
    private LinearLayoutManager layoutManager;

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

        adapter = new GaleriaAdapter(new ArrayList<Post>());
        recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        setupObservers();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int totalItemCount = adapter.getItemCount();
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (totalItemCount > 0 && firstVisibleItemPosition < adapter.getPostCount() * 2) {
                    int newPosition = (int) (adapter.getItemCount() / 2.0);
                    recyclerView.scrollToPosition(newPosition);
                } else if (totalItemCount > 0 && lastVisibleItemPosition > adapter.getItemCount() - (adapter.getPostCount() * 2) ) {
                    int newPosition = (int) (adapter.getItemCount() / 2.0);
                    recyclerView.scrollToPosition(newPosition);
                }

                if (lastVisibleItemPosition >= totalItemCount - 5) {
                    viewModel.loadMorePosts();
                }

                int centerPosition = findCenteredItemPosition();
                if (centerPosition != -1) {
                    adapter.setPlayingPosition(centerPosition);
                }
            }
        });

        if (adapter.getItemCount() > 0) {
            int middlePosition = (int) (adapter.getItemCount() / 2.0);
            recyclerView.scrollToPosition(middlePosition);
        }
    }

    public static GaleriaFragment newInstance() {
        return new GaleriaFragment();
    }

    private int findCenteredItemPosition() {
        int center = recyclerView.getHeight() / 2;
        Rect r = new Rect();
        for (int i = 0; i < layoutManager.getChildCount(); i++) {
            View v = layoutManager.getChildAt(i);
            if (v != null) {
                v.getGlobalVisibleRect(r);
                if (r.top <= center && r.bottom >= center) {
                    return layoutManager.getPosition(v);
                }
            }
        }
        return -1;
    }

    private void setupObservers() {
        viewModel.getFilteredPosts().observe(getViewLifecycleOwner(), posts -> {
            adapter.updatePosts(posts);
        });
    }
}