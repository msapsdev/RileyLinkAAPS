package com.gxwtech.roundtrip2;

import android.app.Application;
import android.content.res.Resources;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Tim on 15/06/2016.
 */
public class MainApp extends Application {

    private static MainApp sInstance;
    private static ServiceClientConnection serviceClientConnection;
    public static Resources sResources;


    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
        serviceClientConnection = new ServiceClientConnection();

        // initialize Realm
        Realm.init(instance());
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder() //
            .name("rt2.realm") //
            .schemaVersion(0) //
            .deleteRealmIfMigrationNeeded() // TODO: 03/08/2016 @TIM remove
            .build();
        Realm.setDefaultConfiguration(realmConfiguration);

        sResources = getResources();

        // TODO remove this code. Configuration of Frequency is now in UI
        // you need to set where pump comes from (because US pumps have different freq.
        // that Worldwide pumps). You need this only once.

        // boolean isUSPump = false;
        //
        // if (isUSPump)
        // SP.putString(MedtronicConst.Prefs.PumpFrequency, gs(R.string.medtronic_pump_frequency_us));
        // else
        // SP.putString(MedtronicConst.Prefs.PumpFrequency, gs(R.string.medtronic_pump_frequency_worldwide));

        // SP.putString(MedtronicConst.Prefs.RileyLinkAddress, "CD:72:E1:4C:D5:9D");
        // SP.remove(RileyLinkConst.Prefs.RileyLinkAddress);

        // TODO: If you used RileyLinkAPS before 23/10/2018 you will need to enable this part of code
        // (you need it just once, then comment back)
        // float lastGoodFrequency = SP.getFloat(RileyLinkConst.Prefs.LastGoodDeviceFrequency, 0.0f);
        // double lastGoodFrequency2 = lastGoodFrequency;
        // lastGoodFrequency2 = Math.round(lastGoodFrequency2 * 1000d) / 1000d;
        // SP.remove(RileyLinkConst.Prefs.LastGoodDeviceFrequency);
        // SP.putDouble(RileyLinkConst.Prefs.LastGoodDeviceFrequency, lastGoodFrequency2);

    }


    public static MainApp instance() {
        return sInstance;
    }


    public static ServiceClientConnection getServiceClientConnection() {
        if (serviceClientConnection == null) {
            serviceClientConnection = new ServiceClientConnection();
        }
        return serviceClientConnection;
    }


    // TODO: 09/07/2016 @TIM uncomment ServiceClientConnection once class is added

    public static String gs(int id) {
        return sResources.getString(id);
    }

}
