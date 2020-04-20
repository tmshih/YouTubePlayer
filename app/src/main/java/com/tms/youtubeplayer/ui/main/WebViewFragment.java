package com.tms.youtubeplayer.ui.main;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.tms.youtubeplayer.R;

/**
 * Abstract:<br/><br/>
 * 1. Youtube IFrame:<br/>
 *   - API https://developers.google.com/youtube/iframe_api_reference <br/>
 *   - Only support 480P.<br/><br/>
 * 2. Auto play survey:<br/>
 *   - https://bugs.chromium.org/p/chromium/issues/detail?id=159336 <br/>
 *   - https://stackoverflow.com/questions/16416935/youtube-iframe-embeds-cannot-autoplay-on-android <br/>
 *   - https://stackoverflow.com/questions/15946183/android-webview-html5-video-autoplay-not-working-on-android-4-0-3 <br/><br/>
 * 3. Auto play workaround: Checking to {@link AutoPlayRunnable}, <br/>
 */
public class WebViewFragment extends Fragment {

    private static final String ARG_VIDEO_ID = "video_id";

    private final String TAG = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    private TextView viewTitle;
    private WebView webView;
    private String videoID;

    public static WebViewFragment newInstance(String videoId) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_VIDEO_ID, videoId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        videoID = "";
        if (getArguments() != null) {
            videoID = getArguments().getString(ARG_VIDEO_ID);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_web_view, container, false);
        viewTitle = root.findViewById(R.id.section_tv_title);
        viewTitle.setText(viewTitle.getText() + videoID);
        webView = root.findViewById(R.id.section_web_view);
        InitRunnable initRunnable = new InitRunnable(webView, videoID);
        webView.postDelayed(initRunnable, 1000L);
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webView.destroy();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.i(TAG, "onPageStarted(): " + url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.i(TAG, "onPageFinished(): " + url);
            super.onPageFinished(view, url);
            AutoPlayRunnable mAutoPlayRunnable = new AutoPlayRunnable(view);
            view.post(mAutoPlayRunnable);
        }
    }

    private class InitRunnable implements Runnable {
        private final WebView webView;
        private final String videoID;

        protected InitRunnable(WebView view, String videoID) {
            this.webView = view;
            this.videoID = videoID;
        }

        @Override
        public void run() {
            Log.v(TAG, "run(): " + webView.getWidth() + "x" + webView.getHeight());
            String data = "<!doctype html>" +
                    "<html>" +
                    "  <body style='margin:0;padding:0;'>" +
                    "    <div id=\"player\"></div>" +
                    "    <script>" +

                    "      var tag = document.createElement('script');" +
                    "      tag.src = \"https://www.youtube.com/iframe_api\";" +
                    "      var firstScriptTag = document.getElementsByTagName('script')[0];" +
                    "      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);" +

                    "      var player;" +
                    "      function onYouTubeIframeAPIReady() {" +
                    "        player = new YT.Player('player', {" +
                    "          width: '" + webView.getWidth() + "'," +
                    "          height: '" + webView.getHeight() + "'," +
                    "          videoId: '" + videoID + "'," +
                    "          events: {" +
                    "            'onReady': onPlayerReady," +
                    "            'onStateChange': onPlayerStateChange" +
                    "          }" +
                    "        });" +
                    "      }" +

                    "      function onPlayerReady(event) {" +
                    "        event.target.playVideo();" +
                    "      }" +

                    "      var done = false;" +
                    "      function onPlayerStateChange(event) {" +
                    "      }" +
                    "      function stopVideo() {" +
                    "        player.stopVideo();" +
                    "      }" +
                    "    </script>" +
                    "  </body>" +
                    "</html>";
            webView.setWebViewClient(new MyWebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            WebSettings webSettings = webView.getSettings();
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setSupportZoom(true);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            webView.loadData(data, "text/html", "utf-8");
        }
    }

    private class AutoPlayRunnable implements Runnable {
        private final WebView webView;

        protected AutoPlayRunnable(WebView view) {
            this.webView = view;
        }

        @Override
        public void run() {
            Log.i(TAG, "run(): Click " + webView + " current=" + System.currentTimeMillis() + " " + SystemClock.uptimeMillis());
            if (webView == null) {return;}
            long downTime = SystemClock.uptimeMillis();
            MotionEvent downEvent = MotionEvent.obtain(
                    downTime,
                    downTime,
                    MotionEvent.ACTION_DOWN,
                    webView.getX() + webView.getWidth() / 2,
                    webView.getY() + webView.getHeight() / 2,
                    0);
            MotionEvent upEvent = MotionEvent.obtain(
                    downTime,
                    downTime,
                    MotionEvent.ACTION_UP,
                    webView.getX() + webView.getWidth() / 2,
                    webView.getY() + webView.getHeight() / 2,
                    0);
            webView.onTouchEvent(downEvent);
            webView.onTouchEvent(upEvent);
            downEvent.recycle();
            upEvent.recycle();
        }
    }
}