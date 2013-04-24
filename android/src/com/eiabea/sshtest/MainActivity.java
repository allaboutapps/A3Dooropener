package com.eiabea.sshtest;

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
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class MainActivity extends SherlockActivity {
	
	public static final int CONNECTION_OK = 0;
	public static final int CONNECTION_FAILED = -1;
	
	private static final String TAG = "DoorOpener";
	
	private Button btnOpenDoor;
	private Button btnKillConnection;
	
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
        
        if(!intent.getAction().equals(Intent.ACTION_MAIN)){
        	resolveIntent(intent);
        }
        
    }

    private void initUI() {
		btnOpenDoor = (Button) findViewById(R.id.btn_open_door);
		btnKillConnection = (Button) findViewById(R.id.btn_kill_connection);
	}

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
	
	private void openDoor(){
		showLoading(true);
    	new Thread(new Runnable() {
			
			@Override
			public void run() {
				Message msg = new Message();
		    	try {
		    		if(sshSession == null || !sshSession.isConnected()){
		    			
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
		    			sshSession.setTimeout(25000);
		    			sshSession.connect();
		    			
		    			msg.arg1 = CONNECTION_OK;
		    		}
		    		
					channel = (ChannelExec) sshSession.openChannel("exec");
					// Insert your command to execute the open.php script on your raspberry
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

    private void killConnection(){
    	sshSession.disconnect();
    	btnKillConnection.setEnabled(false);
    }
    
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
				Log.d("SSH", "Connected to Pi");
				theAct.btnKillConnection.setEnabled(true);
				break;

			default:
				Toast.makeText(theAct, "Connection failed", Toast.LENGTH_SHORT).show();
				Log.d("SSH", "Connection failed");
				break;
			}
			theAct.showLoading(false);
			super.handleMessage(msg);
		}

    }
    
	private void showLoading(boolean show){
		if(show){
			getSherlock().setProgressBarIndeterminateVisibility(true);
		}else{
			getSherlock().setProgressBarIndeterminateVisibility(false);
		}
	}
	
    @Override
    public void onNewIntent(Intent intent) {
    	Log.i(TAG, "onNewIntent");
    	
    	setIntent(intent);
        
        resolveIntent(intent);
    }
    
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
