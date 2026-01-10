package com.example.appfinancetest

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.client.http.FileContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Collections

class GoogleDriveService(private val context: Context) {

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null) {
            Log.d("GoogleDriveService", "Creating Drive service for account: ${account.email}")
            val credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account
            Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("AppFinanceTest").build()
        } else {
            Log.w("GoogleDriveService", "No signed in account found when creating service")
            null
        }
    }

    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    suspend fun uploadBackup(backupFile: java.io.File): String? = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService() ?: return@withContext null
            
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

            val resultId = if (existingFiles != null && existingFiles.isNotEmpty()) {
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

    suspend fun downloadBackup(targetFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService() ?: return@withContext false
            Log.d("GoogleDriveService", "Searching for backup to download...")
            val query = "name = 'finance_backup.db' and trashed = false"
            val fileList = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            val files = fileList.files
            
            if (files == null || files.isEmpty()) {
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
