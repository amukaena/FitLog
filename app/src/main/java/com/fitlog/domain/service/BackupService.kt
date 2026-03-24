package com.fitlog.domain.service

interface BackupService {
    suspend fun createBackupJson(): String
    suspend fun restoreFromJson(json: String)
}
