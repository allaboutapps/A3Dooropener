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
	
	private static final String TAG = "DoorOpener";
	private static final int SSH_TIMEOUT = 25000;
	
	private Button btnOpenDoor;
	private Button btnKillConnection;
	private TextView txtStatus;
	
	private Session sshSession = null;
	private ChannelExec channel;
	
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
        	resolveIntent(intent);
        }
        
    }

    /**
     * Initializes the UI of the App
     */
    private void initUI() {
		btnOpenDoor = (Button) findViewById(R.id.btn_open_door);
		btnKillConnection = (Button) findViewById(R.id.btn_kill_connection);
		
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
		    	try {
		    		if(sshSession == null || !sshSession.isConnected()){
		    			txtStatus.setText("Connecting to Pi");
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
					txtStatus.setText("Running Script on Pi");
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
				theAct.btnKillConnection.setEnabled(true);
				break;

			default:
				Toast.makeText(theAct, "Connection failed", Toast.LENGTH_SHORT).show();
				theAct.txtStatus.setText("Not connected");
				Log.d(TAG, "Connection failed");
				break;
			}
			theAct.showLoading(false);
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
			getSherlock().setProgressBarIndeterminateVisibility(true);
		}else{
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
