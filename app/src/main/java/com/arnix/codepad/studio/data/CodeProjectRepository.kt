package com.arnix.codepad.studio.data

import kotlinx.coroutines.flow.Flow

class CodeProjectRepository(private val dao: CodeProjectDao) {
    val activeProjectFlow: Flow<CodeProjectEntity?> = dao.observeActiveProject()

    suspend fun getOrInitProject(): CodeProjectEntity {
        return try {
            val existing = dao.getActiveProjectOnce()
            if (existing != null) return existing
            val defaultProject = CodeProjectEntity()
            dao.upsertProject(defaultProject)
            defaultProject
        } catch (_: Throwable) {
            CodeProjectEntity()
        }
    }

    suspend fun saveProject(project: CodeProjectEntity) {
        try {
            dao.upsertProject(project.copy(lastSavedTimestamp = System.currentTimeMillis()))
        } catch (_: Throwable) {
        }
    }

    val snapshotsFlow: Flow<List<CodeRevisionSnapshot>> = dao.observeSnapshots()

    suspend fun saveSnapshot(tag: String, html: String, css: String, js: String): Long {
        return try {
            val totalChars = html.length + css.length + js.length
            val snapshot = CodeRevisionSnapshot(
                timestamp = System.currentTimeMillis(),
                tag = tag.ifBlank { "Manual Snapshot" },
                htmlCode = html,
                cssCode = css,
                jsCode = js,
                totalChars = totalChars
            )
            dao.insertSnapshot(snapshot)
        } catch (_: Throwable) {
            -1L
        }
    }

    suspend fun deleteSnapshot(id: Long) {
        try {
            dao.deleteSnapshot(id)
        } catch (_: Throwable) {
        }
    }

    suspend fun clearAllSnapshots() {
        try {
            dao.clearAllSnapshots()
        } catch (_: Throwable) {
        }
    }
}
