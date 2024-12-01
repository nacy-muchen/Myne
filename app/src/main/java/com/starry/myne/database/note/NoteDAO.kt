package com.starry.myne.database.note
import androidx.room.*
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for interacting with the `notes` table in the database.
 * Provides methods for inserting, querying, updating, and deleting notes.
 */
@Dao
interface NoteDAO {

    /**
     * Inserts a note into the database. If a note with the same ID already exists, it will be replaced.
     *
     * @param note The note to be inserted.
     * @return The generated ID of the inserted note.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note):Long

    /**
     * Retrieves all notes from the database as a Flow. The Flow will emit a new list of notes whenever
     * the data in the `notes` table changes.
     *
     * @return A Flow containing a list of all notes in the database.
     */
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<Note>>

    /**
     * Retrieves all notes from the database as a Flow. The Flow will emit a new list of notes whenever
     * the data in the `notes` table changes.
     *
     * @return A Flow containing a list of all notes in the database.
     */
    @Delete
    suspend fun delete(note: Note)

    /**
     * Updates an existing note in the database.
     *
     * @param note The note to be updated with new data.
     */
    @Update
    suspend fun update(note: Note)

    /**
     * Retrieves a specific note by its ID.
     *
     * @param noteId The ID of the note to be retrieved.
     * @return The note with the given ID, or null if no such note exists.
     */
    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("SELECT summary FROM notes WHERE id = :noteId LIMIT 1")
    fun getSummaryById(noteId: Long): Flow<String>?
}
