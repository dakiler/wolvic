package com.igalia.wolvic.browser.api.impl;

import android.app.Activity;
import android.graphics.Matrix;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igalia.wolvic.browser.SettingsStore;
import com.igalia.wolvic.browser.api.WContentBlocking;
import com.igalia.wolvic.browser.api.WDisplay;
import com.igalia.wolvic.browser.api.WMediaSession;
import com.igalia.wolvic.browser.api.WPanZoomController;
import com.igalia.wolvic.browser.api.WRuntime;
import com.igalia.wolvic.browser.api.WSession;
import com.igalia.wolvic.browser.api.WSessionSettings;
import com.igalia.wolvic.browser.api.WSessionState;
import com.igalia.wolvic.browser.api.WTextInput;

import org.chromium.base.ApplicationStatus;
import org.chromium.components.embedder_support.view.ContentView;
import org.chromium.components.embedder_support.view.WolvicContentRenderView;
import org.chromium.components.url_formatter.UrlFormatter;
import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.content_public.browser.NavigationController;
import org.chromium.content_public.browser.WebContents;
import org.chromium.ui.base.ActivityWindowAndroid;
import org.chromium.ui.base.IntentRequestTracker;
import org.chromium.ui.base.ViewAndroidDelegate;

public class SessionImpl implements WSession {
    RuntimeImpl mRuntime;
    SettingsImpl mSettings;
    ContentDelegate mContentDelegate;
    ProgressDelegate mProgressDelegate;
    PermissionDelegate mPermissionDelegate;
    NavigationDelegate mNavigationDelegate;
    ScrollDelegate mScrollDelegate;
    HistoryDelegate mHistoryDelegate;
    WContentBlocking.Delegate mContentBlockingDelegate;
    PromptDelegate mPromptDelegate;
    SelectionActionDelegate mSelectionActionDelegate;
    WMediaSession.Delegate mMediaSessionDelegate;
    DisplayImpl mDisplay;
    TextInputImpl mTextInput;
    PanZoomControllerImpl mPanZoomController;
    private ActivityWindowAndroid mWindowAndroid;
    private IntentRequestTracker mIntentRequestTracker;
    private TabWebContentsObserver mTabWebContentsObserver;
    private WolvicContentRenderView mContentViewRenderView;
    private ContentView mCurrentContentView;
    private NavigationController mNavigationController = null;
    private boolean mIsDisplayAcquired = false;
    private String mInitialUri = null;

    public SessionImpl(@Nullable WSessionSettings settings) {
        mSettings = settings != null ? (SettingsImpl) settings : new SettingsImpl(false);
        init();
    }

    private void init() {
        mTextInput = new TextInputImpl(this);
        mPanZoomController = new PanZoomControllerImpl(this);
    }

    // TODO: Consider refactoring this method to move more appropriate place.
    private BrowserDisplay createBrowserDisplay() {
        Activity activity = ApplicationStatus.getLastTrackedFocusedActivity();

        mIntentRequestTracker = IntentRequestTracker.createFromActivity(activity);
        mWindowAndroid = new ActivityWindowAndroid(activity, false, mIntentRequestTracker);

        mContentViewRenderView = new WolvicContentRenderView(mRuntime.getContext());
        mContentViewRenderView.onNativeLibraryLoaded(mWindowAndroid);

        WebContents webContents = mRuntime.createWebContents();
        ContentView cv = ContentView.createContentView(
                activity, null /* eventOffsetHandler */, webContents);
        webContents.initialize(
                "", ViewAndroidDelegate.createBasicDelegate(cv), cv,
                mWindowAndroid, WebContents.createDefaultInternalsHolder());
        mCurrentContentView = cv;

        mWindowAndroid.setAnimationPlaceholderView(mContentViewRenderView);

        BrowserDisplay browserDisplay = new BrowserDisplay(mRuntime.getContext());

        ContentShellFragment fragment = new ContentShellFragment();
        fragment.setContentViewRenderView(mContentViewRenderView);
        browserDisplay.attach(mRuntime.getFragmentManager(), mRuntime.getViewContainer(), fragment);

        mContentViewRenderView.setCurrentWebContents(webContents);
        webContents.onShow();
        return browserDisplay;
    }

    private void registerCallbacks() {
        mTabWebContentsObserver = new TabWebContentsObserver(getCurrentWebContents(), this);
    }

    private void unRegisterCallbacks() {
        mTabWebContentsObserver = null;
    }

    @Override
    public void loadUri(@NonNull String uri, int flags) {
        // if loadUri() is called before the display has been acquired, save the uri and load it
        // later
        if (!mIsDisplayAcquired) {
            mInitialUri = uri;
            return;
        }

        LoadUrlParams params = new LoadUrlParams(UrlFormatter.fixupUrl(uri).getPossiblyInvalidSpec());
        mNavigationController.loadUrl(params);
    }

    private void loadInitialUri() {
        if (mInitialUri == null) {
            // if no initial uri has been provided, load the homepage
            mInitialUri = SettingsStore.getInstance(mRuntime.getContext()).getHomepage();
        }
        loadUri(mInitialUri);
        mInitialUri = null;
    }

    @Override
    public void loadData(@NonNull byte[] data, String mymeType) {
        // TODO: Implement
    }

    @Override
    public void reload(int flags) {
        mNavigationController.reload(true);
    }

    @Override
    public void stop() {
        // TODO: Implement
    }

    @Override
    public void setActive(boolean active) {
        // TODO: Implement
    }

    @Override
    public void setFocused(boolean focused) {
        // TODO: Implement
    }

