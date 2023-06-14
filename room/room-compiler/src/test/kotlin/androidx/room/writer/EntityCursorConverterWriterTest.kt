/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.writer

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.processor.BaseEntityParserTest
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.element.Modifier

@RunWith(JUnit4::class)
class EntityCursorConverterWriterTest : BaseEntityParserTest() {
    companion object {
        val OUT_PREFIX = """
            package foo.bar;
            import android.database.Cursor;
            import androidx.room.util.CursorUtil;
            import java.lang.SuppressWarnings;
            import javax.annotation.processing.Generated;
            @Generated("androidx.room.RoomProcessor")
            @SuppressWarnings({"unchecked", "deprecation"})
            public class MyContainerClass {
        """.trimIndent()
        const val OUT_SUFFIX = "}"
    }

    @Test
    fun generateSimple() {
        generateAndMatch(
            """
                @PrimaryKey
                private int id;
                String name;
                String lastName;
                int age;
                public int getId() { return id; }
                public void setId(int id) { this.id = id; }
                """,
            """
                private MyEntity __entityCursorConverter_fooBarMyEntity(Cursor cursor) {
                  final MyEntity _entity;
                  final int _cursorIndexOfId = CursorUtil.getColumnIndex(cursor, "id");
                  final int _cursorIndexOfName = CursorUtil.getColumnIndex(cursor, "name");
                  final int _cursorIndexOfLastName = CursorUtil.getColumnIndex(cursor, "lastName");
                  final int _cursorIndexOfAge = CursorUtil.getColumnIndex(cursor, "age");
                  _entity = new MyEntity();
                  if (_cursorIndexOfId != -1) {
                    final int _tmpId;
                    _tmpId = cursor.getInt(_cursorIndexOfId);
                    _entity.setId(_tmpId);
                  }
                  if (_cursorIndexOfName != -1) {
                    if (cursor.isNull(_cursorIndexOfName)) {
                      _entity.name = null;
                    } else {
                      _entity.name = cursor.getString(_cursorIndexOfName);
                    }
                  }
                  if (_cursorIndexOfLastName != -1) {
                    if (cursor.isNull(_cursorIndexOfLastName)) {
                      _entity.lastName = null;
                    } else {
                      _entity.lastName = cursor.getString(_cursorIndexOfLastName);
                    }
                  }
                  if (_cursorIndexOfAge != -1) {
                    _entity.age = cursor.getInt(_cursorIndexOfAge);
                  }
                  return _entity;
                }
            """.trimIndent()
        )
    }

    private fun generateAndMatch(
        input: String,
        output: String,
        attributes: Map<String, String> = mapOf()
    ) {
        generate(input, attributes) {
            it.assertCompilationResult {
                generatedSource(
                    Source.java(
                        qName = "foo.bar.MyContainerClass",
                        code = listOf(OUT_PREFIX, output, OUT_SUFFIX).joinToString("\n")
                    )
                )
            }
        }
    }

    private fun generate(
        input: String,
        attributes: Map<String, String> = mapOf(),
        handler: (XTestInvocation) -> Unit
    ) {
        singleEntity(input, attributes) { entity, invocation ->
            val className = ClassName.get("foo.bar", "MyContainerClass")
            val writer = object : ClassWriter(className) {
                override fun createTypeSpecBuilder(): TypeSpec.Builder {
                    getOrCreateMethod(EntityCursorConverterWriter(entity))
                    return TypeSpec.classBuilder(className).apply {
                        addModifiers(Modifier.PUBLIC)
                    }
                }
            }
            writer.write(invocation.processingEnv)
            handler(invocation)
        }
    }
}
