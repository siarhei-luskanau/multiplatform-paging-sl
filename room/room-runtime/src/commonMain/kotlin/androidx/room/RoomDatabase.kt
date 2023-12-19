/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TODO(b/317120607): Rename to RoomDatabaseKt once the room-ktx artifact is merged.
@file:JvmName("RoomDatabaseUtils")

package androidx.room

import androidx.annotation.RestrictTo
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.contains
import androidx.room.util.isAssignableFrom
import androidx.sqlite.SQLiteDriver
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/**
 * Base class for all Room databases. All classes that are annotated with [Database] must
 * extend this class.
 *
 * RoomDatabase provides direct access to the underlying database implementation but you should
 * prefer using [Dao] classes.
 *
 * @see Database
 */
expect abstract class RoomDatabase {

    /**
     * Creates a connection manager to manage database connection. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @param configuration The database configuration
     * @return A new connection manager
     */
    internal fun createConnectionManager(
        configuration: DatabaseConfiguration
    ): RoomConnectionManager

    /**
     * Creates a delegate to configure and initialize the database when it is being opened.
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A new delegate to be used while opening the database
     */
    protected open fun createOpenDelegate(): RoomOpenDelegateMarker

    /**
     * Returns a Set of required [AutoMigrationSpec] classes.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return Creates a set that will include the classes of all required auto migration specs for
     * this database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>>

    /**
     * Returns a list of automatic [Migration]s that have been generated.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @param autoMigrationSpecs the provided specs needed by certain migrations.
     * @return A list of migration instances each of which is a generated 'auto migration'.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration>

    /**
     * Builder for [RoomDatabase].
     *
     * @param T The type of the abstract database class.
     */
    class Builder<T : RoomDatabase> {
        /**
         * Sets the [SQLiteDriver] implementation to be used by Room to open database connections.
         *
         * @param driver The driver
         * @return This builder instance.
         */
        fun setDriver(driver: SQLiteDriver): Builder<T>

        /**
         * Creates the database and initializes it.
         *
         * @return A new database instance.
         * @throws IllegalArgumentException if the builder was misconfigured.
         */
        fun build(): T
    }

    /**
     * A container to hold migrations. It also allows querying its contents to find migrations
     * between two versions.
     */
    class MigrationContainer {
        /**
         * Returns the map of available migrations where the key is the start version of the
         * migration, and the value is a map of (end version -> Migration).
         *
         * @return Map of migrations keyed by the start version
         */
        fun getMigrations(): Map<Int, Map<Int, Migration>>

        /**
         * Add a [Migration] to the container. If the container already has a migration with the
         * same start-end versions then it will be overwritten.
         *
         * @param migration the migration to add.
         */
        internal fun addMigration(migration: Migration)

        /**
         * Returns a pair corresponding to an entry in the map of available migrations whose key
         * is [migrationStart] and its sorted keys in ascending order.
         */
        internal fun getSortedNodes(
            migrationStart: Int
        ): Pair<Map<Int, Migration>, Iterable<Int>>?

        /**
         * Returns a pair corresponding to an entry in the map of available migrations whose key
         * is [migrationStart] and its sorted keys in descending order.
         */
        internal fun getSortedDescendingNodes(
            migrationStart: Int
        ): Pair<Map<Int, Migration>, Iterable<Int>>?
    }
}

internal fun RoomDatabase.validateAutoMigrations(configuration: DatabaseConfiguration) {
    val autoMigrationSpecs = mutableMapOf<KClass<out AutoMigrationSpec>, AutoMigrationSpec>()
    val requiredAutoMigrationSpecs = getRequiredAutoMigrationSpecClasses()
    val usedSpecs = BooleanArray(requiredAutoMigrationSpecs.size)
    for (spec in requiredAutoMigrationSpecs) {
        var foundIndex = -1
        for (providedIndex in configuration.autoMigrationSpecs.indices.reversed()) {
            val provided: Any = configuration.autoMigrationSpecs[providedIndex]
            // TODO(b/317210564): For native only FQN is compared
            if (spec.isAssignableFrom(provided::class)) {
                foundIndex = providedIndex
                usedSpecs[foundIndex] = true
                break
            }
        }
        require(foundIndex >= 0) {
            "A required auto migration spec (${spec.qualifiedName}) is missing in the " +
                "database configuration."
        }
        autoMigrationSpecs[spec] = configuration.autoMigrationSpecs[foundIndex]
    }
    for (providedIndex in configuration.autoMigrationSpecs.indices.reversed()) {
        require(usedSpecs[providedIndex]) {
            "Unexpected auto migration specs found. " +
                "Annotate AutoMigrationSpec implementation with " +
                "@ProvidedAutoMigrationSpec annotation or remove this spec from the " +
                "builder."
        }
    }
    val autoMigrations = createAutoMigrations(autoMigrationSpecs)
    for (autoMigration in autoMigrations) {
        val migrationExists = configuration.migrationContainer.contains(
            autoMigration.startVersion,
            autoMigration.endVersion
        )
        if (!migrationExists) {
            configuration.migrationContainer.addMigration(autoMigration)
        }
    }
}
