package com.example.ufabcirco.ui;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.adapter.GaleriaAdapter;
import com.example.ufabcirco.model.Post;
import com.example.ufabcirco.ui.custom.OutlineTextView;
import com.example.ufabcirco.viewmodel.CircoViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class GaleriaFragment extends Fragment {

    private static final String TAG = "GaleriaFragment";
    private CircoViewModel viewModel;
    private RecyclerView recyclerView;
    private GaleriaAdapter adapter;
    private LinearLayoutManager layoutManager;
    private OutlineTextView globalTitleTextViewCenter, globalTitleTextViewAbove, globalTitleTextViewBelow;
    private HashMap<String, Integer> postColors = new HashMap<>();

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
        globalTitleTextViewCenter = view.findViewById(R.id.text_view_global_post_title_center);
        globalTitleTextViewAbove = view.findViewById(R.id.text_view_global_post_title_below);
        globalTitleTextViewBelow = view.findViewById(R.id.text_view_global_post_title_below);

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

                updateTitlesPosition();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int centerPosition = findCenteredItemPosition();
                    if (centerPosition != -1) {
                        adapter.setPlayingPosition(centerPosition);
                    } else {
                        adapter.setPlayingPosition(-1);
                    }
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

    private void updateTitlesPosition() {
        int centerPosition = findCenteredItemPosition();
        if (centerPosition != -1) {
            positionGlobalTitle(centerPosition, globalTitleTextViewCenter);

            if (centerPosition > 0) {
                positionGlobalTitle(centerPosition - 1, globalTitleTextViewAbove);
            } else {
                globalTitleTextViewAbove.setVisibility(View.INVISIBLE);
            }

            if (centerPosition < adapter.getItemCount() - 1) {
                positionGlobalTitle(centerPosition + 1, globalTitleTextViewBelow);
            } else {
                globalTitleTextViewBelow.setVisibility(View.INVISIBLE);
            }
        } else {
            globalTitleTextViewCenter.setVisibility(View.INVISIBLE);
            globalTitleTextViewAbove.setVisibility(View.INVISIBLE);
            globalTitleTextViewBelow.setVisibility(View.INVISIBLE);
        }
    }

    private void positionGlobalTitle(int postPosition, OutlineTextView titleTextView) {
        View postView = layoutManager.findViewByPosition(postPosition);
        if (postView == null) {
            titleTextView.setVisibility(View.INVISIBLE);
            return;
        }

        Post post = adapter.getPostAt(postPosition);
        if (post == null) {
            titleTextView.setVisibility(View.INVISIBLE);
            return;
        }

        VideoView videoView = postView.findViewById(R.id.video_view_post);
        if (videoView == null) {
            titleTextView.setVisibility(View.INVISIBLE);
            return;
        }

        int[] videoLocation = new int[2];
        videoView.getLocationOnScreen(videoLocation);

        float dpX = 25f * getResources().getDisplayMetrics().density;
        float dpY = -60f * getResources().getDisplayMetrics().density;

        float x = videoLocation[0] + dpX;
        float y = videoLocation[1] + dpY;

        titleTextView.setX(x);
        titleTextView.setY(y);

        titleTextView.setOutlineWidth(10.0f);

        titleTextView.setText(post.getMovimentoNome());
        titleTextView.setTextColor(getPostColor(post.getId()));
        titleTextView.setVisibility(View.VISIBLE);
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

    private int getPostColor(String postId) {
        if (!postColors.containsKey(postId)) {
            Random random = new Random();
            final float hue = random.nextFloat() * 360;
            final float saturation = 0.5f;
            final float lightness = 0.9f;
            int color = Color.HSVToColor(new float[]{hue, saturation, lightness});
            postColors.put(postId, color);
        }
        return postColors.get(postId);
    }

    private void setupObservers() {
        viewModel.getFilteredPosts().observe(getViewLifecycleOwner(), posts -> {
            adapter.updatePosts(posts);
            if (adapter.getPostCount() > 0) {
                int middlePosition = (int) (adapter.getItemCount() / 2.0);
                recyclerView.scrollToPosition(middlePosition);
            }
        });
    }
}