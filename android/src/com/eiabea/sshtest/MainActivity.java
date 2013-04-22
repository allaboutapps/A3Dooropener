package com.eiabea.sshtest;

import java.lang.ref.WeakReference;
import java.util.Properties;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class MainActivity extends SherlockActivity {
	
	public static final int CONNECTION_OK = 0;
	public static final int CONNECTION_FAILED = -1;
	
	private Button btnConnectToPi;
	private Button btnOpenDoor;
	private Button btnKillConnection;
	
	private Session sshSession;
	private ChannelExec channel;
	
	private mHandler handler = new mHandler(this);
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        
        initUI();
        
        setListeners();
        
    }

    private void initUI() {
    	btnConnectToPi = (Button) findViewById(R.id.btn_connect_to_pi);
		btnOpenDoor = (Button) findViewById(R.id.btn_open_door);
		btnKillConnection = (Button) findViewById(R.id.btn_kill_connection);
	}

	private void setListeners() {
		
		btnConnectToPi.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sshConnect();
			}
		});
		
		btnOpenDoor.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sendDoorOpenCommand();
			}
		});

		btnKillConnection.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				killConnection();
			}
		});
	}

	/** Called when the user clicks the send button */
    public void sshConnect() {
    	showLoading(true);
    	new Thread(new Runnable() {
			
			@Override
			public void run() {
				Message msg = new Message();
		    	try {
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
    public void sendDoorOpenCommand() { 
    	showLoading(true);
    	Message msg = new Message();
    			try{
					channel = (ChannelExec) sshSession.openChannel("exec");
					// Insert your command to execute the open.php script on your raspberry
					channel.setCommand(getApplicationContext().getResources().getString(R.string.command));
					channel.setInputStream(null);
					channel.setErrStream(System.err);
					channel.connect();
					
					msg.arg1 = CONNECTION_OK;
					
					handler.sendMessage(msg);
				} catch (JSchException e) {
					e.printStackTrace();
				}

    }
    
    private void killConnection(){
    	sshSession.disconnect();
    	btnConnectToPi.setEnabled(true);
    	btnOpenDoor.setEnabled(false);
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
				theAct.btnConnectToPi.setEnabled(false);
				theAct.btnOpenDoor.setEnabled(true);
				theAct.btnKillConnection.setEnabled(true);
				break;

			default:
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
}
