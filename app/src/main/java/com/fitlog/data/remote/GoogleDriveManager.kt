package com.fitlog.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupInfo(
    val fileId: String,
    val fileName: String,
    val modifiedTime: Long
)

@Singleton
class GoogleDriveManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var driveService: Drive? = null

    companion object {
        private const val BACKUP_FILE_NAME = "fitlog_backup.json"
        private const val MIME_TYPE_JSON = "application/json"
    }

    fun initialize(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("FitLog")
            .build()
    }

    fun isInitialized(): Boolean = driveService != null

    suspend fun uploadBackup(jsonData: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(
                Exception("Drive가 초기화되지 않았습니다")
            )

            // 기존 백업 파일 찾기
            val existingFile = findBackupFile()

            val fileMetadata = File().apply {
                name = BACKUP_FILE_NAME
                if (existingFile == null) {
                    parents = listOf("appDataFolder")
                }
            }

            val content = ByteArrayContent.fromString(MIME_TYPE_JSON, jsonData)

            val file = if (existingFile != null) {
                // 기존 파일 업데이트
                drive.files().update(existingFile.id, fileMetadata, content)
                    .setFields("id, name, modifiedTime")
                    .execute()
            } else {
                // 새 파일 생성
                drive.files().create(fileMetadata, content)
                    .setFields("id, name, modifiedTime")
                    .execute()
            }

            Result.success(file.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(
                Exception("Drive가 초기화되지 않았습니다")
            )

            val backupFile = findBackupFile()
                ?: return@withContext Result.failure(Exception("백업 파일을 찾을 수 없습니다"))

            val outputStream = ByteArrayOutputStream()
            drive.files().get(backupFile.id).executeMediaAndDownloadTo(outputStream)

            Result.success(outputStream.toString("UTF-8"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBackupInfo(): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            val file = findBackupFile() ?: return@withContext null
            BackupInfo(
                fileId = file.id,
                fileName = file.name,
                modifiedTime = file.modifiedTime?.value ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun findBackupFile(): File? {
        val drive = driveService ?: return null

        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id, name, modifiedTime)")
            .execute()

        return result.files?.firstOrNull()
    }

    fun clear() {
        driveService = null
    }
}
