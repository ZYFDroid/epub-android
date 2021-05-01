package com.zyfdroid.epub;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.StrictMode;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BookshelfActivity extends AppCompatActivity {
    Gson JsonConvert = new Gson();
    NavigationView navMain;
    Picasso mCoverLoader;
    Handler hWnd = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);
        mCoverLoader = new Picasso.Builder(this).downloader(new BookCoverDownloader()).build();
        setSupportActionBar((Toolbar) findViewById(R.id.titMain));
        DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
        ActionBarDrawerToggle drwButton = new ActionBarDrawerToggle(this,drwMain,(Toolbar) findViewById(R.id.titMain),R.string.app_name,R.string.app_name);
        drwMain.setDrawerListener(drwButton);
        drwButton.syncState();
        navMain = (NavigationView) findViewById(R.id.navMain);

        navMain.getMenu().findItem(R.id.mnuAllBook).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
                if(drwMain.isDrawerOpen(GravityCompat.START)){
                    drwMain.closeDrawer(GravityCompat.START);
                }
                loadData();
                return true;
            }
        });
        navMain.getMenu().findItem(R.id.mnuSetting).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
                if(drwMain.isDrawerOpen(GravityCompat.START)){
                    drwMain.closeDrawer(GravityCompat.START);
                }
                startActivity(new Intent(BookshelfActivity.this,SettingActivity.class));
                return true;
            }
        });
        navMain.getMenu().findItem(R.id.mnuAbout).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
                if(drwMain.isDrawerOpen(GravityCompat.START)){
                    drwMain.closeDrawer(GravityCompat.START);
                }
                startActivity(new Intent(BookshelfActivity.this,AboutActivity.class));
                return true;
            }
        });
        hWnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        },300);
    }

    public void loadData(){
        setTitle(R.string.all_books);
        isAllBook = true;
        loadMenuRange(navMain.getMenu().findItem(R.id.mnuFolders),DBUtils.queryFoldersNotEmpty().toArray(new DBUtils.BookEntry[0]));
        loadBooksList(DBUtils.queryBooks("type=0 order by lastopen desc"));
    }

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

            menu.add(R.string.bookshelf_search).setIcon(R.drawable.ic_menu_searchinlibrary).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setActionView(actionView);
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
            List<DBUtils.BookEntry> lsResult = DBUtils.queryBooks("type=0 and display_name like ? order by display_name","%"+text+"%");
            loadBooksList(lsResult);
        }
        else{

            List<DBUtils.BookEntry> lsResult = DBUtils.queryBooks("parent_uuid=? and type=0 and display_name like ? order by display_name",currentDirectory.getUUID(),"%"+text+"%");
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
        findViewById(R.id.txtScanHint).setVisibility(books.size()>0 ? View.GONE : View.VISIBLE);
    }

    List<MenuItem> folderMenuItems = new ArrayList<>();

    private static final int MENU_GROUP =392844;
    public void loadMenuRange(MenuItem mi,DBUtils.BookEntry... strs){
        mi.getSubMenu().removeGroup(MENU_GROUP);
        folderMenuItems.clear();
        for (int i = 0; i < strs.length; i++) {
            MenuItem mmi =
                mi.getSubMenu().add(MENU_GROUP,Menu.NONE,Menu.NONE,strs[i].getDisplayName()).setIcon(R.drawable.ic_menu_folder).setOnMenuItemClickListener(new FolderMenuClickListener(strs[i]));
            folderMenuItems.add(mmi);
        }
        mi.getSubMenu().setGroupCheckable(MENU_GROUP,true,true);
    }
    private class FolderMenuClickListener implements MenuItem.OnMenuItemClickListener{
        DBUtils.BookEntry folder;

        public FolderMenuClickListener(DBUtils.BookEntry folder) {
            this.folder = folder;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            for (MenuItem omi :
                    folderMenuItems) {
                omi.setChecked(false);
            }
            item.setChecked(true);
            DrawerLayout drwMain = (DrawerLayout) findViewById(R.id.drwMain);
            if(drwMain.isDrawerOpen(GravityCompat.START)){
                drwMain.closeDrawer(GravityCompat.START);
            }
            List<DBUtils.BookEntry> lsResult = DBUtils.queryBooks("parent_uuid=? and type=0 order by display_name",folder.getUUID());
            setTitle(folder.getDisplayName());
            loadBooksList(lsResult);
            isAllBook = false;currentDirectory = folder;
            return false;
        }
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


    public void onBookClick(DBUtils.BookEntry be){
        DBUtils.execSql("update library set lastopen=? where uuid=?",System.currentTimeMillis(),be.getUUID());
        if(isAllBook){loadData();}
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
            holder.bookName.setText(bk.getDisplayName());
            mCoverLoader.load(Uri.parse("epubentry://"+Base64.encodeToString(JsonConvert.toJson(bk).getBytes(),Base64.URL_SAFE))).into(holder.bookCover);
            holder.crdBook.setOnClickListener(new BookClicker(bk));
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
            public BookAdapterViewHolder(View itemView) {
                super(itemView);
                rootView = (LinearLayout) itemView;
                bookCover = rootView.findViewById(R.id.imgCover);
                bookName = rootView.findViewById(R.id.txtTitle);
                crdBook = rootView.findViewById(R.id.crdBook);
            }
        }
        class BookClicker implements View.OnClickListener{
            DBUtils.BookEntry entry;
            public BookClicker(DBUtils.BookEntry entry) {
                this.entry = entry;
            }

            @Override
            public void onClick(View v) {
                onBookClick(entry);
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

}

