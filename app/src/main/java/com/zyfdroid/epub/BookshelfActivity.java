package com.zyfdroid.epub;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.StrictMode;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.zyfdroid.epub.utils.BookScanner;
import com.zyfdroid.epub.utils.DBUtils;
import com.zyfdroid.epub.utils.EpubUtils;
import com.zyfdroid.epub.utils.SpUtils;
import com.zyfdroid.epub.utils.TextUtils;
import com.zyfdroid.epub.utils.ViewUtils;
import com.zyfdroid.epub.views.EinkRecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BookshelfActivity extends AppCompatActivity {
    Gson JsonConvert = new Gson();
    EinkRecyclerView navMain;
    Picasso mCoverLoader;
    Handler hWnd = new Handler();
    DrawerLayout drwMain;
    ActionBarDrawerToggle drwButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);
        mCoverLoader = new Picasso.Builder(this).downloader(new BookCoverDownloader()).indicatorsEnabled(false).build();

        setSupportActionBar((Toolbar) findViewById(R.id.titMain));
        drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        drwButton = new ActionBarDrawerToggle(this,drwMain,(Toolbar) findViewById(R.id.titMain),R.string.app_name,R.string.app_name);
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
        navMain = (EinkRecyclerView) findViewById(R.id.navMain);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        navMain.setLayoutManager(linearLayoutManager);
        folderAdapter = new FolderDrawerAdapter(new ArrayList<>());
        navMain.setAdapter(folderAdapter);

        hWnd.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(SpUtils.getInstance(BookshelfActivity.this).isDesktopEmpty()){

                    loadData();
                }
                else{
                    loadMenuRange(DBUtils.queryFoldersNotEmpty().toArray(new DBUtils.BookEntry[0]));
                    loadDesktop(null);
                }
            }
        },300);
        hWnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(SpUtils.getInstance(BookshelfActivity.this).getEinkMode()){
                    EinkRecyclerView rv = (EinkRecyclerView) findViewById(R.id.listBooks);
                    rv.startEinkMode(LinearLayout.HORIZONTAL,rv.getWidth());
                    navMain.startEinkMode(LinearLayout.VERTICAL, (int) ((float)navMain.getHeight() * 0.9f));
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
                ViewGroup.LayoutParams lp = navMain.getLayoutParams();
                lp.width = drwMain.getWidth() * 2 / 3;
                navMain.setLayoutParams(lp);
                displayingEinkPage = findViewById(R.id.listBooks);
                drwMain.closeDrawer(GravityCompat.START);
            }
        },301);
    }


    DrawerLayout.DrawerListener einkGestureSwitcher = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            findViewById(R.id.einkDrawerCloser).setVisibility(View.VISIBLE);
            displayingEinkPage = findViewById(R.id.navMain);
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            findViewById(R.id.einkDrawerCloser).setVisibility(View.GONE);
            displayingEinkPage = findViewById(R.id.listBooks);
        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    };

    public void loadData(){
        setTitle(R.string.all_books);
        isAllBook = true;
        loadMenuRange(DBUtils.queryFoldersNotEmpty().toArray(new DBUtils.BookEntry[0]));

        loadBooksList(getRecommandBook(null));
    }

    private List<DBUtils.BookEntry> getRecommandBook(String query,String... param){
        String sql = query == null ? "" : query;
        if(sql.length() > 0){
            sql = sql+" and ";
        }
        List<DBUtils.BookEntry> notFinishedRead = DBUtils.queryBooks(sql+" type=0 order by lastopen desc",param);
        List<DBUtils.BookEntry> finishedRead = DBUtils.queryBooks(sql+" type = 2 order by lastopen desc",param);
        List<DBUtils.BookEntry> outList = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            if(notFinishedRead.size() > 0) {
                outList.add(notFinishedRead.remove(0));
            }
        }
        for (int i = 0; i < 3; i++) {
            if(notFinishedRead.size() > 0){
                if(finishedRead.size()>0){
                    if(finishedRead.get(0).getLastOpenTime() >= notFinishedRead.get(0).getLastOpenTime()){
                        outList.add(finishedRead.remove(0));
                    }
                    else{
                        outList.add(notFinishedRead.remove(0));
                    }
                }
            }
        }
        outList.addAll(notFinishedRead);
        outList.addAll(finishedRead);

        return outList;
    }

    private MenuItem _mnuSearchButton;
    private MenuItem _mnuAllBookButton;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            SearchView actionView = new SearchView(new ContextThemeWrapper(this,R.style.Theme_AppCompat));
            actionView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    doSearching(s);
                    return false;
                }
            });

            actionView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    loadData();
                    return false;
                }
            });

            _mnuSearchButton = menu.add(R.string.bookshelf_search).setIcon(R.drawable.ic_menu_searchinlibrary).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setActionView(actionView);
            _mnuAllBookButton = menu.add(R.string.all_books).setVisible(false).setIcon(R.drawable.ic_menu_bookmark_light).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    loadAll(null);
                    return true;
                }
            });
            menu.add(R.string.bookshelf_scan_for_books).setIcon(R.drawable.ic_menu_rescan).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    doRescan();
                    return true;
                }
            });
        return super.onCreateOptionsMenu(menu);
    }


    public void doRescan(){
        scanDefaultPath();
    }

    public void doSearching(String text){
        if(isAllBook){
            List<DBUtils.BookEntry> lsResult = DBUtils.queryBooks("(type=0 or type=2) and display_name like ?  order by lastopen desc","%"+text+"%");
            loadBooksList(lsResult);
        }
        else{

            List<DBUtils.BookEntry> lsResult = DBUtils.queryBooks("parent_uuid=? and (type=0 or type=2) and display_name like ?  order by lastopen desc",currentDirectory.getUUID(),"%"+text+"%");
            loadBooksList(lsResult);
        }
    }

    public void scanDefaultPath(){
        new AlertDialog.Builder(this).setTitle(R.string.bookshelf_scan_for_books).setMessage(R.string.bookshelf_begin_scan)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BookScanner.scanBooks(BookshelfActivity.this, EpubUtils.bookRoot, new BookScanner.OnBookFinished() {
                            @Override
                            public void onFinish() {
                                loadData();
                            }
                        },false);
                    }
                }).setNegativeButton(android.R.string.no,null).setNeutralButton(R.string.bookshelf_rescan_all, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new AlertDialog.Builder(BookshelfActivity.this).setTitle(R.string.bookshelf_rescan_all).setMessage(R.string.bookshelf_rescan_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                BookScanner.scanBooks(BookshelfActivity.this, EpubUtils.bookRoot, new BookScanner.OnBookFinished() {
                                    @Override
                                    public void onFinish() {
                                        loadData();
                                    }
                                },true);
                            }
                        }).setNegativeButton(android.R.string.no,null).create().show();
            }
        }).create().show();
    }

    boolean isAllBook = false;
    DBUtils.BookEntry currentDirectory = null;

    public void loadBooksList(List<DBUtils.BookEntry> books){
        RecyclerView rv = (RecyclerView) findViewById(R.id.listBooks);
        BookAdapter ba = new BookAdapter(this,rv,books);
        GridLayoutManager glm = new GridLayoutManager(this,3, GridLayoutManager.HORIZONTAL,false);
        rv.setLayoutManager(glm);
        rv.setAdapter(ba);
        findViewById(R.id.txtDesktopHint).setVisibility(View.GONE);
        findViewById(R.id.txtScanHint).setVisibility(books.size()>0 ? View.GONE : View.VISIBLE);
        if(_mnuAllBookButton == null){return;}
        _mnuAllBookButton.setVisible(false);
        _mnuSearchButton.setVisible(true);
        isDesktop = false;
    }

    List<MenuItem> folderMenuItems = new ArrayList<>();

    FolderDrawerAdapter folderAdapter;

    public void loadCompleteBooks(View v){
        isAllBook = false;
        setTitle(getString(R.string.title_completed_books));
        loadBooksList(DBUtils.queryBooks("type=2 order by lastopen desc"));
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        if(drwMain.isDrawerOpen(GravityCompat.START)){
            drwMain.closeDrawer(GravityCompat.START);
        }
    }

    public void loadMenuRange(DBUtils.BookEntry... strs){
        folderAdapter.data.clear();
        folderAdapter.data.add(new FolderViewData(0,null,0));
        folderAdapter.data.add(new FolderViewData(1,getText(R.string.all).toString(),0));
        folderAdapter.data.add(new FolderViewData(2,getText(R.string.desktop).toString(),R.drawable.ic_menu_folder){{clicker = BookshelfActivity.this::loadDesktop;}});
        folderAdapter.data.add(new FolderViewData(2,getText(R.string.all_books).toString(),R.drawable.ic_menu_folder){{clicker = BookshelfActivity.this::loadAll;}});
        folderAdapter.data.add(new FolderViewData(1,getText(R.string.folders).toString(),0));
        for (int i = 0; i < strs.length; i++) {
            FolderViewData fd = new FolderViewData(2,strs[i].getDisplayName(),R.drawable.ic_menu_folder);
            fd.clicker = new FolderClicker(strs[i]);
            folderAdapter.data.add(fd);
        }

        folderAdapter.data.add(new FolderViewData(1,getString(R.string.complete_books),0));
        folderAdapter.data.add(new FolderViewData(2,getText(R.string.complete_books).toString(),R.drawable.ic_menu_folder){{clicker = BookshelfActivity.this::loadCompleteBooks;}});
        folderAdapter.data.add(new FolderViewData(1,getText(R.string.preference).toString(),0));
//        folderAdapter.data.add(new FolderViewData(2,getText(R.string.server).toString(),R.drawable.ic_list_go){{
//            clicker = new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
//                    if(drwMain.isDrawerOpen(GravityCompat.START)){
//                        drwMain.closeDrawer(GravityCompat.START);
//                    }
//                    startActivity(new Intent(BookshelfActivity.this,ServerActivity.class));
//                }
//            };
//        }});
        folderAdapter.data.add(new FolderViewData(2,getText(R.string.settings).toString(),R.drawable.ic_menu_setting){{
            clicker = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
                    if(drwMain.isDrawerOpen(GravityCompat.START)){
                        drwMain.closeDrawer(GravityCompat.START);
                    }
                    startActivity(new Intent(BookshelfActivity.this,SettingActivity.class));
                }
            };
        }});
        folderAdapter.data.add(new FolderViewData(2,getText(R.string.about).toString(),R.drawable.ic_menu_about){{
            clicker = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
                    if(drwMain.isDrawerOpen(GravityCompat.START)){
                        drwMain.closeDrawer(GravityCompat.START);
                    }
                    startActivity(new Intent(BookshelfActivity.this,AboutActivity.class));
                }
            };
        }});
        folderAdapter.update();
    }

    class FolderClicker implements View.OnClickListener{
        DBUtils.BookEntry be;

        public FolderClicker(DBUtils.BookEntry be) {
            this.be = be;
        }

        @Override
        public void onClick(View v) {
            DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
            if(drwMain.isDrawerOpen(GravityCompat.START)){
                drwMain.closeDrawer(GravityCompat.START);
            }
            List<DBUtils.BookEntry> lsResult = getRecommandBook("parent_uuid=? ",be.getUUID());
            setTitle(be.getDisplayName());
            loadBooksList(lsResult);
            isAllBook = false;currentDirectory = be;
        }
    }

    void loadAll(View v){
        this.loadData();
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        if(drwMain.isDrawerOpen(GravityCompat.START)){
            drwMain.closeDrawer(GravityCompat.START);
        }
    }
    boolean isDesktop = false;
    void loadDesktop(View v){
        setTitle(R.string.desktop);
        isAllBook = false;

        List<DBUtils.BookEntry> desktopBooks = SpUtils.getInstance(this).getDesktopBooks();
        boolean hasBook = false;
        for (int i = 0; i < desktopBooks.size(); i++) {
            if(desktopBooks.get(i) != null){
                hasBook = true;
            }
        }


        loadBooksList(desktopBooks);
        findViewById(R.id.txtDesktopHint).setVisibility(hasBook ? View.GONE : View.VISIBLE);
        _mnuSearchButton.setVisible(false);
        _mnuAllBookButton.setVisible(true);
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        if(drwMain.isDrawerOpen(GravityCompat.START)){
            drwMain.closeDrawer(GravityCompat.START);
        }

        isDesktop = true;
    }

    boolean skipInitialResume = true;

    @Override
    protected void onResume() {
        super.onResume();

        if(skipInitialResume){
            skipInitialResume = false;
            return;
        }

        if(isAllBook){
            loadData();
        }
        if(isDesktop){
            loadDesktop(null);
        }
        Log.e("Activity:::","OnResume::::::::::::::::::::::::::::");
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        if(drwMain.isDrawerOpen(GravityCompat.START)){
            drwMain.closeDrawer(GravityCompat.START);
        }else{
            super.onBackPressed();
        }
    }
    private EinkRecyclerView displayingEinkPage = null;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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


    public void onBookClick(DBUtils.BookEntry be){
        DBUtils.execSql("update library set lastopen=? where uuid=?",System.currentTimeMillis(),be.getUUID());
        hWnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isAllBook){loadData();}
            }
        },2000);
        if(SpUtils.getInstance(this).shouldOpenWithExternalReader()){
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.fromFile(new File(be.getPath())));
            try{
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectFileUriExposure().build());
                startActivity(i);
            }catch (Exception ex){
                ex.printStackTrace();
                Toast.makeText(this, R.string.open_no_external, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        Intent i = new Intent(this,ReadingActivity.class);
        i.putExtra("book",JsonConvert.toJson(be));
        startActivity(i);
    }

    class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookAdapterViewHolder>{
        public List<DBUtils.BookEntry> books;
        private Context context;
        RecyclerView root;

        public Context getContext() {
            return context;
        }

        public BookAdapter(Context ctx,RecyclerView root, List<DBUtils.BookEntry> books) {
            this.books = books;
            context = ctx;
            this.root = root;
        }

        @Override
        public BookAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            BookAdapterViewHolder bavh = new BookAdapterViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.adapter_book_entry,parent,false));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(root.getWidth()/3,root.getHeight()/3);
            bavh.rootView.setLayoutParams(lp);
            return bavh;
        }

        @Override
        public void onBindViewHolder(BookAdapterViewHolder holder, int position) {
            DBUtils.BookEntry bk = books.get(position);
            holder.crdBook.setVisibility(View.VISIBLE);
            if(bk == null) {
                holder.crdBook.setVisibility(View.INVISIBLE);
                return;
            }
            holder.bookName.setText(bk.getDisplayName());
            mCoverLoader.load(Uri.parse("epubentry://"+Base64.encodeToString(JsonConvert.toJson(bk).getBytes(),Base64.URL_SAFE))).noFade().into(holder.bookCover);
            holder.crdBook.setClickable(true);
            holder.crdBook.setOnTouchListener(new BookClicker(bk));
            if(bk.getType()==2){
                holder.badgeFin.setVisibility(View.VISIBLE);
                holder.bookCover.setAlpha(0.3f);
            }
            else{
                holder.badgeFin.setVisibility(View.INVISIBLE);
                holder.bookCover.setAlpha(1f);
            }
        }
        @Override
        public int getItemCount() {
            return books.size();
        }

        class BookAdapterViewHolder extends RecyclerView.ViewHolder{
            LinearLayout rootView;
            ImageView bookCover;
            TextView bookName;
            CardView crdBook;
            TextView badgeFin;
            public BookAdapterViewHolder(View itemView) {
                super(itemView);
                rootView = (LinearLayout) itemView;
                bookCover = rootView.findViewById(R.id.imgCover);
                bookName = rootView.findViewById(R.id.txtTitle);
                crdBook = rootView.findViewById(R.id.crdBook);
                badgeFin = rootView.findViewById(R.id.badgeFin);
            }
        }
         class BookClicker implements View.OnTouchListener{
            DBUtils.BookEntry entry;
            public BookClicker(DBUtils.BookEntry entry) {
                this.entry = entry;
            }


            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction()==MotionEvent.ACTION_UP){
                    onBookClick(entry);
                    return true;
                }
                return false;
            }
        }
    }

    class BookCoverDownloader implements Downloader{
        @Override
        public Response load(Uri uri, boolean localCacheOnly) throws IOException {
            if(uri.getScheme().startsWith("epubentry")){
                String str = new String(Base64.decode(uri.getHost(),Base64.URL_SAFE));
                DBUtils.BookEntry be = JsonConvert.fromJson(str, DBUtils.BookEntry.class);
                return new Response(EpubUtils.getBookCoverInCache(be),false,-1);
            }
            throw new IOException();
        }
    }


    class FolderDrawerAdapter extends RecyclerView.Adapter<FolderDrawerAdapter.FolderDrawerViewHolder>{

        public List<FolderViewData> data;

        public FolderDrawerAdapter(List<FolderViewData> data) {
            this.data = data;
        }

        public void update(){
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FolderDrawerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if(viewType==0){
                return new FolderDrawerViewHolder(getLayoutInflater().inflate(R.layout.activity_bookshelf_navhead,parent,false),0);
            }
            if(viewType==1){
                return new FolderDrawerViewHolder(getLayoutInflater().inflate(R.layout.adapter_folder_divider,parent,false),1);
            }
            if(viewType==2){
                return new FolderDrawerViewHolder(getLayoutInflater().inflate(R.layout.adapter_folder_item,parent,false),2);
            }

            return null;
        }


        @Override
        public void onBindViewHolder(@NonNull BookshelfActivity.FolderDrawerAdapter.FolderDrawerViewHolder holder, int position) {
            if(getItemViewType(position)==0){

            }
            if(getItemViewType(position)==1){
                holder.txtText.setText(data.get(position).text);
            }
            if(getItemViewType(position)==2){
                FolderViewData d = data.get(position);
                holder.txtText.setText(d.text);
                holder.imgIcon.setImageResource(d.icon);
                holder.rootView.setOnClickListener(d.clicker);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(position==0){return 0;}
            return data.get(position).type;
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class FolderDrawerViewHolder extends RecyclerView.ViewHolder{
            private int type = 0;
            TextView txtText;
            ImageView imgIcon;
            View rootView;
            public FolderDrawerViewHolder(@NonNull View itemView,int type) {
                super(itemView);
                this.type=type;
                rootView = itemView;
                if(type==1){
                    txtText = itemView.findViewById(R.id.txtText);
                }
                if(type==2){
                    txtText = itemView.findViewById(R.id.txtText);
                    imgIcon = itemView.findViewById(R.id.imgIcon);
                    rootView = itemView.findViewById(R.id.rootView);
                }
            }
        }


    }
    class FolderViewData{
        public int type;
        public String text;
        public int icon;
        public View.OnClickListener clicker = null;
        public FolderViewData(int type, String text, int icon) {
            this.type = type;
            this.text = text;
            this.icon = icon;
        }
    }
}

