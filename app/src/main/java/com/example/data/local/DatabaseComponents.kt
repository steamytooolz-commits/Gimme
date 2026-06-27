package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.CustodyEvent
import com.example.data.model.GamePhase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "evidence")
data class EvidenceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val physicalDescription: String,
    val forensicReport: String,
    val locationFound: String,
    val collectingOfficer: String,
    val timestamp: Long,
    val warrantUsed: String?,
    val userAnnotations: String,
    val admissibilityStatus: String
)

@Entity(tableName = "evidence_links")
data class EvidenceLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourceEvidenceId: String,
    val targetEvidenceId: String,
    val relationshipType: String,
    val magistrateJustification: String
)

@Entity(tableName = "npcs")
data class NpcEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
    val stress: Int,
    val statement: String,
    val profile: String,
    val hiddenMotive: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val phaseName: String, // INVESTIGATION or COURTROOM
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isSystem: Boolean,
    val isToolCall: Boolean,
    val toolName: String?
)

@Entity(tableName = "world_state")
data class WorldStateEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "case_ground_truth")
data class CaseGroundTruthEntity(
    @PrimaryKey val caseId: String,
    val archetype: String,
    val coreCrime: String,
    val absoluteTruth: String,
    val suspectCulpabilitiesJson: String,
    val criticalMissableCluesJson: String
)

@Entity(tableName = "case_evaluation_reports")
data class CaseEvaluationReportEntity(
    @PrimaryKey val caseId: String,
    val justiceMetric: Int,
    val proceduralIntegrity: Int,
    val conspiracyUnraveledPercentage: Int,
    val overallGrade: String,
    val appellateCritique: String,
    val targetSuspectId: String,
    val wasConvicted: Boolean,
    val actualCulpability: String
)

@Entity(tableName = "world_bibles")
data class WorldBibleEntity(
    @PrimaryKey val caseId: String,
    val bibleJson: String
)

@Entity(tableName = "case_progress")
data class CaseProgressEntity(
    @PrimaryKey val caseId: String,
    val status: String,
    val daysElapsed: Int,
    val maxDaysBeforeCold: Int,
    val activeLeadsRemaining: Int,
    val publicPressure: Float,
    val degradationLevel: Int
)

@Entity(tableName = "legal_statutes")
data class LegalStatuteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val clausesJson: String
)

@Entity(tableName = "newspaper_articles")
data class NewspaperArticleEntity(
    @PrimaryKey val id: String,
    val headline: String,
    val content: String,
    val dayPublished: Int,
    val publicSentimentShift: Float
)



// --- Type Converters ---

class ThemisConverters {
    private val moshi = Moshi.Builder().build()
    private val custodyEventListType = Types.newParameterizedType(List::class.java, CustodyEvent::class.java)
    private val custodyEventAdapter = moshi.adapter<List<CustodyEvent>>(custodyEventListType)

    @TypeConverter
    fun fromCustodyEvents(events: List<CustodyEvent>?): String {
        return events?.let { custodyEventAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toCustodyEvents(json: String?): List<CustodyEvent> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            custodyEventAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// --- DAOs ---

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence")
    fun getAllEvidence(): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence")
    suspend fun getEvidenceListDirect(): List<EvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: EvidenceEntity)

    @Query("SELECT * FROM evidence WHERE id = :id LIMIT 1")
    suspend fun getEvidenceById(id: String): EvidenceEntity?

    @Query("DELETE FROM evidence")
    suspend fun clearAll()
}

@Dao
interface EvidenceLinkDao {
    @Query("SELECT * FROM evidence_links")
    fun getAllLinks(): Flow<List<EvidenceLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: EvidenceLinkEntity)

    @Query("DELETE FROM evidence_links")
    suspend fun clearAll()
}

@Dao
interface NpcDao {
    @Query("SELECT * FROM npcs")
    fun getAllNpcs(): Flow<List<NpcEntity>>

    @Query("SELECT * FROM npcs")
    suspend fun getNpcListDirect(): List<NpcEntity>

