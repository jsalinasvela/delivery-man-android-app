package com.chupapp.distributor;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.chupapp.distributor.database.Contract;
import com.chupapp.distributor.database.Db;
import com.chupapp.distributor.helper.network.StringRequest;
import com.chupapp.distributor.service.Serving;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Db db;
    private JSONObject table;
    private LinearLayout scroll_orders;
    private Toolbar toolbar;
    private ProgressBar progressord;
    private ScrollView scrollord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_main);

        //INICIALIZO LA BASE DE DATOS
        db = new Db(getApplicationContext());
        table = db.init();
        progressord = (ProgressBar) findViewById(R.id.progressord);
        scrollord =  (ScrollView) findViewById(R.id.scrollord);

        scroll_orders = (LinearLayout) findViewById(R.id.scroll_orders);

        registerReceiver(receiver, new IntentFilter(Serving.IDENTIFY));

        if (!CheckService(Serving.class)) {
            Intent serv = new Intent(getApplicationContext(), Serving.class);
            getApplicationContext().startService(serv);
            //Log.v("ESTADO servicio", "Service started");
        } else {
            //Log.v("ESTADO servicio", "Service already running");
        }

        try {
            if (!table.getString(Contract.Entry.COLUMN_ORDERS).isEmpty()){
                String dat = table.getString(Contract.Entry.COLUMN_ORDERS);
                setListerOrders(dat);
            }else{
                //obtener ordenes
                getOrders();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //TOOLBAR
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Lista de ordenes");


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.update_list) {
            progressord.setVisibility(View.VISIBLE);
            scrollord.setVisibility(View.GONE);
            getOrders();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("ResourceType")
    private void setListerOrders(String orders){
        try {
            scroll_orders.removeAllViews();


            JSONObject ords = new JSONObject(orders);
            JSONObject details = ords.getJSONObject("details");
            Iterator<?> keys = details.keys();
            while(keys.hasNext()) {
                final String key = (String)keys.next();
                JSONObject orden = details.getJSONObject(key);
                Log.v("ORDEN "," -> "+orden.toString());
                JSONObject user = orden.getJSONObject("user");

                LinearLayout layout_category = (LinearLayout)this.getLayoutInflater().inflate(R.layout.layout_order,null);

                SimpleDraweeView user_picture = (SimpleDraweeView) layout_category.findViewById(R.id.user_picture);
                user_picture.setImageURI(Uri.parse(user.getString("social_photo")));

                TextView denominacion = (TextView) layout_category.findViewById(R.id.denominacion);
                denominacion.setText("Orden "+key);

                TextView lista_orden = (TextView) layout_category.findViewById(R.id.lista_orden);

                TextView distributor_id = (TextView) layout_category.findViewById(R.id.distributor_id);
                distributor_id.setText("Distribuidor Asignado: "+orden.getString("distributor"));

                TextView fecha_orden = (TextView) layout_category.findViewById(R.id.fecha_orden);
                fecha_orden.setText(""+orden.getString("fecha"));

                //obtengo la lista de ordenes
                JSONArray products_orders_array = orden.getJSONArray("orders");

                for (int i = 0; i < products_orders_array.length(); i++) {
                    JSONObject row = products_orders_array.getJSONObject(i);
                    //row.getString("quantity");
                    //row.getString("price");

                    JSONObject detailes_arr = row.getJSONObject("details");

                    //name = row.getString("name");
                    //Log.v("ORDERS -> ","nombre: "+);
                    lista_orden.setText(detailes_arr.getString("name")+" + "+lista_orden.getText());
                }

                layout_category.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        //Toast.makeText(MainActivity.this, "Probando", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(getApplicationContext(), OrderActivity.class);

                        //preparo el parametro
                        Bundle b = new Bundle();
                        b.putString("identifier", key);
                        i.putExtras(b);

                        //inicio
                        startActivity(i);
                    }

                });

                //Log.v("COLOR ESTADO","--> "+orden.getString("status"));
                int color = getColorStatus(Integer.parseInt(orden.getString("status")));
                //Log.v("COLOR ","-->"+color);
                layout_category.setBackgroundColor(color);

                scroll_orders.addView(layout_category);
            }

            progressord.setVisibility(View.GONE);
            scrollord.setVisibility(View.VISIBLE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ResourceType")
    private int getColorStatus(int status){
        String color = "#FFFFFF";
        //Log.v("COLOR","->"+color);

        if (status==1){
            color = getString(R.color.colorDanger);
        }else if (status==2){
            color = getString(R.color.colorInverse);
        }else if (status==3){
            color = getString(R.color.colorSuccess);
        }else if (status==4){
            color = getString(R.color.colorDefault);
        }else if (status==5){
            color = getString(R.color.colorWarning);
        }else if (status==6){
            color = getString(R.color.colorDefault);
        }else if (status==7){
            color = getString(R.color.colorPrimaryDark);
        }

        return Color.parseColor(color);
    }

    private void getOrders() {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        // Request a string response from the provided URL.
        StringRequest s = new StringRequest(Request.Method.POST, getString(R.string.ORDERS), new Response.Listener<StringRequest.reply>() {

            @Override
            public void onResponse(StringRequest.reply result) {
                //Log.v("ESTADO GETORDERS ORDER", "-> " + result.response);

                ContentValues ords = new ContentValues();
                ords.put(Contract.Entry.COLUMN_ORDERS,result.response);
                table = db.update(ords);


                setListerOrders(result.response);

            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("ESTADO VHTTP MainActivity > getOrders", "That didn't work!");
            }
        }) {

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();
                //params.put("prueba", "111111");

                /*
                try {

                    JSONObject status = new JSONObject(table.getString(Contract.Entry.COLUMN_STATUS));
                    JSONObject order = status.getJSONObject("order");
                    params.put("order", order.getString("order_id"));

                    JSONObject dat = new JSONObject(table.getString(Contract.Entry.COLUMN_STATUS));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                */

                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                /*
                try {
                    headers.put("Token", table.getString(Contract.Entry.COLUMN_TOKEN));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                */

                headers.put("User-Agent", "ChupApp/1.0");
                return headers;
            }

        };

        queue.add(s);
    }

        /*
     * FUNCIONES PARA EL SERVICIO
     */

    private boolean CheckService(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(getApplicationContext().ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("LongLogTag")
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            /*
            if (bundle != null && bundle.getString("status") != null) {
            */
                try {
                    String data_status = bundle.getString("status");
                    Log.v("ESTADO BroadcastReceiver receiver ","---> "+bundle.getString("status").toString());
                    setListerOrders(data_status);

                }catch (Exception e){//
                    e.printStackTrace();
                }
            /*}else{
                Log.v("ESTADO BroadcastReceiver receiver ","SIN DATOS");
            }
            */
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //Log.v("ESTADO onBackPressed"," back en categories activity");

        try {
            table = db.read();

            if (table.getString(Contract.Entry.COLUMN_ORDERS)!=""){
                String dat = table.getString(Contract.Entry.COLUMN_ORDERS);
                setListerOrders(dat);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