    @Override
    public void open(@NonNull WRuntime runtime) {
        mRuntime = (RuntimeImpl) runtime;
    }

    @Override
    public boolean isOpen() {
        // TODO: Implement
        return false;
    }

    @Override
    public void close() {
        // TODO: Implement
    }

    @Override
    public void goBack(boolean userInteraction) {
        mNavigationController.goBack();
    }

    @Override
    public void goForward(boolean userInteraction) {
        mNavigationController.goForward();
    }

    @Override
    public void gotoHistoryIndex(int index) {
        // TODO: Implement
    }

    @Override
    public void purgeHistory() {
        // TODO: Implement
    }

    @NonNull
    @Override
    public WSessionSettings getSettings() {
        return mSettings;
    }

    @NonNull
    @Override
    public String getDefaultUserAgent(int mode) {
        // TODO: implement
        return "";
    }

    @Override
    public void exitFullScreen() {
        // TODO: implement
    }

    @NonNull
    @Override
    public WDisplay acquireDisplay() {
        assert mDisplay == null;
        mDisplay = new DisplayImpl(createBrowserDisplay(), this, mContentViewRenderView);
        registerCallbacks();
        getTextInput().setView(getContentView());
        mNavigationController = getCurrentWebContents().getNavigationController();
        mIsDisplayAcquired = true;
        loadInitialUri();
        return mDisplay;
    }

    @Override
    public void releaseDisplay(@NonNull WDisplay display) {
        assert mDisplay != null;
        unRegisterCallbacks();
        mDisplay = null;
        getTextInput().setView(null);
    }

    @Override
    public void restoreState(@NonNull WSessionState state) {

    }

    @Override
    public void getClientToSurfaceMatrix(@NonNull Matrix matrix) {

    }

    @Override
    public void getClientToScreenMatrix(@NonNull Matrix matrix) {

    }

    @Override
    public void getPageToScreenMatrix(@NonNull Matrix matrix) {

    }

    @Override
    public void getPageToSurfaceMatrix(@NonNull Matrix matrix) {

    }

    @Override
    public void dispatchLocation(double latitude, double longitude, double altitude, float accuracy, float altitudeAccuracy, float heading, float speed, float time) {

    }

    @NonNull
    @Override
    public WTextInput getTextInput() {
        return mTextInput;
    }

    @NonNull
    @Override
    public WPanZoomController getPanZoomController() {
        return mPanZoomController;
    }

    @Override
    public void setContentDelegate(@Nullable ContentDelegate delegate) {
        // TODO: Implement bridge
        mContentDelegate = delegate;
    }

    @Nullable
    @Override
    public ContentDelegate getContentDelegate() {
        return mContentDelegate;
    }

    @Override
    public void setPermissionDelegate(@Nullable PermissionDelegate delegate) {
        // TODO: Implement bridge
        mPermissionDelegate = delegate;
    }

    @Nullable
    @Override
    public PermissionDelegate getPermissionDelegate() {
        return mPermissionDelegate;
    }

    @Override
    public void setProgressDelegate(@Nullable ProgressDelegate delegate) {
        // TODO: Implement bridge
        mProgressDelegate = delegate;
    }

    @Nullable
    @Override
    public ProgressDelegate getProgressDelegate() {
        return mProgressDelegate;
    }

    @Override
    public void setNavigationDelegate(@Nullable NavigationDelegate delegate) {
        // TODO: Implement bridge
        mNavigationDelegate = delegate;
    }

    @Nullable
    @Override
    public NavigationDelegate getNavigationDelegate() {
        return mNavigationDelegate;
    }

    @Override
    public void setScrollDelegate(@Nullable ScrollDelegate delegate) {
        // TODO: Implement bridge
        mScrollDelegate = delegate;
    }

    @Nullable
    @Override
    public ScrollDelegate getScrollDelegate() {
        return mScrollDelegate;
    }

    @Override
    public void setHistoryDelegate(@Nullable HistoryDelegate delegate) {
        // TODO: Implement bridge
        mHistoryDelegate = delegate;
    }

    @Nullable
    @Override
    public HistoryDelegate getHistoryDelegate() {
        return mHistoryDelegate;
    }

    @Override
    public void setContentBlockingDelegate(@Nullable WContentBlocking.Delegate delegate) {
        // TODO: Implement bridge
        mContentBlockingDelegate = delegate;
    }

    @Nullable
    @Override
    public WContentBlocking.Delegate getContentBlockingDelegate() {
        return mContentBlockingDelegate;
    }

    @Override
    public void setPromptDelegate(@Nullable PromptDelegate delegate) {
        // TODO: Implement bridge
        mPromptDelegate = delegate;
    }

    @Nullable
    @Override
    public PromptDelegate getPromptDelegate() {
        return mPromptDelegate;
    }

    @Override
    public void setSelectionActionDelegate(@Nullable SelectionActionDelegate delegate) {
        // TODO: Implement bridge
        mSelectionActionDelegate = delegate;
    }

    @Override
    public void setMediaSessionDelegate(@Nullable WMediaSession.Delegate delegate) {
        // TODO: Implement bridge
        mMediaSessionDelegate = delegate;
    }

    @Nullable
    @Override
    public WMediaSession.Delegate getMediaSessionDelegate() {
        return mMediaSessionDelegate;
    }

    @Nullable
    @Override
    public SelectionActionDelegate getSelectionActionDelegate() {
        return mSelectionActionDelegate;
    }

    @NonNull
    public WebContents getCurrentWebContents() {
        return mContentViewRenderView.getCurrentWebContents();
    }

    @NonNull
    public ViewGroup getContentView() {
        return mCurrentContentView;
    }

}
