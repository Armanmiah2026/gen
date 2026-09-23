package com.alinaj.paidgen6;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.StrictMode;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.alinaj.paidgen6.utils.AESCrypt;
import com.alinaj.paidgen6.utils.PayloadGeneratorDialog;
import com.alinaj.paidgen6.utils.TeaBase64;
import com.alinaj.paidgen6.views.TouchInterceptorListView;
import com.alinaj.paidgen6.views.TouchInterceptorListView2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import io.github.cutelibs.cutedialog.CuteDialog;

@SuppressWarnings("all")
@Obfuscate
public class MainActivity extends AppCompatActivity {

    private PageAdapter pageAdapter;
    private ViewPager viewpager;
    private TabLayout tabLayout;
    private ImageView menu_import, main_menu;
    private TouchInterceptorListView serverListView;
    private ArrayList<JSONObject> listProfiles;
    private ArrayList<String> listProfiles2;
    private ServerAdapter mServerAdapter;
    private SharedPreferences prefs;
    private ArrayList<String> myFlags;
    private ArrayList<JSONObject> listNetworks;
    private TouchInterceptorListView2 networkListView;
    private NetworkAdapter mPayloadAdapter;
    private TextView user_info, server_info, payload_info;
    private TextView app_info;
    private static final int MAX_RETRY = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy.Builder builder = new StrictMode.ThreadPolicy.Builder();
        StrictMode.setThreadPolicy(builder.permitAll().build());

        setContentView(R.layout.activity_main);
        View header = findViewById(R.id.header_container);

        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
            ).top;

            v.setPadding(
                    v.getPaddingLeft(),
                    topInset,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets;
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        prefs = getSharedPreferences("CloudGen", Context.MODE_PRIVATE);
        setpager();
        careExtractJson();
        myFlags = listFlags();
        menu_import = findViewById(R.id.menu_import);
        menu_import.setOnClickListener(v -> showImportMenu(menu_import));
        main_menu = findViewById(R.id.menu_btn);
        main_menu.setOnClickListener(this::showMainMenu);
        ImageView resetConfig = findViewById(R.id.menu_reset);
        resetConfig.setOnClickListener(this::showResetMenu);
        setupServerListView();
        setupNetowrksListView();
        if (prefs.getString("login_user", "").isEmpty()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

        Button save = findViewById(R.id.btnSave);
        save.setOnClickListener(v -> exportDialog());

    }
    public void setupServerListView() {
        listProfiles = new ArrayList<>();
        listProfiles2 = new ArrayList<>();
        serverListView = findViewById(R.id.server_listview);
        mServerAdapter = new ServerAdapter(this, listProfiles, listProfiles2);
        serverListView.setAdapter(mServerAdapter);
        serverListView.setCacheColorHint(0);
        serverListView.setOnCreateContextMenuListener(this);
        serverListView.setDragListener((from, to) -> {
            if (from != to) {
                if (to >= listProfiles.size() || from >= listProfiles.size()) {
                    Collections.swap(listProfiles, listProfiles.size() - 1, listProfiles.size() - 1);
                    mServerAdapter.notifyDataSetChanged();
                    saveServerSwap();
                } else {
                    Collections.swap(listProfiles, from, to);
                    mServerAdapter.notifyDataSetChanged();
                    saveServerSwap();
                }
            }


        });

        //  serverListView.setActivated(true);
        serverListView.setDropListener((from, to) -> {

        });
        loadServers();
        EditText search_server = findViewById(R.id.server_search);
        search_server.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ImageView iv = findViewById(R.id.server_search_icon);
                iv.setImageResource(R.drawable.ic_arrow_back_black_24dp);
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        search_server.clearFocus();
                        search_server.setText("");

                    }
                });
                ((TouchInterceptorListView) serverListView).setDragListener(new TouchInterceptorListView.DragListener() {
                    @Override
                    public void drag(int from, int to) {


                    }
                });
            } else {
                ImageView iv = findViewById(R.id.server_search_icon);
                iv.setImageResource(R.drawable.ic_search_black_24dp);
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        search_server.requestFocus();


                    }
                });
                ((TouchInterceptorListView) serverListView).setDragListener(new TouchInterceptorListView.DragListener() {
                    @Override
                    public void drag(int from, int to) {
                        if (from != to) {
                            if (to >= listProfiles.size() || from >= listProfiles.size()) {
                                Collections.swap(listProfiles, listProfiles.size() - 1, listProfiles.size() - 1);
                                mServerAdapter.notifyDataSetChanged();
                                saveServerSwap();
                            } else {
                                Collections.swap(listProfiles, from, to);
                                mServerAdapter.notifyDataSetChanged();
                                saveServerSwap();
                            }
                        }


                    }
                });
            }
        });
        search_server.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count != 0) {
                    ImageView iv = findViewById(R.id.server_search_clear);
                    iv.setVisibility(VISIBLE);
                } else {
                    ImageView iv = findViewById(R.id.server_search_clear);
                    iv.setVisibility(GONE);
                }
                searchServers(search_server.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        ImageView clear_search = findViewById(R.id.server_search_clear);
        clear_search.setOnClickListener(v -> search_server.setText(""));
        ImageView add_server = findViewById(R.id.add_server);
        add_server.setOnClickListener(v -> showServerMenu(v));
        LinearLayout server_placeholder = findViewById(R.id.server_empty_placeholder);
        if (!listProfiles.isEmpty()) {
            server_placeholder.setVisibility(GONE);
        } else {
            server_placeholder.setVisibility(VISIBLE);
        }
        setMainLayout();
    }

    public void setupNetowrksListView() {
        listNetworks = new ArrayList<>();

        networkListView = findViewById(R.id.payload_listview);
        mPayloadAdapter = new NetworkAdapter(this, listNetworks);
        networkListView.setAdapter(mPayloadAdapter);
        networkListView.setCacheColorHint(0);
        networkListView.setOnCreateContextMenuListener(this);
        ((TouchInterceptorListView2) networkListView).setDragListener(new TouchInterceptorListView2.DragListener() {
            @Override
            public void drag(int from, int to) {
                if (from != to) {
                    if (to >= listNetworks.size() || from >= listNetworks.size()) {
                        Collections.swap(listNetworks, listNetworks.size() - 1, listNetworks.size() - 1);
                        mPayloadAdapter.notifyDataSetChanged();
                        savePayloadSwap();
                    } else {
                        Collections.swap(listNetworks, from, to);
                        mPayloadAdapter.notifyDataSetChanged();
                        savePayloadSwap();
                    }
                }


            }
        });

        // networkListView.setActivated(true);
        ((TouchInterceptorListView2) networkListView).setDropListener(new TouchInterceptorListView2.DropListener() {
            @Override
            public void drop(int from, int to) {

            }
        });
        loadNetworks();
        EditText payload_search = findViewById(R.id.payload_search);
        payload_search.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ImageView iv = findViewById(R.id.payload_search_icon);
                    iv.setImageResource(R.drawable.ic_arrow_back_black_24dp);
                    iv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            payload_search.clearFocus();
                            payload_search.setText("");
                        }
                    });
                    ((TouchInterceptorListView2) networkListView).setDragListener(new TouchInterceptorListView2.DragListener() {
                        @Override
                        public void drag(int from, int to) {

                        }
                    });
                } else {
                    ImageView iv = findViewById(R.id.payload_search_icon);
                    iv.setImageResource(R.drawable.ic_search_black_24dp);
                    iv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            payload_search.requestFocus();
                        }
                    });
                    ((TouchInterceptorListView2) networkListView).setDragListener(new TouchInterceptorListView2.DragListener() {
                        @Override
                        public void drag(int from, int to) {
                            if (from != to) {
                                if (to >= listNetworks.size() || from >= listNetworks.size()) {
                                    Collections.swap(listNetworks, listNetworks.size() - 1, listNetworks.size() - 1);
                                    mPayloadAdapter.notifyDataSetChanged();
                                    savePayloadSwap();
                                } else {
                                    Collections.swap(listNetworks, from, to);
                                    mPayloadAdapter.notifyDataSetChanged();
                                    savePayloadSwap();
                                }
                            }


                        }
                    });
                }
            }
        });
        payload_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count != 0) {
                    ImageView iv = findViewById(R.id.payload_search_clear);
                    iv.setVisibility(VISIBLE);
                } else {
                    ImageView iv = findViewById(R.id.payload_search_clear);
                    iv.setVisibility(GONE);
                }
                searchNetworks(payload_search.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        ImageView clear_search = findViewById(R.id.payload_search_clear);
        clear_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payload_search.setText("");
            }
        });
        ImageView add_payload = findViewById(R.id.add_payload);
        add_payload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPayloadMenu(v);
            }
        });
        LinearLayout network_placeholder = findViewById(R.id.payload_empty_placeholder);
        if (!listNetworks.isEmpty()) {
            network_placeholder.setVisibility(GONE);
        } else {
            network_placeholder.setVisibility(VISIBLE);
        }
        setMainLayout();
    }

    public void savePayloadSwap() {
        JSONArray js = new JSONArray();
        JSONArray jss = new JSONArray();
        for (int i = 0; i <= listNetworks.size() - 1; i++) {
            js.put(listNetworks.get(i));
        }
        JSONObject jo = getJSONObject();
        jo.remove("Networks");
        jo.remove("SSLNetworks");
        try {
            jo.put("Networks", js);
            jo.put("SSLNetworks", jss);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try {
            File file = new File(getFilesDir(), "Servers.js");

            OutputStream out = new FileOutputStream(file);
            out.write(jo.toString().getBytes());
            out.flush();
            out.close();
            //     TextView tv=findViewById(R.id.result);
            //     tv.setText(getJsonString());

        } catch (Exception e) {
        }
    }

    public void saveServerSwap() {
        JSONArray js = new JSONArray();
        for (int i = 0; i <= listProfiles.size() - 1; i++) {
            js.put(listProfiles.get(i));
        }
        JSONObject jo = getJSONObject();
        jo.remove("Servers");
        try {
            jo.put("Servers", js);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try {
            File file = new File(getFilesDir(), "Servers.js");

            OutputStream out = new FileOutputStream(file);
            out.write(jo.toString().getBytes());
            out.flush();
            out.close();
            //  TextView tv=findViewById(R.id.result);
            //  tv.setText(getJsonString());

        } catch (Exception e) {
        }
    }

    public void loadServers() {
        if (listProfiles.size() > 0) {
            listProfiles.clear();
            listProfiles2.clear();
        }
        try {
            JSONArray serversArray = getServersArray();
            for (int i = 0; i < serversArray.length(); i++) {
                listProfiles.add(serversArray.getJSONObject(i));
                listProfiles2.add(serversArray.getJSONObject(i).getString("Name"));
                mServerAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {

        }
    }

    public void loadNetworks() {
        try {
            if (listNetworks.size() > 0) {
                listNetworks.clear();
            }
            JSONArray network = getNetworksArray();
            for (int i = 0; i < network.length(); i++) {
                listNetworks.add(network.getJSONObject(i));
            }
            if (getJSONObject().has("SSLNetworks")) {
                JSONArray sslnetwork = getJSONObject().getJSONArray("SSLNetworks");
                for (int i = 0; i < sslnetwork.length(); i++) {
                    listNetworks.add(sslnetwork.getJSONObject(i));
                }
                JSONObject jo = getJSONObject();
                jo.remove("SSLNetworks");
                JSONArray js = new JSONArray();
                JSONArray jss = new JSONArray();
                for (int i = 0; i <= listNetworks.size() - 1; i++) {
                    js.put(listNetworks.get(i));
                }
                jo.remove("Networks");
                try {
                    jo.put("Networks", js);
                    jo.put("SSLNetworks", jss);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                try {
                    File file = new File(getFilesDir(), "Servers.js");
                    OutputStream out = new FileOutputStream(file);
                    out.write(jo.toString().getBytes());
                    out.flush();
                    out.close();
                    //    TextView tv=findViewById(R.id.result);
                    //    tv.setText(getJsonString());

                } catch (Exception e) {
                }
            }
            mPayloadAdapter.notifyDataSetChanged();
        } catch (Exception e) {
        }
        // TODO: Implement this method
    }

    public int getServerPosition(String name) {
        try {
            JSONArray serversArray = getServersArray();
            for (int i = 0; i < serversArray.length(); i++) {
                if (serversArray.getJSONObject(i).getString("Name").equals(name)) {
                    return i;
                }
            }
        } catch (Exception e) {

        }
        return 0;
    }

    public int getnetworkPosition(String name) {
        try {
            JSONArray netArray = getNetworksArray();
            for (int i = 0; i < netArray.length(); i++) {
                if (netArray.getJSONObject(i).getString("Name").equals(name)) {
                    return i;
                }
            }
        } catch (Exception e) {

        }
        return 0;
    }

    public void searchServers(String a) {
        try {
            listProfiles.clear();
            listProfiles2.clear();
            JSONArray serversArray = getServersArray();
            for (int i = 0; i < serversArray.length(); i++) {
                if (serversArray.getJSONObject(i).getString("Name").toLowerCase().contains(a.toLowerCase())) {
                    listProfiles.add(serversArray.getJSONObject(i));
                    listProfiles2.add(serversArray.getJSONObject(i).getString("Name"));

                }

            }
            mServerAdapter.notifyDataSetChanged();
        } catch (Exception e) {

        }
    }

    public void searchNetworks(String a) {
        try {
            listNetworks.clear();

            JSONArray netArray = getNetworksArray();
            for (int i = 0; i < netArray.length(); i++) {
                if (netArray.getJSONObject(i).getString("Name").toLowerCase().contains(a.toLowerCase())) {
                    listNetworks.add(netArray.getJSONObject(i));

                }

            }
            mPayloadAdapter.notifyDataSetChanged();
        } catch (Exception e) {

        }
    }

    protected JSONObject getJSONObject() {
        File file = new File(getFilesDir(), "Servers.js");
        try {
            if (file.exists()) {
                String s = readStream(new FileInputStream(file));
                return new JSONObject(s);
            } else {

            }
        } catch (Exception e) {

        }
        return null;
    }

    protected JSONArray getServersArray() {
        try {
            JSONArray obj = getJSONObject().getJSONArray("Servers");
            return obj;
        } catch (Exception e) {
            //showToast(e.getMessage());
        }
        return null;
    }

    protected JSONArray getNetworksArray() {
        try {
            JSONArray obj = getJSONObject().getJSONArray("Networks");
            return obj;
        } catch (Exception e) {
            //showToast(e.getMessage());
        }
        return null;
    }

    public class ServerAdapter extends ArrayAdapter<JSONObject> {
        private ArrayList<JSONObject> listServer;

        private Context context;
        final Map<JSONObject, Integer> mIdMap = new HashMap<>();

        public ServerAdapter(Context context, ArrayList<JSONObject> listServer, ArrayList<String> list2) {
            super(context, R.layout.server_item, listServer);
            this.listServer = listServer;
            this.context = context;
            for (int i = 0; i < listServer.size(); ++i) {
                mIdMap.put(listServer.get(i), i);
            }
        }

        @Override
        public JSONObject getItem(int position) {
            // TODO: Implement this method
            return listServer.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO: Implement this method
            return MyView(position, convertView, parent, false);
        }

        public View MyView(int position, View convertView, ViewGroup parent, boolean isdrop) {
            View v = LayoutInflater.from(context).inflate(R.layout.server_item, parent, false);
            TextView tv = (TextView) v.findViewById(R.id.server_item_text);
            TextView tv2 = (TextView) v.findViewById(R.id.server_item_indicator);
            ImageView iv = (ImageView) v.findViewById(R.id.server_item_icon);

            try {
                JSONObject js = getItem(position);

                tv.setText(js.getString("Name"));
                setFlag(iv, js.getString("Flag"));
                if (position != 0) {

                    tv2.setText(js.getString("Category"));
                    if (js.getString("Category").contains("PRIVATE")) {
                        tv2.setTextColor(Color.RED);
                    }
                    if (js.getString("Category").contains("VIP")) {
                        tv2.setTextColor(Color.MAGENTA);
                    }
                    if (js.getString("Category").contains("PREMIUM")) {
                        tv2.setTextColor(Color.BLUE);
                    }
                }
                ImageView edit = v.findViewById(R.id.server_edit);
                ImageView delete = v.findViewById(R.id.server_delete);
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        try {
                            editServer(getServerPosition(js.getString("Name")));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            removeServer(getServerPosition(js.getString("Name")), js.getString("Name"));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

            } catch (Exception e) {

            }
            // TODO: Implement this method
            return v;
        }

        public void setFlag(ImageView f, String ff) {
            try {
                InputStream open = context.getAssets().open(new StringBuffer().append("flag/").append(ff).toString());
                f.setImageDrawable(Drawable.createFromStream(open, (String) null));
                if (open != null) {
                    open.close();
                }
            } catch (Exception e) {
                f.setImageResource(R.mipmap.ic_launcher);
            }
        }
    }

    public void setpager() {
        pageAdapter = new PageAdapter(this);
        pageAdapter.setTitle(new String[]{"HOME", "SERVERS", "PAYLOADS"});
        viewpager = (ViewPager) findViewById(R.id.viewpager);
        viewpager.setOffscreenPageLimit(2);
        viewpager.setAdapter(pageAdapter);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewpager);
        viewpager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                prefs.edit().putInt("viewpage", position).apply();
                if (position == 0) {
                    setMainLayout();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewpager.setCurrentItem(prefs.getInt("viewpage", 0), true);
    }

    public static class PageAdapter extends PagerAdapter {
        private MainActivity act;
        private String[] titles;

        public PageAdapter(MainActivity act) {
            this.act = act;
        }

        public void setTitle(String[] titles) {
            this.titles = titles;
        }

        @Override
        public int getCount() {
            // TODO: Implement this method
            return titles.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            // TODO: Implement this method
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            int[] ids = new int[]{R.id.tab_home, R.id.tab_servers, R.id.tab_payloads};
            // TODO: Implement this method
            return act.findViewById(ids[position]);
        }

        @Override
        public Parcelable saveState() {
            // TODO: Implement this method
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // TODO: Implement this method
            return titles[position];
        }
    }

    private void showMainMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
        insertMenuItemIcons(this, popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if (menuItem.getItemId() == R.id.menu_about) {
                    aboutdialog();
                } else if (menuItem.getItemId() == R.id.menu_dec) {
                    en_de_text("", "");
                } else if (menuItem.getItemId() == R.id.menu_api) {
                    setChangeSetting();
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void en_de_text(String m, String r) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.en_de, null);
        AlertDialog.Builder certDialog = new AlertDialog.Builder(this);
        certDialog.setView(view);
        certDialog.setTitle("Encrypt/Decrypt");
        certDialog.setIcon(R.drawable.ic_whatsapp);
        certDialog.setCancelable(false);
        final EditText msg = view.findViewById(R.id.text_message);
        final TextView result = view.findViewById(R.id.result_message);
        msg.setText(m);
        result.setText(r);
        certDialog.setNeutralButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                p1.dismiss();
            }
        });
        certDialog.setNegativeButton("Encrypt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                String ms = msg.getText().toString().trim();
                String en = AESCrypt.Parser.parse(ms);
                en_de_text(ms, en);
            }
        });
        certDialog.setPositiveButton("Paste Decrypt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {

                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                String encryptedString = clipboard.getText().toString();

                //String decryptedString = MTKmain.decrypt(encryptedString.replace("https://t.me/Zenze000",""));

                String ms = AESCrypt.Parser.parseToString(encryptedString);
                en_de_text("", ms);
                // String ms = msg.getText().toString().trim();
                // String de = FileUtil.showJs(MTKmain.this, ms);
                // en_de_text(ms, de);
            }
        });
        AlertDialog cDialog = certDialog.create();
        cDialog.show();
    }

    private void showServerMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
        popupMenu.getMenuInflater().inflate(R.menu.server_menu, popupMenu.getMenu());
        insertMenuItemIcons(this, popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if (menuItem.getItemId() == R.id.menu_add_ovpn) {
                    addServer("ovpn");
                }
                if (menuItem.getItemId() == R.id.menu_add_udp) {
                    addServer("udp");
                }
                if (menuItem.getItemId() == R.id.menu_add_v2ray) {
                    addServer("v2ray");
                }
                if (menuItem.getItemId() == R.id.menu_add_openConnect) {
                    addServer("openConnect");
                }
                if (menuItem.getItemId() == R.id.menu_add_ssh) {
                    addServer("ssh");
                }
                if (menuItem.getItemId() == R.id.menu_add_slow_dns) {
                    addServer("slowdns");
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void showPayloadMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
        popupMenu.getMenuInflater().inflate(R.menu.payload_menu, popupMenu.getMenu());
        insertMenuItemIcons(this, popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if (menuItem.getItemId() == R.id.menu_add_http) {
                    addPayload("ovpn");
                }
                if (menuItem.getItemId() == R.id.menu_add_ssl) {
                    addSSL();
                }
                if (menuItem.getItemId() == R.id.menu_add_v2ray) {
                    addPayload("v2ray");
                }
                if (menuItem.getItemId() == R.id.menu_add_ssh) {
                    addPayload("ssh");
                }
                if (menuItem.getItemId() == R.id.menu_add_udp) {
                    addPayload("udp");
                }
                if (menuItem.getItemId() == R.id.menu_add_openConnect) {
                    addPayload("openConnect");
                }
                if (menuItem.getItemId() == R.id.menu_add_slow_dns) {
                    addPayload("slowdns");
                }
                return true;
            }
        });
        popupMenu.show();
    }

    public void aboutdialog() {
        new CuteDialog.withAnimation(this)
                .setAnimation(R.raw.anim2)
                .setTitle("About Us")
                .hideNegativeButton(true)
                .setDescription("This application is a configmaker for OpenVpn3 build by farhan devz @najmuldev with implementation of HTTP Injector, SSL Injector, Websocket, UDP Hysteria and V2ray configuration.")
                .setPositiveButtonText("Okay", v2 -> {

                })
                .setNegativeButtonText("Cancel", v2 -> {

                })
                .show();
    }

    private void showImportMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
        popupMenu.getMenuInflater().inflate(R.menu.import_menu, popupMenu.getMenu());
        insertMenuItemIcons(this, popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if (menuItem.getItemId() == R.id.import_file) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    startActivityForResult(Intent.createChooser(intent, "Select File"), 215);

                }
                if (menuItem.getItemId() == R.id.import_clip) {
                    importFromClipboard(readFromClipboard());
                }
                if (menuItem.getItemId() == R.id.import_url) {
                    importFromOnline();
                }


                if (menuItem.getItemId() == R.id.migate_config_clipboard) {
                    migrate_from_clip();
                }
                if (menuItem.getItemId() == R.id.migrate_config_online) {
                    migrate_from_online();
                }

                return true;
            }
        });
        popupMenu.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0215) {
            if (resultCode == RESULT_OK) {
                try {
                    Uri uri = data.getData();
                    String fileContent = importer(uri);
                    String full = AESCrypt.Parser.parseToString(fileContent);
                    JSONObject sObj = new JSONObject(full);

                    File file = new File(getFilesDir(), "Servers.js");
                    if (sObj.has("Version")) {
                        OutputStream out = new FileOutputStream(file);
                        out.write(sObj.toString().toString().getBytes());
                        out.flush();
                        out.close();
                        Toast.makeText(MainActivity.this, "Import Successfull!", Toast.LENGTH_SHORT).show();
                        setupServerListView();
                        setupNetowrksListView();
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid file config!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Invalid file config!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String importer(Uri uri) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));

            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private void showResetMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
        popupMenu.getMenuInflater().inflate(R.menu.reset_menu, popupMenu.getMenu());
        insertMenuItemIcons(this, popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if (menuItem.getItemId() == R.id.reset_server) {
                    new CuteDialog.withAnimation(MainActivity.this)
                            .setAnimation(R.raw.delete)
                            .setTitle("Clear Servers")
                            .setDescription("Are you sure you want to clear all current Servers? \n\nNote: This method cannot revert back the configs once reset done.")
                            .setPositiveButtonText("Okay", v2 -> {
                                JSONArray jsonArray = new JSONArray();
                                JSONObject jo = getJSONObject();
                                jo.remove("Servers");
                                try {
                                    jo.put("Servers", jsonArray);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                try {
                                    File file = new File(getFilesDir(), "Servers.js");
                                    OutputStream out = new FileOutputStream(file);
                                    out.write(jo.toString().getBytes());
                                    out.flush();
                                    out.close();
                                    showToast("Servers Cleared!");
                                    //    TextView tv=findViewById(R.id.result);
                                    //    tv.setText(getJsonString());

                                } catch (Exception e) {
                                }
                                setupServerListView();
                            })
                            .setNegativeButtonText("Cancel", v2 -> {

                            })
                            .show();
                }
                if (menuItem.getItemId() == R.id.reset_payloads) {
                    new CuteDialog.withAnimation(MainActivity.this)
                            .setAnimation(R.raw.delete)
                            .setTitle("Clear Payloads")
                            .setDescription("Are you sure you want to clear all current Payloads? \n\nNote: This method cannot revert back the configs once reset done.")
                            .setPositiveButtonText("Okay", v2 -> {
                                JSONArray jsonArray = new JSONArray();
                                JSONArray jsonArray1 = new JSONArray();
                                JSONObject jo = getJSONObject();
                                jo.remove("Networks");
                                jo.remove("SSLNetworks");
                                try {
                                    jo.put("Networks", jsonArray);
                                    jo.put("SSLNetworks", jsonArray1);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }

                                try {
                                    File file = new File(getFilesDir(), "Servers.js");
                                    OutputStream out = new FileOutputStream(file);
                                    out.write(jo.toString().getBytes());
                                    out.flush();
                                    out.close();
                                    //    TextView tv=findViewById(R.id.result);
                                    //    tv.setText(getJsonString());
                                    showToast("Payloads Cleared!");
                                } catch (Exception e) {
                                }
                                setupNetowrksListView();
                            })
                            .setNegativeButtonText("Cancel", v2 -> {

                            })
                            .show();
                }
                if (menuItem.getItemId() == R.id.reset_all_configs) {
                    new CuteDialog.withAnimation(MainActivity.this)
                            .setAnimation(R.raw.delete)
                            .setTitle("Clear Configs")
                            .setDescription("Are you sure you want to clear all current config? \n\nNote: This method cannot revert back the configs once reset done.")
                            .setPositiveButtonText("Okay", v2 -> {
                                resetJson();
                                showToast("Configs Cleared!");
                            })
                            .setNegativeButtonText("Cancel", v2 -> {

                            })
                            .show();
                }
                return true;
            }
        });
        popupMenu.show();
    }

    public static void insertMenuItemIcons(Context context, PopupMenu popupMenu) {
        Menu menu = popupMenu.getMenu();
        if (hasIcon(menu)) {
            for (int i = 0; i < menu.size(); i++) {
                insertMenuItemIcon(context, menu.getItem(i));
            }
        }
    }

    private static boolean hasIcon(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getIcon() != null) return true;
        }
        return false;
    }

    private static void insertMenuItemIcon(Context context, MenuItem menuItem) {
        Drawable icon = menuItem.getIcon();
        if (icon == null) icon = new ColorDrawable(Color.TRANSPARENT);

        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.menu_item_icon_size);
        icon.setBounds(0, 0, iconSize, iconSize);
        ImageSpan imageSpan = new ImageSpan(icon);
        SpannableStringBuilder ssb = new SpannableStringBuilder("      " + menuItem.getTitle());
        ssb.setSpan(imageSpan, 1, 2, 0);
        menuItem.setTitle(ssb);
        menuItem.setIcon(null);
    }

    public void showToast(String a) {
        Toast.makeText(this, a, Toast.LENGTH_SHORT).show();
    }

    public void resetJson() {
        try {
            String result = getAssetsConfig();
            File file = new File(getFilesDir(), "Servers.js");
            OutputStream out = new FileOutputStream(file);
            out.write(result.getBytes());
            out.flush();
            out.close();
            setupServerListView();
            setupNetowrksListView();
        } catch (Exception e) {
        }
    }

    public void careExtractJson() {
        try {
            String result = getAssetsConfig();
            File file = new File(getFilesDir(), "Servers.js");
            if (file.exists()) {
            } else {
                OutputStream out = new FileOutputStream(file);
                out.write(result.getBytes());
                out.flush();
                out.close();
            }

        } catch (Exception e) {
        }
    }

    //    public String getJsonString() {
