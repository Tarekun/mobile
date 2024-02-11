package com.example.mobile.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mobile.R
import com.example.mobile.composables.serviceName
import com.example.mobile.database.MeasurementsUtils
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun storePayload(payload: Payload, context: Context): Unit {
    val payloadFile: Uri? = payload.asFile()?.asUri()
    payloadFile?.let {
        CoroutineScope(Dispatchers.IO).launch {
            MeasurementsUtils.storeJsonDumpUri(
                context.contentResolver.openInputStream(it)
            )
        }
    }
}

fun sendCollectionAsPayload(endpointId: String, context: Context): Unit {
    CoroutineScope(Dispatchers.IO).launch {
        val fileToSend: File = File.createTempFile("dump", ".json")
        fileToSend.writeText(MeasurementsUtils.getJsonFullLocalCollection())
        val filePayload = Payload.fromFile(fileToSend)
        Nearby.getConnectionsClient(context)
            .sendPayload(endpointId, filePayload)
    }
}

fun makePayloadCallback(context: Context): PayloadCallback {
    return object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            storePayload(payload, context)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {

            }
        }
    }
}
fun makeSendConnectionCallback(context: Context): ConnectionLifecycleCallback {
    return object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Nearby.getConnectionsClient(context)
                .acceptConnection(endpointId, makePayloadCallback(context))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    sendCollectionAsPayload(endpointId, context)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> { }
                ConnectionsStatusCodes.STATUS_ERROR -> { }
            }
        }

        override fun onDisconnected(endpointId: String) { }
    }
}


@Composable
fun ProximityShareScreen(
    endpointId: String
) {
    val context = LocalContext.current
    val spacing = Modifier.padding(bottom = 16.dp)

    fun requestConnection() {
        Nearby.getConnectionsClient(context)
            .requestConnection(
                serviceName,
                endpointId,
                makeSendConnectionCallback(context)
            ).addOnFailureListener {
                Toast.makeText(
                    context,
                    context.getString(R.string.proximity_share_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        OutlinedButton(
            onClick = {
                requestConnection()
            },
            modifier = spacing
        ) {
            Text(text = "${context.getString(R.string.proximityscreen_start_sharing)} $endpointId")
        }
    }

}