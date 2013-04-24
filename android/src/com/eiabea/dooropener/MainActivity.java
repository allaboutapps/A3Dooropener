package com.eiabea.dooropener;

import java.lang.ref.WeakReference;
import java.util.Properties;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Represents the Main Screen of the DoorOpener App
 * 
 * @author eiabea
 *
 */

public class MainActivity extends SherlockActivity {
	
	public static final int CONNECTION_OK = 0;
	public static final int CONNECTION_FAILED = -1;

	public static final int STATUS_RED = 10;
	public static final int STATUS_ORANGE = 20;
	public static final int STATUS_GREEN = 30;
	
	private static final String TAG = "DoorOpener";
	private static final int SSH_TIMEOUT = 25000;
	
	private Button btnOpenDoor;
	private Button btnKillConnection;
	private FrameLayout frmStatus;
	private TextView txtStatus;
	
	private Session sshSession = null;
	private ChannelExec channel;
	
	private boolean appWasClosed = false;
	
	private mHandler handler = new mHandler(this);
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        
        initUI();
        
        setListeners();
        
        Intent intent = getIntent();
        
        // Call resolveIntent only when it has nothing to do with default Actions
        if(!intent.getAction().equals(Intent.ACTION_MAIN)){
        	appWasClosed = true;
        	resolveIntent(intent);
        }
        
    }

    @Override
	protected void onPause() {
		appWasClosed = false;
		super.onPause();
	}

	/**
     * Initializes the UI of the App
     */
    private void initUI() {
		btnOpenDoor = (Button) findViewById(R.id.btn_open_door);
		btnKillConnection = (Button) findViewById(R.id.btn_kill_connection);
		
		frmStatus = (FrameLayout) findViewById(R.id.frm_status);
		frmStatus.setBackgroundResource(R.drawable.shape_status_red);
		
		txtStatus = (TextView) findViewById(R.id.txt_status);
		txtStatus.setText("Not connected");
	}

    /**
     * Sets the Listeners to all the UI Elements
     */
	private void setListeners() {
		
		btnOpenDoor.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openDoor();
			}
		});

		btnKillConnection.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				killConnection();
			}
		});
	}
	
	/**
	 * Tries to open a SSH-Connection to a RaspberryPi and executes a Command to handle a Servo and a Led 
	 */
	private void openDoor(){
		showLoading(true);
    	new Thread(new Runnable() {
			
			@Override
			public void run() {
				Message msg = new Message();
				Message statusMsg = new Message();
		    	try {
		    		if(sshSession == null || !sshSession.isConnected()){
		    			statusMsg.arg1 = STATUS_ORANGE;
		    			handler.sendMessage(statusMsg);
		    			// Insert your parameters of your server
		    			String host = getApplicationContext().getResources().getString(R.string.host);
		    			String user = getApplicationContext().getResources().getString(R.string.user);
		    			String pwd = getApplicationContext().getResources().getString(R.string.pwd);
		    			int port = getApplicationContext().getResources().getInteger(R.integer.port);
		    			
		    			Properties config = new Properties();
		    			config.put("StrictHostKeyChecking", "no");
		    			config.put("compression.s2c", "zlib,none");
		    			config.put("compression.c2s", "zlib,none");
		    			
		    			JSch jsch=new JSch();  
		    			
		    			sshSession = jsch.getSession(user, host, port);
		    			
		    			sshSession.setConfig(config);
		    			sshSession.setPassword(pwd);
		    			sshSession.setTimeout(SSH_TIMEOUT);
		    			sshSession.connect();
		    			
		    			msg.arg1 = CONNECTION_OK;
		    		}
		    		
					channel = (ChannelExec) sshSession.openChannel("exec");
					// Set Command which should be executed by the Server e.g. "sudo php /open.php"
					channel.setCommand(getApplicationContext().getResources().getString(R.string.command));
					channel.setInputStream(null);
					channel.setErrStream(System.err);
					channel.connect();
		            
		    	} catch (JSchException e) {
		    		msg.arg1 = CONNECTION_FAILED;
		    		e.printStackTrace();
		    	} catch (Exception e) {
		    		msg.arg1 = CONNECTION_FAILED;
					e.printStackTrace();
				} 
		    	handler.sendMessage(msg);
		    	
		    	
			}
		}).start();
	}

	/**
	 * Kills the SSH-Connection between the App and the RaspberryPi
	 */
    private void killConnection(){
    	sshSession.disconnect();
    	btnKillConnection.setEnabled(false);
    	txtStatus.setText("Not connected");
    	frmStatus.setBackgroundResource(R.drawable.shape_status_red);
    }
    
    /**
     * Handles the Async Messages
     * 
     * @author eiabea
     *
     */
    static class mHandler extends Handler{
        WeakReference<MainActivity> mAct;

        mHandler(MainActivity aAct) {
            mAct = new WeakReference<MainActivity>(aAct);
        }
        
		@Override
		public void handleMessage(Message msg) {
			MainActivity theAct = mAct.get();
			switch (msg.arg1) {
			case CONNECTION_OK:
				Log.d(TAG, "Connected to Pi");
				theAct.txtStatus.setText("Connected to Pi");
				theAct.frmStatus.setBackgroundResource(R.drawable.shape_status_green);
				theAct.btnKillConnection.setEnabled(true);
				theAct.showLoading(false);
				if(theAct.appWasClosed){
					theAct.moveTaskToBack(true);
				}
				break;
				
			case STATUS_RED:
				
				break;
			case STATUS_ORANGE:
				theAct.txtStatus.setText("Connecting to Pi");
				theAct.frmStatus.setBackgroundResource(R.drawable.shape_status_orange);
				break;
			case STATUS_GREEN:
				
				break;

			default:
				Toast.makeText(theAct, "Connection failed", Toast.LENGTH_SHORT).show();
				theAct.txtStatus.setText("Not connected");
				theAct.frmStatus.setBackgroundResource(R.drawable.shape_status_red);
				Log.d(TAG, "Connection failed");
				theAct.showLoading(false);
				break;
			}
			super.handleMessage(msg);
		}

    }
    
    /**
     * Shows Indeterminate Loadingcircle
     * 
     * @param show true or false
     */
	private void showLoading(boolean show){
		if(show){
			btnOpenDoor.setEnabled(false);
			getSherlock().setProgressBarIndeterminateVisibility(true);
		}else{
			btnOpenDoor.setEnabled(true);
			getSherlock().setProgressBarIndeterminateVisibility(false);
		}
	}
	
	/**
	 * Gets called when the App is opened and a new NFC-Tag was found
	 */
    @Override
    public void onNewIntent(Intent intent) {
    	Log.i(TAG, "onNewIntent");
    	
    	setIntent(intent);
        
        resolveIntent(intent);
    }
    
    /**
     * Resolve the Intent which comes from a NFC-Tag
     * 
     * @param intent
     */
    void resolveIntent(Intent intent) {
        // Parse the intent
        String action = intent.getAction();
        Log.i("Ndef", "Action: " + action);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

        	Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            
            String ndefDataString = getNdefData(rawMsgs);
            
            // Open the Door only when the NFC-Tag contains the correct Data
            if(ndefDataString.equals(getResources().getString(R.string.nfc_data))){
            	openDoor();
            }
        	
        } else {
            Log.e("Ndef", "Unknown intent " + intent);
            finish();
            return;
        }
    }

    /**
     * Extracts the necessary Datastring from the NFC-Tag
     * 
     * @param rawMsgs Raw Data from the NFC-Tag
     * @return clean Datastring or empty String
     */
	private String getNdefData(Parcelable[] rawMsgs) {
		NdefMessage[] msgs;
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }
            if(msgs.length > 0){
            	NdefRecord[] records = msgs[0].getRecords();
            	
            	if(records.length > 0){
            		byte[] complete = records[0].getPayload();
            		byte[] data = new byte[complete.length];
            		
            		int offset = 3;
            		
            		for(int i = offset; i < complete.length; i++){
            			data[i - offset] = complete[i];
            		}
            		String output = new String(data);
            		return output.trim();
            	}
            }
        } else {
            // Unknown tag type
            byte[] empty = new byte[] {};
            NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
            NdefMessage msg = new NdefMessage(new NdefRecord[] {record});
            msgs = new NdefMessage[] {msg};
        }
        return "";
	}
    
}
