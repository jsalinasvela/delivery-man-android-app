package com.chupapp.distributor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

public class OrderActivity extends AppCompatActivity {

    private String DISTRIBUIDOR_ID = "9";

    private Db db;
    private JSONObject table;
    private Button button_changestatus, button_assignme;
    private String ORDER_ID = "";
    private String LANLON = "0,0";
    private String DIRECTION = "";
    private String PHONE = "";
    private LinearLayout group_button;
    private Toolbar toolbar;

    @SuppressLint("LongLogTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_order);

        //INICIALIZO LA BASE DE DATOS
        db = new Db(getApplicationContext());
        table = db.read();

        //RECIBO PARAMETROS
        Bundle b = getIntent().getExtras();
        ORDER_ID = "";

        if (b != null)
            ORDER_ID = b.getString("identifier");

        //inicializo
        TextView name_user = (TextView) findViewById(R.id.name_user);
        TextView direction_user = (TextView) findViewById(R.id.direction_user);
        TextView reference_user = (TextView) findViewById(R.id.reference_user);
        button_assignme = (Button) findViewById(R.id.button_assignme);
        Button button_viewmap = (Button) findViewById(R.id.button_viewmap);
        button_changestatus = (Button) findViewById(R.id.button_changestatus);
        group_button = (LinearLayout) findViewById(R.id.group_button);
        final LinearLayout list_orders = (LinearLayout) findViewById(R.id.list_orders);

        Button button_unassign = (Button) findViewById(R.id.button_unassign);


        try {
            JSONObject orders = new JSONObject(table.getString(Contract.Entry.COLUMN_ORDERS));
            //String o = orders.getString(identifier);

            JSONObject details = orders.getJSONObject("details");
            JSONObject orden = details.getJSONObject(ORDER_ID);

            Log.v("ESTADO MainActivity ","--> "+orden.toString());

            //ASIGNO EL COLOR
            setStatus(Integer.parseInt(orden.getString("status")));
            //Toast.makeText(getApplicationContext(),details.getString("status").toString(),Toast.LENGTH_LONG).show();

            //OBTENGO DATOS DEL USUARIO
            JSONObject user = orden.getJSONObject("user");
            name_user.setText(user.getString("first_name") + " " + user.getString("last_name"));

            //OBTENGO LA DIRECCION DEL USUARIO
            JSONObject location = orden.getJSONObject("location");

            direction_user.setText(location.getString("address"));
            DIRECTION = location.getString("address");

            reference_user.setText(location.getString("latitude") + ", " + location.getString("longitude"));
            LANLON = location.getString("latitude") + "," + location.getString("longitude");

            //Obtengo al distributor
            if (orden.getString("distributor").equals(DISTRIBUIDOR_ID)) {
                button_assignme.setVisibility(View.GONE);
                group_button.setVisibility(View.VISIBLE);
            } else if (orden.getString("distributor").equals("null") || orden.getString("distributor").equals("0")) {
                button_assignme.setVisibility(View.VISIBLE);
                group_button.setVisibility(View.GONE);
            } else {
                button_assignme.setVisibility(View.GONE);
                group_button.setVisibility(View.GONE);
            }

            //LISTO LOS PRODUCTOS
            //obtengo la lista de ordenes
            JSONArray orders_array = orden.getJSONArray("orders");

            list_orders.removeAllViews();
            Double total = 0.0;
            for (int i = 0; i < orders_array.length(); i++) {

                //cargo la plantilla
                LinearLayout layout_product = (LinearLayout) this.getLayoutInflater().inflate(R.layout.layout_product, null);

                JSONObject row = orders_array.getJSONObject(i);
                //row.getString("quantity");
                //row.getString("price");

                //obtengo los detalles del produc to
                JSONObject detailes_arr = row.getJSONObject("details");

                SimpleDraweeView image_product = (SimpleDraweeView) layout_product.findViewById(R.id.image_product);
                image_product.setImageURI(Uri.parse(detailes_arr.getString("image")));

                CheckBox check_product = (CheckBox) layout_product.findViewById(R.id.check_product);
                check_product.setText(row.getString("quantity") + " - " + detailes_arr.getString("name"));

                PHONE = user.getString("mobile");

                TextView price_product = (TextView) layout_product.findViewById(R.id.price_product);
                price_product.setText("S/ " + row.getString("price"));

                total = total + Double.parseDouble(row.getString("price"));
                //name = row.getString("name");
                //Log.v("ORDERS -> ","nombre: "+);
                //lista_orden.setText(detailes_arr.getString("name")+" + "+lista_orden.getText());

                list_orders.addView(layout_product);
            }

            LinearLayout layout_total = (LinearLayout) this.getLayoutInflater().inflate(R.layout.layout_total, null);
            TextView price_product_total = (TextView) layout_total.findViewById(R.id.price_product_total);
            price_product_total.setText("S/ " + total);
            list_orders.addView(layout_total);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        //eventos
        //final String finalKey = key;
        button_assignme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assignme(DISTRIBUIDOR_ID, ORDER_ID, Boolean.TRUE);
            }
        });

        button_unassign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assignme("0", ORDER_ID, Boolean.FALSE);
            }
        });

        button_viewmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri gmmIntentUri = Uri.parse("geo:" + LANLON + "?q=" + LANLON + "(" + DIRECTION + ")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        optionStatus();

        //TOOLBAR
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Orden " + ORDER_ID);

        registerReceiver(receiver, new IntentFilter(Serving.IDENTIFY));

    }

    @SuppressLint("ResourceType")
    private void setStatus(Integer status) {
        String color = "#FFFFFF";

        if (status == 1) {
            color = getString(R.color.colorDanger);
        } else if (status == 2) {
            color = getString(R.color.colorInverse);
        } else if (status == 3) {
            color = getString(R.color.colorSuccess);
        } else if (status == 4) {
            color = getString(R.color.colorDefault);
        } else if (status == 5) {
            color = getString(R.color.colorWarning);
        } else if (status == 6) {
            color = getString(R.color.colorDefault);
        } else if (status == 7) {
            color = getString(R.color.colorPrimaryDark);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(Color.parseColor(color));
            getWindow().setStatusBarColor(Color.parseColor(color));
            //toolbar.setBackgroundColor(Color.parseColor(color));
        }

    }

    private void setStatusApi(final Integer status, final String order_id) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        // Request a string response from the provided URL.
        StringRequest s = new StringRequest(Request.Method.POST, getString(R.string.SET_STATUS), new Response.Listener<StringRequest.reply>() {

            @Override
            public void onResponse(StringRequest.reply result) {
                Log.v("ESTADO setStatus(final String " + status + ")", "-> " + result.response);
                setStatus(status);
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("ESTADO VHTTP", "Error en la consulta " + error.toString());
            }

        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("status_id", String.valueOf(status));
                params.put("order_id", order_id);
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
                headers.put("Token", table.getString(Contract.Entry.COLUMN_TOKEN));
                */
                headers.put("User-Agent", "ChupApp/1.0");
                return headers;
            }

        };

        queue.add(s);
    }


    private void assignme(final String distributor_id, final String order_id, final Boolean hidden) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        // Request a string response from the provided URL.
        StringRequest s = new StringRequest(Request.Method.POST, getString(R.string.SET_DISTRIBUTOR), new Response.Listener<StringRequest.reply>() {

            @Override
            public void onResponse(StringRequest.reply result) {
                Log.v("ESTADO assignme", "-> " + result.response);

                if (hidden) {
                    button_assignme.setVisibility(View.GONE);
                    group_button.setVisibility(View.VISIBLE);
                } else {
                    button_assignme.setVisibility(View.VISIBLE);
                    group_button.setVisibility(View.GONE);
                }

            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("ESTADO VHTTP", "Error en la consulta " + error.toString());
            }

        }) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();
                params.put("distributor_id", distributor_id);
                params.put("order_id", order_id);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_default, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            //onBackPressed();
            startActivity(new Intent(getApplicationContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
            return true;
        }

        if (id == R.id.add_observation) {
            prompt();
            return true;
        }

        if (id == R.id.call_user) {
            call();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void call() {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+PHONE));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { return; }
        startActivity(callIntent);
    }

    private void prompt(){
        final Dialog d = new Dialog(OrderActivity.this);
        d.setTitle("Agregar observación:");

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        d.getWindow().setAttributes(lp);
        d.setContentView(R.layout.layout_prompt);


        LinearLayout b1 = (LinearLayout) d.findViewById(R.id.aceptar);
        LinearLayout b2 = (LinearLayout) d.findViewById(R.id.cancelar);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //accion
                d.dismiss();
            }
        });

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });

        d.show();
    }

    private void optionStatus(){
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(OrderActivity.this);
        builderSingle.setIcon(R.mipmap.ic_launcher);
        builderSingle.setTitle("Selecciona un estado");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(OrderActivity.this, android.R.layout.select_dialog_item);

        arrayAdapter.add("Alistando orden");
        arrayAdapter.add("Estoy en camino");
        arrayAdapter.add("Eh llegado");
        arrayAdapter.add("Venta entregada");
        arrayAdapter.add("Cancelar Orden");
        arrayAdapter.add("No encontre la ubicación");
        arrayAdapter.add("Rechazar orden");


        builderSingle.setNegativeButton("Cerrar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);
                Log.v("ESTADO ","--> "+strName);

                int status = 0;

                if(strName.equals("Alistando orden")) {
                    setStatusApi(1, ORDER_ID);
                    status = 1;
                    //setStatusApi(final Integer status, final String order_id)
                }else if(strName.equals("Estoy en camino")) {
                    setStatusApi(2, ORDER_ID);
                    status = 2;
                }else if(strName.equals("Eh llegado")) {
                    setStatusApi(7, ORDER_ID);
                    status = 7;
                }else if(strName.equals("Venta entregada")) {
                    confirm();
                    status = 3;
                }else if(strName.equals("Cancelar Orden")) {
                    setStatusApi(4, ORDER_ID);
                    status = 4;
                }else if(strName.equals("No encontre la ubicación")) {
                    setStatusApi(5, ORDER_ID);
                    status = 5;
                }else if(strName.equals("Rechazar orden")) {
                    setStatusApi(6, ORDER_ID);
                    status = 6;
                }


                try {

                    Log.v("ESTADO OrderActivity > optionStatus",""+table.getString(Contract.Entry.COLUMN_ORDERS).toString());

                    JSONObject ords = new JSONObject(table.getString(Contract.Entry.COLUMN_ORDERS));
                    JSONObject details = ords.getJSONObject("details");
                    JSONObject orden = details.getJSONObject(ORDER_ID);

                    if (status!=0){
                        orden.put("status",status);
                        details.put(ORDER_ID,orden);
                        ords.put("details",details);



                        ContentValues dbupd = new ContentValues();
                        dbupd.put(Contract.Entry.COLUMN_ORDERS,ords.toString());
                        db.update(dbupd);

                        Log.v("ESTADO OrderActivity > optionStatus 2 ",""+ords.toString());
                    }




                    //{"distributor":"1","status":"4","user":{"user_id":"1","first_name":"Heber","last_name":"Futuri","email":"h@futuri.pe","mobile":"+51959191425","social_id":"911294895699771","social_photo":"https:\/\/graph.facebook.com\/911294895699771\/picture?width=200&height=200","confirm":"1"},"location":{"location_id":"22","user_id":"1","order_id":"18","address":"Avenida Cayma Nro. 203","latitude":"-16.378780358782","longitude":"-71.544610597193"},"orders":[{"order_detail_id":"25","order_id":"18","product_id":"23","quantity":"1","price":"6","purity":"1","status":"1","created":"2017-12-27 15:53:54","updated":"2017-12-27 15:53:54","details":{"product_id":"23","category_id":"8","name":"Gaseosa Guarana","liters":"3000","purity":"1","image":"https:\/\/chupapp.com\/files\/product\/gaseosa_guarana.png","description":"Botella de 3l","price":"6","color":"#ff7d0a","promotion":null,"status":"1","created":"2017-12-07 00:15:58","updated":"2017-12-07 00:15:58"},"liters":3000}]}


                    /*//actualizo la bd con el estado nuevo
                    ContentValues s_update = new ContentValues();
                    s_update.put(Contract.Entry.COLUMN_ORDERS,"");
                    db.update(s_update);
                    */

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                /*
                AlertDialog.Builder builderInner = new AlertDialog.Builder(OrderActivity.this);
                builderInner.setMessage(strName);
                builderInner.setTitle("El estado seleccionado es");
                builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        dialog.dismiss();
                    }
                });
                builderInner.show();
                */
            }
        });

        button_changestatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builderSingle.show();
            }
        });

    }

        /*
     * CONFIRMAR VENTA
     */

    public void confirm() {
        //.setIcon(android.R.drawable.ic_dialog_alert)
        AlertDialog.Builder confirm = new AlertDialog.Builder(this)
                .setTitle("Confirmar venta")
                .setMessage("Presiona OK solo si tu venta ha sido realizada y cobrada correctamente.\n\nDespués de dar en OK la venta en la base de datos no podrá cambiarse por este medio.");

        confirm.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //confirmar venta
                setStatusApi(3, ORDER_ID);
            }
        });

        confirm.setNegativeButton("CANCELAR", null);

        confirm.show();

    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("LongLogTag")
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Log.v("ESTADO BroadcastReceiver receiver ",""+bundle.toString());
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
