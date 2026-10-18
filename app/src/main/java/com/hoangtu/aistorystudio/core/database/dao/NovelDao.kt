package com.hoangtu.aistorystudio.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hoangtu.aistorystudio.core.database.entity.NovelChapterEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelCharacterCrossRefEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelProjectEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelSceneEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelSummaryEntity
import com.hoangtu.aistorystudio.core.database.entity.SceneRevisionEntity
import com.hoangtu.aistorystudio.core.database.entity.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    @Upsert
    suspend fun upsertProject(project: NovelProjectEntity)

    @Query("SELECT * FROM novel_projects WHERE id = :id")
    suspend fun getProject(id: String): NovelProjectEntity?

    @Query("SELECT * FROM novel_projects ORDER BY updated_at DESC, id ASC")
    fun observeProjects(): Flow<List<NovelProjectEntity>>

    @Query("SELECT * FROM novel_projects WHERE title LIKE '%' || :query || '%' OR synopsis LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC, id ASC")
    suspend fun searchProjects(query: String): List<NovelProjectEntity>

    @Query("DELETE FROM novel_projects WHERE id = :id")
    suspend fun deleteProject(id: String): Int

    @Upsert
    suspend fun upsertChapter(chapter: NovelChapterEntity)

    @Query("SELECT * FROM novel_chapters WHERE project_id = :projectId ORDER BY chapter_order ASC, id ASC")
    fun observeChapters(projectId: String): Flow<List<NovelChapterEntity>>

    @Query("SELECT COALESCE(MAX(chapter_order), 0) + 1 FROM novel_chapters WHERE project_id = :projectId")
    suspend fun nextChapterOrder(projectId: String): Long

    @Query("DELETE FROM novel_chapters WHERE id = :id")
    suspend fun deleteChapter(id: String): Int

    @Upsert
    suspend fun upsertScene(scene: NovelSceneEntity)

    @Query("SELECT * FROM novel_scenes WHERE chapter_id = :chapterId ORDER BY scene_order ASC, id ASC")
    fun observeScenes(chapterId: String): Flow<List<NovelSceneEntity>>

    @Query("SELECT COALESCE(MAX(scene_order), 0) + 1 FROM novel_scenes WHERE chapter_id = :chapterId")
    suspend fun nextSceneOrder(chapterId: String): Long

    @Query("DELETE FROM novel_scenes WHERE id = :id")
    suspend fun deleteScene(id: String): Int

    @Upsert
    suspend fun upsertRevision(revision: SceneRevisionEntity)

    @Query("SELECT * FROM scene_revisions WHERE scene_id = :sceneId ORDER BY revision_number DESC, id ASC")
    fun observeRevisions(sceneId: String): Flow<List<SceneRevisionEntity>>

    @Query("SELECT COALESCE(MAX(revision_number), 0) + 1 FROM scene_revisions WHERE scene_id = :sceneId")
    suspend fun nextRevisionNumber(sceneId: String): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCharacterLink(link: NovelCharacterCrossRefEntity): Long

    @Query("SELECT character_id FROM novel_characters WHERE project_id = :projectId ORDER BY linked_at ASC, character_id ASC")
    fun observeCharacterIds(projectId: String): Flow<List<String>>

    @Query("DELETE FROM novel_characters WHERE project_id = :projectId AND character_id = :characterId")
    suspend fun unlinkCharacter(projectId: String, characterId: String): Int

    @Upsert
    suspend fun upsertSummary(summary: NovelSummaryEntity)

    @Query("SELECT * FROM novel_summaries WHERE project_id = :projectId ORDER BY created_at DESC, id ASC")
    fun observeSummaries(projectId: String): Flow<List<NovelSummaryEntity>>

    @Query("SELECT * FROM novel_summaries WHERE id = :id")
    suspend fun getSummary(id: String): NovelSummaryEntity?

    @Upsert
    suspend fun upsertTimelineEvent(event: TimelineEventEntity)

    @Query("SELECT * FROM timeline_events WHERE project_id = :projectId ORDER BY event_order ASC, id ASC")
    fun observeTimeline(projectId: String): Flow<List<TimelineEventEntity>>

    @Query("SELECT COALESCE(MAX(event_order), 0) + 1 FROM timeline_events WHERE project_id = :projectId")
    suspend fun nextTimelineOrder(projectId: String): Long

    @Query("DELETE FROM timeline_events WHERE id = :id")
    suspend fun deleteTimelineEvent(id: String): Int
}
