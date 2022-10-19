package com.zyfdroid.epub;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.zyfdroid.epub.utils.DBUtils;
import com.zyfdroid.epub.utils.EpubUtils;
import com.zyfdroid.epub.utils.ScreenUtils;
import com.zyfdroid.epub.utils.SpUtils;
import com.zyfdroid.epub.utils.TextUtils;
import com.zyfdroid.epub.utils.ViewUtils;
import com.zyfdroid.epub.views.EinkRecyclerView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ReadingActivity extends AppCompatActivity {

    private static final String TAG = "ReadingActivity";
    Handler hWnd = new Handler();

    DBUtils.BookEntry readingBook;
    Gson JsonConvert = new Gson();
    String bookRootPath = "";
    WebView bookView;
    String internalUrlStatic = "http://epub.zyfdroid.com/static";
    String internalUrlBook = "http://epub.zyfdroid.com/book";
    String dummyScriptUrl = "http://epub.zyfdroid.com/api/reportmode.js";
    String homeUrl = "http://epub.zyfdroid.com/static/index.html";

    String fontUrl = "http://epub.zyfdroid.com/static/defaultFont.ttf";

    TabLayout drawerTab;

    BookSpine[] spines = new BookSpine[0];
    TocEntry[] toc = new TocEntry[0];

    BookmarkAdapter bookmarkAdapter;

    byte[] cachedFont = new byte[0];



    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < menu.size(); i++) {
                MenuItem mi = menu.getItem(i);
                if(mi.getItemId()!=R.id.mnuTempBookmark) {
                    ColorStateList color = (ColorStateList.valueOf(getResources().getColor(R.color.textdaylight)));
                    MenuItemCompat.setIconTintList(mi, color);
                }
                if(mi.getItemId()==R.id.mnuComplete){
                    if(readingBook.getType() == 2){
                        mi.setTitle(R.string.mark_as_not_completed);
                    }
                }
            }
        }

        return super.onMenuOpened(featureId, menu);
    }

    float readActionBarSize = 0;
    View readActionBar = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);
        ScreenUtils.adaptScreens(this);
        if(!SpUtils.getInstance(this).getFullscreen()){
            findViewById(R.id.readingContainer).setPadding(0,0,0,ViewUtils.dip2px(this,16));
        }
        tocHashMap.clear();
        findViewById(R.id.tblStatusBar).setVisibility(SpUtils.getInstance(this).getShowStatusBar() ? View.VISIBLE : View.GONE);


        readActionBarSize = getResources().getDimensionPixelSize(R.dimen.readActionbarHeight);
        readActionBar = findViewById(R.id.readingTitleBar);
        readActionBar.setTranslationY(- readActionBarSize);
        tocList.clear();
        setSupportActionBar((Toolbar) findViewById(R.id.titMain));
        final DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        ActionBarDrawerToggle drwButton = new ActionBarDrawerToggle(this,drwMain,(Toolbar) findViewById(R.id.titMain),R.string.app_name,R.string.app_name);
        if(SpUtils.getInstance(this).getEinkMode()) {
            ((Toolbar) findViewById(R.id.titMain)).setNavigationOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if(drwMain.isDrawerOpen(GravityCompat.START)){
                        drwMain.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    }
                    else{
                        drwMain.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
                    }
                }

            });
        }
        drwMain.addDrawerListener(drwButton);
        drwButton.syncState();
        readingBook = JsonConvert.fromJson(getIntent().getStringExtra("book"), DBUtils.BookEntry.class);
        bookRootPath = new File(EpubUtils.cacheBookPath, readingBook.getUUID()).getAbsolutePath();
        drawerTab = findViewById(R.id.tabMain);
        bookView = findViewById(R.id.webEpub);

        if(getString(R.string.isnightmode).contains("yes")){
            if(SpUtils.getInstance(ReadingActivity.this).getAllowNightMode()){
                bookView.setBackgroundColor(0);
            }
        }

        bookmarkAdapter = new BookmarkAdapter();
        ((RecyclerView) findViewById(R.id.listBookmarks)).setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        ((RecyclerView) findViewById(R.id.listBookmarks)).setAdapter(bookmarkAdapter);
        drawerTab.addTab(drawerTab.newTab().setText(R.string.tab_chapters).setIcon(R.drawable.ic_menu_chapters).setTag(R.id.listChapters));
        drawerTab.addTab(drawerTab.newTab().setText(R.string.tab_saves).setIcon(R.drawable.ic_menu_bookmark).setTag(R.id.listBookmarks));
        drawerTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View v = findViewById((Integer) tab.getTag());
                v.setVisibility(View.VISIBLE);
                if(v instanceof EinkRecyclerView){
                    displayingEinkPage = ((EinkRecyclerView) v);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                findViewById((Integer) tab.getTag()).setVisibility(View.GONE);

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                View v = findViewById((Integer) tab.getTag());
                v.setVisibility(View.VISIBLE);
                if(v instanceof EinkRecyclerView){
                    displayingEinkPage = ((EinkRecyclerView) v);
                }
            }
        });
        File extractFlat = new File(bookRootPath, EpubUtils.FLAG_EXTRACTED);
        if (extractFlat.exists()) {
            initBook();
        } else {
            extractBook(readingBook, bookRootPath);
        }
        setTitle(readingBook.getDisplayName());
        getSupportActionBar().setSubtitle(getString(R.string.load_loading));
        hWnd.postDelayed(loadingLazyShower,200);

            hWnd.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(SpUtils.getInstance(ReadingActivity.this).getEinkMode()) {
                        EinkRecyclerView rv = findViewById(R.id.listChapters);
                        EinkRecyclerView rv2 = findViewById(R.id.listBookmarks);
                        rv.startEinkMode(LinearLayout.VERTICAL, (int) ((float) rv.getHeight() * 0.9f));
                        rv2.startEinkMode(LinearLayout.VERTICAL, (int) ((float) rv.getHeight() * 0.9f));
                        //                                                      ↑ There is no mistakes.
                        //                                               The rv2 is invisible and its height is 0
                        //                                                  so use rv instead.
                        findViewById(R.id.einkDrawerOpener).setVisibility(View.VISIBLE);
                        findViewById(R.id.einkDrawerOpener).setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                if(event.getAction()==MotionEvent.ACTION_UP && event.getX() > v.getWidth()){
                                    drwMain.openDrawer(GravityCompat.START);
                                }
                                return true;
                            }
                        });
                        drwMain.addDrawerListener(einkGestureSwitcher);
                        View closer = findViewById(R.id.einkDrawerCloser);
                        ViewGroup.LayoutParams lp = closer.getLayoutParams();
                        lp.width = drwMain.getWidth() /3;
                        closer.setLayoutParams(lp);
                        closer.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                if(event.getAction()==MotionEvent.ACTION_UP){
                                    drwMain.closeDrawer(GravityCompat.START);
                                }
                                return true;
                            }
                        });
                    }
                    else{
                        drwMain.addDrawerListener(nonEinkGestureSwitcher);
                    }
                    View drvLeft = findViewById(R.id.drwLeft);
                    ViewGroup.LayoutParams lp = drvLeft.getLayoutParams();
                    lp.width = drwMain.getWidth() *2 / 3;
                    drvLeft.setLayoutParams(lp);

                }
            }, 300);
            if(SpUtils.getInstance(this).shouldShowFullscreenHint()){
                Toast.makeText(this, R.string.fullscreen_hint,Toast.LENGTH_LONG).show();
            }
    }

    DrawerLayout.DrawerListener nonEinkGestureSwitcher = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            readActionBar.setTranslationY(-(1-slideOffset) * readActionBarSize);
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            readActionBar.setTranslationY(0);
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            readActionBar.setTranslationY( - readActionBarSize);
        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    };

    DrawerLayout.DrawerListener einkGestureSwitcher = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            findViewById(R.id.einkDrawerCloser).setVisibility(View.VISIBLE);
            if(findViewById(R.id.listChapters).getVisibility() == View.VISIBLE){
                displayingEinkPage = findViewById(R.id.listChapters);
            }
            if(findViewById(R.id.listBookmarks).getVisibility() == View.VISIBLE){
                displayingEinkPage = findViewById(R.id.listBookmarks);
            }
            readActionBar.setTranslationY(0);
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            findViewById(R.id.einkDrawerCloser).setVisibility(View.GONE);
            displayingEinkPage = null;
            readActionBar.setTranslationY( - readActionBarSize);
        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    };




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_read,menu);
        ColorStateList color =(ColorStateList.valueOf(Color.WHITE));
        MenuItemCompat.setIconTintList(menu.findItem(R.id.mnuTempBookmark),color);
        return super.onCreateOptionsMenu(menu);
    }

    private EinkRecyclerView displayingEinkPage = null;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {



        if(isDrawerOpen()){
            if(SpUtils.getInstance(this).getEinkMode()){
                if(displayingEinkPage != null){
                    if(keyCode==KeyEvent.KEYCODE_VOLUME_UP){
                        displayingEinkPage.pageUp();
                        return true;
                    }

                    if(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN){
                        displayingEinkPage.pageDown();
                        return true;
                    }
                }
            }
            return super.onKeyDown(keyCode,event);
        }

        if(keyCode==KeyEvent.KEYCODE_VOLUME_UP){
            evaluteJavascriptFunction("prev");
            return true;
        }

        if(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN){
            evaluteJavascriptFunction("next");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.mnuFontSizes:

                //Base font size = 15;
                final String[] fontsizes = new String[(200-50)/10 + 1];
                for (int i = 0;i< fontsizes.length;i++){
                    fontsizes[i] = ((i+5)*10) + "%";
                }
                new AlertDialog.Builder(this).setItems(fontsizes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String percentStr = fontsizes[which];
                        float percent =Float.parseFloat(percentStr.substring(0,percentStr.length()-1));
                        setFontSize((int)Math.round(15f * percent / 100));
                    }
                }).create().show();
                break;
            case R.id.mnuQuickLoad:
                DBUtils.BookMark ql = DBUtils.quickLoad(this,readingBook.getUUID());
                if(ql.getEpubcft().isEmpty()){
                    snack(getString(R.string.load_empty_save));
                    break;
                }
                navTo(ql.getEpubcft());
                snack(getString(R.string.loaded));
                break;
            case R.id.mnuQuickSave:
                if(!currentProgressCfi.isEmpty()){
                    DBUtils.quickSave(readingBook.getUUID(),currentProgressCfi,currentChapter+"\n"+currentPage);
                    snack(getString(R.string.saved));
                    bookmarkAdapter.update();
                }
                break;
            case R.id.mnuReload:
                setFontSize(SpUtils.getInstance(this).getTextSize());
                break;
            case R.id.mnuTempBookmark:
                if(tempBookmark==null){
                    tempBookmark = currentProgressCfi;
                    item.setIcon(R.drawable.ic_menu_bookmark_lock);
                }
                else{
                    evaluteJavascriptFunction("navTo", tempBookmark);
                    tempBookmark = null;
                    item.setIcon(R.drawable.ic_menu_bookmark_unlock);
                }
                break;
            case R.id.mnuComplete:
                if(readingBook.getType() == 0) {
                    new AlertDialog.Builder(this).setTitle(R.string.mark_as_complete).setMessage(R.string.dlg_complete_msg).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DBUtils.execSql("update library set type=2 where uuid=?", readingBook.getUUID());
                            finish();
                        }
                    }).setNegativeButton(android.R.string.no, null).create().show();
                }else if(readingBook.getType() == 2){
                    readingBook.setType(0);
                    DBUtils.execSql("update library set type=0 where uuid=?", readingBook.getUUID());
                    snack(getString(R.string.success));
                }
                break;
            default:
                Log.w("Unknown menu clicked: ","id="+item.getItemId());
                Log.w("Unknown menu clicked: ","text="+item.getTitle());

        }
        return super.onOptionsItemSelected(item);
    }

    private String tempBookmark = null;

    public void setFontSize(int fontSize){
        Log.w(TAG, "setTextSize: "+fontSize);
        SpUtils.getInstance(this).setTextSize(fontSize);
        final String cfi = currentProgressCfi;
        evaluteJavascriptFunction("setTextSize",fontSize);
        hWnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                navTo(cfi);
            }
        },500);
    }

    private int loadingFlag = -1;

    private Runnable loadingLazyShower = new Runnable() {
        @Override
        public void run() {
            if(loadingFlag>0){loadingFlag--;}
            findViewById(R.id.pbrLoading).setVisibility(loadingFlag == 0 ? View.VISIBLE : View.INVISIBLE);
            hWnd.postDelayed(this,50);
        }
    };


    @Override
    protected void onDestroy() {
        bookView.clearCache(false);
        bookView.destroy();
        hWnd.removeCallbacks(loadingLazyShower);
        cachedFont = null;
        super.onDestroy();
    }

    public void extractBook(DBUtils.BookEntry be, String target) {
        EpubUtils.ExtractBook(this, be, target, new EpubUtils.BookLoadCallback() {
            @Override
            public void onResult(boolean b) {
                if (b) {
                    initBook();
                } else {
                    finish();
                }
            }
        });
    }

    String contentOpfPath = "";

    AssetManager assetManager;

    @SuppressLint("SetJavaScriptEnabled")
    public void initBook() {
        assetManager = getResources().getAssets();

        WebSettings ws = bookView.getSettings();
        ws.setAllowContentAccess(true);
        ws.setAllowFileAccess(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setAppCacheEnabled(true);
        ws.setAppCachePath(getCacheDir()+"/bvc");
        ws.setDatabaseEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setJavaScriptEnabled(true);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setDefaultFontSize(SpUtils.getInstance(this).getTextSize());
        bookView.setWebContentsDebuggingEnabled(true);
        initHtmlCallback();
        bookView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().startsWith(internalUrlStatic)) {
                    return false;
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    startActivity(i);
                    return true;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.toString().startsWith(internalUrlStatic)) {
                    return false;
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

                try {

                    String url = request.getUrl().toString();
                    Log.e("url", url);
                    try {
                        if (url.startsWith(internalUrlStatic)) {
                            String path = url.substring(internalUrlStatic.length());
                            return processStaticResource(path);
                        }
                        if (url.startsWith(internalUrlBook)) {
                            String path = url.substring(internalUrlBook.length());
                            return processBookResource(path);
                        }
                    } catch (Exception ex) {
                    }
                    if (url.endsWith(".ttf")) {
                        //Fix slow load while missing fonts.
                        HashMap<String, String> resp = new HashMap<String, String>();
                        resp.put("Access-Control-Allow-Origin", "*");
                        resp.put("Access-Control-Allow-Methods", "POST,GET,OPTIONS,DELETE");
                        resp.put("Access-Control-Max-Age", "3600");
                        resp.put("Cache-Control","max-age=114514");
                        resp.put("Access-Control-Allow-Headers", "x-requested-with,Authorization");
                        resp.put("Access-Control-Allow-Credentials", "true");
                        InputStream fontInputStream = null;
                        String customFont = SpUtils.getInstance(ReadingActivity.this).getCustomFont();
                        if(TextUtils.isEmpty(customFont)){
                            fontInputStream = assetManager.open("roboto.ttf");
                        }
                        else{
                            fontInputStream = getCustomFontStream(customFont);
                        }

                        WebResourceResponse wr = new WebResourceResponse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl("roboto.ttf")), "UTF-8", 200, "OK", resp, fontInputStream);

                        return wr;
                    }
                } catch (Exception ex) {
                }

                HashMap<String, String> resp = new HashMap<String, String>();
                resp.put("Access-Control-Allow-Origin", "*");
                resp.put("Access-Control-Allow-Methods", "POST,GET,OPTIONS,DELETE");
                resp.put("Access-Control-Max-Age", "3600");
                resp.put("Cache-Control","max-age=114514");
                resp.put("Access-Control-Allow-Headers", "x-requested-with,Authorization");
                resp.put("Access-Control-Allow-Credentials", "true");

                WebResourceResponse wr = new WebResourceResponse("text/html", "UTF-8", 200, "OK", resp, new ByteArrayInputStream("fuck".getBytes()));

                return wr;
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                try {
                    if (url.startsWith(internalUrlStatic)) {
                        String path = url.substring(internalUrlStatic.length());
                        return processStaticResource(path);
                    }
                    if (url.startsWith(internalUrlBook)) {
                        String path = url.substring(internalUrlBook.length());
                        return processBookResource(path);
                    }
                    if(url.startsWith(dummyScriptUrl)){
                        return new WebResourceResponse("text/javascript", "UTF-8", 200, "OK", null, new ByteArrayInputStream(new byte[0]));
                    }
                } catch (Exception ex) {
                }
                return new WebResourceResponse("text/html", "UTF-8", 404, "Not found", null, null);
            }

            public WebResourceResponse processStaticResource(String path) throws IOException {
                MimeTypeMap mmp = MimeTypeMap.getSingleton();
                InputStream is = assetManager.open("epubjs/static" + path, AssetManager.ACCESS_STREAMING);
                String type = mmp.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
                return new WebResourceResponse(type, null, is);
            }

            public WebResourceResponse processBookResource(String path) throws FileNotFoundException {
                MimeTypeMap mmp = MimeTypeMap.getSingleton();
                InputStream is = new FileInputStream(new File(bookRootPath, path));
                String type = mmp.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
                return new WebResourceResponse(type, null, is);
            }
        });
        bookView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage e) {
                String msg = e.message();
                if (msg.startsWith("<::")) {
                    int end = msg.indexOf(':', 3);
                    final String type = (msg.substring(3, end));
                    final String data = (msg.substring(end + 1));
                    if (htmlCallbacks.containsKey(type)) {
                        hWnd.post(new Runnable() {
                            @Override
                            public void run() {
                                if (htmlCallbacks.containsKey(type)) {
                                    htmlCallbacks.get(type).run(data);
                                }
                            }
                        });
                        return true;
                    }
                }
                return super.onConsoleMessage(e);
            }

            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                super.onConsoleMessage(message, lineNumber, sourceID);
            }
        });
        bookView.loadUrl(homeUrl);
    }

    private InputStream getCustomFontStream(String customFont) throws IOException {
        if(cachedFont!=null && cachedFont.length > 0){
            return new ByteArrayInputStream(cachedFont);
        }
        ByteArrayOutputStream byteArrayOutputStream;
        try (InputStream is = new FileInputStream(customFont)) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            int read = 0;
            byte[] buffer = new byte[4096];
            while ((read = is.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, read);
            }
        }
        cachedFont = byteArrayOutputStream.toByteArray();
        return new ByteArrayInputStream(cachedFont);
    }

    String currentChapter = "";
    String currentPage = "";

    HashMap<String, TocEntry> tocHashMap = new HashMap<>();
    HashMap<String, Action<String>> htmlCallbacks = new HashMap<>();

    private String currentProgressCfi="";

    ArrayList<TocEntry> tocList = new ArrayList<>();



    public void initHtmlCallback() {
        htmlCallbacks.put("GET_SAVING", new Action<String>() {
            @Override
            public void run(String arg) {
                currentProgressCfi = arg;
            }
        });

        htmlCallbacks.put("EPUB_BOOK_INIT_START", new Action<String>() {
            @Override
            public void run(String arg) {
                if(getString(R.string.isnightmode).contains("yes")){
                    if(SpUtils.getInstance(ReadingActivity.this).getAllowNightMode()){
                        evaluteJavascriptFunction("setNight()");
                    }
                }
                evaluteJavascriptFunction("loadBookAtUrl", contentOpfPath, DBUtils.autoLoad(ReadingActivity.this,readingBook.getUUID()).getEpubcft(),SpUtils.getInstance(ReadingActivity.this).getTextSize());
            }
        });
        htmlCallbacks.put("EPUB_BOOK_INIT_SUCCESS", new Action<String>() {
            @Override
            public void run(String arg) {
                evaluteJavascriptFunction("reportBookInfo");

            }
        });

        htmlCallbacks.put("EPUBTOC", new Action<String>() {
            @Override
            public void run(String arg) {
                toc = JsonConvert.fromJson(arg, TocEntry[].class);
                enumrateToc(toc);
                ((RecyclerView) findViewById(R.id.listChapters)).setLayoutManager(new LinearLayoutManager(ReadingActivity.this, LinearLayoutManager.VERTICAL, false));
                ((RecyclerView) findViewById(R.id.listChapters)).setAdapter(new TocAdapter());
            }

            int stack = 0;

            void enumrateToc(TocEntry[] root) {
                if(root==null){return;}
                for (TocEntry toc : root) {
                    tocList.add((TocEntry) toc.clone());
                    String tabs = "";
                    for (int i = 0; i < stack; i++) {
                        tabs += "    ";
                    }
                    String str = "";
                    try{
                        tocList.get(tocList.size() - 1).label = tabs + tocList.get(tocList.size() - 1).label.trim();
                        tocHashMap.put(toc.href, toc);
                    }catch (NullPointerException npe){
                        Log.e("Unknown toc","",npe);
                    }
                    stack++;
                    enumrateToc(toc.subitems);
                    stack--;
                }
            }
        });

        htmlCallbacks.put("EPUBSPINE", new Action<String>() {
            @Override
            public void run(String arg) {
                spines = JsonConvert.fromJson(arg, BookSpine[].class);
            }
        });

        htmlCallbacks.put("REPORT_LOCATION", new Action<String>() {
            @Override
            public void run(String arg) {
                currentPage = arg;
                displayPageInfo();
            }
        });


        htmlCallbacks.put("REPORT_CHAPTER", new Action<String>() {
            @Override
            public void run(String arg) {
                currentChapter = arg;
                displayPageInfo();
            }
        });
        htmlCallbacks.put("SHOW_PROGRESS", new Action<String>() {
            @Override
            public void run(String arg) {
                loadingFlag = arg.equals("1") ? 10 : -1;
            }
        });

        htmlCallbacks.put("AUTO_SAVE_REQUIRE", new Action<String>() {
            @Override
            public void run(String arg) {
                DBUtils.autoSave(readingBook.getUUID(), arg, currentChapter + "\n" + currentPage);
            }
        });

        htmlCallbacks.put("CENTER_CLICKED", new Action<String>() {
            long lastTime = -1;
            @Override
            public void run(String arg) {
                if(System.currentTimeMillis() - lastTime < 800){
                    ((DrawerLayout)findViewById(R.id.drwMain)).openDrawer(GravityCompat.START);
                }
                lastTime = System.currentTimeMillis();
            }
        });
    }
    void displayPageInfo() {
        getSupportActionBar().setSubtitle("[" + currentPage + "] " + currentChapter);
        ((TextView)findViewById(R.id.txtChapterInfo2)).setText(currentPage + " " + currentChapter);
    }

    public void evaluteJavascriptFunction(String functionName, Object... arguments) {
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append(functionName).append("(");
        for (int i = 0; i < arguments.length; i++) {
            Object obj = arguments[i];
            if (obj == null) {
                scriptBuilder.append("null");
            } else if (obj instanceof String) {
                scriptBuilder.append(TextUtils.escapeText((String) obj));
            } else if (obj instanceof Boolean) {
                scriptBuilder.append(obj.toString().toLowerCase());
            } else {
                scriptBuilder.append(obj.toString());
            }
            if (i != arguments.length - 1) {
                scriptBuilder.append(",");
            }
        }
        scriptBuilder.append(");");
        bookView.evaluateJavascript(scriptBuilder.toString(), null);
    }



    class TocAdapter extends RecyclerView.Adapter<TocAdapter.TocHolder> {


        @NonNull
        @Override
        public TocHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new TocHolder(getLayoutInflater().inflate(R.layout.adapter_toc, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull TocHolder tocHolder, int i) {
            tocHolder.tv.setText(tocList.get(i).label);
            tocHolder.btn.setTag(tocList.get(i));
            tocHolder.btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TocEntry toc = (TocEntry) view.getTag();
                    evaluteJavascriptFunction("navTo", toc.href);
                    closeDrawer();
                }
            });
        }

        @Override
        public int getItemCount() {
            return tocList.size();
        }

        class TocHolder extends RecyclerView.ViewHolder {
            TextView tv;
            View root;
            ImageButton btn;

            public TocHolder(@NonNull View itemView) {
                super(itemView);
                root = itemView;
                tv = itemView.findViewById(R.id.txtListItem);
                btn = itemView.findViewById(R.id.btnGo);
            }
        }
    }

    class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder> {
        List<DBUtils.BookMark> bookmarls;

        public BookmarkAdapter() {
            bookmarls = DBUtils.queryBookmarks(ReadingActivity.this,readingBook.getUUID());
        }

        public void update() {
            bookmarls = DBUtils.queryBookmarks(ReadingActivity.this,readingBook.getUUID());
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new BookmarkViewHolder(getLayoutInflater().inflate(R.layout.adapter_bookmark, viewGroup, false));
        }

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull BookmarkViewHolder bh, int i) {
            DBUtils.BookMark bm = bookmarls.get(i);
            if (bm.getSaveTime() < 0) {
                bh.txtTime.setVisibility(View.INVISIBLE);
            } else {
                bh.txtTime.setVisibility(View.VISIBLE);
                if(bm.getSlot()==0){
                    bh.txtTime.setText(getString(R.string.save_auto)+sdf.format(new Date(bm.getSaveTime())));}else
                if(bm.getSlot()==1){
                    bh.txtTime.setText(getString(R.string.save_quick)+sdf.format(new Date(bm.getSaveTime())));}else
                bh.txtTime.setText(String.format(getString(R.string.save_normal), bm.getSlot() - 1, sdf.format(new Date(bm.getSaveTime()))));
            }
            bh.txtTitle.setText(bm.getName());
            bh.btnSave.setTag(bm);
            bh.btnLoad.setTag(bm);
            bh.btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DBUtils.BookMark bm = (DBUtils.BookMark) view.getTag();
                    if (!currentProgressCfi.isEmpty()) {
                        DBUtils.setBookmark(readingBook.getUUID(), bm.getSlot(), currentChapter + "\n" + currentPage, currentProgressCfi);
                        //closeDrawer();
                        snack(getString(R.string.saved));
                        update();
                    }
                }
            });

            bh.btnLoad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DBUtils.BookMark bm = (DBUtils.BookMark) view.getTag();
                    if (!bm.getEpubcft().isEmpty()) {
                        evaluteJavascriptFunction("navTo", bm.getEpubcft());
                        closeDrawer();
                        snack(getString(R.string.loaded));
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return bookmarls.size();
        }


        class BookmarkViewHolder extends RecyclerView.ViewHolder {
            Button btnLoad;
            Button btnSave;
            TextView txtTime;
            TextView txtTitle;

            public BookmarkViewHolder(View itemView) {
                super(itemView);
                btnLoad = itemView.findViewById(R.id.btnBookmarkLoad);
                btnSave = itemView.findViewById(R.id.btnBookmarkSave);
                txtTime = itemView.findViewById(R.id.txtBookmarkTime);
                txtTitle = itemView.findViewById(R.id.txtBookmarkTitle);
            }
        }

    }

    @SuppressLint("WrongConstant")
    private void closeDrawer() {
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        drwMain.closeDrawer(Gravity.START);
    }

    @SuppressLint("WrongConstant")
    private boolean isDrawerOpen() {
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        return drwMain.isDrawerOpen(Gravity.START);
    }

    public void snack(String str) {
        Snackbar.make(findViewById(R.id.readingRootView), str, 1500).show();
    }

    public void navTo(String cfi){
        if(!cfi.isEmpty()){
            evaluteJavascriptFunction("navTo", cfi);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer();
            return;
        }
        if (!currentProgressCfi.isEmpty()) {
            DBUtils.autoSave(readingBook.getUUID(), currentProgressCfi, currentChapter + "\n" + currentPage);
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bookView.onResume();
        if(null!=bookmarkAdapter){bookmarkAdapter.update();}
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!currentProgressCfi.isEmpty()) {
            DBUtils.autoSave(readingBook.getUUID(), currentProgressCfi, currentChapter + "\n" + currentPage);

        }
        bookView.onPause();
    }
}


class TocEntry implements Cloneable {
    public String id;
    public String href;
    public String label;
    public TocEntry[] subitems;

    @Override
    protected Object clone() {
        TocEntry obj = new TocEntry();
        obj.href = href;
        obj.id = id;
        obj.label = label;
        obj.subitems = null;
        return obj;
    }
}

class BookSpine {
    public String idref;
    public int index;
    public String href;
}

interface Action<T> {
    public void run(T arg);
}