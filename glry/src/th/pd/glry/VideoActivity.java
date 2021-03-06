package th.pd.glry;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import cc.typedef.droid.common.FormatUtil;

public class VideoActivity extends AbsMediaActivity {

    private static final int STARTING_DELAY = 1000;

    private AudioManager mAudioManager;

    private VideoPlayer mPlayer;

    private VideoPlayer.Callback mCallback = new VideoPlayer.Callback() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            finish();
            return true;
        }

        @Override
        public boolean onAction(int actionId, int extra) {
            switch (actionId) {
                case ACTION_PLAY:
                    // TODO set button to 'playing' state
                    return true;
                case ACTION_PAUSE:
                    // TODO set button to 'paused' state
                    return true;
                case ACTION_UPDATE_PROGRESS: {
                    int progress = extra;
                    SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
                    TextView tvTime = (TextView) findViewById(R.id.tvTime);
                    seekBar.setProgress(progress);
                    tvTime.setText(FormatUtil
                            .formatTimespan(progress / 1000));
                    return true;
                }
                case ACTION_UPDATE_TOTAL_PROGRESS: {
                    int total = extra;
                    SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
                    TextView tvTimeTotal = (TextView) findViewById(R.id.tvTimeTotal);
                    if (total > 0) {
                        seekBar.setVisibility(View.VISIBLE);
                        seekBar.setMax(total);
                        tvTimeTotal.setText(FormatUtil
                                .formatTimespan((total + 200) / 1000));
                    } else {
                        seekBar.setVisibility(View.INVISIBLE);
                    }
                    return true;
                }
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Uri videoUri = getIntent().getData();
        if (videoUri == null) {
            finish();
        }

        onCreate(savedInstanceState, R.layout.video_main);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        setupPlayer(videoUri);
    }

    @Override
    protected void onStart() {
        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAudioManager.abandonAudioFocus(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.onPause();
    }

    private void setupPlayer(Uri videoUri) {
        mPlayer = new VideoPlayer((VideoView) findViewById(R.id.videoView),
                mCallback);
        mPlayer.setVideoUri(videoUri);
        // TODO play after animation done
        mPlayer.playDelayed(STARTING_DELAY);

        findViewById(R.id.btnPlayPause).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mPlayer.togglePlayPause();
                    }
                });

        findViewById(R.id.btnStop).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mPlayer.stop();
                    }
                });

        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(
                new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                            int progress, boolean fromUser) {
                        if (fromUser) {
                            mPlayer.seekTo(progress);
                        }
                    }
                });
    }
}
