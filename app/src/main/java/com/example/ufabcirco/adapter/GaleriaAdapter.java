package com.example.ufabcirco.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ufabcirco.R;
import com.example.ufabcirco.model.Post;
import com.example.ufabcirco.ui.FullscreenVideoActivity;
import com.example.ufabcirco.ui.custom.OutlineTextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GaleriaAdapter extends RecyclerView.Adapter<GaleriaAdapter.PostViewHolder> {

    private List<Post> postList;
    private static final String TAG = "GaleriaAdapter";
    private int playingPosition = -1;

    public GaleriaAdapter(List<Post> postList) {
        this.postList = postList;
    }

    public void updatePosts(List<Post> newPostList) {
        this.postList = newPostList != null ? newPostList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public int getPostCount() {
        return postList.size();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_galeria_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        if (postList.isEmpty()) {
            return;
        }
        Post post = postList.get(position % postList.size());
        holder.bind(post);

        if (position == playingPosition) {
            holder.startVideo();
        } else {
            holder.pauseVideo();
        }
    }

    @Override
    public int getItemCount() {
        return postList.isEmpty() ? 0 : Integer.MAX_VALUE;
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull PostViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.pauseVideo();
    }

    public void setPlayingPosition(int position) {
        if (playingPosition != position) {
            int oldPosition = playingPosition;
            playingPosition = position;
            if (oldPosition != -1) {
                notifyItemChanged(oldPosition);
            }
            if (playingPosition != -1) {
                notifyItemChanged(playingPosition);
            }
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private final VideoView videoView;
        private final OutlineTextView textViewTitle;
        private String videoUrl;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.video_view_post);
            textViewTitle = itemView.findViewById(R.id.text_view_post_title);
        }

        public void bind(Post post) {
            textViewTitle.setText(post.getMovimentoNome());
            this.videoUrl = post.getUrl();

            int randomPastelColor = generateRandomPastelColor();
            textViewTitle.setTextColor(randomPastelColor);
            textViewTitle.setOutlineColor(Color.BLACK);

            if (videoUrl != null && !videoUrl.isEmpty()) {
                Uri uri = Uri.parse(videoUrl);
                videoView.setVideoURI(uri);
                setupVideoPlayback();

                videoView.setOnClickListener(v -> {
                    Context context = itemView.getContext();
                    Intent intent = new Intent(context, FullscreenVideoActivity.class);
                    intent.putExtra(FullscreenVideoActivity.EXTRA_VIDEO_URL, videoUrl);
                    context.startActivity(intent);
                });
            } else {
                videoView.setVisibility(View.GONE);
            }
        }

        private void setupVideoPlayback() {
            videoView.setOnPreparedListener(mp -> mp.setLooping(true));
        }

        private int generateRandomPastelColor() {
            Random random = new Random();
            final float hue = random.nextFloat() * 360;
            final float saturation = 0.5f;
            final float lightness = 0.9f;
            return Color.HSVToColor(new float[]{hue, saturation, lightness});
        }

        public void startVideo() {
            if (videoView != null && !videoView.isPlaying()) {
                videoView.start();
            }
        }

        public void pauseVideo() {
            if (videoView != null && videoView.isPlaying()) {
                videoView.pause();
            }
        }
    }
}