    @Query("SELECT * FROM npcs WHERE id = :id LIMIT 1")
    suspend fun getNpcById(id: String): NpcEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNpc(npc: NpcEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNpcs(npcs: List<NpcEntity>)

    @Query("UPDATE npcs SET stress = :stress WHERE id = :id")
    suspend fun updateNpcStress(id: String, stress: Int)

    @Query("DELETE FROM npcs")
    suspend fun clearAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesDirect(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface WorldStateDao {
    @Query("SELECT * FROM world_state")
    fun getAllState(): Flow<List<WorldStateEntity>>

    @Query("SELECT value FROM world_state WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: WorldStateEntity)

    @Query("DELETE FROM world_state")
    suspend fun clearAll()
}

@Dao
interface GroundTruthDao {
    @Query("SELECT * FROM case_ground_truth WHERE caseId = :caseId LIMIT 1")
    suspend fun getGroundTruth(caseId: String): CaseGroundTruthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroundTruth(groundTruth: CaseGroundTruthEntity)

    @Query("DELETE FROM case_ground_truth")
    suspend fun clearAll()
}

@Dao
interface CaseEvaluationDao {
    @Query("SELECT * FROM case_evaluation_reports WHERE caseId = :caseId LIMIT 1")
    suspend fun getEvaluationReport(caseId: String): CaseEvaluationReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluationReport(report: CaseEvaluationReportEntity)

    @Query("DELETE FROM case_evaluation_reports")
    suspend fun clearAll()
}

@Dao
interface WorldBibleDao {
    @Query("SELECT * FROM world_bibles WHERE caseId = :caseId LIMIT 1")
    suspend fun getWorldBible(caseId: String): WorldBibleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldBible(bible: WorldBibleEntity)

    @Query("DELETE FROM world_bibles")
    suspend fun clearAll()
}

@Dao
interface CaseProgressDao {
    @Query("SELECT * FROM case_progress WHERE caseId = :caseId LIMIT 1")
    suspend fun getCaseProgress(caseId: String): CaseProgressEntity?

    @Query("SELECT * FROM case_progress")
    suspend fun getAllCaseProgress(): List<CaseProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaseProgress(progress: CaseProgressEntity)

    @Query("DELETE FROM case_progress")
    suspend fun clearAll()
}


@Entity(tableName = "case_linkages")
data class CaseLinkageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val activeCaseId: String,
    val coldCaseId: String,
    val connectionType: String,
    val description: String
)

@Entity(tableName = "cold_case_digests")
data class ColdCaseDigestEntity(
    @PrimaryKey val originalCaseId: String,
    val summary: String,
    val keyPlayersJson: String,
    val unresolvedThreadsJson: String
)

@Dao
interface CaseLinkageDao {
    @Query("SELECT * FROM case_linkages WHERE activeCaseId = :activeCaseId")
    suspend fun getLinkagesForCase(activeCaseId: String): List<CaseLinkageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinkage(linkage: CaseLinkageEntity)
}

@Dao
interface ColdCaseDigestDao {
    @Query("SELECT * FROM cold_case_digests WHERE originalCaseId = :caseId LIMIT 1")
    suspend fun getDigest(caseId: String): ColdCaseDigestEntity?

    @Query("SELECT * FROM cold_case_digests")
    suspend fun getAllDigests(): List<ColdCaseDigestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDigest(digest: ColdCaseDigestEntity)
}

@Dao
interface LegalStatuteDao {
    @Query("SELECT * FROM legal_statutes")
    fun getAllStatutes(): Flow<List<LegalStatuteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatute(statute: LegalStatuteEntity)
    
    @Query("DELETE FROM legal_statutes")
    suspend fun clearAll()
}

@Dao
interface NewspaperArticleDao {
    @Query("SELECT * FROM newspaper_articles ORDER BY dayPublished DESC")
    fun getAllArticles(): Flow<List<NewspaperArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: NewspaperArticleEntity)

    @Query("DELETE FROM newspaper_articles")
    suspend fun clearAll()
}

// --- Database ---

@Database(
    entities = [
        EvidenceEntity::class,
        EvidenceLinkEntity::class,
        NpcEntity::class,
        ChatMessageEntity::class,
        WorldStateEntity::class,
        CaseGroundTruthEntity::class,
        CaseEvaluationReportEntity::class,
        WorldBibleEntity::class,
        CaseProgressEntity::class,
        CaseLinkageEntity::class,
        ColdCaseDigestEntity::class,
        LegalStatuteEntity::class,
        NewspaperArticleEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(ThemisConverters::class)
abstract class ThemisDatabase : RoomDatabase() {
    abstract fun evidenceDao(): EvidenceDao
    abstract fun evidenceLinkDao(): EvidenceLinkDao
    abstract fun npcDao(): NpcDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun worldStateDao(): WorldStateDao
    abstract fun groundTruthDao(): GroundTruthDao
    abstract fun caseEvaluationDao(): CaseEvaluationDao
    abstract fun worldBibleDao(): WorldBibleDao
    abstract fun caseProgressDao(): CaseProgressDao
    abstract fun caseLinkageDao(): CaseLinkageDao
    abstract fun coldCaseDigestDao(): ColdCaseDigestDao
    abstract fun legalStatuteDao(): LegalStatuteDao
    abstract fun newspaperArticleDao(): NewspaperArticleDao
}

