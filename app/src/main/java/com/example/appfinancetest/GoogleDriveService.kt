package com.example.appfinancetest

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class GoogleDriveService(private val context: Context) {

    private fun getDriveService(accessToken: String): Drive {
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance()
        ) { request ->
            request.headers.authorization = "Bearer $accessToken"
        }.setApplicationName("AppFinanceTest").build()
    }

    fun createAuthorizationRequest(): AuthorizationRequest {
        val requestedScopes = listOf(Scope(DriveScopes.DRIVE_FILE))
        return AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()
    }

    fun getAuthorizationResultFromIntent(data: Intent?): AuthorizationResult {
        return Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
    }

    suspend fun uploadBackup(accessToken: String, backupFile: java.io.File): String? = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService(accessToken)
            
            Log.d("GoogleDriveService", "Searching for existing backup 'finance_backup.db'...")
            val query = "name = 'finance_backup.db' and trashed = false"
            val fileList = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            val existingFiles = fileList.files

            val fileMetadata = File().apply {
                name = "finance_backup.db"
                mimeType = "application/x-sqlite3"
            }
            val mediaContent = FileContent("application/x-sqlite3", backupFile)

            val resultId = if (!existingFiles.isNullOrEmpty()) {
                val fileId = existingFiles[0].id
                Log.d("GoogleDriveService", "Updating existing backup file ID: $fileId")
                service.files().update(fileId, null, mediaContent).execute().id
            } else {
                Log.d("GoogleDriveService", "No existing backup found. Creating new file.")
                service.files().create(fileMetadata, mediaContent).execute().id
            }
            Log.d("GoogleDriveService", "Backup successful! ID: $resultId")
            resultId
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Error during uploadBackup", e)
            null
        }
    }

    suspend fun downloadBackup(accessToken: String, targetFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService(accessToken)
            Log.d("GoogleDriveService", "Searching for backup to download...")
            val query = "name = 'finance_backup.db' and trashed = false"
            val fileList = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            val files = fileList.files
            
            if (files.isNullOrEmpty()) {
                Log.w("GoogleDriveService", "No backup file 'finance_backup.db' found on Drive")
                return@withContext false
            }
            
            val fileId = files[0].id
            Log.d("GoogleDriveService", "Downloading file ID: $fileId")
            FileOutputStream(targetFile).use { outputStream ->
                service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }
            Log.d("GoogleDriveService", "Download and save to ${targetFile.absolutePath} successful")
            true
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Error during downloadBackup", e)
            false
        }
    }
}
