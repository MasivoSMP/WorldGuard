/*
 * WorldGuard, a suite of tools for Minecraft
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldGuard team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.util.messages;

import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Placeholder values used when rendering configured messages.
 */
public final class MessageContext {

    private static final MessageContext EMPTY = new MessageContext(ImmutableMap.of());

    private final Map<String, String> placeholders;

    private MessageContext(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    public static MessageContext empty() {
        return EMPTY;
    }

    public static MessageContext of(String key, Object value) {
        return builder().put(key, value).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, String> placeholders() {
        return placeholders;
    }

    public static final class Builder {
        private final Map<String, String> placeholders = new LinkedHashMap<>();

        public Builder put(String key, Object value) {
            placeholders.put(key, value == null ? "" : String.valueOf(value));
            return this;
        }

        public MessageContext build() {
            if (placeholders.isEmpty()) {
                return empty();
            }
            return new MessageContext(ImmutableMap.copyOf(placeholders));
        }
    }
}
