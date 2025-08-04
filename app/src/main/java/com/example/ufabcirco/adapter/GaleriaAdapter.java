package com.example.ufabcirco.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    public GaleriaAdapter(List<Post> postList) {
        this.postList = postList;
    }

    public void updatePosts(List<Post> newPostList) {
        this.postList = newPostList != null ? newPostList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_galeria_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull PostViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.startVideo();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull PostViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.pauseVideo();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private final VideoView videoView;
        private final TextView textViewTags;
        private final OutlineTextView textViewTitle;
        private String videoUrl;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.video_view_post);
            textViewTags = itemView.findViewById(R.id.text_view_post_tags);
            textViewTitle = itemView.findViewById(R.id.text_view_post_title);
        }

        public void bind(Post post) {
            textViewTags.setText(post.getTags().toString());
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
            }
        }

        private void setupVideoPlayback() {
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.start();
            });
            videoView.start();
        }

        private int generateRandomPastelColor() {
            Random random = new Random();
            final float hue = random.nextFloat() * 360;
            final float saturation = 0.5f;
            final float lightness = 0.9f;
            return Color.HSVToColor(new float[]{hue, saturation, lightness});
        }

        public void startVideo() {
            if (videoView != null && videoUrl != null && !videoUrl.isEmpty()) {
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