//        File file = new File(getFilesDir(), "Servers.js");
//        try {
//            if (file.exists()) {
//                String s = readStream(new FileInputStream(file));
//                return s;
//            }
//        } catch (Exception e) {
//
//        }
//        return "";
//    }
    public String getJsonString() {
        File file = new File(getFilesDir(), "Servers.js");
        try {
            if (file.exists()) {
                String rawJson = readStream(new FileInputStream(file));

                // If it's a JSON Object
                JSONObject jsonObject = new JSONObject(rawJson);
                return jsonObject.toString(4); // 4 = number of spaces

                // If it's a JSON Array, use this instead:
                // JSONArray jsonArray = new JSONArray(rawJson);
                // return jsonArray.toString(4);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    protected String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(in));
            char[] buff = new char[1024];
            while (true) {
                int read = reader.read(buff, 0, buff.length);
                if (read <= 0) {
                    break;
                }
                sb.append(buff, 0, read);
            }
        } catch (Exception e) {
        }
        return sb.toString();
    }

    public String getAssetsConfig() {
        StringBuilder sb = new StringBuilder();

        try {
            InputStream is = getAssets().open("config.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    public void importFromClipboard(String text) {
        try {
            File file = new File(getFilesDir(), "Servers.js");
            JSONObject jo = new JSONObject(AESCrypt.Parser.parseToString(text));
            if (jo.has("Version")) {
                OutputStream out = new FileOutputStream(file);
                out.write(jo.toString().toString().getBytes());
                out.flush();
                out.close();
                Toast.makeText(MainActivity.this, "Import Successfull!", Toast.LENGTH_SHORT).show();
                setupServerListView();
                setupNetowrksListView();
            } else {
                Toast.makeText(MainActivity.this, "Invalid clip config!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Invalid clip config!", Toast.LENGTH_SHORT).show();
        }
    }

    public void importFromOnline() {
        String domain = prefs.getString("login_domain", "");
        String hash = prefs.getString("login_hash", "").replace(" ", "").replace(".", "").replace("  ", "");
        String url = "https://" + domain + "/api/files/app?json=" + hash;
        serverupdateApi(url);
    /* AlertDialog.Builder nBuilder2 = new AlertDialog.Builder(this);
     final View mView2 = getLayoutInflater().inflate(R.layout.dialog_set_edittext, null);
     nBuilder2.setTitle("Set Online URL");
     nBuilder2.setView(mView2);
     final AlertDialog dialog2 = nBuilder2.create();
     final EditText defVersion2 = (EditText)mView2.findViewById(R.id.edittextPassword);
     defVersion2.setHint("Set api link");
     defVersion2.setText(prefs.getString("api_link",""));
     nBuilder2.setPositiveButton("Import now", new DialogInterface.OnClickListener(){
         @Override
         public void onClick(DialogInterface p1, int p2) {
             if(defVersion2.getText().toString().isEmpty()){
                 Toast.makeText(MainActivity.this,"Please input api link!",Toast.LENGTH_SHORT).show();
                 dialog2.dismiss();
             }else {
                 prefs.edit().putString("api_link", defVersion2.getText().toString()).commit();
                 serverupdateApi(defVersion2.getText().toString());
                 dialog2.dismiss();
             }

         }
     });
     nBuilder2.show();

     */

    }

    private void serverupdateApi(String url) {
        final CuteDialog.withAnimation cdd = new CuteDialog.withAnimation(this)
                .setAnimation(R.raw.anim3)
                .setTitle("Import Online")
                .hideNegativeButton(true)
                .setDescription("Checking for api config...")
                .setPositiveButtonText("Okay", v2 -> {

                })
                .setNegativeButtonText("Cancel", v2 -> {

                });

        cdd.show();
        final RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest req = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String result) {

                        if (result == null) {
                            Toast.makeText(MainActivity.this, "Error checking update from api", Toast.LENGTH_SHORT).show();
                        } else if (result.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Config is empty from api", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                File file = new File(getFilesDir(), "Servers.js");
                                JSONObject jo = new JSONObject(AESCrypt.Parser.parseToString(result));
                                if (jo.has("Version")) {
                                    OutputStream out = new FileOutputStream(file);
                                    out.write(jo.toString().toString().getBytes());
                                    out.flush();
                                    out.close();
                                    Toast.makeText(MainActivity.this, "Import Successfull!", Toast.LENGTH_SHORT).show();
                                    setupServerListView();
                                    setupNetowrksListView();
                                } else {
                                    Toast.makeText(MainActivity.this, "Invalid api config!", Toast.LENGTH_SHORT).show();
                                }

                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Invalid api config!", Toast.LENGTH_SHORT).show();
                                //	result.setText(e.getMessage());
                            }
                        }
                        if (cdd != null) {
                            cdd.cancel();
                        }
                        requestQueue.getCache().clear();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (cdd != null) {
                    cdd.cancel();
                }
                Toast.makeText(MainActivity.this, "Error checking update from api", Toast.LENGTH_SHORT).show();
                requestQueue.getCache().clear();
            }

        });

        requestQueue.add(req);


        // TODO: Implement this method
    }

    public static String parse(String str) {
        try {
            str = AESCrypt.encrypt("fuckboy", str);
        } catch (Exception e) {
        }
        return str;
    }

    public static String parseToString(String str) {
        try {
            str = AESCrypt.decrypt("fuckboy", str);
        } catch (Exception e) {
        }
        return str;
    }

    public static String parseToString(String str, String pass) {
        try {
            str = AESCrypt.decrypt(pass, str);
        } catch (Exception e) {
        }
        return str;
    }

    public String readFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
            android.content.ClipData data = clipboard.getPrimaryClip();
            if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                return String.valueOf(data.getItemAt(0).getText());
        }
        return "";
    }

    private MaterialButton radiobtn_ovpn, radiobtn_ssh;

    void addServer(String isProtocol) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialog = inflater.inflate(R.layout.server_dialog, null);
        AlertDialog.Builder buidler = new AlertDialog.Builder(this);
        buidler.setTitle("Add Server");
        buidler.setCancelable(true);
        buidler.setView(dialog);

        final AlertDialog alert = buidler.create();
        alert.getWindow().setGravity(Gravity.CENTER);
        alert.show();


        // ShanDevz Telegram @Zenze000
        Spinner flagspin = (Spinner) dialog.findViewById(R.id.flagspin);
        FlagAdapter fa = new FlagAdapter(this, myFlags);
        flagspin.setAdapter(fa);


        EditText etServerName = (EditText) dialog.findViewById(R.id.server_name_dialog);
        EditText etServerHost = (EditText) dialog.findViewById(R.id.server_ip_dialog);
        EditText etCloudFront = (EditText) dialog.findViewById(R.id.etServerCloudFront);
        EditText etHTTPServer = (EditText) dialog.findViewById(R.id.etServerHTTP);
        EditText etTcpPort = dialog.findViewById(R.id.openvpn_tcp_port);
        EditText etSSlPort = dialog.findViewById(R.id.openvpn_ssl_port);
        EditText etPubKey = dialog.findViewById(R.id.etPubKey);
        EditText etNameServer = dialog.findViewById(R.id.etNameServer);
        EditText etServerEntry = dialog.findViewById(R.id.server_entry);
        EditText etConfigs = dialog.findViewById(R.id.etConfigs);
        CheckBox use_default_servers = dialog.findViewById(R.id.use_default_servers);

        Button btnPayloadGen = dialog.findViewById(R.id.btnPayloadGen);
        LinearLayout psiphon_layout = dialog.findViewById(R.id.s7);
        LinearLayout port_layout = dialog.findViewById(R.id.s5);
        TextInputLayout publicly = dialog.findViewById(R.id.publickey);
        TextInputLayout nameserver = dialog.findViewById(R.id.nameserver);
        TextInputLayout config1 = dialog.findViewById(R.id.config1);
        TextInputLayout server_entry_layout = dialog.findViewById(R.id.server_entry_layout);

        Spinner categorySpinner = (Spinner) dialog.findViewById(R.id.category_spin);
        ArrayAdapter categoryAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item);
        categoryAdapter.add("PREMIUM");
        categoryAdapter.add("VIP");
        categoryAdapter.add("PRIVATE");
        categorySpinner.setAdapter(categoryAdapter);

        Spinner ProtocolSpinner = (Spinner) dialog.findViewById(R.id.protocol_spin);
        ArrayAdapter ProtocolAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item);
        ProtocolAdapter.add("OpenVPN");
        ProtocolAdapter.add("UDP");
        ProtocolAdapter.add("V2Ray");
        ProtocolAdapter.add("Psiphon");
        ProtocolAdapter.add("OpenConnect");
        ProtocolAdapter.add("SSH");
        ProtocolAdapter.add("Slow DNS");
        ProtocolSpinner.setAdapter(ProtocolAdapter);

        Spinner certSpinner = (Spinner) dialog.findViewById(R.id.cert);
        ArrayAdapter certAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item);
        certAdapter.add("Cert 1");
        certAdapter.add("Cert 2");
        certAdapter.add("Cert 3");
        certSpinner.setAdapter(certAdapter);

        CheckBox ckUseAutoLogin = (CheckBox) dialog.findViewById(R.id.autoLogin);
        EditText etServerUser = (EditText) dialog.findViewById(R.id.etUsername);
        EditText etServerPass = (EditText) dialog.findViewById(R.id.etPassword);

        if (isProtocol.equals("ovpn")) {
            ProtocolSpinner.setSelection(0);
        } else if (isProtocol.equals("udp")) {
            ProtocolSpinner.setSelection(1);
        } else if (isProtocol.equals("v2ray")) {
            ProtocolSpinner.setSelection(2);
        } else if (isProtocol.equals("psiphon")) {
            ProtocolSpinner.setSelection(3);
        } else if (isProtocol.equals("openConnect")) {
            ProtocolSpinner.setSelection(4);
        } else if (isProtocol.equals("ssh")) {
            ProtocolSpinner.setSelection(5);
        } else if (isProtocol.equals("slowdns")) {
            ProtocolSpinner.setSelection(6);
        }

        ProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //String selectedItem = adapterView.getItemAtPosition(position).toString();

                if (position == 0) {
                    etServerName.setVisibility(VISIBLE);
                    etServerHost.setVisibility(VISIBLE);
                    etCloudFront.setVisibility(VISIBLE);
                    etHTTPServer.setVisibility(VISIBLE);
                    etServerPass.setVisibility(VISIBLE);
                    etServerUser.setVisibility(VISIBLE);
                    etSSlPort.setVisibility(VISIBLE);
                    etTcpPort.setVisibility(VISIBLE);
                    certSpinner.setVisibility(VISIBLE);
                    ckUseAutoLogin.setVisibility(VISIBLE);
                    port_layout.setVisibility(VISIBLE);
                    psiphon_layout.setVisibility(GONE);
                    publicly.setVisibility(GONE);
                    nameserver.setVisibility(GONE);
                    config1.setVisibility(GONE);
                    btnPayloadGen.setVisibility(GONE);
                } else if (position == 1) {
                    etServerName.setVisibility(VISIBLE);
                    etServerHost.setVisibility(GONE);
                    etCloudFront.setVisibility(GONE);
                    etHTTPServer.setVisibility(GONE);
                    etServerPass.setVisibility(GONE);
                    etServerUser.setVisibility(GONE);
                    etSSlPort.setVisibility(GONE);
                    etTcpPort.setVisibility(GONE);
                    certSpinner.setVisibility(GONE);
                    ckUseAutoLogin.setVisibility(GONE);
                    port_layout.setVisibility(GONE);
                    psiphon_layout.setVisibility(GONE);
                    publicly.setVisibility(GONE);
                    nameserver.setVisibility(GONE);
                    config1.setVisibility(VISIBLE);
                    btnPayloadGen.setVisibility(VISIBLE);
                } else if (position == 2) {
                    etServerName.setVisibility(VISIBLE);
                    etServerHost.setVisibility(GONE);
                    etCloudFront.setVisibility(GONE);
                    etHTTPServer.setVisibility(GONE);
                    etServerPass.setVisibility(GONE);
                    etServerUser.setVisibility(GONE);
                    etSSlPort.setVisibility(GONE);
                    etTcpPort.setVisibility(GONE);
                    certSpinner.setVisibility(GONE);
                    ckUseAutoLogin.setVisibility(GONE);
                    port_layout.setVisibility(GONE);
                    psiphon_layout.setVisibility(GONE);
                    publicly.setVisibility(GONE);
                    nameserver.setVisibility(GONE);
                    config1.setVisibility(VISIBLE);
                    btnPayloadGen.setVisibility(GONE);
                } else if (position == 3) {
                    etServerName.setVisibility(VISIBLE);
                    etServerHost.setVisibility(GONE);
                    etCloudFront.setVisibility(GONE);
                    etHTTPServer.setVisibility(GONE);
                    etServerPass.setVisibility(GONE);
                    etServerUser.setVisibility(GONE);
                    etSSlPort.setVisibility(GONE);
                    etTcpPort.setVisibility(GONE);
                    certSpinner.setVisibility(GONE);
                    ckUseAutoLogin.setVisibility(GONE);
                    port_layout.setVisibility(GONE);
                    psiphon_layout.setVisibility(VISIBLE);
                    publicly.setVisibility(GONE);
                    nameserver.setVisibility(GONE);
                    config1.setVisibility(GONE);
                    btnPayloadGen.setVisibility(GONE);
                } else if (position == 4) {
                    etServerName.setVisibility(VISIBLE);
                    etServerHost.setVisibility(VISIBLE);
                    etCloudFront.setVisibility(VISIBLE);
                    etHTTPServer.setVisibility(VISIBLE);
                    etServerPass.setVisibility(VISIBLE);
                    etServerUser.setVisibility(VISIBLE);
                    etSSlPort.setVisibility(VISIBLE);
                    etTcpPort.setVisibility(VISIBLE);
                    certSpinner.setVisibility(GONE);
                    ckUseAutoLogin.setVisibility(VISIBLE);
                    port_layout.setVisibility(VISIBLE);
                    psiphon_layout.setVisibility(GONE);
                    publicly.setVisibility(GONE);
                    nameserver.setVisibility(GONE);
                    config1.setVisibility(GONE);
                    btnPayloadGen.setVisibility(GONE);
                } else if (position == 5) {
                    etServerName.setVisibility(VISIBLE);
                    etServerHost.setVisibility(VISIBLE);
                    etCloudFront.setVisibility(VISIBLE);
                    etHTTPServer.setVisibility(VISIBLE);
                    etServerPass.setVisibility(VISIBLE);
                    etServerUser.setVisibility(VISIBLE);
                    etSSlPort.setVisibility(VISIBLE);
                    etTcpPort.setVisibility(VISIBLE);
                    certSpinner.setVisibility(GONE);
                    ckUseAutoLogin.setVisibility(VISIBLE);
                    port_layout.setVisibility(VISIBLE);
                    psiphon_layout.setVisibility(GONE);
                    publicly.setVisibility(GONE);
                    nameserver.setVisibility(GONE);
                    config1.setVisibility(GONE);
                    btnPayloadGen.setVisibility(GONE);
                } else if (position == 6) {
                    etServerName.setVisibility(VISIBLE);
                    etServerHost.setVisibility(GONE);
                    etCloudFront.setVisibility(GONE);
                    etHTTPServer.setVisibility(GONE);
                    etServerPass.setVisibility(VISIBLE);
                    etServerUser.setVisibility(VISIBLE);
                    etSSlPort.setVisibility(GONE);
                    etTcpPort.setVisibility(GONE);
                    certSpinner.setVisibility(GONE);
                    ckUseAutoLogin.setVisibility(VISIBLE);
                    port_layout.setVisibility(GONE);
                    psiphon_layout.setVisibility(GONE);
                    publicly.setVisibility(VISIBLE);
                    nameserver.setVisibility(VISIBLE);
                    config1.setVisibility(GONE);
                    btnPayloadGen.setVisibility(GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        ckUseAutoLogin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                if (isChecked) {
                    etServerUser.setVisibility(VISIBLE);
                    etServerPass.setVisibility(VISIBLE);
                } else {
                    etServerPass.setVisibility(GONE);
                    etServerUser.setVisibility(GONE);
                }
            }
        });
        if (ckUseAutoLogin.isChecked()) {
            etServerUser.setVisibility(VISIBLE);
            etServerPass.setVisibility(VISIBLE);
        } else {
            etServerPass.setVisibility(GONE);
            etServerUser.setVisibility(GONE);
        }
        use_default_servers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                if (isChecked) {
                    server_entry_layout.setVisibility(VISIBLE);
                } else {
                    server_entry_layout.setVisibility(GONE);
                }
            }
        });
        if (use_default_servers.isChecked()) {
            server_entry_layout.setVisibility(VISIBLE);
        } else {
            server_entry_layout.setVisibility(GONE);
        }
        ((Button) dialog.findViewById(R.id.save_server_dialog)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    JSONArray jsonArray = getServersArray();
                    String sName = etServerName.getText().toString();
                    String sHost = etServerHost.getText().toString();
                    String sCF = etCloudFront.getText().toString();
                    String sHS = etHTTPServer.getText().toString();
                    String sCategory = categorySpinner.getSelectedItem().toString();
                    String sTcpPort = etTcpPort.getText().toString();
                    String sSSLPort = etSSlPort.getText().toString();
                    String PubKey = etPubKey.getText().toString();
                    String NameServer = etNameServer.getText().toString();
                    String serverEntry = etServerEntry.getText().toString();
                    String configs = etConfigs.getText().toString();
                    String flagname = (String) flagspin.getSelectedItem();
                    int tunnelType = (int) ProtocolSpinner.getSelectedItemPosition();
                    int certType = certSpinner.getSelectedItemPosition() + 1;

                    String user = "";
                    String pass = "";
                    if (ckUseAutoLogin.isChecked()) {
                        user = etServerUser.getText().toString();
                        pass = etServerPass.getText().toString();
                    }
                    jsonObject.put("Name", sName);
                    jsonObject.put("Flag", flagname);
                    jsonObject.put("ServerIPHost", encrypt(sHost));
                    jsonObject.put("ServerCloudFrontHost", encrypt(sCF));
                    jsonObject.put("ServerHTTPHost", encrypt(sHS));
                    jsonObject.put("Category", sCategory);
                    jsonObject.put("OpenVPNTCPPort", sTcpPort);
                    jsonObject.put("OpenVPNSSLPort", sSSLPort);
                    jsonObject.put("PublicKey", PubKey);
                    jsonObject.put("Nameserver", NameServer);
                    jsonObject.put("Username", encrypt(user));
                    jsonObject.put("Password", encrypt(pass));
                    jsonObject.put("Protocol", tunnelType);

                    jsonObject.put("Payload", encrypt(configs));
                    jsonObject.put("AutoLogin", ckUseAutoLogin.isChecked());
                    jsonObject.put("Cert", certType);
                    jsonObject.put("UseDefaultServers", use_default_servers.isChecked());
                    jsonObject.put("ServerEntry", encrypt(serverEntry));


                    jsonArray.put(jsonObject);
                    JSONObject jo = getJSONObject();
                    jo.remove("Servers");
                    try {
                        jo.put("Servers", jsonArray);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        File file = new File(getFilesDir(), "Servers.js");

                        OutputStream out = new FileOutputStream(file);
                        out.write(jo.toString().getBytes());
                        out.flush();
                        out.close();
                        //   TextView tv=findViewById(R.id.result);
                        //    tv.setText(getJsonString());
                        showToast(sName + " is added Successfully");
                    } catch (Exception e) {
                    }
                    setupServerListView();
                    serverListView.setSelection(listProfiles.size());
                    alert.dismiss();

                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }


        });
        // ShanDevz Telegram @Zenze000
        dialog.findViewById(R.id.btnPayloadGen).setOnClickListener(v -> {
            generateUDPConfig(etConfigs);
        });
    }

    private void updateButtonColors(int selectedId) {
        int selectedColor = Color.parseColor("#2196F3"); // ShanDevz Telegram @Zenze000
        int selectedTextColor = Color.parseColor("#FFFFFFFF"); // ShanDevz Telegram @Zenze000
        int unselectedTextColor = Color.parseColor("#FF000000"); // ShanDevz Telegram @Zenze000
        int unselectedColor = Color.parseColor("#DDDDDD"); //ShanDevz Telegram @Zenze000

        radiobtn_ovpn.setBackgroundColor(selectedId == R.id.radiobtn_ovpn ? selectedColor : unselectedColor);
        radiobtn_ovpn.setTextColor(selectedId == R.id.radiobtn_ovpn ? selectedTextColor : unselectedTextColor);
        radiobtn_ssh.setBackgroundColor(selectedId == R.id.radiobtn_ssh ? selectedColor : unselectedColor);
        radiobtn_ssh.setTextColor(selectedId == R.id.radiobtn_ssh ? selectedTextColor : unselectedTextColor);
    }

    void editServer(int position) {
        try {
            // final DataBaseHelper db = new DataBaseHelper(MainActivity.this, "Server");
            final JSONArray jsonArray = getServersArray();
            // final int position = serverSpinner.getSelectedItemPosition();

            LayoutInflater inflater = LayoutInflater.from(this);
            final View dialog = inflater.inflate(R.layout.server_dialog, null);
            AlertDialog.Builder buidler = new AlertDialog.Builder(this);
            buidler.setTitle("Edit Server");
            buidler.setCancelable(true);
            buidler.setView(dialog);

            final AlertDialog alert = buidler.create();
            alert.getWindow().setGravity(Gravity.CENTER);
            alert.show();

            Spinner flagspin = (Spinner) dialog.findViewById(R.id.flagspin);
            FlagAdapter fa = new FlagAdapter(this, myFlags);
            flagspin.setAdapter(fa);
            EditText etServerName = (EditText) dialog.findViewById(R.id.server_name_dialog);
            EditText etServerHost = (EditText) dialog.findViewById(R.id.server_ip_dialog);
            EditText etCloudFront = (EditText) dialog.findViewById(R.id.etServerCloudFront);
            EditText etHTTPServer = (EditText) dialog.findViewById(R.id.etServerHTTP);
            EditText etTcpPort = dialog.findViewById(R.id.openvpn_tcp_port);
            EditText etSSlPort = dialog.findViewById(R.id.openvpn_ssl_port);
            EditText etPubKey = dialog.findViewById(R.id.etPubKey);
            EditText etNameServer = dialog.findViewById(R.id.etNameServer);
            EditText etServerEntry = dialog.findViewById(R.id.server_entry);
            EditText etConfigs = dialog.findViewById(R.id.etConfigs);
            CheckBox use_default_servers = dialog.findViewById(R.id.use_default_servers);

            Button btnPayloadGen = dialog.findViewById(R.id.btnPayloadGen);
            LinearLayout psiphon_layout = dialog.findViewById(R.id.s7);
            LinearLayout port_layout = dialog.findViewById(R.id.s5);
            TextInputLayout publicly = dialog.findViewById(R.id.publickey);
            TextInputLayout nameserver = dialog.findViewById(R.id.nameserver);
            TextInputLayout config1 = dialog.findViewById(R.id.config1);
            TextInputLayout server_entry_layout = dialog.findViewById(R.id.server_entry_layout);

            Spinner categorySpinner = (Spinner) dialog.findViewById(R.id.category_spin);
            ArrayAdapter categoryAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item);
            categoryAdapter.add("PREMIUM");
            categoryAdapter.add("VIP");
            categoryAdapter.add("PRIVATE");
            categorySpinner.setAdapter(categoryAdapter);

            Spinner ProtocolSpinner = (Spinner) dialog.findViewById(R.id.protocol_spin);
            ArrayAdapter ProtocolAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item);
            ProtocolAdapter.add("OpenVPN");
            ProtocolAdapter.add("UDP");
            ProtocolAdapter.add("V2Ray");
            ProtocolAdapter.add("Psiphon");
            ProtocolAdapter.add("OpenConnect");
            ProtocolAdapter.add("SSH");
            ProtocolAdapter.add("Slow DNS");
            ProtocolSpinner.setAdapter(ProtocolAdapter);

            Spinner certSpinner = (Spinner) dialog.findViewById(R.id.cert);
            ArrayAdapter certAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item);
            certAdapter.add("Cert 1");
            certAdapter.add("Cert 2");
            certAdapter.add("Cert 3");
            certSpinner.setAdapter(certAdapter);

            CheckBox ckUseAutoLogin = (CheckBox) dialog.findViewById(R.id.autoLogin);
            EditText etServerUser = (EditText) dialog.findViewById(R.id.etUsername);
            EditText etServerPass = (EditText) dialog.findViewById(R.id.etPassword);

            ProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //String selectedItem = adapterView.getItemAtPosition(position).toString();

                    if (position == 0) {
                        etServerName.setVisibility(VISIBLE);
                        etServerHost.setVisibility(VISIBLE);
                        etCloudFront.setVisibility(VISIBLE);
                        etHTTPServer.setVisibility(VISIBLE);
                        etServerPass.setVisibility(VISIBLE);
                        etServerUser.setVisibility(VISIBLE);
                        etSSlPort.setVisibility(VISIBLE);
                        etTcpPort.setVisibility(VISIBLE);
                        certSpinner.setVisibility(VISIBLE);
                        ckUseAutoLogin.setVisibility(VISIBLE);
                        port_layout.setVisibility(VISIBLE);
                        psiphon_layout.setVisibility(GONE);
                        publicly.setVisibility(GONE);
                        nameserver.setVisibility(GONE);
                        config1.setVisibility(GONE);
                        btnPayloadGen.setVisibility(GONE);
                    } else if (position == 1) {
                        etServerName.setVisibility(VISIBLE);
                        etServerHost.setVisibility(GONE);
                        etCloudFront.setVisibility(GONE);
                        etHTTPServer.setVisibility(GONE);
                        etServerPass.setVisibility(GONE);
                        etServerUser.setVisibility(GONE);
                        etSSlPort.setVisibility(GONE);
                        etTcpPort.setVisibility(GONE);
                        certSpinner.setVisibility(GONE);
                        ckUseAutoLogin.setVisibility(GONE);
                        port_layout.setVisibility(GONE);
                        psiphon_layout.setVisibility(GONE);
                        publicly.setVisibility(GONE);
                        nameserver.setVisibility(GONE);
                        config1.setVisibility(VISIBLE);
                        btnPayloadGen.setVisibility(VISIBLE);
                    } else if (position == 2) {
                        etServerName.setVisibility(VISIBLE);
                        etServerHost.setVisibility(GONE);
                        etCloudFront.setVisibility(GONE);
                        etHTTPServer.setVisibility(GONE);
                        etServerPass.setVisibility(GONE);
                        etServerUser.setVisibility(GONE);
                        etSSlPort.setVisibility(GONE);
                        etTcpPort.setVisibility(GONE);
                        certSpinner.setVisibility(GONE);
                        ckUseAutoLogin.setVisibility(GONE);
                        port_layout.setVisibility(GONE);
                        psiphon_layout.setVisibility(GONE);
                        publicly.setVisibility(GONE);
                        nameserver.setVisibility(GONE);
                        config1.setVisibility(VISIBLE);
                        btnPayloadGen.setVisibility(GONE);
                    } else if (position == 3) {
                        etServerName.setVisibility(VISIBLE);
                        etServerHost.setVisibility(GONE);
                        etCloudFront.setVisibility(GONE);
                        etHTTPServer.setVisibility(GONE);
                        etServerPass.setVisibility(GONE);
                        etServerUser.setVisibility(GONE);
                        etSSlPort.setVisibility(GONE);
                        etTcpPort.setVisibility(GONE);
                        certSpinner.setVisibility(GONE);
                        ckUseAutoLogin.setVisibility(GONE);
                        port_layout.setVisibility(GONE);
                        psiphon_layout.setVisibility(VISIBLE);
                        publicly.setVisibility(GONE);
                        nameserver.setVisibility(GONE);
                        config1.setVisibility(GONE);
                        btnPayloadGen.setVisibility(GONE);
                    } else if (position == 4) {
                        etServerName.setVisibility(VISIBLE);
                        etServerHost.setVisibility(VISIBLE);
                        etCloudFront.setVisibility(VISIBLE);
                        etHTTPServer.setVisibility(VISIBLE);
                        etServerPass.setVisibility(VISIBLE);
                        etServerUser.setVisibility(VISIBLE);
                        etSSlPort.setVisibility(VISIBLE);
                        etTcpPort.setVisibility(VISIBLE);
                        certSpinner.setVisibility(GONE);
                        ckUseAutoLogin.setVisibility(VISIBLE);
                        port_layout.setVisibility(VISIBLE);
                        psiphon_layout.setVisibility(GONE);
                        publicly.setVisibility(GONE);
                        nameserver.setVisibility(GONE);
                        config1.setVisibility(GONE);
                        btnPayloadGen.setVisibility(GONE);
                    } else if (position == 5) {
                        etServerName.setVisibility(VISIBLE);
                        etServerHost.setVisibility(VISIBLE);
                        etCloudFront.setVisibility(VISIBLE);
                        etHTTPServer.setVisibility(VISIBLE);
                        etServerPass.setVisibility(VISIBLE);
                        etServerUser.setVisibility(VISIBLE);
                        etSSlPort.setVisibility(VISIBLE);
                        etTcpPort.setVisibility(VISIBLE);
                        certSpinner.setVisibility(GONE);
                        ckUseAutoLogin.setVisibility(VISIBLE);
                        port_layout.setVisibility(VISIBLE);
                        psiphon_layout.setVisibility(GONE);
                        publicly.setVisibility(GONE);
                        nameserver.setVisibility(GONE);
                        config1.setVisibility(GONE);
                        btnPayloadGen.setVisibility(GONE);
                    } else if (position == 6) {
                        etServerName.setVisibility(VISIBLE);
                        etServerHost.setVisibility(GONE);
                        etCloudFront.setVisibility(GONE);
                        etHTTPServer.setVisibility(GONE);
                        etServerPass.setVisibility(VISIBLE);
                        etServerUser.setVisibility(VISIBLE);
                        etSSlPort.setVisibility(GONE);
                        etTcpPort.setVisibility(GONE);
                        certSpinner.setVisibility(GONE);
                        ckUseAutoLogin.setVisibility(VISIBLE);
                        port_layout.setVisibility(GONE);
                        psiphon_layout.setVisibility(GONE);
                        publicly.setVisibility(VISIBLE);
                        nameserver.setVisibility(VISIBLE);
                        config1.setVisibility(GONE);
                        btnPayloadGen.setVisibility(GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            ckUseAutoLogin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                    if (isChecked) {
                        etServerUser.setVisibility(VISIBLE);
                        etServerPass.setVisibility(VISIBLE);
                    } else {
                        etServerPass.setVisibility(GONE);
                        etServerUser.setVisibility(GONE);
                    }
                }
            });
            use_default_servers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                    if (isChecked) {
                        server_entry_layout.setVisibility(VISIBLE);
                    } else {
                        server_entry_layout.setVisibility(GONE);
                    }
                }
            });
            if (use_default_servers.isChecked()) {
                server_entry_layout.setVisibility(VISIBLE);
            } else {
                server_entry_layout.setVisibility(GONE);
            }
            JSONObject obj = jsonArray.getJSONObject(position);

            switch (obj.getInt("Protocol")) {
                case 0:
                    ProtocolSpinner.setSelection(0);
                    break;
                case 1:
                    ProtocolSpinner.setSelection(1);
                    break;
                case 2:
                    ProtocolSpinner.setSelection(2);
                    break;
                case 3:
                    ProtocolSpinner.setSelection(3);
                    break;
                case 4:
                    ProtocolSpinner.setSelection(4);
                    break;
                case 5:
                    ProtocolSpinner.setSelection(5);
                    break;
                case 6:
                    ProtocolSpinner.setSelection(6);
                    break;
            }

            switch (obj.getString("Category")) {
                case "PREMIUM":
                    categorySpinner.setSelection(0);
                    break;
                case "VIP":
                    categorySpinner.setSelection(1);
                    break;
                case "PRIVATE":
                    categorySpinner.setSelection(2);
                    break;
            }

            switch (obj.getInt("Cert")) {
                case 1:
                    certSpinner.setSelection(0);
                    break;
                case 2:
                    certSpinner.setSelection(1);
                    break;
                case 3:
                    certSpinner.setSelection(2);
                    break;
            }

            if (obj.has("Username")) {
                etServerUser.setText(decrypt(obj.getString("Username")));
            }
            if (obj.has("Password")) {
                etServerPass.setText(decrypt(obj.getString("Password")));
            }

            if (obj.has("Payload")) {
                etConfigs.setText(decrypt(obj.getString("Payload").replace("~!~", "")));
            }

            if (obj.has("Flag")) {
                flagspin.setSelection(flagPosition(obj.getString("Flag")));
            }
            etServerName.setText(obj.getString("Name"));
            etServerHost.setText(decrypt(obj.getString("ServerIPHost")));
            etServerEntry.setText(decrypt(obj.getString("ServerEntry")));
            if (obj.has("ServerCloudFrontHost")) {
                etCloudFront.setText(decrypt(obj.getString("ServerCloudFrontHost")));
            }
            if (obj.has("ServerHTTPHost")) {
                etHTTPServer.setText(decrypt(obj.getString("ServerHTTPHost")));
            }
            etTcpPort.setText(obj.getString("OpenVPNTCPPort"));
            etSSlPort.setText(obj.getString("OpenVPNSSLPort"));

            etSSlPort.setText(obj.getString("OpenVPNSSLPort"));

            ckUseAutoLogin.setChecked(obj.getBoolean("AutoLogin"));

            use_default_servers.setChecked(obj.getBoolean("UseDefaultServers"));

            if (obj.getBoolean("AutoLogin")) {
                etServerUser.setVisibility(VISIBLE);
                etServerPass.setVisibility(VISIBLE);
            } else {
                etServerPass.setVisibility(GONE);
                etServerUser.setVisibility(GONE);
            }

            ((Button) dialog.findViewById(R.id.save_server_dialog)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View p1) {
                    try {

                        final JSONObject jsonObject = new JSONObject();

                        String sName = etServerName.getText().toString();
                        String sHost = etServerHost.getText().toString();
                        String sCF = etCloudFront.getText().toString();
                        String sHS = etHTTPServer.getText().toString();
                        String sCategory = categorySpinner.getSelectedItem().toString();
                        String sTcpPort = etTcpPort.getText().toString();
                        String sSSLPort = etSSlPort.getText().toString();
                        String PubKey = etPubKey.getText().toString();
                        String NameServer = etNameServer.getText().toString();
                        String serverEntry = etServerEntry.getText().toString();
                        String configs = etConfigs.getText().toString();
                        String flagname = (String) flagspin.getSelectedItem();
                        int tunnelType = (int) ProtocolSpinner.getSelectedItemPosition();
                        int certType = certSpinner.getSelectedItemPosition() + 1;

                        String user = "";
                        String pass = "";
                        if (ckUseAutoLogin.isChecked()) {
                            user = etServerUser.getText().toString();
                            pass = etServerPass.getText().toString();
                        }
                        jsonObject.put("Name", sName);
                        jsonObject.put("Flag", flagname);
                        jsonObject.put("ServerIPHost", encrypt(sHost));
                        jsonObject.put("ServerCloudFrontHost", encrypt(sCF));
                        jsonObject.put("ServerHTTPHost", encrypt(sHS));
                        jsonObject.put("Category", sCategory);
                        jsonObject.put("OpenVPNTCPPort", sTcpPort);
                        jsonObject.put("OpenVPNSSLPort", sSSLPort);
                        jsonObject.put("PublicKey", PubKey);
                        jsonObject.put("Nameserver", NameServer);
                        jsonObject.put("Username", encrypt(user));
                        jsonObject.put("Password", encrypt(pass));
                        jsonObject.put("Protocol", tunnelType);

                        jsonObject.put("Payload", encrypt(configs));
                        jsonObject.put("AutoLogin", ckUseAutoLogin.isChecked());
                        jsonObject.put("Cert", certType);
                        jsonObject.put("UseDefaultServers", use_default_servers.isChecked());
                        jsonObject.put("ServerEntry", encrypt(serverEntry));

                        jsonArray.put(position, jsonObject);

                        JSONObject jo = getJSONObject();
                        jo.remove("Servers");
                        try {
                            jo.put("Servers", jsonArray);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        try {
                            File file = new File(getFilesDir(), "Servers.js");

                            OutputStream out = new FileOutputStream(file);
                            out.write(jo.toString().getBytes());
                            out.flush();
                            out.close();
                            //   TextView tv=findViewById(R.id.result);
                            //   tv.setText(getJsonString());
                            showToast(sName + " is edited Successfully");
                        } catch (Exception e) {
                        }
                        setupServerListView();
                        serverListView.setSelection(listProfiles.size());
                        alert.dismiss();

                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }


            });
            // ShanDevz Telegram @Zenze000
        } catch (JSONException e) {
            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void removeServer(int position, String name) {
        try {

            new CuteDialog.withAnimation(this)
                    .setAnimation(R.raw.delete)
                    .setTitle(name)
                    .setDescription("Are you sure, you want to delete this item?")
                    .setPositiveButtonText("Okay", v2 -> {
                        try {
                            //  DataBaseHelper db = new DataBaseHelper(MainActivity.this, "Server");
                            JSONArray jarr = getServersArray();
                            jarr.remove(position);
                            JSONObject jo = getJSONObject();
                            jo.remove("Servers");
                            try {
                                jo.put("Servers", jarr);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                            try {
                                File file = new File(getFilesDir(), "Servers.js");
                                OutputStream out = new FileOutputStream(file);
                                out.write(jo.toString().getBytes());
                                out.flush();
                                out.close();
                                //  TextView tv=findViewById(R.id.result);
                                //   tv.setText(getJsonString());

                            } catch (Exception e) {
                            }


                            setupServerListView();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButtonText("Cancel", v2 -> {

                    })
                    .show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Empty", Toast.LENGTH_SHORT).show();
        }
    }

    public class FlagAdapter extends ArrayAdapter<String> {
        private Context context;

        public FlagAdapter(Context context, ArrayList<String> listServer) {
            super(context, R.layout.flag_items, listServer);
            this.context = context;
        }

        @Override
        public String getItem(int position) {
            // TODO: Implement this method
            return super.getItem(position);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = MyView(position, convertView, parent, true);
            return v;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO: Implement this method
            return MyView(position, convertView, parent, false);
        }

        public View MyView(int position, View convertView, ViewGroup parent, boolean isdrop) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.flag_items, parent, false);
            TextView tv = (TextView) v.findViewById(R.id.server_item_text);
            ImageView iv = (ImageView) v.findViewById(R.id.server_item_icon);
            try {
                String name = getItem(position);
                tv.setText(name);

                setFlag(iv, name);

            } catch (Exception e) {

            }
            // TODO: Implement this method
            return v;
        }

        public void setFlag(ImageView f, String ff) {
            try {
                InputStream open = context.getAssets().open(new StringBuffer().append("flag/").append(ff).toString());
                f.setImageDrawable(Drawable.createFromStream(open, (String) null));
                if (open != null) {
                    open.close();
                }
            } catch (Exception e) {
                f.setImageResource(R.mipmap.ic_launcher);
            }
        }
    }

    public int flagPosition(String name) {
        ArrayList<String> f = myFlags;
        for (int i = 0; i <= f.size(); i++) {
            if (f.get(i).equals(name)) {
                return i;
            }
        }
        return 0;
    }

    public ArrayList<String> listFlags() {
        ArrayList<String> flags = new ArrayList<String>();
        String[] list;
        try {
            list = getAssets().list("flag/");
            if (list.length > 0) {
                for (String file : list) {
                    flags.add(file);
                }
            }
        } catch (IOException e) {

        }
        return flags;
    }

    void addPayload(String isProtocol) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View v = inflater.inflate(R.layout.network_dialog, null);
        AlertDialog.Builder buidler = new AlertDialog.Builder(this);
        buidler.setTitle("Add Payload");
        buidler.setCancelable(true);
        buidler.setView(v);
        EditText etNetworkName = (EditText) v.findViewById(R.id.network_name_dialog);
        EditText etNetworkPayload = (EditText) v.findViewById(R.id.network_payload_dialog);
        EditText etNetworkProxyIP = (EditText) v.findViewById(R.id.proxy_edit);
        EditText etNetworkProxyPort = (EditText) v.findViewById(R.id.proxy_port_edit);
        CheckBox ckUseDefProxy = (CheckBox) v.findViewById(R.id.use_default_proxy);
        EditText etNetworkInfo = (EditText) v.findViewById(R.id.network_info_dialog);
        EditText etFrontQuery = (EditText) v.findViewById(R.id.network_front_query);
        EditText etBackQuery = (EditText) v.findViewById(R.id.network_back_query);

        CheckBox is_custom_config = (CheckBox) v.findViewById(R.id.is_custom_config);
        RadioGroup modeGroup = (RadioGroup) v.findViewById(R.id.modeGroup);
        Button btnPayloadGen = (Button) v.findViewById(R.id.btnPayloadGen);

        String[] prior = new String[]{"Priority Low", "Priority Medium", "Priority High"};
        Spinner stPriority = (Spinner) v.findViewById(R.id.priority_spin);
        ArrayAdapter ad2 = new ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                prior);
        ad2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stPriority.setAdapter(ad2);


        String[] proto = new String[]{"HTTP Proxy", "UDP Hysteria", "V2Ray", "Psiphon", "OpenConnect", "SSH", "Slow DNS"};
        Spinner stProto = (Spinner) v.findViewById(R.id.proto_spin);
        ArrayAdapter ad3 = new ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                proto);
        ad3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stProto.setAdapter(ad3);

        RadioButton cf = (RadioButton) v.findViewById(R.id.cf_radio);
        RadioButton ws = (RadioButton) v.findViewById(R.id.ws_radio);
        RadioButton http = (RadioButton) v.findViewById(R.id.http_radio);

        RadioButton mode1 = (RadioButton) v.findViewById(R.id.mode1);
        RadioButton mode2 = (RadioButton) v.findViewById(R.id.mode2);
        RadioButton mode3 = (RadioButton) v.findViewById(R.id.mode3);

        stProto.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                    ex.setVisibility(VISIBLE);
                    TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                    tl.setHint("Payload");
                    etNetworkName.setVisibility(VISIBLE);
                    etNetworkPayload.setVisibility(VISIBLE);
                    etNetworkProxyIP.setVisibility(VISIBLE);
                    etNetworkProxyPort.setVisibility(VISIBLE);
                    etNetworkInfo.setVisibility(VISIBLE);
                    etBackQuery.setVisibility(VISIBLE);
                    etFrontQuery.setVisibility(VISIBLE);
                    is_custom_config.setVisibility(GONE);
                    modeGroup.setVisibility(GONE);
                    cf.setVisibility(VISIBLE);
                    ws.setVisibility(VISIBLE);
                    http.setVisibility(VISIBLE);
                    btnPayloadGen.setVisibility(VISIBLE);
                } else if (position == 1) {
                    LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                    ex.setVisibility(GONE);
                    TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                    tl.setHint("Config.json");
                    if (position == 4) {
                        tl.setHint("UDP Config/Json");
                    }
                    etNetworkName.setVisibility(VISIBLE);
                    if (is_custom_config.isChecked()) {
                        etNetworkPayload.setVisibility(VISIBLE);
                    } else {
                        etNetworkPayload.setVisibility(GONE);
                    }

                    etNetworkProxyIP.setVisibility(GONE);
                    etNetworkProxyPort.setVisibility(GONE);
                    etNetworkInfo.setVisibility(VISIBLE);
                    etBackQuery.setVisibility(GONE);
                    etFrontQuery.setVisibility(GONE);
                    is_custom_config.setVisibility(VISIBLE);
                    modeGroup.setVisibility(VISIBLE);
                    cf.setVisibility(GONE);
                    ws.setVisibility(GONE);
                    http.setVisibility(GONE);
                    btnPayloadGen.setVisibility(GONE);
                } else if (position == 2) {
                    LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                    ex.setVisibility(GONE);
                    TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                    tl.setHint("Config.json");
                    if (position == 4) {
                        tl.setHint("V2ray Config/Json");
                    }
                    etNetworkName.setVisibility(VISIBLE);
                    if (is_custom_config.isChecked()) {
                        etNetworkPayload.setVisibility(VISIBLE);
                    } else {
                        etNetworkPayload.setVisibility(GONE);
                    }
                    etNetworkProxyIP.setVisibility(GONE);
                    etNetworkProxyPort.setVisibility(GONE);
                    etNetworkInfo.setVisibility(VISIBLE);
                    etBackQuery.setVisibility(GONE);
                    etFrontQuery.setVisibility(GONE);
                    is_custom_config.setVisibility(VISIBLE);
                    modeGroup.setVisibility(VISIBLE);
                    cf.setVisibility(GONE);
                    ws.setVisibility(GONE);
                    http.setVisibility(GONE);
                    btnPayloadGen.setVisibility(GONE);
                } else if (position == 3) {
                    LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                    ex.setVisibility(GONE);
                    etNetworkName.setVisibility(VISIBLE);
                    etNetworkPayload.setVisibility(GONE);
                    etNetworkProxyIP.setVisibility(GONE);
                    etNetworkProxyPort.setVisibility(GONE);
                    etNetworkInfo.setVisibility(VISIBLE);
                    etBackQuery.setVisibility(GONE);
                    etFrontQuery.setVisibility(GONE);
                    is_custom_config.setVisibility(GONE);
                    modeGroup.setVisibility(GONE);
                    cf.setVisibility(GONE);
                    ws.setVisibility(GONE);
                    http.setVisibility(GONE);
                    btnPayloadGen.setVisibility(GONE);
                } else if (position == 4) {
                    LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                    ex.setVisibility(VISIBLE);
                    TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                    tl.setHint("Payload");
                    etNetworkName.setVisibility(VISIBLE);
                    etNetworkPayload.setVisibility(VISIBLE);
                    etNetworkProxyIP.setVisibility(VISIBLE);
                    etNetworkProxyPort.setVisibility(VISIBLE);
                    etNetworkInfo.setVisibility(VISIBLE);
                    etBackQuery.setVisibility(VISIBLE);
                    etFrontQuery.setVisibility(VISIBLE);
                    is_custom_config.setVisibility(GONE);
                    modeGroup.setVisibility(GONE);
                    cf.setVisibility(VISIBLE);
                    ws.setVisibility(VISIBLE);
                    http.setVisibility(VISIBLE);
                    btnPayloadGen.setVisibility(VISIBLE);
                } else if (position == 5) {
                    LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                    ex.setVisibility(VISIBLE);
                    TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                    tl.setHint("Payload");
                    etNetworkName.setVisibility(VISIBLE);
                    etNetworkPayload.setVisibility(VISIBLE);
                    etNetworkProxyIP.setVisibility(VISIBLE);
                    etNetworkProxyPort.setVisibility(VISIBLE);
                    etNetworkInfo.setVisibility(VISIBLE);
                    etBackQuery.setVisibility(VISIBLE);
                    etFrontQuery.setVisibility(VISIBLE);
                    is_custom_config.setVisibility(GONE);
                    modeGroup.setVisibility(GONE);
                    cf.setVisibility(VISIBLE);
                    ws.setVisibility(VISIBLE);
                    http.setVisibility(VISIBLE);
                    btnPayloadGen.setVisibility(VISIBLE);
                } else if (position == 6) {
                    LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                    ex.setVisibility(GONE);
                    TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                    tl.setHint("DNS Address");
                    etNetworkName.setVisibility(VISIBLE);
                    etNetworkPayload.setVisibility(VISIBLE);
                    etNetworkProxyIP.setVisibility(GONE);
                    etNetworkProxyPort.setVisibility(GONE);
                    etNetworkInfo.setVisibility(VISIBLE);
                    etBackQuery.setVisibility(GONE);
                    etFrontQuery.setVisibility(GONE);
                    is_custom_config.setVisibility(GONE);
                    modeGroup.setVisibility(GONE);
                    cf.setVisibility(VISIBLE);
                    ws.setVisibility(VISIBLE);
                    http.setVisibility(VISIBLE);
                    btnPayloadGen.setVisibility(VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        is_custom_config.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                if (isChecked) {
                    etNetworkPayload.setVisibility(VISIBLE);
                } else {
                    etNetworkPayload.setVisibility(GONE);
                }
            }
        });

        cf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ws.setChecked(false);
                http.setChecked(false);
                cf.setChecked(true);
            }
        });
        ws.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ws.setChecked(true);
                http.setChecked(false);
                cf.setChecked(false);
            }
        });
        http.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ws.setChecked(false);
                http.setChecked(true);
                cf.setChecked(false);
            }
        });
        cf.callOnClick();

        mode1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode1.setChecked(true);
                mode2.setChecked(false);
                mode3.setChecked(false);
            }
        });
        mode2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode1.setChecked(false);
                mode2.setChecked(true);
                mode3.setChecked(false);
            }
        });
        mode3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode1.setChecked(false);
                mode2.setChecked(false);
                mode3.setChecked(true);
            }
        });
        mode1.callOnClick();

        etNetworkProxyIP.setEnabled(false);
        etNetworkProxyIP.setText("[Default]");
        etNetworkProxyPort.setText("8080");
        ckUseDefProxy.setChecked(true);
        ckUseDefProxy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean p2) {
                if (p2) {
                    etNetworkProxyIP.setEnabled(false);
                    etNetworkProxyIP.setText("[Default]");
                } else {
                    etNetworkProxyIP.setEnabled(true);
                    etNetworkProxyIP.setText("");
                }
            }
        });

        if (isProtocol.equals("ovpn")) {
            stProto.setSelection(0);
        } else if (isProtocol.equals("udp")) {
            stProto.setSelection(1);
        } else if (isProtocol.equals("v2ray")) {
            stProto.setSelection(2);
        } else if (isProtocol.equals("psiphon")) {
            stProto.setSelection(3);
        } else if (isProtocol.equals("openConnect")) {
            stProto.setSelection(4);
        } else if (isProtocol.equals("ssh")) {
            stProto.setSelection(5);
        } else if (isProtocol.equals("slowdns")) {
            stProto.setSelection(6);
        }
        final AlertDialog alert = buidler.create();
        alert.getWindow().setGravity(Gravity.CENTER);
        alert.show();
        ((Button) v.findViewById(R.id.btnSaveNetwork)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    JSONArray jsonArray = getNetworksArray();
                    String sName = etNetworkName.getText().toString();
                    String sPayload = etNetworkPayload.getText().toString();
                    String sSquidProxy = etNetworkProxyIP.getText().toString();
                    String sSquidPort = etNetworkProxyPort.getText().toString();
                    String sNetworkInfo = etNetworkInfo.getText().toString();
                    String frontQ = etFrontQuery.getText().toString();
                    String backQ = etBackQuery.getText().toString();

                    jsonObject.put("Name", sName);
                    jsonObject.put("Payload", encrypt(sPayload));
                    jsonObject.put("Info", sNetworkInfo);
                    int i = stProto.getSelectedItemPosition();

                    if (i == 0) {
                        jsonObject.put("TunnelType", 2);
                        jsonObject.put("Protocol", 0);
                    } else if (i == 1) {
                        jsonObject.put("TunnelType", 6);
                        jsonObject.put("Protocol", 0);
                    } else if (i == 2) {
                        jsonObject.put("TunnelType", 7);
                        jsonObject.put("Protocol", 0);
                    } else if (i == 3) {
                        jsonObject.put("TunnelType", 8);
                        jsonObject.put("Protocol", 0);
                    } else if (i == 4) {
                        jsonObject.put("TunnelType", 2);
                        jsonObject.put("Protocol", 1);
                    } else if (i == 5) {
                        jsonObject.put("TunnelType", 2);
                        jsonObject.put("Protocol", 2);
                    } else if (i == 6) {
                        jsonObject.put("TunnelType", 9);
                        jsonObject.put("Protocol", 0);
                    }

                    jsonObject.put("FrontQuery", frontQ);
                    jsonObject.put("BackQuery", backQ);
                    jsonObject.put("Priority", stPriority.getSelectedItemPosition());

                    if (cf.isChecked()) {
                        jsonObject.put("ServerHostDNS", "cf");
                    }
                    if (ws.isChecked()) {
                        jsonObject.put("ServerHostDNS", "ws");
                    }
                    if (http.isChecked()) {
                        jsonObject.put("ServerHostDNS", "http");
                    }

                    if (mode1.isChecked()) {
                        jsonObject.put("Mode", 1);
                    }
                    if (mode2.isChecked()) {
                        jsonObject.put("Mode", 2);
                    }
                    if (mode3.isChecked()) {
                        jsonObject.put("Mode", 3);
                    }

                    JSONObject proxy = new JSONObject();
                    proxy.put("Squid", encrypt(sSquidProxy));
                    proxy.put("Port", sSquidPort);
                    jsonObject.put("ProxySettings", proxy);

                    jsonObject.put("isCustomConfig", is_custom_config.isChecked());

                    jsonArray.put(jsonObject);
                    JSONObject jo = getJSONObject();
                    jo.remove("Networks");
                    try {
                        jo.put("Networks", jsonArray);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        File file = new File(getFilesDir(), "Servers.js");

                        OutputStream out = new FileOutputStream(file);
                        out.write(jo.toString().getBytes());
                        out.flush();
                        out.close();
                        //   TextView tv=findViewById(R.id.result);
                        //    tv.setText(getJsonString());
                        showToast(sName + " is added Successfully");
                    } catch (Exception e) {
                    }
                    setupNetowrksListView();
                    networkListView.setSelection(listNetworks.size());
                    alert.dismiss();

                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        ((Button) v.findViewById(R.id.btnPayloadGen)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {
                PayloadGeneratorDialog payloadGenerator = new PayloadGeneratorDialog(MainActivity.this);
                payloadGenerator.setDialogTitle("Payload Generator");
                payloadGenerator.setGenerateListener("Save", new PayloadGeneratorDialog.OnGenerateListener() {
                    @Override
                    public void onGenerate(String payloadGenerated) {
                        etNetworkPayload.setText(payloadGenerated);
                    }
                });
                payloadGenerator.show();

            }
        });
    }

    void editPayload(int position) {
        try {

            final JSONArray jsonArray = getNetworksArray();

            LayoutInflater inflater = LayoutInflater.from(this);
            final View v = inflater.inflate(R.layout.network_dialog, null);
            AlertDialog.Builder buidler = new AlertDialog.Builder(this);
            buidler.setTitle("Edit Payload");
            buidler.setView(v);
            buidler.setCancelable(true);

            EditText etNetworkName = (EditText) v.findViewById(R.id.network_name_dialog);
            EditText etNetworkPayload = (EditText) v.findViewById(R.id.network_payload_dialog);
            EditText etNetworkProxyIP = (EditText) v.findViewById(R.id.proxy_edit);
            EditText etNetworkProxyPort = (EditText) v.findViewById(R.id.proxy_port_edit);
            CheckBox ckUseDefProxy = (CheckBox) v.findViewById(R.id.use_default_proxy);
            EditText etNetworkInfo = (EditText) v.findViewById(R.id.network_info_dialog);
            EditText etFrontQuery = (EditText) v.findViewById(R.id.network_front_query);
            EditText etBackQuery = (EditText) v.findViewById(R.id.network_back_query);

            CheckBox is_custom_config = (CheckBox) v.findViewById(R.id.is_custom_config);
            RadioGroup modeGroup = (RadioGroup) v.findViewById(R.id.modeGroup);
            Button btnPayloadGen = (Button) v.findViewById(R.id.btnPayloadGen);

            etNetworkProxyIP.setEnabled(false);
            ckUseDefProxy.setChecked(true);
            ckUseDefProxy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton p1, boolean p2) {
                    if (p2) {
                        etNetworkProxyIP.setEnabled(false);
                        etNetworkProxyIP.setText("[Default]");
                    } else {
                        etNetworkProxyIP.setEnabled(true);
                    }
                }
            });

            JSONObject obj = jsonArray.getJSONObject(position);
            if (obj.toString().contains("FrontQuery")) {
                etFrontQuery.setText(obj.getString("FrontQuery"));
            }
            if (obj.toString().contains("BackQuery")) {
                etBackQuery.setText(obj.getString("BackQuery"));

            }
            etNetworkName.setText(obj.getString("Name"));
            if (!obj.getString("Info").isEmpty()) {
                etNetworkInfo.setText(obj.getString("Info"));
            }
            if (!obj.getString("Payload").isEmpty()) {
                etNetworkPayload.setText(parseToString(decrypt(obj.getString("Payload"))));
            }


            RadioButton cf = (RadioButton) v.findViewById(R.id.cf_radio);
            RadioButton ws = (RadioButton) v.findViewById(R.id.ws_radio);
            RadioButton http = (RadioButton) v.findViewById(R.id.http_radio);

            RadioButton mode1 = (RadioButton) v.findViewById(R.id.mode1);
            RadioButton mode2 = (RadioButton) v.findViewById(R.id.mode2);
            RadioButton mode3 = (RadioButton) v.findViewById(R.id.mode3);

            cf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ws.setChecked(false);
                    http.setChecked(false);
                    cf.setChecked(true);
                }
            });
            ws.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ws.setChecked(true);
                    http.setChecked(false);
                    cf.setChecked(false);
                }
            });
            http.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ws.setChecked(false);
                    http.setChecked(true);
                    cf.setChecked(false);
                }
            });
            cf.callOnClick();

            mode1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mode1.setChecked(true);
                    mode2.setChecked(false);
                    mode3.setChecked(false);
                }
            });
            mode2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mode1.setChecked(false);
                    mode2.setChecked(true);
                    mode3.setChecked(false);
                }
            });
            mode3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mode1.setChecked(false);
                    mode2.setChecked(false);
                    mode3.setChecked(true);
                }
            });
            mode1.callOnClick();

            if (obj.toString().contains("ServerHostDNS")) {
                String sg = obj.getString("ServerHostDNS");
                if (sg.equals("ws")) {
                    ws.callOnClick();
                }
                if (sg.equals("cf")) {
                    cf.callOnClick();
                }
                if (sg.equals("http")) {
                    http.callOnClick();
                }
            } else {
                cf.callOnClick();
            }
            if (obj.toString().contains("Mode")) {
                if (obj.getInt("Mode") == 1) {
                    mode1.callOnClick();
                }
                if (obj.getInt("Mode") == 2) {
                    mode2.callOnClick();
                }
                if (obj.getInt("Mode") == 3) {
                    mode3.callOnClick();
                }
            } else {
                mode1.callOnClick();
            }
            String[] proto = new String[]{"HTTP Proxy", "UDP Hysteria", "V2Ray", "Psiphon", "OpenConnect", "SSH", "Slow DNS"};
            Spinner stProto = (Spinner) v.findViewById(R.id.proto_spin);
            ArrayAdapter ad3 = new ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    proto);
            ad3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stProto.setAdapter(ad3);
            stProto.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                        ex.setVisibility(VISIBLE);
                        TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                        tl.setHint("Payload");
                        etNetworkName.setVisibility(VISIBLE);
                        etNetworkPayload.setVisibility(VISIBLE);
                        etNetworkProxyIP.setVisibility(VISIBLE);
                        etNetworkProxyPort.setVisibility(VISIBLE);
                        etNetworkInfo.setVisibility(VISIBLE);
                        etBackQuery.setVisibility(VISIBLE);
                        etFrontQuery.setVisibility(VISIBLE);
                        is_custom_config.setVisibility(GONE);
                        modeGroup.setVisibility(GONE);
                        cf.setVisibility(VISIBLE);
                        ws.setVisibility(VISIBLE);
                        http.setVisibility(VISIBLE);
                        btnPayloadGen.setVisibility(VISIBLE);
                    } else if (position == 1) {
                        LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                        ex.setVisibility(GONE);
                        TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                        tl.setHint("Config.json");
                        if (position == 4) {
                            tl.setHint("UDP Config/Json");
                        }
                        etNetworkName.setVisibility(VISIBLE);
                        if (is_custom_config.isChecked()) {
                            etNetworkPayload.setVisibility(VISIBLE);
                        } else {
                            etNetworkPayload.setVisibility(GONE);
                        }

                        etNetworkProxyIP.setVisibility(GONE);
                        etNetworkProxyPort.setVisibility(GONE);
                        etNetworkInfo.setVisibility(VISIBLE);
                        etBackQuery.setVisibility(GONE);
                        etFrontQuery.setVisibility(GONE);
                        is_custom_config.setVisibility(VISIBLE);
                        modeGroup.setVisibility(VISIBLE);
                        cf.setVisibility(GONE);
                        ws.setVisibility(GONE);
                        http.setVisibility(GONE);
                        btnPayloadGen.setVisibility(GONE);
                    } else if (position == 2) {
                        LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                        ex.setVisibility(GONE);
                        TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                        tl.setHint("Config.json");
                        if (position == 4) {
                            tl.setHint("V2ray Config/Json");
                        }
                        etNetworkName.setVisibility(VISIBLE);
                        if (is_custom_config.isChecked()) {
                            etNetworkPayload.setVisibility(VISIBLE);
                        } else {
                            etNetworkPayload.setVisibility(GONE);
                        }
                        etNetworkProxyIP.setVisibility(GONE);
                        etNetworkProxyPort.setVisibility(GONE);
                        etNetworkInfo.setVisibility(VISIBLE);
                        etBackQuery.setVisibility(GONE);
                        etFrontQuery.setVisibility(GONE);
                        is_custom_config.setVisibility(VISIBLE);
                        modeGroup.setVisibility(VISIBLE);
                        cf.setVisibility(GONE);
                        ws.setVisibility(GONE);
                        http.setVisibility(GONE);
                        btnPayloadGen.setVisibility(GONE);
                    } else if (position == 3) {
                        LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                        ex.setVisibility(GONE);
                        etNetworkName.setVisibility(VISIBLE);
                        etNetworkPayload.setVisibility(GONE);
                        etNetworkProxyIP.setVisibility(GONE);
                        etNetworkProxyPort.setVisibility(GONE);
                        etNetworkInfo.setVisibility(VISIBLE);
                        etBackQuery.setVisibility(GONE);
                        etFrontQuery.setVisibility(GONE);
                        is_custom_config.setVisibility(GONE);
                        modeGroup.setVisibility(GONE);
                        cf.setVisibility(GONE);
                        ws.setVisibility(GONE);
                        http.setVisibility(GONE);
                        btnPayloadGen.setVisibility(GONE);
                    } else if (position == 4) {
                        LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                        ex.setVisibility(VISIBLE);
                        TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                        tl.setHint("Payload");
                        etNetworkName.setVisibility(VISIBLE);
                        etNetworkPayload.setVisibility(VISIBLE);
                        etNetworkProxyIP.setVisibility(VISIBLE);
                        etNetworkProxyPort.setVisibility(VISIBLE);
                        etNetworkInfo.setVisibility(VISIBLE);
                        etBackQuery.setVisibility(VISIBLE);
                        etFrontQuery.setVisibility(VISIBLE);
                        is_custom_config.setVisibility(GONE);
                        modeGroup.setVisibility(GONE);
                        cf.setVisibility(VISIBLE);
                        ws.setVisibility(VISIBLE);
                        http.setVisibility(VISIBLE);
                        btnPayloadGen.setVisibility(VISIBLE);
                    } else if (position == 5) {
                        LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                        ex.setVisibility(VISIBLE);
                        TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                        tl.setHint("Payload");
                        etNetworkName.setVisibility(VISIBLE);
                        etNetworkPayload.setVisibility(VISIBLE);
                        etNetworkProxyIP.setVisibility(VISIBLE);
                        etNetworkProxyPort.setVisibility(VISIBLE);
                        etNetworkInfo.setVisibility(VISIBLE);
                        etBackQuery.setVisibility(VISIBLE);
                        etFrontQuery.setVisibility(VISIBLE);
                        is_custom_config.setVisibility(GONE);
                        modeGroup.setVisibility(GONE);
                        cf.setVisibility(VISIBLE);
                        ws.setVisibility(VISIBLE);
                        http.setVisibility(VISIBLE);
                        btnPayloadGen.setVisibility(VISIBLE);
                    } else if (position == 6) {
                        LinearLayout ex = (LinearLayout) v.findViewById(R.id.excluded_udp_layout);
                        ex.setVisibility(GONE);
                        TextInputLayout tl = (TextInputLayout) v.findViewById(R.id.etNetworkPayloadInput);
                        tl.setHint("DNS Address");
                        etNetworkName.setVisibility(VISIBLE);
                        etNetworkPayload.setVisibility(VISIBLE);
                        etNetworkProxyIP.setVisibility(GONE);
                        etNetworkProxyPort.setVisibility(GONE);
                        etNetworkInfo.setVisibility(VISIBLE);
                        etBackQuery.setVisibility(GONE);
                        etFrontQuery.setVisibility(GONE);
                        is_custom_config.setVisibility(GONE);
                        modeGroup.setVisibility(GONE);
                        cf.setVisibility(VISIBLE);
                        ws.setVisibility(VISIBLE);
                        http.setVisibility(VISIBLE);
                        btnPayloadGen.setVisibility(VISIBLE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            is_custom_config.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                    if (isChecked) {
                        etNetworkPayload.setVisibility(VISIBLE);
                    } else {
                        etNetworkPayload.setVisibility(GONE);
                    }
                }
            });

            if (obj.getInt("TunnelType") == 2) {
                if (obj.getInt("Protocol") == 1) {
                    stProto.setSelection(4);
                } else if (obj.getInt("Protocol") == 2) {
                    stProto.setSelection(5);
                } else {
                    stProto.setSelection(0);
                }
            } else if (obj.getInt("TunnelType") == 6) {
                stProto.setSelection(1);
            } else if (obj.getInt("TunnelType") == 7) {
                stProto.setSelection(2);
            } else if (obj.getInt("TunnelType") == 8) {
                stProto.setSelection(3);
            } else if (obj.getInt("TunnelType") == 9) {
                stProto.setSelection(6);
            }

            String[] prior = new String[]{"Priority Low", "Priority Medium", "Priority High"};
            Spinner stPriority = (Spinner) v.findViewById(R.id.priority_spin);
            ArrayAdapter ad2 = new ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    prior);
            ad2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stPriority.setAdapter(ad2);
            if (obj.has("Priority")) {
                stPriority.setSelection(obj.getInt("Priority"));
            }
            JSONObject proxy = new JSONObject(obj.getJSONObject("ProxySettings").toString());
            etNetworkProxyIP.setText(decrypt(proxy.getString("Squid")));
            etNetworkProxyPort.setText(proxy.getString("Port"));
            if (etNetworkProxyIP.getText().toString().contains("[Default]")) {
                ckUseDefProxy.setChecked(true);
                etNetworkProxyIP.setEnabled(false);
            } else {
                ckUseDefProxy.setChecked(false);
                etNetworkProxyIP.setEnabled(true);
            }

            if (obj.toString().contains("isCustomConfig")) {
                is_custom_config.setChecked(obj.getBoolean("isCustomConfig"));
            }

            final AlertDialog alert = buidler.create();
            alert.getWindow().setGravity(Gravity.CENTER);
            alert.show();
            ((Button) v.findViewById(R.id.btnSaveNetwork)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View p1) {
                    try {

                        final JSONObject jsonObject = new JSONObject();
                        String sName = etNetworkName.getText().toString();
                        String sPayload = etNetworkPayload.getText().toString();
                        String sSquidProxy = etNetworkProxyIP.getText().toString();
                        String sSquidPort = etNetworkProxyPort.getText().toString();
                        String sNetworkInfo = etNetworkInfo.getText().toString();
                        String frontQ = etFrontQuery.getText().toString();
                        String backQ = etBackQuery.getText().toString();
                        jsonObject.put("Name", sName);
                        jsonObject.put("Payload", encrypt(sPayload));
                        jsonObject.put("Info", sNetworkInfo);
                        int i = stProto.getSelectedItemPosition();

                        if (i == 0) {
                            jsonObject.put("TunnelType", 2);
                            jsonObject.put("Protocol", 0);
                        } else if (i == 1) {
                            jsonObject.put("TunnelType", 6);
                            jsonObject.put("Protocol", 0);
                        } else if (i == 2) {
                            jsonObject.put("TunnelType", 7);
                            jsonObject.put("Protocol", 0);
                        } else if (i == 3) {
                            jsonObject.put("TunnelType", 8);
                            jsonObject.put("Protocol", 0);
                        } else if (i == 4) {
                            jsonObject.put("TunnelType", 2);
                            jsonObject.put("Protocol", 1);
                        } else if (i == 5) {
                            jsonObject.put("TunnelType", 2);
                            jsonObject.put("Protocol", 2);
                        } else if (i == 6) {
                            jsonObject.put("TunnelType", 9);
                            jsonObject.put("Protocol", 0);
                        }

                        jsonObject.put("FrontQuery", frontQ);
                        jsonObject.put("BackQuery", backQ);
                        jsonObject.put("Priority", stPriority.getSelectedItemPosition());

                        if (cf.isChecked()) {
                            jsonObject.put("ServerHostDNS", "cf");
                        }
                        if (ws.isChecked()) {
                            jsonObject.put("ServerHostDNS", "ws");
                        }
                        if (http.isChecked()) {
                            jsonObject.put("ServerHostDNS", "http");
                        }

                        if (mode1.isChecked()) {
                            jsonObject.put("Mode", 1);
                        }
                        if (mode2.isChecked()) {
                            jsonObject.put("Mode", 2);
                        }
                        if (mode3.isChecked()) {
                            jsonObject.put("Mode", 3);
                        }

                        JSONObject proxy = new JSONObject();
                        proxy.put("Squid", encrypt(sSquidProxy));
                        proxy.put("Port", sSquidPort);
                        jsonObject.put("ProxySettings", proxy);

                        jsonObject.put("isCustomConfig", is_custom_config.isChecked());

                        jsonArray.put(position, jsonObject);

                        //jsonArray.put(jsonObject);

                        JSONObject jo = getJSONObject();
                        jo.remove("Networks");
                        try {
                            jo.put("Networks", jsonArray);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        try {
                            File file = new File(getFilesDir(), "Servers.js");

                            OutputStream out = new FileOutputStream(file);
                            out.write(jo.toString().getBytes());
                            out.flush();
                            out.close();
                            //    TextView tv=findViewById(R.id.result);
                            //   tv.setText(getJsonString());
                            showToast(sName + " is edited Successfully");
                        } catch (Exception e) {
                        }
                        setupNetowrksListView();
                        networkListView.setSelection(listNetworks.size());
                        alert.dismiss();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            ((Button) v.findViewById(R.id.btnPayloadGen)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View p1) {

                    PayloadGeneratorDialog payloadGenerator = new PayloadGeneratorDialog(MainActivity.this);
                    payloadGenerator.setDialogTitle("Payload Generator");
                    payloadGenerator.setGenerateListener("Save", new PayloadGeneratorDialog.OnGenerateListener() {
                        @Override
                        public void onGenerate(String payloadGenerated) {
                            etNetworkPayload.setText(payloadGenerated);
                        }
                    });
                    payloadGenerator.show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void removePayload(int a) {
        try {
            new CuteDialog.withAnimation(this)
                    .setAnimation(R.raw.delete)
                    .setTitle(listNetworks.get(a).getString("Name"))
                    .setDescription("Are you sure, you want to delete this item?")
                    .setPositiveButtonText("Okay", v2 -> {
                        try {
                            JSONArray jarr = getNetworksArray();
                            jarr.remove(a);
                            JSONObject jo = getJSONObject();
                            jo.remove("Networks");
                            try {
                                jo.put("Networks", jarr);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                            try {
                                File file = new File(getFilesDir(), "Servers.js");
                                OutputStream out = new FileOutputStream(file);
                                out.write(jo.toString().getBytes());
                                out.flush();
                                out.close();
                                //    TextView tv=findViewById(R.id.result);
                                //    tv.setText(getJsonString());

                            } catch (Exception e) {
                            }


                            setupNetowrksListView();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButtonText("Cancel", v2 -> {

                    })
                    .show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Empty", Toast.LENGTH_SHORT).show();
        }
    }

    void generateUDPConfig(EditText udppayload) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View v = inflater.inflate(R.layout.dialog_udp_maker, null);
        AlertDialog.Builder buidler = new AlertDialog.Builder(this);
        buidler.setTitle("UDP Generator");
        buidler.setCancelable(true);
        buidler.setView(v);
        EditText sIpPort = (EditText) v.findViewById(R.id.etServerIP);
        EditText obfs = (EditText) v.findViewById(R.id.etServerObfs);
        EditText upmbps = (EditText) v.findViewById(R.id.etServerUpMBPS);
        EditText downmbps = (EditText) v.findViewById(R.id.etServerDownMBPS);
        EditText rwconn = (EditText) v.findViewById(R.id.etServerRWConn);
        EditText rw = (EditText) v.findViewById(R.id.etServerRW);
        final AlertDialog alert = buidler.create();
        alert.getWindow().setGravity(Gravity.CENTER);
        alert.show();
        ((Button) v.findViewById(R.id.btnSaveServer)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {


                String sHost = sIpPort.getText().toString();
                String sObfs = obfs.getText().toString();
                int up = 1;
                int down = 2;
                int rwc = 196608;
                int rwv = 491520;
                if (!upmbps.getText().toString().isEmpty()) {
                    up = Integer.valueOf(upmbps.getText().toString());
                }
                if (!downmbps.getText().toString().isEmpty()) {
                    down = Integer.valueOf(downmbps.getText().toString());
                }
                if (!rwconn.getText().toString().isEmpty()) {
                    rwc = Integer.valueOf(rwconn.getText().toString());
                }
                if (!rw.getText().toString().isEmpty()) {
                    rwv = Integer.valueOf(rw.getText().toString());
                }
                if (sHost.isEmpty() || sObfs.isEmpty()) {
                    Toast.makeText(MainActivity.this, "All fields are required!", Toast.LENGTH_SHORT).show();
                } else {
                    String udpstr = readFromRaw(MainActivity.this, R.raw.udp);
                    udppayload.setText(udpstr.replace("serverIPPort_xxx", sHost).replace("obfs_xxx", sObfs).replace("up_xxx", up + "").replace("down_xxx", down + "").replace("rwcon_xxx", rwc + "").replace("rw_xxx", rwv + ""));
                    alert.dismiss();
                }

            }
        });
    }

    public static String readFromRaw(Context context, int resId) {
        InputStream in = context.getResources().openRawResource(resId);
        Scanner scanner = new Scanner(in, "UTF-8").useDelimiter("\\A");
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            sb.append(scanner.next());
        }
        scanner.close();
        return sb.toString();
    }

    public class NetworkAdapter extends BaseAdapter {

        private ArrayList<JSONObject> listNetwork;
        private Context context;

        public NetworkAdapter(Context context, ArrayList<JSONObject> mlistNetwork) {
            // super(context, R.layout.network_item, mlistNetwork);
            this.listNetwork = mlistNetwork;
            this.context = context;
        }

        @Override
        public JSONObject getItem(int position) {
            // TODO: Implement this method
            return listNetwork.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getCount() {
            // TODO: Implement this method
            return listNetwork.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO: Implement this method
            JSONObject js = getItem(position);
            return MyView(position, convertView, parent, js, false);
        }

        public View MyView(int position, View convertView, ViewGroup parent, JSONObject js, boolean isdrop) {
            View v = LayoutInflater.from(context).inflate(R.layout.network_item, parent, false);
            TextView tv = (TextView) v.findViewById(R.id.network_item_title);
            TextView info = (TextView) v.findViewById(R.id.network_tunnel_info);
            ImageView iv = (ImageView) v.findViewById(R.id.network_item_icon);

            try {

                String name2 = js.getString("Name");
                String inf = js.getString("Info");
                info.setText(inf);
                tv.setText(name2);
                String name = name2.toLowerCase();
                //tunnel_title.setText(js.getInt("TunnelType") == 0 ? "SSH/INJECT" : "SSH/SSL");
                if (name.contains("banglalink")) {
                    setIcon(iv, R.drawable.banglalink);
                } else if (name.contains("dhiraagu")) {
                    setIcon(iv, R.drawable.dhiraagu);
                } else if (name.contains("omantel")) {
                    setIcon(iv, R.drawable.omantel);
                } else if (name.contains("starhub")) {
                    setIcon(iv, R.drawable.starhub);
                } else if (name.contains("singtel")) {
                    setIcon(iv, R.drawable.singtel);
                } else if (name.contains("banglalink")) {
                    setIcon(iv, R.drawable.ic_banglalink);
                } else if (name.contains("vodafone")) {
                    setIcon(iv, R.drawable.ic_vodafone);
                } else if (name.contains("dhiraagu")) {
                    setIcon(iv, R.drawable.dhiraagu);
                } else if (name.contains("yaqoot")) {
                    setIcon(iv, R.drawable.ic_yaqoot);
                } else if (name.contains("jio")) {
                    setIcon(iv, R.drawable.ic_jio);
                } else if (name.contains("vargin")) {
                    setIcon(iv, R.drawable.vargin);
                } else if (name.contains("omantel")) {
                    setIcon(iv, R.drawable.ic_omantel);
                } else if (name.contains("salam")) {
                    setIcon(iv, R.drawable.ic_salam);
                } else if (name.contains("friendi")) {
                    setIcon(iv, R.drawable.ic_friendi);
                } else if (name.contains("facebook")) {
                    setIcon(iv, R.drawable.ic_facebook);
                } else if (name.contains("google")) {
                    setIcon(iv, R.drawable.ic_google);
                } else if (name.contains("youtube")) {
                    setIcon(iv, R.drawable.ic_youtube);
                } else if (name.contains("instagram")) {
                    setIcon(iv, R.drawable.ic_instagram);
                } else if (name.contains("iflix")) {
                    setIcon(iv, R.drawable.ic_iflix);
                } else if (name.contains("snapchat")) {
                    setIcon(iv, R.drawable.ic_snapchat);
                } else if (name.contains("twitter")) {
                    setIcon(iv, R.drawable.ic_twitter);
                } else if (name.contains("neflix")) {
                    setIcon(iv, R.drawable.ic_netflix);
                } else if (name.contains("mobile legends")) {
                    setIcon(iv, R.drawable.ic_ml);
                } else if (name.contains("du")) {
                    setIcon(iv, R.drawable.ic_du);
                } else if (name.contains("etisalat")) {
                    setIcon(iv, R.drawable.ic_eti);
                } else if (name.contains("wifi")) {
                    setIcon(iv, R.drawable.ic_wifi);
                } else if (name.contains("whatsapp")) {
                    setIcon(iv, R.drawable.ic_whatsapp);
                } else if (name.contains("tiktok")) {
                    setIcon(iv, R.drawable.ic_tiktok);
                } else if (name.contains("viber")) {
                    setIcon(iv, R.drawable.ic_viber);
                } else if (name.contains("airtel")) {
                    setIcon(iv, R.drawable.ic_airtel);
                } else if (name.contains("grameenphone")) {
                    setIcon(iv, R.drawable.ic_grameenphone);
                } else if (name.contains("jawwy")) {
                    setIcon(iv, R.drawable.ic_jawwy);
                } else if (name.contains("digi")) {
                    setIcon(iv, R.drawable.ic_digi);
                } else if (name.contains("mobily")) {
                    setIcon(iv, R.drawable.ic_mobily);
                } else if (name.contains("airtel")) {
                    setIcon(iv, R.drawable.ic_airtel);
                } else if (name.contains("pubg")) {
                    setIcon(iv, R.drawable.ic_pubg);
                } else if (name.contains("stc")) {
                    setIcon(iv, R.drawable.ic_stc);
                } else if (name.contains("skype")) {
                    setIcon(iv, R.drawable.ic_skype);
                } else if (name.contains("telegram")) {
                    setIcon(iv, R.drawable.ic_telegram);
                } else if (name.contains("vivobee")) {
                    setIcon(iv, R.drawable.ic_vivobee);
                } else if (name.contains("zain")) {
                    setIcon(iv, R.drawable.ic_zain);
                } else if (name.contains("zain free")) {
                    setIcon(iv, R.drawable.zain_free);
                } else if (name.contains("ooredoo")) {
                    setIcon(iv, R.drawable.ic_ooreedo);
                } else if (name.contains("viva")) {
                    setIcon(iv, R.drawable.ic_viva);
                } else if (name.contains("progresif")) {
                    setIcon(iv, R.drawable.ic_progresif);
                } else if (name.contains("jio")) {
                    setIcon(iv, R.drawable.ic_jio);
                } else if (name.contains("lebara")) {
                    setIcon(iv, R.drawable.ic_lebara);
                } else if (name.contains("vodaphone")) {
                    setIcon(iv, R.drawable.ic_vodafone);
                } else if (name.contains("mobily")) {
                    setIcon(iv, R.drawable.ic_mobily);
                } else {
                    setIcon(iv, R.mipmap.ic_launcher);
                }
                ImageView edit = v.findViewById(R.id.server_edit);
                ImageView delete = v.findViewById(R.id.server_delete);
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (js.getInt("TunnelType") == 3 || js.getInt("TunnelType") == 4 || js.getInt("TunnelType") == 5) {
                                editSSL(getnetworkPosition(name2));
                            } else {
                                editPayload(getnetworkPosition(name2));
                            }

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }
                });
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removePayload(getnetworkPosition(name2));
                    }
                });

                //setAnimation(v, position);
            } catch (Exception e) {
                Toast.makeText(context, "Payload SpinnerView - " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            // TODO: Implement this method
            return v;
        }

        public void setIcon(ImageView iv, int icon) {
            iv.setImageResource(icon);
        }
    }


    void addSSL() {
        String[] sslmethod = new String[]{"SSL", "SSL+PAYLOAD", "SSL+PAYLOAD+WS"};
        final int[] sslmethodcode = new int[]{3, 4, 5};

        LayoutInflater inflater = LayoutInflater.from(this);
        final View v = inflater.inflate(R.layout.ssl_network_dialog, null);
        AlertDialog.Builder buidler = new AlertDialog.Builder(this);
        buidler.setTitle("Add SSL Network");
        buidler.setCancelable(true);
        buidler.setView(v);

        EditText etSSLPayload = (EditText) v.findViewById(R.id.etSSLPayload);
        EditText etSSLName = (EditText) v.findViewById(R.id.etSSLName);
        EditText etSSLPort = (EditText) v.findViewById(R.id.etSSLPort);

        EditText etSSLSNI = (EditText) v.findViewById(R.id.etSSLSNI);
        EditText etSSLInfo = (EditText) v.findViewById(R.id.etSSLInfo);
        EditText etSSLProxyIP = (EditText) v.findViewById(R.id.etSquidProxy);
        EditText etSSLProxyPort = (EditText) v.findViewById(R.id.etSquidPort);
        CheckBox ckSSLUseDefProxy = (CheckBox) v.findViewById(R.id.ckUseDefProxy);


        etSSLProxyIP.setEnabled(false);
        etSSLProxyIP.setText("[Default]");
        etSSLProxyPort.setText("8080");
        ckSSLUseDefProxy.setChecked(true);
        ckSSLUseDefProxy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean p2) {
                if (p2) {
                    etSSLProxyIP.setEnabled(false);
                    etSSLProxyIP.setText("[Default]");
                } else {
                    etSSLProxyIP.setEnabled(true);
                }
            }
        });

        RadioButton openvpn = (RadioButton) v.findViewById(R.id.openvpn);
        RadioButton openconnect = (RadioButton) v.findViewById(R.id.openconnect);
        RadioButton ssh = (RadioButton) v.findViewById(R.id.ssh);

        RadioButton cf = (RadioButton) v.findViewById(R.id.cf_radio);
        RadioButton ws = (RadioButton) v.findViewById(R.id.ws_radio);
        RadioButton http = (RadioButton) v.findViewById(R.id.http_radio);
        cf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ws.setChecked(false);
                http.setChecked(false);
                cf.setChecked(true);
            }
        });
        ws.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ws.setChecked(true);
                http.setChecked(false);
                cf.setChecked(false);
            }
        });
        http.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ws.setChecked(false);
                http.setChecked(true);
                cf.setChecked(false);
            }
        });
        cf.callOnClick();

        openvpn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openvpn.setChecked(true);
                openconnect.setChecked(false);
                ssh.setChecked(false);
            }
        });
        openconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openvpn.setChecked(false);
                openconnect.setChecked(true);
                ssh.setChecked(false);
            }
        });
        ssh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openvpn.setChecked(false);
                openconnect.setChecked(false);
                ssh.setChecked(true);
            }
        });
        openvpn.callOnClick();

        final LinearLayout proxyLay = (LinearLayout) v.findViewById(R.id.sslproxylay);
        Spinner stSSLMethod = (Spinner) v.findViewById(R.id.sslmethod);
        ArrayAdapter ad = new ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                sslmethod);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stSSLMethod.setAdapter(ad);


        String[] prior = new String[]{"Priority Low", "Priority Medium", "Priority High"};
        Spinner stPriority = (Spinner) v.findViewById(R.id.priority_spin);
        ArrayAdapter ad2 = new ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                prior);
        ad2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stPriority.setAdapter(ad2);


        stSSLMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4) {
                if (p3 == 0) {
                    etSSLPayload.setVisibility(GONE);
                    proxyLay.setVisibility(GONE);
                }
                if (p3 == 1) {
                    etSSLPayload.setVisibility(VISIBLE);
                    proxyLay.setVisibility(GONE);
                }
                if (p3 == 2) {
                    etSSLPayload.setVisibility(VISIBLE);
                    proxyLay.setVisibility(VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> p1) {
            }
        });

        stSSLMethod.setSelection(0);
        final AlertDialog alert = buidler.create();
        alert.getWindow().setGravity(Gravity.CENTER);
        alert.show();
        ((Button) v.findViewById(R.id.btnSaveSSL)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {
                try {

                    JSONObject jsonObject = new JSONObject();

                    JSONArray jsonArray = getNetworksArray();
                    String sPort = etSSLPort.getText().toString();
                    String sName = etSSLName.getText().toString();
                    String sPayload = etSSLPayload.getText().toString();
                    String sSNI = etSSLSNI.getText().toString();
                    String sNetworkInfo = etSSLInfo.getText().toString();
                    int method = sslmethodcode[stSSLMethod.getSelectedItemPosition()];
                    jsonObject.put("Name", sName);
                    jsonObject.put("SNIHost", encrypt(sSNI));
                    jsonObject.put("Payload", encrypt(sPayload));
                    jsonObject.put("Info", sNetworkInfo);
                    jsonObject.put("TunnelType", method);

                    if (openvpn.isChecked()) {
                        jsonObject.put("Protocol", 0);
                    } else if (openconnect.isChecked()) {
                        jsonObject.put("Protocol", 1);
                    } else if (ssh.isChecked()) {
                        jsonObject.put("Protocol", 2);
                    }
                    jsonObject.put("Priority", stPriority.getSelectedItemPosition());
                    if (!sPort.isEmpty()) {
                        jsonObject.put("CustomSSLPort", sPort);
                    }
                    if (cf.isChecked()) {
                        jsonObject.put("ServerHostDNS", "cf");
                    }
                    if (ws.isChecked()) {
                        jsonObject.put("ServerHostDNS", "ws");
                    }
                    if (http.isChecked()) {
                        jsonObject.put("ServerHostDNS", "http");
                    }
                    JSONObject proxy = new JSONObject();
                    String sSquidProxy = etSSLProxyIP.getText().toString();
                    String sSquidPort = etSSLProxyPort.getText().toString();

                    proxy.put("Squid", encrypt(sSquidProxy));
                    proxy.put("Port", sSquidPort);
                    jsonObject.put("ProxySettings", proxy);

                    jsonArray.put(jsonObject);

                    JSONObject jo = getJSONObject();
                    jo.remove("Networks");
                    try {
                        jo.put("Networks", jsonArray);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        File file = new File(getFilesDir(), "Servers.js");

                        OutputStream out = new FileOutputStream(file);
                        out.write(jo.toString().getBytes());
                        out.flush();
                        out.close();
                        //    TextView tv=findViewById(R.id.result);
                        //    tv.setText(getJsonString());
                        showToast(sName + " is added Successfully");
                    } catch (Exception e) {
                    }
                    setupNetowrksListView();
                    networkListView.setSelection(listNetworks.size());
                    alert.dismiss();
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void editSSL(int position) {
        try {
            String[] sslmethod = new String[]{"SSL", "SSL+PAYLOAD", "SSL+PAYLOAD+WS"};
            final int[] sslmethodcode = new int[]{3, 4, 5};
            final JSONArray jsonArray = getNetworksArray();
            LayoutInflater inflater = LayoutInflater.from(this);
            final View v = inflater.inflate(R.layout.ssl_network_dialog, null);
            AlertDialog.Builder buidler = new AlertDialog.Builder(this);
            buidler.setTitle("Edit SSL Network");
            buidler.setView(v);
            buidler.setCancelable(true);
            EditText etSSLPayload = (EditText) v.findViewById(R.id.etSSLPayload);

            EditText etSSLName = (EditText) v.findViewById(R.id.etSSLName);
            EditText etSSLSNI = (EditText) v.findViewById(R.id.etSSLSNI);
            EditText etSSLInfo = (EditText) v.findViewById(R.id.etSSLInfo);
            EditText etSSLPort = (EditText) v.findViewById(R.id.etSSLPort);

            JSONObject obj = jsonArray.getJSONObject(position);
            if (!obj.getString("Info").isEmpty()) {
                etSSLInfo.setText(obj.getString("Info"));
            }
            if (!obj.getString("SNIHost").isEmpty()) {
                etSSLSNI.setText(decrypt(obj.getString("SNIHost")));
            }
            if (!obj.getString("Payload").isEmpty()) {
                etSSLPayload.setText(decrypt(obj.getString("Payload")));
            }
            etSSLName.setText(obj.getString("Name"));

            RadioButton openvpn = (RadioButton) v.findViewById(R.id.openvpn);
            RadioButton openconnect = (RadioButton) v.findViewById(R.id.openconnect);
            RadioButton ssh = (RadioButton) v.findViewById(R.id.ssh);

            RadioButton cf = (RadioButton) v.findViewById(R.id.cf_radio);
            RadioButton ws = (RadioButton) v.findViewById(R.id.ws_radio);
            RadioButton http = (RadioButton) v.findViewById(R.id.http_radio);
            cf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ws.setChecked(false);
                    http.setChecked(false);
                    cf.setChecked(true);
                }
            });
            ws.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ws.setChecked(true);
                    http.setChecked(false);
                    cf.setChecked(false);
                }
            });
            http.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ws.setChecked(false);
                    http.setChecked(true);
                    cf.setChecked(false);
                }
            });
            cf.callOnClick();

            openvpn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openvpn.setChecked(true);
                    openconnect.setChecked(false);
                    ssh.setChecked(false);
                }
            });
            openconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openvpn.setChecked(false);
                    openconnect.setChecked(true);
                    ssh.setChecked(false);
                }
            });
            ssh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openvpn.setChecked(false);
                    openconnect.setChecked(false);
                    ssh.setChecked(true);
                }
            });
            openvpn.callOnClick();

            if (obj.toString().contains("ServerHostDNS")) {
                String sg = obj.getString("ServerHostDNS");
                if (sg.equals("ws")) {
                    ws.callOnClick();
                }
                if (sg.equals("cf")) {
                    cf.callOnClick();
                }
                if (sg.equals("http")) {
                    http.callOnClick();
                }
            } else {
                cf.callOnClick();
            }

            if (obj.toString().contains("Protocol")) {
                int sg = obj.getInt("Protocol");
                if (sg == 0) {
                    openvpn.callOnClick();
                }
                if (sg == 1) {
                    openconnect.callOnClick();
                }
                if (sg == 2) {
                    ssh.callOnClick();
                }
            } else {
                openvpn.callOnClick();
            }


            if (obj.has("CustomSSLPort")) {
                etSSLPort.setText(obj.getString("CustomSSLPort"));
            }
            EditText etSSLProxyIP = (EditText) v.findViewById(R.id.etSquidProxy);
            EditText etSSLProxyPort = (EditText) v.findViewById(R.id.etSquidPort);
            CheckBox ckSSLUseDefProxy = (CheckBox) v.findViewById(R.id.ckUseDefProxy);

            ckSSLUseDefProxy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton p1, boolean p2) {
                    if (p2) {
                        etSSLProxyIP.setEnabled(false);
                        etSSLProxyIP.setText("[Default]");
                    } else {
                        etSSLProxyIP.setEnabled(true);
                    }
                }
            });


            String[] prior = new String[]{"Priority Low", "Priority Medium", "Priority High"};
            Spinner stPriority = (Spinner) v.findViewById(R.id.priority_spin);
            ArrayAdapter ad2 = new ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    prior);
            ad2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stPriority.setAdapter(ad2);
            if (obj.has("Priority")) {
                stPriority.setSelection(obj.getInt("Priority"));
            }


            JSONObject proxy = new JSONObject(obj.getJSONObject("ProxySettings").toString());
            etSSLProxyIP.setText(decrypt(proxy.getString("Squid")));
            etSSLProxyPort.setText(proxy.getString("Port"));
            if (etSSLProxyIP.getText().toString().contains("[Default]")) {
                ckSSLUseDefProxy.setChecked(true);
                etSSLProxyIP.setEnabled(false);
            } else {
                ckSSLUseDefProxy.setChecked(false);
                etSSLProxyIP.setEnabled(true);
            }
            final LinearLayout proxyLay = (LinearLayout) v.findViewById(R.id.sslproxylay);
            Spinner stSSLMethod = (Spinner) v.findViewById(R.id.sslmethod);
            ArrayAdapter ad = new ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    sslmethod);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stSSLMethod.setAdapter(ad);
            stSSLMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4) {
                    if (p3 == 0) {
                        etSSLPayload.setVisibility(GONE);
                        proxyLay.setVisibility(GONE);
                    }
                    if (p3 == 1) {
                        etSSLPayload.setVisibility(VISIBLE);
                        proxyLay.setVisibility(GONE);
                    }
                    if (p3 == 2) {
                        etSSLPayload.setVisibility(VISIBLE);
                        proxyLay.setVisibility(VISIBLE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> p1) {
                }
            });
            int method = obj.getInt("TunnelType");
            if (method == 3) {
                stSSLMethod.setSelection(0);
            }
            if (method == 4) {
                stSSLMethod.setSelection(1);
            }
            if (method == 5) {
                stSSLMethod.setSelection(2);
            }
            final AlertDialog alert = buidler.create();
            alert.getWindow().setGravity(Gravity.CENTER);
            alert.show();
            ((Button) v.findViewById(R.id.btnSaveSSL)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View p1) {
                    try {

                        final JSONObject jsonObject = new JSONObject();

                        String sName = etSSLName.getText().toString();
                        String sSNI = etSSLSNI.getText().toString();
                        String sNetworkInfo = etSSLInfo.getText().toString();
                        int method = sslmethodcode[stSSLMethod.getSelectedItemPosition()];
                        String sPort = etSSLPort.getText().toString();
                        String sPayload = etSSLPayload.getText().toString();
                        jsonObject.put("Name", sName);
                        jsonObject.put("SNIHost", encrypt(sSNI));
                        jsonObject.put("Payload", encrypt(sPayload));
                        jsonObject.put("Info", sNetworkInfo);
                        jsonObject.put("TunnelType", method);
                        jsonObject.put("Priority", stPriority.getSelectedItemPosition());
                        if (!sPort.isEmpty()) {
                            jsonObject.put("CustomSSLPort", sPort);
                        }

                        if (openvpn.isChecked()) {
                            jsonObject.put("Protocol", 0);
                        } else if (openconnect.isChecked()) {
                            jsonObject.put("Protocol", 1);
                        } else if (ssh.isChecked()) {
                            jsonObject.put("Protocol", 2);
                        }

                        if (cf.isChecked()) {
                            jsonObject.put("ServerHostDNS", "cf");
                        }
                        if (ws.isChecked()) {
                            jsonObject.put("ServerHostDNS", "ws");
                        }
                        if (http.isChecked()) {
                            jsonObject.put("ServerHostDNS", "http");
                        }
                        JSONObject proxy = new JSONObject();
                        String sSquidProxy = etSSLProxyIP.getText().toString();
                        String sSquidPort = etSSLProxyPort.getText().toString();

                        proxy.put("Squid", encrypt(sSquidProxy));
                        proxy.put("Port", sSquidPort);
                        jsonObject.put("ProxySettings", proxy);

                        jsonArray.put(position, jsonObject);

                        JSONObject jo = getJSONObject();
                        jo.remove("Networks");
                        try {
                            jo.put("Networks", jsonArray);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        try {
                            File file = new File(getFilesDir(), "Servers.js");

                            OutputStream out = new FileOutputStream(file);
                            out.write(jo.toString().getBytes());
                            out.flush();
                            out.close();
                            //    TextView tv=findViewById(R.id.result);
                            //    tv.setText(getJsonString());
                            showToast(sName + " is edited Successfully");
                        } catch (Exception e) {
                        }
                        setupNetowrksListView();
                        networkListView.setSelection(listNetworks.size());
                        alert.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    public void setMainLayout() {
        user_info = findViewById(R.id.user_info);
        app_info = findViewById(R.id.app_info);
        app_info.setText("App Name: " + prefs.getString("login_appname", "unknown"));
        payload_info = findViewById(R.id.payload_info);
        server_info = findViewById(R.id.server_info);
        user_info.setText("Hi Admin, " + prefs.getString("login_user", ""));
        server_info.setText("Server Count: " + getServersArray().length());
        payload_info.setText("Payload Count: " + getNetworksArray().length());
        LinearLayout logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
                ab.setMessage("Are you sure do you want to logout?");
                ab.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                ab.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean("isLogin", false).apply();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
                ab.show();
            }
        });
        LinearLayout conf_lay = findViewById(R.id.configuration_layout);
        conf_lay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setOvpnFile();
            }
        });
        LinearLayout prefs_lay = findViewById(R.id.prefs_lay);
        prefs_lay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPrefsSetting();
            }
        });
        CardView announcement = findViewById(R.id.announcement);
        announcement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://tunnel.mtkapi.site/api/files/app?json=8cc3b5c5e9972654219a"; // dev Notice api
                RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
                StringRequest req = new StringRequest(url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {

                                final CuteDialog.withAnimation cdd = new CuteDialog.withAnimation(MainActivity.this)
                                        .setAnimation(R.raw.announcement)
                                        .setTitle("Attention!")
                                        .setDescription(response)

                                        .setPositiveButtonText("Okay", v2 -> {


                                        })
                                        .setNegativeButtonText("Copy", v2 -> {
                                            copyToClipboard(MainActivity.this, response);
                                        });

                                cdd.show();
                                requestQueue.getCache().clear();
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        requestQueue.getCache().clear();
                    }

                });

                requestQueue.add(req);
            }
        });
        CardView contactus = findViewById(R.id.contact_us);
        contactus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CuteDialog.withAnimation cdd = new CuteDialog.withAnimation(MainActivity.this)
                        .setAnimation(R.raw.contactus)
                        .setTitle("Contact Us")

                        .setDescription("Farhan Developer WhatsApp support")

                        .setPositiveButtonText("Contact", v2 -> {
                            setClickToChat(MainActivity.this, "+8801644128820");

                        })
                        .setNegativeButtonText("Cancel", v2 -> {

                        });

                cdd.show();
            }
        });
    }

    public static void setClickToChat(Context con, String toNumber) {
        String url = "https://api.whatsapp.com/send?phone=" + toNumber;
        try {
            PackageManager pm = con.getPackageManager();
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            con.startActivity(i);
        } catch (PackageManager.NameNotFoundException e) {
            con.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    public void setOvpnFile() {
        AlertDialog.Builder nBuilder2 = new AlertDialog.Builder(this);
        final View mView2 = getLayoutInflater().inflate(R.layout.dialog_set_edittext, null);
        nBuilder2.setTitle("Set OVPN Cert");
        nBuilder2.setView(mView2);
        final AlertDialog dialog2 = nBuilder2.create();
        final EditText defVersion2 = (EditText) mView2.findViewById(R.id.edittextPassword);
        defVersion2.setHint("Set OVPN Cert");
        String conf = "";
        if (getJSONObject().has("OVPNCert")) {
            try {
                conf = decrypt(getJSONObject().getString("OVPNCert"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        defVersion2.setText(conf);
        nBuilder2.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                if (defVersion2.getText().toString().isEmpty()) {
                    showToast("Invalid Configuration!");
                } else {
                    String aa = defVersion2.getText().toString();
                    JSONObject jo = getJSONObject();
                    jo.remove("OVPNCert");
                    try {
                        jo.put("OVPNCert", aa);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        File file = new File(getFilesDir(), "Servers.js");

                        OutputStream out = new FileOutputStream(file);
                        out.write(jo.toString().getBytes());
                        out.flush();
                        out.close();
                        //    TextView tv=findViewById(R.id.result);
                        //    tv.setText(getJsonString());
                    } catch (Exception e) {
                    }
                }

            }
        });
        nBuilder2.show();
    }

    public void setPrefsSetting() {
        AlertDialog.Builder nBuilder2 = new AlertDialog.Builder(this);
        final View mView2 = getLayoutInflater().inflate(R.layout.dialog_set_prefs, null);
        nBuilder2.setTitle("Set Preferences");
        nBuilder2.setView(mView2);
        final AlertDialog dialog2 = nBuilder2.create();
        final EditText defVersion2 = (EditText) mView2.findViewById(R.id.version_text_dialog);
        //  defVersion2.setHint("Config Version");
        final EditText releasenotes = (EditText) mView2.findViewById(R.id.release_notes);
        //  releasenotes.setHint("Release Notes");


        String version = "0";
        String notes = "";
        if (getJSONObject().has("Version")) {
            try {
                version = getJSONObject().getString("Version");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        if (getJSONObject().has("Notes")) {
            try {
                notes = getJSONObject().getString("Notes");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        defVersion2.setText(version);
        releasenotes.setText(notes);
        nBuilder2.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                if (defVersion2.getText().toString().isEmpty()) {
                    showToast("Invalid Version Text!");
                } else {
                    String aa = defVersion2.getText().toString();
                    String bb = releasenotes.getText().toString();
                    JSONObject jo = getJSONObject();
                    jo.remove("Version");
                    jo.remove("Notes");
                    try {
                        jo.put("Version", aa);
                        jo.put("Notes", bb);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        File file = new File(getFilesDir(), "Servers.js");
                        OutputStream out = new FileOutputStream(file);
                        out.write(jo.toString().getBytes());
                        out.flush();
                        out.close();
                        //    TextView tv=findViewById(R.id.result);
                        //    tv.setText(getJsonString());
                    } catch (Exception e) {
                    }
                }

            }
        });
        nBuilder2.show();
    }

    public void setChangeSetting() {
        AlertDialog.Builder nBuilder2 = new AlertDialog.Builder(this);
        final View mView2 = getLayoutInflater().inflate(R.layout.change_dialog, null);
        nBuilder2.setTitle("Change Admob IDs & API");
        nBuilder2.setView(mView2);
        final AlertDialog dialog2 = nBuilder2.create();
        final EditText update_api = (EditText) mView2.findViewById(R.id.update_api);
        final EditText auth_api = (EditText) mView2.findViewById(R.id.auth_api);

        //  releasenotes.setHint("Release Notes");



        String update = "";
        String auth = "";
        String notice = "";



        if (getJSONObject().has("update_api")) {
            try {
                update = getJSONObject().getString("update_api");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        if (getJSONObject().has("auth_api")) {
            try {
                auth = getJSONObject().getString("auth_api");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        if (getJSONObject().has("notice_api")) {
            try {
                notice = getJSONObject().getString("notice_api");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }


        update_api.setText(update);
        auth_api.setText(auth);

        nBuilder2.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                if (update_api.getText().toString().isEmpty() && auth_api.getText().toString().isEmpty()) {
                    showToast("Invalid update api!");
                } else {

                    String ee = update_api.getText().toString();
                    String ff = auth_api.getText().toString();
                    JSONObject jo = getJSONObject();
                    jo.remove("banner_ad");
                    jo.remove("interstitial_ad");
                    jo.remove("rewarded_ad");
                    jo.remove("app_open_ad");
                    jo.remove("update_api");
                    jo.remove("auth_api");
                    jo.remove("notice_api");
                    try {
                        jo.put("banner1_url", "");
                        jo.put("banner2_url", "");
                        jo.put("banner3_url", "");
                        jo.put("update_api", ee);
                        jo.put("auth_api", ff);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        File file = new File(getFilesDir(), "Servers.js");
                        OutputStream out = new FileOutputStream(file);
                        out.write(jo.toString().getBytes());
                        out.flush();
                        out.close();
                        //    TextView tv=findViewById(R.id.result);
                        //    tv.setText(getJsonString());
                    } catch (Exception e) {
                    }
                }

            }
        });
        nBuilder2.show();
    }


    void exportDialog() {
        AlertDialog.Builder nBuilder = new AlertDialog.Builder(this);
        final View mView = getLayoutInflater().inflate(R.layout.dialog_set_edittext, null);
        nBuilder.setTitle("Export Config");
        nBuilder.setView(mView);
        final AlertDialog dialog = nBuilder.create();
        final EditText eFilename = (EditText) mView.findViewById(R.id.edittextPassword);
        eFilename.setHint("Filename");
        nBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                StringBuffer sb = new StringBuffer();
                File dir = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    dir = new File(sb.append(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath()).append("/" + getString(R.string.app_name)).toString());
                } else {
                    dir = new File(sb.append(Environment.getExternalStorageDirectory().getAbsolutePath()).append("/" + getString(R.string.app_name)).toString());

                }
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String content = getJsonString();
                String filename = eFilename.getText().toString();
                try {
                    String sResult = AESCrypt.Parser.parse(content);
                    exporter(dir, filename, sResult);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        nBuilder.setNegativeButton("Copy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                try {
                    String content = getJsonString();
                    String sResult = AESCrypt.Parser.parse(content);
                    copyToClipboard(MainActivity.this, sResult);
                } catch (Exception e) {

                }
            }
        });
        //https://candyvip.xyz/uploads/json/5f6b161f9e2ae2311c84.json
        nBuilder.setNeutralButton("Upload Cloud", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String content = getJsonString();
                String sResult = AESCrypt.Parser.parse(content);
                String url = "https://tunnel.mtkapi.site/api/files/update".replace("[domain]", prefs.getString("login_domain", ""));
                String hash = prefs.getString("login_hash", "").replace(" ", "").replace(".", "").replace("  ", "");
                //cloudUpload(url, hash, sResult);
                updateCloudConfig(sResult, hash, url);
            }
        });
        nBuilder.show();
    }
    private void updateCloudConfig(String config, String getAPI, String postAPI) {
        try {
            LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
            linkedHashMap.put("hash", getAPI);
            linkedHashMap.put("data", config);
            if (postAPI.contains("/update")) {
                linkedHashMap.put("code", config);
            } else if (postAPI.contains("edit.php")) {
                linkedHashMap.put("data", config);
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : linkedHashMap.entrySet()) {
                if (sb.length() != 0) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode((String) entry.getKey(), "UTF-8"));
                sb.append('=');
                sb.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
            }

            // Log the full URL with parameters
            String fullURL = postAPI + "?" + sb;
            Log.d("Full URL", fullURL);

            byte[] bytes = sb.toString().getBytes("UTF-8");
            // Create a connection
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(postAPI).openConnection();
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setRequestProperty("Charset", "utf-8");
            httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(sb.length()));
            httpsURLConnection.setUseCaches(false);

            // Write the data to the connection
            OutputStream outputStream = httpsURLConnection.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();

            // Check the HTTP response code
            int responseCode = httpsURLConnection.getResponseCode();
            Log.d("Response Code", String.valueOf(responseCode));

            // Read the response
            StringBuilder response = new StringBuilder();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Reader reader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
                char[] buff = new char[2048];
                int read;
                while ((read = reader.read(buff)) > 0) {
                    response.append(buff, 0, read);
                }
            } else {
                // Read the error response if any
                Reader reader = new BufferedReader(new InputStreamReader(httpsURLConnection.getErrorStream()));
                char[] buff = new char[2048];
                int read;
                while ((read = reader.read(buff)) > 0) {
                    response.append(buff, 0, read);
                }
            }

            // Display the response
            Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "FileNotFoundException: " + e.getMessage(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Exception: " + e, Toast.LENGTH_SHORT).show();
        }
    }
    private void exporter(File directory, String fileName, String content) throws IOException {
        File fileToSave = new File(directory, fileName + ".js");
        FileOutputStream fos = new FileOutputStream(fileToSave);
        String sl = "/";
        fos.write(content.getBytes());
        String saveNot = "Successfully Saved to " + directory + sl + fileToSave.getName();
        Toast.makeText(this, saveNot, Toast.LENGTH_SHORT).show();
        fos.close();
    }

    public void copyToClipboard(Context context, String text) {
        try {
            int sdk = android.os.Build.VERSION.SDK_INT;

            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                        .getSystemService(context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                        .getSystemService(context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData
                        .newPlainText(
                                "Message", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            //tvJSON.setText(e.getMessage());
        }
    }


    public void v2rayGenerator(EditText editText) {

        LayoutInflater inflater = LayoutInflater.from(this);
        final View v = inflater.inflate(R.layout.dialog_v2ray_maker, null);
        AlertDialog.Builder buidler = new AlertDialog.Builder(this);
        String[] proto = new String[]{"vmess", "vless"};
        ArrayAdapter<String> protoadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, proto);
        protoadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner proto_spin = v.findViewById(R.id.v2ray_v2raytype_spin);
        proto_spin.setAdapter(protoadapter);
        proto_spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    LinearLayout vmess_lay = v.findViewById(R.id.vmess_layout);
                    LinearLayout vless_lay = v.findViewById(R.id.vless_layout);
                    vmess_lay.setVisibility(VISIBLE);
                    vless_lay.setVisibility(GONE);
                }
                if (position == 1) {
                    LinearLayout vmess_lay = v.findViewById(R.id.vmess_layout);
                    LinearLayout vless_lay = v.findViewById(R.id.vless_layout);
                    vmess_lay.setVisibility(GONE);
                    vless_lay.setVisibility(VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        proto_spin.setSelection(0);
        //vmess
        Spinner vmess_security = v.findViewById(R.id.vmess_security_spin);
        Spinner vmess_network = v.findViewById(R.id.vmess_network_spin);
        Spinner vmess_headtype = v.findViewById(R.id.vmess_headtype_spin);
        Spinner vmess_tls = v.findViewById(R.id.vmess_tls_spin);
        Spinner vmess_tls_fingerprint = v.findViewById(R.id.vmess_tls_fingerprint_spin);
        Spinner vmess_reality_fingerprint = v.findViewById(R.id.vmess_reality_fingerprint_spin);
        Spinner vmess_tls_alpn = v.findViewById(R.id.vmess_tls_alpn_spin);
        Spinner vmess_tls_insecure = v.findViewById(R.id.vmess_tls_insecure_spin);
        String[] sec_list = new String[]{"chacha20-poly1305", "aes-128-gsm", "auto", "none", "zero"};
        String[] network_list = new String[]{"tcp", "kcp", "ws", "h2", "quic", "grpc"};
        String[] head_list = new String[]{"none", "http"};
        String[] tls_list = new String[]{"", "tls", "reality"};
        String[] fingerprint_list = new String[]{"", "chrome", "firefox", "safari", "ios", "android", "edge", "360", "qq", "random", "randomized"};
        String[] alpn_list = new String[]{"", "h2", "http/1.1", "h2,http/1.1"};
        String[] insec_list = new String[]{"", "true", "false"};

        ArrayAdapter<String> vmess_securityadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sec_list);
        vmess_securityadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vmess_security.setAdapter(vmess_securityadapter);

        ArrayAdapter<String> vmess_networkadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, network_list);
        vmess_networkadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vmess_network.setAdapter(vmess_networkadapter);

        ArrayAdapter<String> vmess_headtypeadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, head_list);
        vmess_headtypeadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vmess_headtype.setAdapter(vmess_headtypeadapter);


        ArrayAdapter<String> vmess_tlsadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, tls_list);
        vmess_tlsadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vmess_tls.setAdapter(vmess_tlsadapter);
        vmess_tls.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LinearLayout tls_lay = v.findViewById(R.id.tls_layout);
                LinearLayout rea_lay = v.findViewById(R.id.reality_layout);
                if (position == 0) {
                    tls_lay.setVisibility(GONE);
                    rea_lay.setVisibility(GONE);
                }
                if (position == 1) {
                    tls_lay.setVisibility(VISIBLE);
                    rea_lay.setVisibility(GONE);
                }
                if (position == 2) {
                    tls_lay.setVisibility(GONE);
                    rea_lay.setVisibility(VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        ArrayAdapter<String> vmess_tls_fingerprinteadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fingerprint_list);
        vmess_tls_fingerprinteadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vmess_tls_fingerprint.setAdapter(vmess_tls_fingerprinteadapter);
        vmess_reality_fingerprint.setAdapter(vmess_tls_fingerprinteadapter);


        ArrayAdapter<String> vmess_tls_alpnadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, alpn_list);
        vmess_tls_alpnadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vmess_tls_alpn.setAdapter(vmess_tls_alpnadapter);


        ArrayAdapter<String> vmess_tls_insecureadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, insec_list);
        vmess_tls_insecureadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vmess_tls_insecure.setAdapter(vmess_tls_insecureadapter);

        buidler.setTitle("Vmess Generator");
        buidler.setCancelable(true);
        buidler.setView(v);
        final AlertDialog alert = buidler.create();
        alert.getWindow().setGravity(Gravity.CENTER);
        alert.show();
        ((Button) v.findViewById(R.id.btnSaveV2ray)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {
                if (proto_spin.getSelectedItemPosition() == 0) {
                    EditText address = v.findViewById(R.id.vmess_address);
                    EditText port = v.findViewById(R.id.vmess_port);
                    EditText id = v.findViewById(R.id.vmess_id);
                    EditText alterid = v.findViewById(R.id.vmess_alterid);
                    EditText req_host = v.findViewById(R.id.vmess_requesthost);
                    EditText path = v.findViewById(R.id.vmess_path);
                    EditText tls_sni = v.findViewById(R.id.vmess_sni);
                    EditText tls_sni_reality = v.findViewById(R.id.vmess_sni_reality);


                    //Spinners
                    String security = (String) vmess_security.getSelectedItem();
                    String network = (String) vmess_network.getSelectedItem();
                    String head = (String) vmess_headtype.getSelectedItem();
                    String tls = (String) vmess_tls.getSelectedItem();
                    String alpn = (String) vmess_tls_alpn.getSelectedItem();

                    String fp_tls = (String) vmess_tls_fingerprint.getSelectedItem();
                    String sni = tls_sni.getText().toString();
                    if (vmess_tls.getSelectedItemPosition() == 2) {
                        fp_tls = (String) vmess_reality_fingerprint.getSelectedItem();
                        sni = tls_sni_reality.getText().toString();
                    }

                    String front = "vmess://";
                    String json = "{\"add\":\"[address]\",\"aid\":\"[aid]\",\"alpn\":\"[alpn]\",\"fp\":\"[fp]\",\"host\":\"[host]\",\"id\":\"[id]\",\"net\":\"[net]\",\"path\":\"[path]\",\"port\":\"[port]\",\"ps\":\"[ps]\",\"scy\":\"[scy]\",\"sni\":\"[sni]\",\"tls\":\"[tls]\",\"type\":\"[type]\",\"v\":\"2\"}";
                    String formatjson = json.replace("[address]", address.getText().toString()).replace("[aid]", alterid.getText().toString()).replace("[alpn]", alpn).replace("[fp]", fp_tls).replace("[host]", req_host.getText().toString())
                            .replace("[id]", id.getText().toString()).replace("[net]", network).replace("[path]", path.getText().toString()).replace("[port]", port.getText().toString()).replace("[ps]", "cloud").replace("[scy]", security).replace("[sni]", sni)
                            .replace("[tls]", tls).replace("[type]", head);
                    editText.setText(front + toBase64(formatjson).replace("\n", ""));
                    alert.dismiss();
                }
                if (proto_spin.getSelectedItemPosition() == 1) {


                }
            }
        });


    }

    public static String toBase64(String message) {
        byte[] data;
        try {
            data = message.getBytes("UTF-8");
            String base64Sms = Base64.encodeToString(data, Base64.DEFAULT);
            return base64Sms;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void migrate_from_clip() {

        AlertDialog.Builder nBuilder2 = new AlertDialog.Builder(this);
        final View mView2 = getLayoutInflater().inflate(R.layout.migrate_layout, null);
        nBuilder2.setTitle("Migrate From Clipboard");
        nBuilder2.setView(mView2);
        final AlertDialog dialog2 = nBuilder2.create();
        final EditText defVersion2 = (EditText) mView2.findViewById(R.id.edittextConfig);
        final EditText pass = (EditText) mView2.findViewById(R.id.edittextPass);
        //defVersion2.setHint("Clipboard");
        defVersion2.setText(readFromClipboard().replace(" ", "").replace("\n", ""));
        nBuilder2.setPositiveButton("Migrate now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                if (defVersion2.getText().toString().isEmpty() || pass.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "No Clipboard Saved", Toast.LENGTH_SHORT).show();
                    dialog2.dismiss();
                } else {
                    migrate_config(defVersion2.getText().toString(), pass.getText().toString());
                    dialog2.dismiss();
                }

            }
        });
        nBuilder2.show();
    }

    public void migrate_from_online() {

        AlertDialog.Builder nBuilder2 = new AlertDialog.Builder(this);
        final View mView2 = getLayoutInflater().inflate(R.layout.migrate_layout_online, null);
        nBuilder2.setTitle("Migrate From Online");
        nBuilder2.setView(mView2);
        final AlertDialog dialog2 = nBuilder2.create();
        final EditText defVersion2 = (EditText) mView2.findViewById(R.id.edittextConfig);
        final EditText pass = (EditText) mView2.findViewById(R.id.edittextPass);
        //defVersion2.setHint("URL");
        nBuilder2.setPositiveButton("Migrate now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                if (defVersion2.getText().toString().isEmpty() || pass.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "No Clipboard Saved", Toast.LENGTH_SHORT).show();
                    dialog2.dismiss();
                } else {
                    migrateAPI(defVersion2.getText().toString(), pass.getText().toString());
                    dialog2.dismiss();
                }

            }
        });
        nBuilder2.show();
    }

    private void migrateAPI(String url, String pass) {
        final CuteDialog.withAnimation cdd = new CuteDialog.withAnimation(this)
                .setAnimation(R.raw.anim3)
                .setTitle("Migrate Online")
                .hideNegativeButton(true)
                .setDescription("Checking for api config...")
                .setPositiveButtonText("Okay", v2 -> {

                })
                .setNegativeButtonText("Cancel", v2 -> {

                });

        cdd.show();


        final RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest req = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String result) {

                        if (result == null) {
                            Toast.makeText(MainActivity.this, "Error checking update from api", Toast.LENGTH_SHORT).show();
                        } else if (result.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Config is empty from api", Toast.LENGTH_SHORT).show();
                        } else {
                            migrate_config(result.replace(" ", "").replace("\n", ""), pass);
                        }
                        if (cdd != null) {
                            cdd.cancel();
                        }
                        requestQueue.getCache().clear();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (cdd != null) {
                    cdd.cancel();
                }
                Toast.makeText(MainActivity.this, "Error checking update from api", Toast.LENGTH_SHORT).show();
                requestQueue.getCache().clear();
            }

        });

        requestQueue.add(req);


        // TODO: Implement this method
    }

    public void migrate_config(String config, String pass) {

        String decconfig = parseToString(config, pass);
        String newconfig = parse(decconfig);
        if (!decconfig.isEmpty()) {
            importMigrateServers(newconfig);
            importMigrateNetworks(newconfig, pass);
            Toast.makeText(this, "Import Successful!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Invalid Config/Password", Toast.LENGTH_SHORT).show();
        }
    }

    public void importMigrateServers(String text) {
        try {
            JSONObject jo1 = new JSONObject(parseToString(text));
            if (jo1.has("Version")) {
                String fileContent = text;
                String full = parseToString(fileContent);
                JSONObject sObj = new JSONObject(full);
                JSONArray sServer = sObj.getJSONArray("Servers");
                JSONObject jo = getJSONObject();
                jo.remove("Servers");
                try {
                    jo.put("Servers", sServer);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                try {
                    File file = new File(getFilesDir(), "Servers.js");
                    OutputStream out = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        out = Files.newOutputStream(file.toPath());
                    }
                    out.write(jo.toString().getBytes());
                    out.flush();
                    out.close();
                    //  TextView tv=findViewById(R.id.result);
                    //   tv.setText(getJsonString());

                } catch (Exception e) {
                }


                setupServerListView();

            } else {
                Toast.makeText(MainActivity.this, "Invalid Server config!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Invalid Server config!", Toast.LENGTH_SHORT).show();
            //	result.setText(e.getMessage());
        }
    }

    public void importMigrateNetworks(String text, String pass) {
        try {
            JSONObject jo = new JSONObject(parseToString(text));
            if (jo.has("Version")) {
                String fileContent = text;
                String full = parseToString(fileContent);
                JSONObject pObj = new JSONObject(full);

                JSONArray oldp = pObj.getJSONArray("Networks");
                JSONArray newp = new JSONArray();
                for (int i = 0; i < oldp.length(); i++) {
                    JSONObject jnew = new JSONObject();
                    JSONObject jold = oldp.getJSONObject(i);
                    String sName = jold.getString("Name");
                    String sPayload = parseToString(jold.getString("Payload"), pass);
                    String sNetworkInfo = jold.getString("Info");
                    String servhost = jold.getString("ServerHostDNS");
                    String frontQ = "";
                    if (jold.has("FrontQuery")) {
                        frontQ = jold.getString("FrontQuery");
                    }
                    String backQ = "";
                    if (jold.has("BackQuery")) {
                        backQ = jold.getString("BackQuery");
                    }


                    int prior = jold.getInt("Priority");
                    JSONObject proxyold = new JSONObject(jold.getJSONObject("ProxySettings").toString());
                    String sSquidProxy = parseToString(proxyold.getString("Squid"), pass);
                    String sSquidPort = proxyold.getString("Port");
                    jnew.put("Name", sName);
                    jnew.put("Payload", parse(sPayload));
                    jnew.put("Info", sNetworkInfo);
                    jnew.put("TunnelType", 2);
                    jnew.put("FrontQuery", frontQ);
                    jnew.put("BackQuery", backQ);
                    jnew.put("Priority", prior);
                    jnew.put("ServerHostDNS", servhost);
                    JSONObject proxy = new JSONObject();
                    proxy.put("Squid", parse(sSquidProxy));
                    proxy.put("Port", sSquidPort);
                    jnew.put("ProxySettings", proxy);
                    newp.put(jnew);
                }

                JSONObject jo1 = getJSONObject();
                jo1.remove("Networks");
                try {
                    jo1.put("Networks", newp);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                try {
                    File file = new File(getFilesDir(), "Servers.js");

                    OutputStream out = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        out = Files.newOutputStream(file.toPath());
                    }
                    out.write(jo.toString().getBytes());
                    out.flush();
                    out.close();
                    //    TextView tv=findViewById(R.id.result);
                    //    tv.setText(getJsonString());

                } catch (Exception ignored) {
                }
                setupNetowrksListView();
            } else {
                Toast.makeText(MainActivity.this, "Invalid Networks config!", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            //Toast.makeText(MainActivity.this, "Invalid Networks config!", Toast.LENGTH_SHORT).show();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

            //	result.setText(e.getMessage());
        }
    }

    //public static String encrypt(String str) {
    //   return TeaBase64.encryptToBase64String(str, "s!02a");
    //}

   // public static String decrypt(String str) {
    //    return TeaBase64.decryptBase64StringToString(str, "s!02a");
    //}

    public static String encrypt(String str) {
        return TeaBase64.encryptToBase64String(str, TeaBase64.TAG);
    }

    public static String decrypt(String str) {
        return TeaBase64.decryptBase64StringToString(str, TeaBase64.TAG);
    }

}