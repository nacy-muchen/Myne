/**
 * Copyright (c) [2022 - Present] Stɑrry Shivɑm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.starry.myne.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.starry.myne.R
import com.starry.myne.database.note.NoteDAO
import com.starry.myne.database.library.LibraryDao
import com.starry.myne.database.library.LibraryItem
import com.starry.myne.database.note.Note
import com.starry.myne.database.note.NoteEntryConverter
import com.starry.myne.database.progress.ProgressDao
import com.starry.myne.database.progress.ProgressData
import com.starry.myne.helpers.Constants

@Database(
    entities = [LibraryItem::class, ProgressData::class, Note::class],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 4, to = 5),
    ]
)

@TypeConverters(NoteEntryConverter::class)
abstract class MyneDatabase : RoomDatabase() {

    abstract fun getLibraryDao(): LibraryDao
    abstract fun getReaderDao(): ProgressDao
    abstract fun noteDao(): NoteDAO

    companion object {

        private val migration3to4 = Migration(3, 4) { database ->
            database.execSQL("ALTER TABLE reader_table RENAME COLUMN book_id TO library_item_id")
        }
        private val migration5to6 = Migration(5, 6) { database ->
            database.execSQL("""
        CREATE TABLE IF NOT EXISTS `notes` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            'title' TEXT NOT NULL,
            `text` TEXT NOT NULL,
            `thoughts` TEXT NOT NULL
        )
    """.trimIndent())
        }

        private val migration6to7 = Migration(6, 7) { database ->
            // 创建新表
            database.execSQL("""
        CREATE TABLE IF NOT EXISTS `new_notes` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `title` TEXT NOT NULL,
            `entries` TEXT NOT NULL
        )
    """.trimIndent())

            // 迁移数据，确保生成的 JSON 是数组格式
            database.execSQL("""
        INSERT INTO `new_notes` (id, title, entries)
        SELECT id, title, 
               json_array(
                   json_object('text', COALESCE(text, ''), 'thoughts', COALESCE(thoughts, ''))
               )
        FROM `notes`
    """.trimIndent())

            // 删除旧表，重命名新表
            database.execSQL("DROP TABLE `notes`")
            database.execSQL("ALTER TABLE `new_notes` RENAME TO `notes`")
        }
        private val migration7to8 = Migration(7, 8) { database ->
            database.execSQL("ALTER TABLE `notes` ADD COLUMN `background` INTEGER NOT NULL DEFAULT ${R.drawable.p1}")
        }


        @Volatile
        private var INSTANCE: MyneDatabase? = null

        fun getInstance(context: Context): MyneDatabase {
            /*
            if the INSTANCE is not null, then return it,
            if it is, then create the database and save
            in instance variable then return it.
            */
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyneDatabase::class.java,
                    Constants.DATABASE_NAME
                ).addMigrations(migration3to4,migration5to6, migration6to7, migration7to8).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

}