package com.zyfdroid.epub;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.TextView;

import com.google.gson.Gson;
import com.zyfdroid.epub.utils.DBUtils;
import com.zyfdroid.epub.utils.EpubUtils;
import com.zyfdroid.epub.utils.SpUtils;
import com.zyfdroid.epub.utils.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
    String homeUrl = "http://epub.zyfdroid.com/static/index.html";

    TabLayout drawerTab;

    BookSpine[] spines = new BookSpine[0];
    TocEntry[] toc = new TocEntry[0];

    BookmarkAdapter bookmarkAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);
        tocHashMap.clear();
        ;
        tocList.clear();
        setSupportActionBar((Toolbar) findViewById(R.id.titMain));
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        ActionBarDrawerToggle drwButton = new ActionBarDrawerToggle(this, drwMain, (Toolbar) findViewById(R.id.titMain), R.string.app_name, R.string.app_name);
        drwMain.setDrawerListener(drwButton);
        drwButton.syncState();
        readingBook = JsonConvert.fromJson(getIntent().getStringExtra("book"), DBUtils.BookEntry.class);
        bookRootPath = new File(EpubUtils.cacheBookPath, readingBook.getUUID()).getAbsolutePath();
        drawerTab = findViewById(R.id.tabMain);
        bookView = findViewById(R.id.webEpub);
        bookmarkAdapter = new BookmarkAdapter();
        ((RecyclerView) findViewById(R.id.listBookmarks)).setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        ((RecyclerView) findViewById(R.id.listBookmarks)).setAdapter(bookmarkAdapter);
        drawerTab.addTab(drawerTab.newTab().setText(R.string.tab_chapters).setIcon(R.drawable.ic_menu_chapters).setTag(R.id.listChapters));
        drawerTab.addTab(drawerTab.newTab().setText(R.string.tab_saves).setIcon(R.drawable.ic_menu_bookmark).setTag(R.id.listBookmarks));
        drawerTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                findViewById((Integer) tab.getTag()).setVisibility(View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                findViewById((Integer) tab.getTag()).setVisibility(View.GONE);

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                findViewById((Integer) tab.getTag()).setVisibility(View.VISIBLE);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_read,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(isDrawerOpen()){
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
                new AlertDialog.Builder(this).setItems(new String[]{"100%", "125%", "150%", "175%", "200%"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setFontSize(15 + 4 * which);
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
                bookView.setPadding(10,10,10,10);
                hWnd.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bookView.setPadding(0,0,0,0);
                    }
                },300);
                break;
            default:
                Log.w("Unknown menu clicked: ","id="+item.getItemId());
                Log.w("Unknown menu clicked: ","text="+item.getTitle());

        }
        return super.onOptionsItemSelected(item);
    }

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

    @Override
    protected void onDestroy() {
        bookView.destroy();
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
        ws.setDatabaseEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
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
                        resp.put("Access-Control-Allow-Headers", "x-requested-with,Authorization");
                        resp.put("Access-Control-Allow-Credentials", "true");
                        WebResourceResponse wr = new WebResourceResponse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl("roboto.ttf")), "UTF-8", 200, "OK", resp, assetManager.open("roboto.ttf"));

                        return wr;
                    }
                } catch (Exception ex) {
                }

                HashMap<String, String> resp = new HashMap<String, String>();
                resp.put("Access-Control-Allow-Origin", "*");
                resp.put("Access-Control-Allow-Methods", "POST,GET,OPTIONS,DELETE");
                resp.put("Access-Control-Max-Age", "3600");
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

    }
    void displayPageInfo() {
        getSupportActionBar().setSubtitle("[" + currentPage + "] " + currentChapter);
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
                    evaluteJavascriptFunction("renderH.display", toc.href);
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
                        evaluteJavascriptFunction("renderH.display", bm.getEpubcft());
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

    private void closeDrawer() {
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        drwMain.closeDrawer(Gravity.START);
    }

    private boolean isDrawerOpen() {
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        return drwMain.isDrawerOpen(Gravity.START);
    }

    public void snack(String str) {
        Snackbar.make(findViewById(R.id.readingRootView), str, 1500).show();
    }

    public void navTo(String cfi){
        if(!cfi.isEmpty()){
            evaluteJavascriptFunction("renderH.display", cfi);
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