package com.devioussiddy.amkamagclient.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.devioussiddy.amkamagclient.MyCustomAdapter;
import com.devioussiddy.amkamagclient.R;
import com.devioussiddy.amkamagclient.TCPClient;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.ArrayList;
import java.util.List;

public class MyActivity extends Activity {
    private static final String TAG = MyActivity.class.getSimpleName();
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastText;
    private ListView mList;
    private ArrayList<String> arrayList;
    private MyCustomAdapter mAdapter;
    private TCPClient mTcpClient;
    private String[] msgPacket = new String[]{"", "in", "Amkamagwerker", "", "0"}; // 0 = ID#, 1 = in(0)/out(1), 2 = personeel, 3 = klant, 4 = mode (0 = normal, 2 = request info list, 3 = Correction,  4 = close connection)
    private Button scanBtn;
    private Button send;
    private Button ipConfirm;
    private RadioButton radioButIn;
    private TextView formatTxt, contentTxt;
    private TextView restext;

    private EditText editText;
    private EditText editTextIP;
    private AsyncTask connection;
    private final int WRITE_MODE = 1001;
    private final int SCAN_MODE = 1003;
    private final int CLOSE_MODE = 404;
    public  String SERVERIP = "192.168.1.9"; //your computer IP address
    public  final int SERVERPORT = 4444;
    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if(result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }

            lastText = result.getText();
            restext.setText(lastText);
            barcodeView.setStatusText(result.getText());
            beepManager.playBeepSoundAndVibrate();

            //Added preview of scanned barcode
            ImageView imageView = (ImageView) findViewById(R.id.barcodePreview);
            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arrayList = new ArrayList<String>();
        restext = (TextView) findViewById(R.id.result_text);
        editText = (EditText) findViewById(R.id.editText);
        editTextIP = (EditText) findViewById(R.id.editTextIP);
        send = (Button) findViewById(R.id.send_button);
        ipConfirm = (Button) findViewById(R.id.confirm_button);
        scanBtn = (Button) findViewById(R.id.scan_button);
        radioButIn = (RadioButton) findViewById(R.id.radioButtonIn);
        barcodeView = (DecoratedBarcodeView) findViewById(R.id.barcode_scanner);
        //barcodeView.decodeContinuous(callback);
        beepManager = new BeepManager(this);

        //relate the listView from java to the one created in xml
        mList = (ListView) findViewById(R.id.list);
        mAdapter = new MyCustomAdapter(this, arrayList);
        mList.setAdapter(mAdapter);


        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                restext.setText(lastText);

                //sendInfo(WRITE_MODE);

            }
        });


        ipConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = editTextIP.getText().toString();

                //add the text in the arrayList
                arrayList.add("SERVERIP SET: " + message);
                if (connection !=null) {
                    if (!connection.isCancelled()) {
                        sendInfo(CLOSE_MODE);
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        connection.cancel(false); //TODO
                        mTcpClient.stopClient();

                    }
                    //set new IP
                    if (mTcpClient != null) {

                        mTcpClient.setIP(message);
                    }

                }

                SERVERIP = message;
                connection = new connectTask().execute("");
                // connect to the server
               // new connectTask().execute("");
                //add the text in the arrayList
                arrayList.add("New IP set");
                //refresh the list
                mAdapter.notifyDataSetChanged();
//                editTextIP.setText("");
            }
        });
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(MyActivity.this);
                integrator.initiateScan();
            }
            });

    }

//    @Override
//    public void onClick(View v) {
//        if (v.getId() == R.id.scan_button) {
//            IntentIntegrator scanIntegrator = new IntentIntegrator(getActivity());
//
//            scanIntegrator.initiateScan();
//            //add the text in the arrayList
//            arrayList.add("Scan initiated...");
//            mAdapter.notifyDataSetChanged();
//        }
//    }
@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
         String scanContent = scanningResult.getContents().toString();
//            String scanFormat = scanningResult.getFormatName();
            if (scanContent != null){
                msgPacket[0]= scanContent;
                sendInfo(SCAN_MODE);
            }
            else {
                //add the text in the arrayList
                arrayList.add("Content was empty");
                mAdapter.notifyDataSetChanged();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    @Override
    public void onDestroy() {
super.onDestroy();
        sendInfo(CLOSE_MODE);
    }
    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
    }

    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }

    public void triggerScan(View view) {        barcodeView.decodeSingle(callback);    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    public class connectTask extends AsyncTask<String, String, TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {

    //we create a TCPClient object and
    mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
        @Override
        //here the messageReceived method is implemented
        public void messageReceived(String message) {
            //this method calls the onProgressUpdate
            publishProgress(message);
        }
    });
    mTcpClient.run(SERVERIP);

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
//if (values[0].contains("|in|")||values[0].contains("|out|")){


            //in the arrayList we add the messaged received from server
            arrayList.add(values[0]);
    //arrayList.add("Received.");

            // notify the adapter that the data set has changed. This means that new message received
            // from server was added to the list
            mAdapter.notifyDataSetChanged();
//}
        }

    }

    public void sendInfo(int sentMode){

        if (mTcpClient == null) {
            // connect to the server
            connection = new connectTask().execute("");
        }
        else if (!mTcpClient.connected()){
            // connect to the server
            connection = new connectTask().execute("");
        }
        else if (!connection.isCancelled()){
            // connect to the server
            connection = new connectTask().execute("");
        }
        String message = editText.getText().toString();
        String inout = radioButIn.isChecked() ? "in" : "out";
        switch (sentMode) {
            case WRITE_MODE:
                msgPacket[0] = message;
                msgPacket[1] = inout;
                //add the text in the arrayList
                arrayList.add("c: " + msgPacket[0] + " " + inout);
                break;
            case SCAN_MODE:
                msgPacket[1] = inout;
                //add the text in the arrayList
                arrayList.add("c: " + msgPacket[0] + " " + inout);
                break;
            case CLOSE_MODE:
                msgPacket[4] = "4";
                //add the text in the arrayList
                arrayList.add("Closing session");
            break;
            default:
                msgPacket = new String[]{"", "in", "Amkamagwerker", "", "0"};
                break;

        }


        message = TextUtils.join("|", msgPacket);

        //sends the message to the server
        if (mTcpClient != null) {
            mTcpClient.sendMessage(message);
        }
        msgPacket = new String[]{"", "in", "Amkamagwerker", "", "0"};

        //refresh the list
        mAdapter.notifyDataSetChanged();

    }
}