package com.example.user.myproject.Modal;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.user.myproject.About;
import com.example.user.myproject.DetailEventActivity;
import com.example.user.myproject.LoginActivity;
import com.example.user.myproject.ScanQRCodeForStaff;
import com.example.user.myproject.PastJoined;
import com.example.user.myproject.R;
import com.example.user.myproject.RedeemBenefit;
import com.example.user.myproject.SoftSkill;
import com.example.user.myproject.Upcoming;
import com.example.user.myproject.Waiting;
import com.example.user.myproject.WalkInRegistrationActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class Homepage extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener , SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemSelectedListener {

    ListView lstView;
    ArrayList<ApplicationEvent> eventList = new ArrayList<>();
    MqttAndroidClient client;
    MqttAndroidClient client2;
    MqttAndroidClient client3;
    MqttAndroidClient client4;
    MqttAndroidClient client5;
    Context context;
    AlertDialog dialog;
    String studentId = "";
    ProgressDialog pd;
    String name = "";
    MqttConnectOptions options = new MqttConnectOptions();
    private int hot_number = 0;
    private TextView ui_notif = null;

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            connected = true;
        }
        else {
            connected = false;
        }

        if(connected) {

            if(!new SessionManager(this).checkLogin()) {
                finish();
            }

            studentId = new SessionManager(this).getUserDetails().get("id");
            if(studentId == null || studentId.equals("")){
                Intent intent = new Intent(this,LoginActivity.class);
                startActivity(intent);
                return;

            }

            name = new SessionManager(this).getUserDetails().get("name");

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            View hView = navigationView.getHeaderView(0);
            TextView appDrawerName = (TextView) hView.findViewById(R.id.appDrawerName);
            appDrawerName.setText(new SessionManager(this).getUserDetails().get("name"));
            TextView appDrawerId = (TextView) hView.findViewById(R.id.appDrawerId);
            appDrawerId.setText(new SessionManager(this).getUserDetails().get("id").toUpperCase());

            //Toast.makeText(Homepage.this, new SessionManager(this).getUserDetails().get("address"), Toast.LENGTH_LONG).show();
            context = this;

            swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

            swipeRefreshLayout.setOnRefreshListener(this);
            swipeRefreshLayout.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // swipeRefreshLayout.setRefreshing(true);
                                            // readQuestion();
                                            //readEvent();
                                        }
                                    }

            );

            Spinner spinner = (Spinner) findViewById(R.id.eventSpinner);

            spinner.setOnItemSelectedListener(this);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.eventCategory, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            swipeRefreshLayout.setRefreshing(true);


        } else {
            Toast.makeText(Homepage.this, "No internet connection!", Toast.LENGTH_LONG).show();
        }

        options.setUserName(Action.MQTT_USERNAME);
        options.setPassword(Action.MQTT_PASSWORD.toCharArray());
        options.setConnectionTimeout(100);
        options.setKeepAliveInterval(200);
        options.setCleanSession(true);
        //options.setAutomaticReconnect(true);
        conn();
        conn2();
        conn4();
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.homepage_action, menu);
        final RelativeLayout menu_hotlist = (RelativeLayout) menu.findItem(R.id.action_upcoming).getActionView();
        ui_notif = (TextView) menu_hotlist.findViewById(R.id.notif_no);
        updateHotCount(hot_number);
        menu_hotlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Upcoming.class);
                startActivity(intent);
            }
        });
        return super.onCreateOptionsMenu(menu);
    }


    public void updateHotCount(final int new_hot_number) {
        hot_number = new_hot_number;
        if (ui_notif == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (new_hot_number == 0)
                    ui_notif.setVisibility(View.INVISIBLE);
                else {
                    ui_notif.setVisibility(View.VISIBLE);
                    ui_notif.setText(Integer.toString(new_hot_number));
                }
            }
        });
    }

    public void buildSubscriptionDialog(String[] category){
        AlertDialog.Builder mBuilder  = new AlertDialog.Builder(Homepage.this);
        View mView = getLayoutInflater().inflate(R.layout.subscription_layout,null);
        ArrayList<String> subscriptionList = new ArrayList<String>(Arrays.asList(category));
        CheckBox chkBoxSports = (CheckBox)mView.findViewById(R.id.chkBoxSub1);
        CheckBox chkBoxEducation = (CheckBox)mView.findViewById(R.id.chkBoxSub2);
        CheckBox chkBox1 = (CheckBox)mView.findViewById(R.id.chkBoxSub3);
        CheckBox chkBox2 = (CheckBox)mView.findViewById(R.id.chkBoxSub4);
        CheckBox chkBox3 = (CheckBox)mView.findViewById(R.id.chkBoxSub5);
        CheckBox chkBoxMusic = (CheckBox)mView.findViewById(R.id.chkBoxSub6);
        CheckBox chkBox4 = (CheckBox)mView.findViewById(R.id.chkBoxSub7);
        CheckBox chkBox5 = (CheckBox)mView.findViewById(R.id.chkBoxSub8);
        CheckBox chkBox6 = (CheckBox)mView.findViewById(R.id.chkBoxSub9);
        CheckBox chkBox7 = (CheckBox)mView.findViewById(R.id.chkBoxSub10);
        CheckBox chkBox8 = (CheckBox)mView.findViewById(R.id.chkBoxSub11);
        CheckBox chkBox9 = (CheckBox)mView.findViewById(R.id.chkBoxSub12);
        CheckBox chkBox10 = (CheckBox)mView.findViewById(R.id.chkBoxSub13);
        CheckBox chkBox11 = (CheckBox)mView.findViewById(R.id.chkBoxSub14);
        CheckBox chkBox12 = (CheckBox)mView.findViewById(R.id.chkBoxSub15);
        CheckBox chkBox13 = (CheckBox)mView.findViewById(R.id.chkBoxSub16);
        CheckBox chkBox14 = (CheckBox)mView.findViewById(R.id.chkBoxSub17);
        CheckBox chkBox15 = (CheckBox)mView.findViewById(R.id.chkBoxSub18);
        CheckBox chkBox16 = (CheckBox)mView.findViewById(R.id.chkBoxSub19);
        CheckBox chkBox17 = (CheckBox)mView.findViewById(R.id.chkBoxSub20);


        final ArrayList<CheckBox> checkBoxList = new ArrayList<CheckBox>();
        checkBoxList.add(chkBoxSports);
        checkBoxList.add(chkBoxEducation);
        checkBoxList.add(chkBox1);
        checkBoxList.add(chkBox2);
        checkBoxList.add(chkBox3);
        checkBoxList.add(chkBoxMusic);
        checkBoxList.add(chkBox4);
        checkBoxList.add(chkBox5);
        checkBoxList.add(chkBox6);
        checkBoxList.add(chkBox7);
        checkBoxList.add(chkBox8);
        checkBoxList.add(chkBox9);
        checkBoxList.add(chkBox10);
        checkBoxList.add(chkBox11);
        checkBoxList.add(chkBox12);
        checkBoxList.add(chkBox13);
        checkBoxList.add(chkBox14);
        checkBoxList.add(chkBox15);
        checkBoxList.add(chkBox16);
        checkBoxList.add(chkBox17);


        for (CheckBox temp : checkBoxList){
            for (String msg: subscriptionList){
                if(temp.getText().equals(msg)){
                    temp.setChecked(true);
                }
            }
        }

        Button btnSubscribe = (Button) mView.findViewById(R.id.btnSubscribe);
        btnSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pd = new ProgressDialog(Homepage.this);
                pd.setMessage("Updating subscription info...");
                pd.show();
                ArrayList<String> result = new ArrayList<String>();
                for(CheckBox temp:checkBoxList){
                    if(temp.isChecked()){
                        result.add(temp.getText().toString());
                    }

                }
                String[] parseString = {};
                parseString = result.toArray(parseString);
                conn3(parseString);


            }
        });

        Button btnCancel = (Button) mView.findViewById(R.id.btnCancelSubscription);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        mBuilder.setView(mView);
        dialog = mBuilder.create();
        dialog.show();
        pd.dismiss();
    }


    @Override
    protected void onStart() {
        super.onStart();
        studentId = new SessionManager(this).getUserDetails().get("id");

    }

    @Override
    protected void onResume() {
        super.onResume();
        studentId = new SessionManager(this).getUserDetails().get("id");
        name = new SessionManager(this).getUserDetails().get("name");
        options = new MqttConnectOptions();
        options.setUserName(Action.MQTT_USERNAME);
        options.setPassword(Action.MQTT_PASSWORD.toCharArray());

        options.setConnectionTimeout(100);
        options.setKeepAliveInterval(200);

        options.setCleanSession(true);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {


                new AlertDialog.Builder(this)
                        .setTitle("Really Exit?")
                        .setMessage("Are you sure you want to exit?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Homepage.super.onBackPressed();
                                finishAffinity();
                                //System.exit(0);
                                //finishAndRemoveTask();
                            }
                        }).create().show();

        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_scan) {
            Intent intent = new Intent(getApplicationContext(), ScanQRCodeForStaff.class);
            startActivity(intent);
            return true;
        } else if(id == R.id.action_logout) {
            AlertDialog.Builder alert = new AlertDialog.Builder(Homepage.this);
            alert.setTitle("Logout");
            alert.setMessage("Confirm to logout?");
            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    new SessionManager(getApplicationContext()).logoutUser();
                    startActivity(intent);
                    dialog.dismiss();
                }
            });

            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.show();
        } else if(id == R.id.action_about) {
            Intent intent = new Intent(getApplicationContext(), About.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            conn2();

            //readEvent();

            conn4();
        } else if (id == R.id.nav_subscriptionCategory) {
            pd = new ProgressDialog(Homepage.this);
            pd.setMessage("Loading subscription info...");
            pd.show();
            conn5();
        } else if (id == R.id.nav_incomingEvent) {
            Intent intent = new Intent(this, Upcoming.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_waitingList) {
            Intent intent = new Intent(this, Waiting.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_pastJoinedEvent) {
            Intent intent = new Intent(this, PastJoined.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_walkinRegistration) {
            Intent intent = new Intent(getApplicationContext(), WalkInRegistrationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }else if (id == R.id.nav_redeemBenefits){
            Intent intent = new Intent(this, RedeemBenefit.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else if(id == R.id.nav_softskill) {
            Intent intent = new Intent(this, SoftSkill.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void conn(){

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), Action.MQTT_ADDRESS,
                clientId);

        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Toast.makeText(Homepage.this, "Connected!!", Toast.LENGTH_LONG).show();
                    try {
                        client.subscribe(Action.clientTopic+studentId, 1);
                        //Toast.makeText(Homepage.this, Action.clientTopic+studentId, Toast.LENGTH_LONG).show();

                        //here
                        //readEvent();

                    } catch (MqttException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(Homepage.this, "Connection fail!!", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void conn2(){

        String clientId = MqttClient.generateClientId();
        client2 = new MqttAndroidClient(this.getApplicationContext(), Action.MQTT_ADDRESS,
                clientId);

        try {
            IMqttToken token = client2.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Toast.makeText(Homepage.this, "Connected!!", Toast.LENGTH_LONG).show();
                    try {
                        client2.subscribe(Action.clientTopic+studentId, 1);
                        //Toast.makeText(Homepage.this, Action.clientTopic+studentId, Toast.LENGTH_LONG).show();


                        readEvent();
                    } catch (MqttException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(Homepage.this, "Connection fail!!", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void conn3(final String[] parseString){
        String clientId = MqttClient.generateClientId();
        client3 = new MqttAndroidClient(this.getApplicationContext(), Action.MQTT_ADDRESS,
                clientId);

        try {
            IMqttToken token = client3.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Toast.makeText(Homepage.this, "Connected!!", Toast.LENGTH_LONG).show();
                    try {
                        client3.subscribe(Action.clientTopic+studentId, 1);

                    } catch (MqttException ex) {
                        ex.printStackTrace();
                    }


                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("studentId", studentId);
                        Gson convertString = new Gson();
                        String subscriptionListJson = convertString.toJson(parseString,String[].class);
                        obj.put("subscription",subscriptionListJson);
                        String testMessage = obj.toString();
                        publishMessage3(Action.combineMessage("001603",Action.asciiToHex(obj.toString())));

                        if (client3 == null ){
                            Toast.makeText(Homepage.this, "Update subscription fail", Toast.LENGTH_LONG).show();
                        }
                        client3.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {
                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                String strMessage = new String(message.getPayload());
                                strMessage = Action.hexToAscii(strMessage);
                                JSONObject jObj = new JSONObject(strMessage);
                                int rowChanged = jObj.getInt("success");
                                String resultMessage = jObj.getString("message");
                                Toast.makeText(Homepage.this, resultMessage, Toast.LENGTH_LONG).show();
                                dialog.cancel();
                                pd.dismiss();
                            }

                            @Override
                            public void deliveryComplete(IMqttDeliveryToken token) {
                                // Toast.makeText(Homepage.this, "All message received!!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(Homepage.this, "Connection fail!!", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void conn4(){
        String clientId = MqttClient.generateClientId();
        client4 = new MqttAndroidClient(this.getApplicationContext(), Action.MQTT_ADDRESS,
                clientId);

        try {
            IMqttToken token = client4.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Toast.makeText(Homepage.this, "Connected!!", Toast.LENGTH_LONG).show();
                    try {
                        client4.subscribe(Action.clientTopic+studentId, 1);

                    } catch (MqttException ex) {
                        ex.printStackTrace();
                    }catch (Exception ex){
                        ex.printStackTrace();
                        return;
                    }
                    JSONObject obj = new JSONObject();
                    String jsonString = "";
                    try {
                        obj.put("studentId",studentId);
                        jsonString = obj.toString();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    try {
                        byte[] ss= Action.combineMessage("001625",Action.asciiToHex(jsonString)).getBytes();
                        client4.publish(Action.serverTopic, ss, 0, false);
                        //Toast.makeText(Homepage.this, "publish success l!!", Toast.LENGTH_LONG).show();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    if (client4 == null ){
                        Toast.makeText(Homepage.this, "Upcoming events count retrieve failed", Toast.LENGTH_LONG).show();
                    }
                    client4.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            String strMessage = new String(message.getPayload());
                            strMessage = Action.hexToAscii(strMessage);
                            JSONObject jObj = new JSONObject(strMessage);
                            hot_number = Integer.parseInt(jObj.getString("count"));
                            //Toast.makeText(Homepage.this, jObj.getString("count"), Toast.LENGTH_LONG).show();
                            updateHotCount(hot_number);
                            //readEvent();
                            //disconnect4();
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {
                            // Toast.makeText(Homepage.this, "All message received!!", Toast.LENGTH_LONG).show();
                        }
                    });


                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(Homepage.this, "Connection fail!!", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            disconnect4();
        }
    }

    //load subscription
    public void conn5(){
        String clientId = MqttClient.generateClientId();
        client5 = new MqttAndroidClient(this.getApplicationContext(), Action.MQTT_ADDRESS,
                clientId);

        try {
            IMqttToken token = client5.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Toast.makeText(Homepage.this, "Connected!!", Toast.LENGTH_LONG).show();
                    try {
                        client5.subscribe(Action.clientTopic+studentId, 1);
                    } catch (MqttException ex) {
                        ex.printStackTrace();
                    }
                    JSONObject jsonObj = new JSONObject();
                    try{
                        jsonObj.put("studentId",studentId);
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                    try {
                        byte[] ss= Action.combineMessage("001602",Action.asciiToHex(jsonObj.toString())).getBytes();
                        client5.publish(Action.serverTopic, ss, 0, false);

                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    if (client5 == null ){
                        Toast.makeText(Homepage.this, "Connection fail!!", Toast.LENGTH_LONG).show();
                    }
                    client5.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {

                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            String strMessage = new String(message.getPayload());
                            strMessage = Action.hexToAscii(strMessage);
                            String[] messages = {};

                            if (!strMessage.equals("[]")){
                                JSONArray temp1 = new JSONArray(strMessage);
                                JSONObject obj = temp1.getJSONObject(0);
                                String strArr = obj.getString("subscription");
                                Gson gson = new Gson();
                                messages = gson.fromJson(strArr,String[].class);
                            }

                            buildSubscriptionDialog(messages);
                            disconnect5();
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {

                        }
                    });
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(Homepage.this, "Connection fail!!", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }


    }

    public void disconnect(){
        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //  Toast.makeText(Homepage.this, "disconnected!!", Toast.LENGTH_LONG).show();
                    // we are now successfully disconnected
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(Homepage.this, "Disconnect fail!!", Toast.LENGTH_LONG).show();
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }catch(Exception ex){
            ex.printStackTrace();
        }

    }

    public void disconnect2(){
        try {
            IMqttToken disconToken = client2.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //  Toast.makeText(Homepage.this, "disconnected!!", Toast.LENGTH_LONG).show();
                    // we are now successfully disconnected
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(Homepage.this, "Disconnect fail!!", Toast.LENGTH_LONG).show();
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }catch(Exception ex){
            ex.printStackTrace();
        }

    }

    public void disconnect4(){
        try {
            IMqttToken disconToken = client4.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //  Toast.makeText(Homepage.this, "disconnected!!", Toast.LENGTH_LONG).show();
                    // we are now successfully disconnected
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(Homepage.this, "Disconnect fail!!", Toast.LENGTH_LONG).show();
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }catch(Exception ex){
            ex.printStackTrace();
        }


    }

    public void disconnect5(){
        try {
            IMqttToken disconToken = client5.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //  Toast.makeText(Homepage.this, "disconnected!!", Toast.LENGTH_LONG).show();
                    // we are now successfully disconnected
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(Homepage.this, "Disconnect fail!!", Toast.LENGTH_LONG).show();
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void publishMessage(String message) {

        try {
            byte[] ss= message.getBytes();
            client.publish(Action.serverTopic, message.getBytes(), 0, false);
            //Toast.makeText(Homepage.this, "publish success l!!", Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }



    public void publishMessage3(String message){
        try {
            byte[] ss= message.getBytes();
            client3.publish(Action.serverTopic, message.getBytes(), 0, false);
            //Toast.makeText(Homepage.this, "publish success l!!", Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribeEventMessage(){
        if (client2 == null ){
            Toast.makeText(Homepage.this, "Connection fail!!", Toast.LENGTH_LONG).show();
        }
        client2.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

                String s = "";
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String strMessage = new String(message.getPayload());
                swipeRefreshLayout.setRefreshing(false);
                GsonBuilder builder = new GsonBuilder();
                builder.serializeNulls();
                Gson gson = builder.create();
                String decoded = Action.hexToAscii(strMessage);
                EncodedApplicationEvent[] result = gson.fromJson(decoded,EncodedApplicationEvent[].class);
                ArrayList<EncodedApplicationEvent> arrList1 = new ArrayList<>(Arrays.asList(result));
                final ArrayList<ApplicationEvent> arrList = new ArrayList<ApplicationEvent>();

                for(EncodedApplicationEvent e : arrList1){
                    arrList.add(e.getApplicationEvent());

                }

                lstView = (ListView)findViewById(R.id.eventListView);
                lstView.setEmptyView(findViewById(R.id.empty));
                BasicListAdapter eventListView = new BasicListAdapter(context,R.layout.basiclist_entry_layout,arrList);
                lstView.setAdapter(eventListView);

                lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView adapterView, View view, int i, long l) {

                        Intent intent = new Intent(view.getContext(), DetailEventActivity.class);
                        intent.putExtra("TIMETABLEID", arrList.get(i).getTimetableId()+"");
                        intent.putExtra("FROM", "");
                        intent.putExtra("REGISTRATION", new EventRegistration());
                        startActivity(intent);
                    }
                });
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //Toast.makeText(Homepage.this, "All message received!!", Toast.LENGTH_LONG).show();
                String str = "";
                try {
                    str = new String(token.getMessage().getPayload());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });


    }


    public void setSubscription(String topicStr) {
        try {
            client.subscribe(topicStr, 1);
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
        String ss= "";
    }



    public void readEvent(){
        JSONObject obj = new JSONObject();
        Spinner spinner = (Spinner)findViewById(R.id.eventSpinner);
        String jsonString = "";
        try {
            obj.put("studentId",studentId);
            obj.put("criteria",spinner.getSelectedItem().toString());
            jsonString = obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            byte[] ss= Action.combineMessage("001601",Action.asciiToHex(jsonString)).getBytes();
            client2.publish(Action.serverTopic, ss, 0, false);
            //Toast.makeText(Homepage.this, "publish success l!!", Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            e.printStackTrace();
        }


        subscribeEventMessage();
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);

        conn4();
        conn2();
        //readEvent();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        swipeRefreshLayout.setRefreshing(true);
        TextView views = (TextView)findViewById(R.id.txtEventResult);
        views.setText(adapterView.getItemAtPosition(i).toString());

        JSONObject obj = new JSONObject();
        Spinner spinner = (Spinner)findViewById(R.id.eventSpinner);
        String jsonString = "";
        try {
            obj.put("studentId",new SessionManager(this).getUserDetails().get("id"));
            obj.put("criteria",spinner.getSelectedItem().toString());
            jsonString = obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            byte[] ss= Action.combineMessage("001601",Action.asciiToHex(jsonString)).getBytes();
            client2.publish(Action.serverTopic, ss, 0, false);
            //Toast.makeText(Homepage.this, "publish success l!!", Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        subscribeEventMessage();

        //conn4();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            disconnect();
            disconnect2();
        } catch (Exception ex) {

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            //disconnect();
            //disconnect2();
        } catch (Exception ex) {

        }
    }
}
