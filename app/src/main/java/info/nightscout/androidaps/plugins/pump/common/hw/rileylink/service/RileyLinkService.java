package info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.gxwtech.roundtrip2.RT2Const;
import com.gxwtech.roundtrip2.RoundtripService.RileyLinkIPCConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkCommunicationManager;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkConst;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkUtil;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.IRFSpy;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.IRileyLinkBLE;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.RileyLinkEncodingType;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.RileyLinkFirmwareVersion;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.RileyLinkTargetFrequency;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.defs.RileyLinkError;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.defs.RileyLinkServiceState;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.defs.RileyLinkTargetDevice;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.data.ServiceNotification;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.data.ServiceResult;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.data.ServiceTransport;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.tasks.DiscoverGattServicesTask;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.tasks.InitializePumpManagerTask;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.tasks.ServiceTask;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.tasks.ServiceTaskExecutor;
import info.nightscout.utils.SP;

import static info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkUtil.getRileyLinkCommunicationManager;


/**
 * Created by andy on 5/6/18.
 * Split from original file and renamed.
 */
public abstract class RileyLinkService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(RileyLinkService.class);


    protected BluetoothAdapter bluetoothAdapter;

    // Our hardware/software connection
    public IRileyLinkBLE rileyLinkBLE; // android-bluetooth management
    protected IRFSpy rfspy; // interface for RL xxx Mhz radio.
    //protected boolean needBluetoothPermission = true;
    protected RileyLinkIPCConnection rileyLinkIPCConnection;
    protected Context context;
    //public RileyLinkCommunicationManager pumpCommunicationManager;
    protected BroadcastReceiver mBroadcastReceiver;

    protected RileyLinkServiceData rileyLinkServiceData;
    protected RileyLinkTargetFrequency rileyLinkTargetFrequency;

    protected static final String WAKELOCKNAME = "com.gxwtech.roundtrip2.RoundtripServiceWakeLock";
    protected static volatile PowerManager.WakeLock lockStatic = null;


    public RileyLinkService(Context context) {
        super();
        this.context = context;
        RileyLinkUtil.setContext(this.context);
        determineRileyLinkTargetFrequency();
        RileyLinkUtil.setRileyLinkTargetFrequency(rileyLinkTargetFrequency);
        RileyLinkUtil.setEncoding(getEncoding());
        initRileyLinkServiceData();
    }


    public abstract RileyLinkEncodingType getEncoding();


    /**
     * You need to determine which frequencies RileyLink will use, and set rileyLinkTargetFrequency
     */
    protected abstract void determineRileyLinkTargetFrequency();


    /**
     * If you have customized RileyLinkServiceData you need to override this
     */
    public abstract void initRileyLinkServiceData();


    @Override
    public boolean onUnbind(Intent intent) {
        LOG.warn("onUnbind");
        return super.onUnbind(intent);
    }


    @Override
    public void onRebind(Intent intent) {
        LOG.warn("onRebind");
        super.onRebind(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.error("I die! I die!");

        // FIXME this might not work
        if (rileyLinkBLE != null) {
            rileyLinkBLE.disconnect(); // dispose of Gatt (disconnect and close)
            rileyLinkBLE = null;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        LOG.debug("onCreate");

        rileyLinkIPCConnection = new RileyLinkIPCConnection(context); // TODO We might be able to remove this -- Andy
        RileyLinkUtil.setRileyLinkIPCConnection(rileyLinkIPCConnection);


        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            /* here we can listen for local broadcasts, then send ourselves
             * a specific intent to deal with them, if we wish
              */
                if (intent == null) {
                    LOG.error("onReceive: received null intent");
                } else {
                    String action = intent.getAction();
                    if (action == null) {
                        LOG.error("onReceive: null action");
                    } else {

                        if (action.equals(RileyLinkConst.Intents.BluetoothConnected)) {
                            //LOG.warn("serviceLocal.bluetooth_connected");
                            rileyLinkIPCConnection.sendNotification(new ServiceNotification(RT2Const.IPC.MSG_note_FindingRileyLink), null);
                            ServiceTaskExecutor.startTask(new DiscoverGattServicesTask());
                        } else if (action.equals(RileyLinkConst.Intents.RileyLinkDisconnected)) {
                            if (bluetoothAdapter.isEnabled()) {
                                RileyLinkUtil.setServiceState(RileyLinkServiceState.BluetoothReady, RileyLinkError.RileyLinkUnreachable);
                            } else {
                                RileyLinkUtil.setServiceState(RileyLinkServiceState.BluetoothError, RileyLinkError.BluetoothDisabled);
                            }

                        }  else if (action.equals(RileyLinkConst.Intents.RileyLinkReady)) {
                            LOG.warn("MedtronicConst.Intents.RileyLinkReady");
                            // FIXME
                            rileyLinkIPCConnection.sendNotification(new ServiceNotification(RT2Const.IPC.MSG_note_WakingPump), null);
                            rileyLinkBLE.enableNotifications();
                            rfspy.startReader(); // call startReader from outside?

                            rfspy.initializeRileyLink();
                            String bleVersion = rfspy.getBLEVersionCached();
                            RileyLinkFirmwareVersion rlVersion = rfspy.getRLVersionCached();

                            LOG.debug("RfSpy version (BLE113): " + bleVersion);
                            rileyLinkServiceData.versionBLE113 = bleVersion;

                            LOG.debug("RfSpy Radio version (CC110): " + rlVersion.name());
                            rileyLinkServiceData.versionCC110 = rlVersion;

                            ServiceTask task = new InitializePumpManagerTask();
                            ServiceTaskExecutor.startTask(task);
                            LOG.info("Announcing RileyLink open For business");
                        } else if (action.equals(RileyLinkConst.Intents.BluetoothReconnected)) {
                            LOG.debug("Reconnecting Bluetooth");
                            rileyLinkIPCConnection.sendNotification(new ServiceNotification(RT2Const.IPC.MSG_note_FindingRileyLink), null);
                            bluetoothInit();
                            ServiceTaskExecutor.startTask(new DiscoverGattServicesTask(true));
                        } else if (action.equals(RT2Const.serviceLocal.ipcBound)) {
                            // If we still need permission for bluetooth, ask now.
                            // FIXME removed Andy - doesn't do anything
                            //                            if (needBluetoothPermission) {
                            //                                sendBLERequestForAccess();
                            //                            }

                        } /*else if (RT2Const.IPC.MSG_BLE_accessGranted.equals(action)) {
                            //initializeLeAdapter();
                            //bluetoothInit();
                        } else if (RT2Const.IPC.MSG_BLE_accessDenied.equals(action)) {
                            LOG.error("BLE_Access_Denied recived. Stoping the service.");
                            stopSelf(); // This will stop the service.
                        } */ else if (action.equals(RT2Const.IPC.MSG_PUMP_tunePump)) {
                            if (getRileyLinkTargetDevice().isTuneUpEnabled()) {
                                doTuneUpDevice();
                            }
                        } else if (action.equals(RT2Const.IPC.MSG_PUMP_quickTune)) {
                            if (getRileyLinkTargetDevice().isTuneUpEnabled()) {
                                doTuneUpDevice();
                            }
                        } else if (action.startsWith("MSG_PUMP_")) {
                            handlePumpSpecificIntents(intent);
                        } else if (RT2Const.IPC.MSG_ServiceCommand.equals(action)) {
                            handleIncomingServiceTransport(intent);
                        } else if (RT2Const.serviceLocal.INTENT_sessionCompleted.equals(action)) {
                            Bundle bundle = intent.getBundleExtra(RT2Const.IPC.bundleKey);
                            if (bundle != null) {
                                ServiceTransport transport = new ServiceTransport(bundle);
                                rileyLinkIPCConnection.sendTransport(transport, transport.getSenderHashcode());
                            } else {
                                LOG.error("sessionCompleted: no bundle!");
                            }
                        } else {
                            LOG.error("Unhandled broadcast: action=" + action);
                        }
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RileyLinkConst.Intents.BluetoothConnected);
        intentFilter.addAction(RileyLinkConst.Intents.BluetoothDisconnected);
        intentFilter.addAction(RileyLinkConst.Intents.RileyLinkReady);
        intentFilter.addAction(RileyLinkConst.Intents.RileyLinkDisconnected);
        intentFilter.addAction(RileyLinkConst.Intents.BluetoothReconnected);
        intentFilter.addAction(RT2Const.serviceLocal.ipcBound);
        //intentFilter.addAction(RT2Const.IPC.MSG_BLE_accessGranted);
        //intentFilter.addAction(RT2Const.IPC.MSG_BLE_accessDenied);
        //intentFilter.addAction(RT2Const.IPC.MSG_BLE_useThisDevice);
        intentFilter.addAction(RT2Const.IPC.MSG_PUMP_tunePump);
        //intentFilter.addAction(RT2Const.IPC.MSG_PUMP_useThisAddress);
        intentFilter.addAction(RT2Const.IPC.MSG_ServiceCommand);
        intentFilter.addAction(RT2Const.serviceLocal.INTENT_sessionCompleted);

        addPumpSpecificIntents(intentFilter);

        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver, intentFilter);

        LOG.debug("onCreate(): It's ALIVE!");
    }

    public abstract RileyLinkCommunicationManager getDeviceCommunicationManager();

    public abstract void addPumpSpecificIntents(IntentFilter intentFilter);

    public abstract void handlePumpSpecificIntents(Intent intent);

    public abstract void handleIncomingServiceTransport(Intent intent);


    // Here is where the wake-lock begins:
    // We've received a service startCommand, we grab the lock.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.debug("onStartCommand");
        if (intent != null) {
            PowerManager.WakeLock lock = getLock(this.getApplicationContext());

            if (!lock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
                lock.acquire();
            }

            // This will end up running onHandleIntent
            super.onStartCommand(intent, flags, startId);
        } else {
            LOG.error("Received null intent?");
        }

        RileyLinkUtil.setContext(getApplicationContext());

        bluetoothInit();

        return (START_REDELIVER_INTENT | START_STICKY);
    }


    private boolean bluetoothInit() {
        LOG.debug("bluetoothInit: attempting to get an adapter");
        RileyLinkUtil.setServiceState(RileyLinkServiceState.BluetoothInitializing);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            LOG.error("Unable to obtain a BluetoothAdapter.");
            RileyLinkUtil.setServiceState(RileyLinkServiceState.BluetoothError, RileyLinkError.UnableToObtainBluetoothAdapter);
        } else {

            if (!bluetoothAdapter.isEnabled()) {

                sendBLERequestForAccess();

                LOG.error("Bluetooth is not enabled.");
                RileyLinkUtil.setServiceState(RileyLinkServiceState.BluetoothError, RileyLinkError.BluetoothDisabled);
            } else {
                RileyLinkUtil.setServiceState(RileyLinkServiceState.BluetoothReady);
                return true;
            }
        }

        return false;
    }


    // returns true if our Rileylink configuration changed
    public boolean reconfigureRileylink(String deviceAddress) {

        RileyLinkUtil.setServiceState(RileyLinkServiceState.RileyLinkInitializing);

        if (rileyLinkBLE.isConnected()) {
            if (deviceAddress.equals(rileyLinkServiceData.rileylinkAddress)) {
                LOG.info("No change to RL address.  Not reconnecting.");
                return false;
            } else {
                LOG.warn("Disconnecting from old RL (" + rileyLinkServiceData.rileylinkAddress + "), reconnecting to new: " + deviceAddress);
                rileyLinkBLE.disconnect();
                // prolly need to shut down listening thread too?
                //SP.putString(MedtronicConst.Prefs.RileyLinkAddress, deviceAddress);

                rileyLinkServiceData.rileylinkAddress = deviceAddress;
                rileyLinkBLE.findRileyLink(rileyLinkServiceData.rileylinkAddress);
                return true;
            }
        } else {
            Toast.makeText(context, "Using RL " + deviceAddress, Toast.LENGTH_SHORT).show();
            LOG.debug("handleIPCMessage: Using RL " + deviceAddress);

            if (RileyLinkUtil.getServiceState() == RileyLinkServiceState.NotStarted) {
                if (!bluetoothInit()) {
                    LOG.error("RileyLink can't get activated, Bluetooth is not functioning correctly. {}", RileyLinkUtil.getError().name());
                    return false;
                }
            }

            rileyLinkBLE.findRileyLink(deviceAddress);

            return true;
        }
    }


    public synchronized static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCKNAME);
            lockStatic.setReferenceCounted(true);
        }

        return lockStatic;
    }


    public void sendServiceTransportResponse(ServiceTransport transport, ServiceResult serviceResult) {
        // get the key (hashcode) of the client who requested this
        Integer clientHashcode = transport.getSenderHashcode();
        // make a new bundle to send as the message data
        transport.setServiceResult(serviceResult);
        // FIXME
        transport.setTransportType(RT2Const.IPC.MSG_ServiceResult);
        rileyLinkIPCConnection.sendTransport(transport, clientHashcode);
    }


    public boolean sendNotification(ServiceNotification notification, Integer clientHashcode) {
        return rileyLinkIPCConnection.sendNotification(notification, clientHashcode);
    }


    protected void sendBLERequestForAccess() {
        // FIXME
        //serviceConnection.sendMessage(RT2Const.IPC.MSG_BLE_requestAccess);

        //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }


    // FIXME: This needs to be run in a session so that is interruptable, has a separate thread, etc.
    public void doTuneUpDevice() {

        RileyLinkUtil.setServiceState(RileyLinkServiceState.TuneUpDevice);

        double lastGoodFrequency = 0.0d;

        if (rileyLinkServiceData.lastGoodFrequency == null) {
            lastGoodFrequency = SP.getFloat(RileyLinkConst.Prefs.LastGoodDeviceFrequency, 0.0f);
        } else {
            lastGoodFrequency = rileyLinkServiceData.lastGoodFrequency;
        }

        double newFrequency;
        if ((lastGoodFrequency > 0.0d) && getRileyLinkCommunicationManager().isValidFrequency(lastGoodFrequency)) {
            LOG.info("Checking for pump near last saved frequency of {}MHz", lastGoodFrequency);
            // we have an old frequency, so let's start there.
            newFrequency = getDeviceCommunicationManager().quickTuneForPump(lastGoodFrequency);
            if (newFrequency == 0.0) {
                // quick scan failed to find pump.  Try full scan
                LOG.warn("Failed to find pump near last saved frequency, doing full scan");
                newFrequency = getDeviceCommunicationManager().tuneForDevice();
            }
        } else {
            LOG.warn("No saved frequency for pump, doing full scan.");
            // we don't have a saved frequency, so do the full scan.
            newFrequency = getDeviceCommunicationManager().tuneForDevice();
        }

        if ((newFrequency != 0.0) && (newFrequency != lastGoodFrequency)) {
            LOG.info("Saving new pump frequency of {}MHz", newFrequency);
            SP.putFloat(RileyLinkConst.Prefs.LastGoodDeviceFrequency, (float) newFrequency);
            rileyLinkServiceData.lastGoodFrequency = newFrequency;
            rileyLinkServiceData.tuneUpDone = true;
            rileyLinkServiceData.lastTuneUpTime = System.currentTimeMillis();
        }

        if (newFrequency == 0.0d) {
            // error tuning pump, pump not present ??
            RileyLinkUtil.setServiceState(RileyLinkServiceState.PumpConnectorError, RileyLinkError.TuneUpOfDeviceFailed);
        }
    }


    public void disconnectRileyLink() {
        this.rileyLinkBLE.disconnect();
    }



    public RileyLinkTargetDevice getRileyLinkTargetDevice()
    {
        return this.rileyLinkServiceData.targetDevice;
    }
}
