package name.peterbukhal.android.ntpservice;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "NtpService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new UpdateTime().execute();
    }

    private class UpdateTime extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String serverName = "ntp1.stratum1.ru";

            try {
                // Send request
                DatagramSocket socket = new DatagramSocket();
                InetAddress address = InetAddress.getByName(serverName);
                byte[] buf = new NtpMessage().toByteArray();
                DatagramPacket packet =
                        new DatagramPacket(buf, buf.length, address, 123);

                // Set the transmit timestamp *just* before sending the packet
                // ToDo: Does this actually improve performance or not?
                NtpMessage.encodeTimestamp(packet.getData(), 40,
                        (System.currentTimeMillis() / 1000.0) + 2208988800.0);

                socket.send(packet);

                // Get response
                System.out.println("NTP request sent, waiting for response...\n");
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                socket.close();

                // Immediately record the incoming timestamp
                double destinationTimestamp =
                        (System.currentTimeMillis() / 1000.0) + 2208988800.0;

                // Process response
                NtpMessage msg = new NtpMessage(packet.getData());

                // Corrected, according to RFC2030 errata
                double roundTripDelay = (destinationTimestamp - msg.originateTimestamp) -
                        (msg.transmitTimestamp - msg.receiveTimestamp);

                double localClockOffset =
                        ((msg.receiveTimestamp - msg.originateTimestamp) +
                                (msg.transmitTimestamp - destinationTimestamp)) / 2;

                Log.d(TAG, "NTP server: " + serverName);
                Log.d(TAG, msg.toString());
                Log.d(TAG, "Dest. timestamp:     " +
                        NtpMessage.timestampToString(destinationTimestamp));
                Log.d(TAG, "Round-trip delay: " +
                        new DecimalFormat("0.00").format(roundTripDelay*1000) + " ms");
                Log.d(TAG, "Local clock offset: " +
                        new DecimalFormat("0.00").format(localClockOffset*1000) + " ms");
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

    }

}
