package com.chupapp.distributor.service;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.widget.Toast;

/**
 * Created by inmobitec on 15/11/17.
 */

public class BootBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context,"PASO ON RECEIVE BootBroadcast",Toast.LENGTH_LONG).show();
        context.startService(new Intent(context, Serving.class));
        /*
        //lanza servicio
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)){
            Intent serviceIntent = new Intent(context, Serving.class);
            context.startService(serviceIntent);
            Toast.makeText(context,"PASO ON RECEIVE ",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(context,"BOOT NO COMPLETE ",Toast.LENGTH_LONG).show();
        }
        */
    }

}
