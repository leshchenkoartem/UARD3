package com.leshchenko.uard3;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "UARD~";
    UsbManager mUsbManager;
    TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Button button = findViewById(R.id.button);
        text = findViewById(R.id.text);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ReadAsync(new CallbackResult() {
                    @Override
                    public void pmResult(PMData data) {
                        if(data!=null)
                            text.setText("pm5="+data.getPm5()+"\n"+"pm10="+data.getPm10());
                    }
                }).execute(mUsbManager);
            }
        });
    }



}

class ReadAsync extends AsyncTask<UsbManager, Object, PMData> {
    CallbackResult result;

    public ReadAsync(CallbackResult result) {
        this.result = result;
    }


    @Override
    protected PMData doInBackground(UsbManager... usbManagers) {

        UsbSerialPort port = null;

        try {


            for(int y=0; y < 1000000; y++) {

                UsbManager mUsbManager = usbManagers[0];
                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                if (availableDrivers.isEmpty()) {
                    return null;
                }
                UsbSerialDriver driver = availableDrivers.get(0);
                UsbDeviceConnection connection = mUsbManager.openDevice(driver.getDevice());
                if (connection == null) {
                    return null;
                }

                port = driver.getPorts().get(0);
                port.open(connection);
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                byte buffer[] = new byte[16];

                int numBytesRead = port.read(buffer, 1000);
                port.close();
                if(numBytesRead == 10) {
                    int check = ((Byte)buffer[8]).intValue();
                    int pm5h = ((Byte)buffer[3]).intValue();
                    int pm5l = ((Byte)buffer[2]).intValue();
                    int pm10l = ((Byte)buffer[4]).intValue();
                    int pm10h = ((Byte)buffer[5]).intValue();
                    int res1 = ((Byte)buffer[6]).intValue();
                    int res2 = ((Byte)buffer[7]).intValue();

                    if(check == res1+res2+pm5h+pm5l+pm10h+pm10l){
                        int pm5 = ((pm5h*256)+pm5l/10);
                        int pm10 = ((pm10h*256)+pm10l/10);
                        pm5 = pm5<0 ? 0 : pm5;
                        pm10 = pm10<0 ? 0 : pm10;

                        PMData pmData = new PMData();
                        pmData.setPm5(pm5);
                        pmData.setPm10(pm10);

                        return pmData;
                    }

                }
            }
        } catch (IOException e) {
            // Deal with error.
        } finally {
            try {
                if(port!=null)port.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(PMData pmData) {
        super.onPostExecute(pmData);
        result.pmResult(pmData);
    }
}
