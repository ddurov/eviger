package com.ddprojects.messager.service;

import static com.ddprojects.messager.service.api.APIRequester.setupApiClient;
import static com.ddprojects.messager.service.fakeContext.APIEndPoints;
import static com.ddprojects.messager.service.fakeContext.PDDEditor;
import static com.ddprojects.messager.service.fakeContext.liveData;
import static com.ddprojects.messager.service.fakeContext.persistentDataOnDisk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ddprojects.messager.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

import okhttp3.HttpUrl;

public class globals {
    public static boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager)
                fakeContext.getInstance().getApplicationContext().
                        getSystemService(Context.CONNECTIVITY_SERVICE);
        Network net = cm.getActiveNetwork();
        if (net == null) return false;
        NetworkCapabilities actNet = cm.getNetworkCapabilities(net);
        return actNet != null &&
                (
                        actNet.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                || actNet.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                                || actNet.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                                || actNet.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                );
    }

    public static void showToastMessage(String text, boolean shortDuration) {
        fakeContext.getMainThreadHandler().post(() -> Toast.makeText(
                fakeContext.getInstance().getApplicationContext(),
                text,
                shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG
        ).show());
    }

    public static String convertTimestampToHuman(long timestamp, String pattern) {
        return new SimpleDateFormat(
                pattern,
                Locale.getDefault()
        ).format(new Date(timestamp * 1000L));
    }

    public static void writeKeyPairsToSP(Object[][] keyPairs) {
        for (Object[] keyPair : keyPairs) {
            if (keyPair[1] instanceof String) {
                PDDEditor.putString((String) keyPair[0], (String) keyPair[1]);
            } else if (keyPair[1] instanceof Boolean) {
                PDDEditor.putBoolean((String) keyPair[0], (Boolean) keyPair[1]);
            } else if (keyPair[1] instanceof Integer) {
                PDDEditor.putInt((String) keyPair[0], (Integer) keyPair[1]);
            } else if (keyPair[1] instanceof Long) {
                PDDEditor.putLong((String) keyPair[0], (Long) keyPair[1]);
            } else if (keyPair[1] instanceof Float) {
                PDDEditor.putFloat((String) keyPair[0], (Float) keyPair[1]);
            }
        }
        PDDEditor.commit();
    }

    public static void writeKeyPairToSP(String key, Object value) {
        if (value instanceof String) {
            PDDEditor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            PDDEditor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            PDDEditor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            PDDEditor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            PDDEditor.putFloat(key, (Float) value);
        }
        PDDEditor.commit();
    }

    public static void removeKeysFromSP(String[] keys) {
        for (String key : keys) {
            PDDEditor.remove(key);
        }
        PDDEditor.commit();
    }

    public static void writeErrorInLog(Exception ex) {
        _createLogFile(ex.getMessage(), _stackTraceToString(ex));
    }

    public static void writeErrorInLog(Exception ex, String additionalInfo) {
        _createLogFile(
                ex.getMessage() + "\nAdditional info: " + additionalInfo,
                _stackTraceToString(ex)
        );
    }

    public static void writeMessageInLogCat(Object message) {
        Log.d("ddMessager", String.valueOf(message));
    }

    public static String generateUrl(
            boolean isSecured,
            String host,
            int port,
            String[] arrayPath,
            @Nullable Hashtable<String, String> params
    ) {
        HttpUrl.Builder builder = new HttpUrl.Builder();

        builder.scheme(!isSecured ? "http" : "https");
        builder.host(host);
        builder.port(port);

        for (String pathSegment : arrayPath) {
            builder.addPathSegment(pathSegment);
        }

        if (params != null) params.forEach(builder::addQueryParameter);

        return builder.toString();
    }

    public static void appInitVars() {
        try {
            APIEndPoints = new Hashtable<>();
            persistentDataOnDisk =
                    fakeContext.getInstance().getSharedPreferences("data", Context.MODE_PRIVATE);
            PDDEditor = persistentDataOnDisk.edit();
            setupApiClient();
            liveData = new observableHashtable<>(cacheService.getInstance());
            liveData.setOnEventListener(map -> {
                try {
                    cacheService.updateInstance(map);
                } catch (IOException IOEx) {
                    writeErrorInLog(IOEx);
                    showToastMessage(
                            fakeContext.getInstance().getString(R.string.error_internal),
                            false
                    );
                }
            });
        } catch (IOException | ClassNotFoundException Ex) {
            writeErrorInLog(Ex);
            showToastMessage(
                    fakeContext.getInstance().getString(R.string.error_internal),
                    false
            );
        }
    }

    private static String _stackTraceToString(Exception ex) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    private static void _createLogFile(
            String error,
            String stacktrace
    ) {
        try {
            File logFile = new File(
                    fakeContext.getInstance().getApplicationContext().getFilesDir(),
                    "log.txt"
            );
            if (logFile.createNewFile()) writeMessageInLogCat("Log file created");

            FileWriter fr = new FileWriter(logFile, true);
            fr.write("Date: " + new Date() + "\n");
            fr.write("Error: " + error + "\n");
            fr.write("Stacktrace:\n" + stacktrace);
            fr.write("==================================\n");
            fr.close();
        } catch (Exception ex) {
            writeMessageInLogCat(
                    "Caused error on log file write: " + ex.getMessage() + "\n" +
                    "Stacktrace: " + _stackTraceToString(ex)
            );
        }
    }
}