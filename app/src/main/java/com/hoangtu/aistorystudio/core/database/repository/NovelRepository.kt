package com.hoangtu.aistorystudio.core.database.repository

import androidx.room.withTransaction
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.dao.NovelDao
import com.hoangtu.aistorystudio.core.database.entity.NovelChapterEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelCharacterCrossRefEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelProjectEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelSceneEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelSummaryEntity
import com.hoangtu.aistorystudio.core.database.entity.SceneRevisionEntity
import com.hoangtu.aistorystudio.core.database.entity.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

interface NovelRepository {
    suspend fun saveProject(project: NovelProjectEntity)
    suspend fun getProject(id: String): NovelProjectEntity?
    fun observeProjects(): Flow<List<NovelProjectEntity>>
    suspend fun searchProjects(query: String): List<NovelProjectEntity>
    suspend fun deleteProject(id: String): Boolean
    suspend fun addChapter(projectId: String, title: String, now: Long): NovelChapterEntity
    fun observeChapters(projectId: String): Flow<List<NovelChapterEntity>>
    suspend fun deleteChapter(id: String): Boolean
    suspend fun addScene(chapterId: String, title: String, now: Long): NovelSceneEntity
    fun observeScenes(chapterId: String): Flow<List<NovelSceneEntity>>
    suspend fun deleteScene(id: String): Boolean
    suspend fun addRevision(sceneId: String, content: String, label: String, now: Long): SceneRevisionEntity
    fun observeRevisions(sceneId: String): Flow<List<SceneRevisionEntity>>
    suspend fun linkCharacter(projectId: String, characterId: String, linkedAt: Long)
    fun observeCharacterIds(projectId: String): Flow<List<String>>
    suspend fun unlinkCharacter(projectId: String, characterId: String): Boolean
    suspend fun saveSummary(summary: NovelSummaryEntity)
    fun observeSummaries(projectId: String): Flow<List<NovelSummaryEntity>>
    suspend fun addTimelineEvent(event: TimelineEventEntity): TimelineEventEntity
    fun observeTimeline(projectId: String): Flow<List<TimelineEventEntity>>
    suspend fun deleteTimelineEvent(id: String): Boolean
}

class RoomNovelRepository(
    private val database: StudioDatabase,
    private val dao: NovelDao
) : NovelRepository {
    override suspend fun saveProject(project: NovelProjectEntity) = dao.upsertProject(project)
    override suspend fun getProject(id: String) = dao.getProject(id)
    override fun observeProjects() = dao.observeProjects()
    override suspend fun searchProjects(query: String) = dao.searchProjects(query.trim())
    override suspend fun deleteProject(id: String) = dao.deleteProject(id) > 0

    override suspend fun addChapter(projectId: String, title: String, now: Long): NovelChapterEntity =
        database.withTransaction {
            val chapter = NovelChapterEntity(
                projectId = projectId,
                title = title,
                chapterOrder = dao.nextChapterOrder(projectId),
                createdAt = now,
                updatedAt = now
            )
            dao.upsertChapter(chapter)
            chapter
        }

    override fun observeChapters(projectId: String) = dao.observeChapters(projectId)
    override suspend fun deleteChapter(id: String) = dao.deleteChapter(id) > 0

    override suspend fun addScene(chapterId: String, title: String, now: Long): NovelSceneEntity =
        database.withTransaction {
            val scene = NovelSceneEntity(
                chapterId = chapterId,
                title = title,
                sceneOrder = dao.nextSceneOrder(chapterId),
                createdAt = now,
                updatedAt = now
            )
            dao.upsertScene(scene)
            scene
        }

    override fun observeScenes(chapterId: String) = dao.observeScenes(chapterId)
    override suspend fun deleteScene(id: String) = dao.deleteScene(id) > 0

    override suspend fun addRevision(
        sceneId: String,
        content: String,
        label: String,
        now: Long
    ): SceneRevisionEntity = database.withTransaction {
        val revision = SceneRevisionEntity(
            sceneId = sceneId,
            revisionNumber = dao.nextRevisionNumber(sceneId),
            contentSnapshot = content,
            label = label,
            createdAt = now
        )
        dao.upsertRevision(revision)
        revision
    }

    override fun observeRevisions(sceneId: String) = dao.observeRevisions(sceneId)

    override suspend fun linkCharacter(projectId: String, characterId: String, linkedAt: Long) {
        dao.insertCharacterLink(
            NovelCharacterCrossRefEntity(
                projectId = projectId,
                characterId = characterId,
                linkedAt = linkedAt
            )
        )
    }

    override fun observeCharacterIds(projectId: String) = dao.observeCharacterIds(projectId)
    override suspend fun unlinkCharacter(projectId: String, characterId: String) =
        dao.unlinkCharacter(projectId, characterId) > 0

    override suspend fun saveSummary(summary: NovelSummaryEntity) = database.withTransaction {
        val existing = dao.getSummary(summary.id)
        require(existing == null || existing.projectId == summary.projectId) {
            "Khong duoc chuyen summary sang du an truyen khac"
        }
        dao.upsertSummary(summary)
    }
    override fun observeSummaries(projectId: String) = dao.observeSummaries(projectId)

    override suspend fun addTimelineEvent(event: TimelineEventEntity): TimelineEventEntity =
        database.withTransaction {
            val sequencedEvent = event.copy(eventOrder = dao.nextTimelineOrder(event.projectId))
            dao.upsertTimelineEvent(sequencedEvent)
            sequencedEvent
        }

    override fun observeTimeline(projectId: String) = dao.observeTimeline(projectId)
    override suspend fun deleteTimelineEvent(id: String) = dao.deleteTimelineEvent(id) > 0
}
