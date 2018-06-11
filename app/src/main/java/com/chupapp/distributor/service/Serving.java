package com.chupapp.distributor.service;
/**
 * Created by inmobitec on 14/11/17.
 */


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.chupapp.distributor.MainActivity;
import com.chupapp.distributor.R;
import com.chupapp.distributor.database.Contract;
import com.chupapp.distributor.database.Db;
import com.chupapp.distributor.helper.network.StringRequest;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

import static com.google.android.gms.internal.zzagz.runOnUiThread;
//import android.widget.Toast;

public class Serving extends Service implements LocationListener {

    private String DISTRIBUIDOR_ID = "9";

    private Db db;
    private JSONObject table;
    private Socket socketio;
    public static final int NOTIFICATION_ID = 26;
    public static final String IDENTIFY = "271217";
    public static final String IDENTIFY_DELIVERY = "2";
    public static final String DATA = "STATUS";
    public static final String DELIVERY = "DELIVERY";
    private LocationListener loclis;
    private LocationManager locman;
    private boolean GpsStatus;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        //Toast.makeText(this,"PASO ONBIND",Toast.LENGTH_LONG).show();
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        db = new Db(getApplicationContext());
        table = db.read();

        //Conecta socketio
        try {

            IO.Options opt = new IO.Options();
            //opt.query = "model=a&movil=b";
            opt.secure = true;

            socketio = IO.socket(getString(R.string.SOCKET_DISTRIBUTOR), opt);
            socketio.connect();
            SocketListen();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        loclis = this;
    }

    private void SocketListen() {
        //socket.emit('room', room);

        socketio.on("change", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                final JSONObject data = (JSONObject) args[0];
                //Log.v("SOCKET SERVICE function socketListen STATUS",""+data);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        send_status(data);
                    }
                });
            }

        });

        socketio.on("location", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final JSONObject data = (JSONObject) args[0];
                //Log.v("SOCKET SERVICE function socketListen STATUS",""+data);
                runOnUiThread(new Runnable() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void run() {
                        Log.v("ESTADO LOCATION emit",""+data.toString());

                        try {
                            if (data.getString("distributor_id").equals(DISTRIBUIDOR_ID)){
                                send_location();
                            }else{
                                Log.v("ESTADO socketio.on('location')","Peticion de ubicacion, no es distribuidor asignado");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }

    private void send_status(final JSONObject data) {
        //Log.v("ESTADO send ------------->",""+type+": "+data.toString());

        //ejemplo de data
        //data = {order:1,title:"titulo_aqui",subtitle:"subtitulo_aqui"}
        //Contract.Entry.COLUMN_STATUS

        ContentValues tb_buy = new ContentValues();
        tb_buy.put(Contract.Entry.COLUMN_STATUS, data.toString());
        db.update(tb_buy);

        //OBtengo los datos del API
        getOrders(data);

    }

    private void getOrders(final JSONObject data) {

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        StringRequest s = new StringRequest(Request.Method.POST, getString(R.string.ORDERS), new Response.Listener<StringRequest.reply>() {

            @Override
            public void onResponse(StringRequest.reply result) {
                Log.v("RETORNO ->",""+ result.toString());

                //Actualizo la base de datos
                ContentValues tb = new ContentValues();
                tb.put(Contract.Entry.COLUMN_ORDERS,result.response);
                db.update(tb);

                //ENVIO LOS DATOS A LA APLICACION ABIERTA
                Intent intent = new Intent(IDENTIFY);
                intent.putExtra("status", result.response);
                sendBroadcast(intent);

                //NOtifico
                displayNotification(data);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("ESTADO VHTTP getOrders", "That didn't work!");
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                //params.put("distributor_id", DISTRIBUIDOR_ID);
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "ChupApp/1.0");
                return headers;
            }

        };

        queue.add(s);
    }

    protected void displayNotification(JSONObject data) {

        try {
            CharSequence title = data.getString("title");
            CharSequence subtitle = data.getString("subtitle");

            Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            //ESTO PARA PASARLE DATOS
            intent.putExtra(DATA, data.toString());

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationManager notification = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            //CharSequence ticker ="Nueva entrada en SekthDroid";

            Notification notify = new NotificationCompat.Builder(this)
                    .setContentIntent(pendingIntent)
                    //.setTicker(ticker)
                    .setContentTitle(title)
                    .setContentText(subtitle)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    //.addAction(R.mipmap.ic_launcher, ticker, pendingIntent)
                    .setVibrate(new long[]{200, 200, 100, 200, 200})
                    .setAutoCancel(Boolean.TRUE)
                    .setSound(soundUri)
                    .build();

            notification.notify(NOTIFICATION_ID, notify);


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        //new BootBroadcast().execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this,"Servicio Destruido",Toast.LENGTH_LONG).show();
    }


    /*
    public void bucleStart() {
        handler.postDelayed(runnable, 3000);
    }

    public void bucleRemove() {
        handler.removeCallbacks(runnable);
    }


    final Handler handler = new Handler();
    final Runnable runnable = new Runnable() {
        public void run() {

            table = db.read();
            try {
                if (table.getString(Contract.Entry.COLUMN_STATUS).equals("2")) {
                    //consulto gps
                    if (checkGPS()){
                        gps();
                    }else{
                        handler.postDelayed(runnable, 3000);
                    }

                    Toast.makeText(getApplicationContext(),"Status 2",Toast.LENGTH_SHORT).show();


                } else {

                    //termino bucle
                    Toast.makeText(getApplicationContext(),"Status diferente",Toast.LENGTH_SHORT).show();

                    //cierro la conexion
                    handler.removeCallbacks(runnable);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    };
    */

    private boolean checkGPS(){
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return GpsStatus;
    }

    //obtengo la ubicacion del usuario
    private void send_location() {
        locman = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { return; }
        locman.requestSingleUpdate(LocationManager.GPS_PROVIDER, loclis, null);
        //locman.requestLocationUpdates();
    }

    private void sendCoordinates(final String latitude, final String longitude){

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        StringRequest s = new StringRequest(Request.Method.POST, getString(R.string.EMIT_LOCATION), new Response.Listener<StringRequest.reply>() {

            @Override
            public void onResponse(StringRequest.reply result) {
                //ContentValues ords = new ContentValues();
                //ords.put(Contract.Entry.COLUMN_ORDERS,result.response);
                //table = db.update(ords);
                Log.v("RETORNO sendCoordinates("+latitude+", "+longitude+")",""+ result.toString());
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("ESTADO VHTTP", "That didn't work!");
            }
        }) {

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();
                params.put("latitude", latitude);
                params.put("longitude", longitude);
                params.put("distributor_id", DISTRIBUIDOR_ID);

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
    public void onLocationChanged(Location location) {
        Toast.makeText(getApplicationContext(),"SE OBTUVO TU UBICACIÓN",Toast.LENGTH_SHORT).show();
        sendCoordinates(String.valueOf(location.getLatitude()),String.valueOf(location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
