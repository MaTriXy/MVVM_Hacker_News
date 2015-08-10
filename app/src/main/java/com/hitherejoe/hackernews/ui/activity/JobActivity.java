package com.hitherejoe.hackernews.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hitherejoe.hackernews.BuildConfig;
import com.hitherejoe.hackernews.HackerNewsApplication;
import com.hitherejoe.hackernews.R;
import com.hitherejoe.hackernews.data.DataManager;
import com.hitherejoe.hackernews.data.model.Post;
import com.hitherejoe.hackernews.data.remote.AnalyticsHelper;
import com.hitherejoe.hackernews.util.DataUtils;
import com.hitherejoe.hackernews.util.ToastFactory;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;

public class JobActivity extends BaseActivity {

    @InjectView(R.id.text_job)
    TextView mJobText;

    private static final String TAG = "JobActivity";
    public static final String EXTRA_POST =
            "com.hitherejoe.HackerNews.ui.activity.JobActivity.EXTRA_POST";
    private static final String URL_PLAY_STORE =
            "https://play.google.com/store/apps/details?id=com.hitherejoe.hackernews&hl=en_GB";
    private Post mPost;
    private DataManager mDataManager;
    private List<Subscription> mSubscriptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job);
        ButterKnife.inject(this);
        Bundle bundle = getIntent().getExtras();
        mPost = bundle.getParcelable(EXTRA_POST);
        mDataManager = HackerNewsApplication.get().getDataManager();
        mSubscriptions = new ArrayList<>();
        setupActionBar();
        setupJobDetails();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Subscription subscription : mSubscriptions) subscription.unsubscribe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_story, menu);
        setupShareActionProvider(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_browser:
                if (!BuildConfig.DEBUG) AnalyticsHelper.trackViewStoryInBrowserMenuItemClicked();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mPost.url)));
                return true;
            case R.id.action_bookmark:
                addBookmark();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mPost.title);
        }
    }

    private void setupJobDetails() {
        //if (mPost.text != null) {
            mJobText.setText(mPost.text);
        //}
    }

    private void setupShareActionProvider(Menu menu) {
        ShareActionProvider shareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.menu_item_share));
        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(getShareIntent());
            shareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {
                @Override
                public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
                    if (!BuildConfig.DEBUG) AnalyticsHelper.trackStoryShared(intent.getComponent().getPackageName());
                    return false;
                }
            });
        }
    }

    private Intent getShareIntent() {
        String shareText = mPost.title + " " + getString(R.string.seperator_name_points)
                + " " + mPost.url + " " + getString(R.string.via) + " " + URL_PLAY_STORE;
        return new Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, shareText);
    }

    private void addBookmark() {
        mSubscriptions.add(AppObservable.bindActivity(this,
                mDataManager.addBookmark(mPost))
                .subscribeOn(mDataManager.getScheduler())
                .subscribe(new Observer<Post>() {

                    private Post bookmarkResult;

                    @Override
                    public void onCompleted() {
                        ToastFactory.createToast(
                                JobActivity.this,
                                bookmarkResult == null ? getString(R.string.bookmark_exists) : getString(R.string.bookmark_added)
                        ).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "There was an error bookmarking the story " + e);
                        ToastFactory.createToast(
                                JobActivity.this,
                                getString(R.string.bookmark_error)
                        ).show();
                    }

                    @Override
                    public void onNext(Post story) {
                        bookmarkResult = story;
                    }
                }));
    }

}