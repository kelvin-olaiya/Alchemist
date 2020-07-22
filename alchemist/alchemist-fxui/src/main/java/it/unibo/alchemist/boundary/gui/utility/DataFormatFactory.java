/*
 * Copyright (C) 2010-2020, Danilo Pianini and contributors
 * listed in the main project's alchemist/build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.gui.utility;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javafx.scene.input.DataFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Simple factory that returns the {@link DataFormat} for the specified class.
 * <p>
 * The DataFormat is cached to return only one per class and avoid
 * {@code IllegalArgumentException: DataFormat 'xxx' already exists}.
 * <p>
 * 
 * @see <a href=
 *      "https://bugs.openjdk.java.net/browse/JDK-8118672?focusedCommentId=13730400&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13730400">Issue
 *      JDK-8118672</a>
 * 
 */
public final class DataFormatFactory {

    /**
     * Static {@link LoadingCache} for a single {@link DataFormat} per class
     * loaded.
     */
    private static final LoadingCache<Class<?>, DataFormat> DATA_FORMATS = CacheBuilder.newBuilder()
            .build(new CacheLoader<Class<?>, DataFormat>() {
                @Override
                public DataFormat load(final @NotNull Class<?> key) {
                    return new DataFormat(key.getName());
                }
            });

    /**
     * Static {@link DataFormat} loader for the specified class.
     * 
     * @param object
     *            the object you want the {@code DataFormat} for
     * @return the {@code DataFormat}
     */
    public static DataFormat getDataFormat(final Object object) {
        return DATA_FORMATS.getUnchecked(object.getClass());
    }

    /**
     * Static {@link DataFormat} loader for the specified class.
     * 
     * @param clazz
     *            the class you want the {@code DataFormat} for
     * @return the {@code DataFormat}
     */
    public static DataFormat getDataFormat(final Class<?> clazz) {
        return DATA_FORMATS.getUnchecked(clazz);
    }

    /**
     * Private, empty, constructor, as this is an utility class.
     */
    private DataFormatFactory() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

}
