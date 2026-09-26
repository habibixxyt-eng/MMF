package com.example

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Item(
    val id: String,
    val kind: String,
    val data: JSONObject
)

class Store(private val context: Context) : SQLiteOpenHelper(context, "mmf.db", null, 1) {
    val vault: File = File(context.filesDir, "vault").apply { mkdirs() }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE items(id TEXT PRIMARY KEY, kind TEXT NOT NULL, data TEXT NOT NULL)")
        db.execSQL("CREATE INDEX item_kind ON items(kind)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Explicit database migration required
    }

    @Synchronized
    fun all(kind: String? = null): List<Item> {
        val result = mutableListOf<Item>()
        readableDatabase.query(
            "items",
            null,
            if (kind == null) null else "kind=?",
            kind?.let { arrayOf(it) },
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += Item(
                    cursor.getString(0),
                    cursor.getString(1),
                    JSONObject(cursor.getString(2))
                )
            }
        }
        return result
    }

    fun get(id: String): Item? = all().firstOrNull { it.id == id }

    @Synchronized
    fun save(kind: String, data: JSONObject, id: String = UUID.randomUUID().toString()): String {
        val values = ContentValues().apply {
            put("id", id)
            put("kind", kind)
            put("data", data.toString())
        }
        check(writableDatabase.insertWithOnConflict("items", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
        return id
    }

    @Synchronized
    fun delete(item: Item) {
        if (item.kind == "file") {
            val f = file(item)
            check(!f.exists() || f.delete()) { "Cannot remove file" }
        }
        writableDatabase.delete("items", "id=?", arrayOf(item.id))
    }

    fun file(item: Item): File = File(vault, item.id)

    fun addFile(source: File, name: String, mime: String, folder: String = ""): String {
        val id = UUID.randomUUID().toString()
        val target = File(vault, id)
        try {
            source.inputStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            save(
                "file",
                JSONObject()
                    .put("name", name)
                    .put("mime", mime)
                    .put("folder", folder)
                    .put("size", target.length())
                    .put("created", System.currentTimeMillis())
                    .put("favorite", false)
                    .put("opened", 0L),
                id
            )
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        return id
    }

    fun importUri(uri: Uri, folder: String = ""): String {
        var name = "Imported file"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        val temp = File.createTempFile("import", null, context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open selected file")
            input.use { src ->
                temp.outputStream().use { dst ->
                    src.copyTo(dst)
                }
            }
            return addFile(
                temp,
                name,
                context.contentResolver.getType(uri) ?: "application/octet-stream",
                folder
            )
        } finally {
            temp.delete()
        }
    }
}
