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
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.serializer.legacy.LegacyComponentSerializer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.commands.CommandUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.function.Supplier;

/**
 * Loads and renders WorldGuard's configurable user-facing messages.
 */
public final class MessageService {

    private static final String MISSING_PREFIX = "<missing message: ";
    private static final String PREFIX_KEY = "prefix";

    private final File file;
    private final Supplier<InputStream> defaultSupplier;
    private Map<String, String> messages = ImmutableMap.of();
    private Map<String, String> defaultMessages = ImmutableMap.of();

    public MessageService(File file, Supplier<InputStream> defaultSupplier) {
        this.file = file;
        this.defaultSupplier = defaultSupplier;
    }

    public void load() {
        defaultMessages = loadDefaults(defaultSupplier);
        messages = loadFile(file, "language");
    }

    public TextComponent component(String key) {
        return component(key, MessageContext.empty());
    }

    public TextComponent component(String key, MessageContext context) {
        return LegacyComponentSerializer.INSTANCE.deserialize(plain(key, context));
    }

    public TextComponent prefixedComponent(String key) {
        return prefixedComponent(key, MessageContext.empty());
    }

    public TextComponent prefixedComponent(String key, MessageContext context) {
        return LegacyComponentSerializer.INSTANCE.deserialize(prefixedPlain(key, context));
    }

    public String plain(String key) {
        return plain(key, MessageContext.empty());
    }

    public String plain(String key, MessageContext context) {
        String template = getTemplate(key);
        if (template == null) {
            return MISSING_PREFIX + key + ">";
        }
        if (template.isEmpty()) {
            return "";
        }
        return render(template, context);
    }

    public String prefixedPlain(String key) {
        return prefixedPlain(key, MessageContext.empty());
    }

    public String prefixedPlain(String key, MessageContext context) {
        String message = plain(key, context);
        if (message.isEmpty() || key.equals(PREFIX_KEY)) {
            return message;
        }

        String prefix = plain(PREFIX_KEY);
        if (prefix.isEmpty() || prefix.startsWith(MISSING_PREFIX)) {
            return message;
        }

        return prefixLines(prefix, message);
    }

    public void send(Actor actor, String key) {
        send(actor, key, MessageContext.empty());
    }

    public void send(Actor actor, String key, MessageContext context) {
        String message = prefixedPlain(key, context);
        if (!message.isEmpty()) {
            actor.printRaw(message);
        }
    }

    public CommandException commandException(String key) {
        return commandException(key, MessageContext.empty());
    }

    public CommandException commandException(String key, MessageContext context) {
        return new LocalizedCommandException(key, context);
    }

    private String getTemplate(String key) {
        String message = messages.get(key);
        if (message != null) {
            return message;
        }
        return defaultMessages.get(key);
    }

    private String render(String template, MessageContext context) {
        String message = parseFormatting(template);
        for (Map.Entry<String, String> entry : context.placeholders().entrySet()) {
            String value = entry.getValue();
            message = message.replace("<" + entry.getKey() + ">", value);
            message = message.replace("%" + entry.getKey() + "%", value);
        }
        return message;
    }

    private static String prefixLines(String prefix, String message) {
        String[] lines = message.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                lines[i] = prefix + lines[i];
            }
        }
        return String.join("\n", lines);
    }

    private String parseFormatting(String message) {
        // MiniMessage-style tags are converted to the legacy format used by WorldEdit actors.
        message = message.replace("<reset>", "&r");
        message = message.replace("<bold>", "&l").replace("</bold>", "&r");
        message = message.replace("<italic>", "&o").replace("</italic>", "&r");
        message = message.replace("<underlined>", "&n").replace("</underlined>", "&r");
        message = message.replace("<underline>", "&n").replace("</underline>", "&r");
        message = message.replace("<strikethrough>", "&m").replace("</strikethrough>", "&r");
        message = message.replace("<obfuscated>", "&k").replace("</obfuscated>", "&r");

        message = replaceColorTag(message, "black", "0");
        message = replaceColorTag(message, "dark_blue", "1");
        message = replaceColorTag(message, "dark_green", "2");
        message = replaceColorTag(message, "dark_aqua", "3");
        message = replaceColorTag(message, "dark_red", "4");
        message = replaceColorTag(message, "dark_purple", "5");
        message = replaceColorTag(message, "gold", "6");
        message = replaceColorTag(message, "gray", "7");
        message = replaceColorTag(message, "grey", "7");
        message = replaceColorTag(message, "dark_gray", "8");
        message = replaceColorTag(message, "dark_grey", "8");
        message = replaceColorTag(message, "blue", "9");
        message = replaceColorTag(message, "green", "a");
        message = replaceColorTag(message, "aqua", "b");
        message = replaceColorTag(message, "red", "c");
        message = replaceColorTag(message, "light_purple", "d");
        message = replaceColorTag(message, "yellow", "e");
        message = replaceColorTag(message, "white", "f");

        return CommandUtils.replaceColorMacros(message);
    }

    private static String replaceColorTag(String message, String name, String code) {
        return message.replace("<" + name + ">", "&" + code).replace("</" + name + ">", "&r");
    }

    private static Map<String, String> loadFile(File file, String description) {
        if (!file.exists()) {
            return ImmutableMap.of();
        }
        try (InputStream stream = new FileInputStream(file)) {
            Object loaded = new Yaml().load(stream);
            if (!(loaded instanceof Map)) {
                return ImmutableMap.of();
            }
            Map<String, String> flattened = new LinkedHashMap<>();
            flattenMap("", (Map<?, ?>) loaded, flattened);
            return ImmutableMap.copyOf(flattened);
        } catch (IOException e) {
            WorldGuard.logger.log(Level.WARNING, "Unable to read " + description + " file " + file.getAbsolutePath(), e);
            return ImmutableMap.of();
        }
    }

    private static Map<String, String> loadDefaults(Supplier<InputStream> supplier) {
        try (InputStream stream = supplier.get()) {
            if (stream == null) {
                WorldGuard.logger.warning("Unable to read bundled default language file.");
                return ImmutableMap.of();
            }
            Object loaded = new Yaml().load(stream);
            if (!(loaded instanceof Map)) {
                return ImmutableMap.of();
            }
            Map<String, String> flattened = new LinkedHashMap<>();
            flattenMap("", (Map<?, ?>) loaded, flattened);
            return ImmutableMap.copyOf(flattened);
        } catch (IOException e) {
            WorldGuard.logger.log(Level.WARNING, "Unable to read bundled default language file.", e);
            return ImmutableMap.of();
        }
    }

    private static void flattenMap(String prefix, Map<?, ?> source, Map<String, String> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenMap(path, (Map<?, ?>) value, target);
            } else if (value instanceof List) {
                target.put(path, String.join("\n", ((List<?>) value).stream().map(String::valueOf).toArray(String[]::new)));
            } else if (value != null) {
                target.put(path, String.valueOf(value));
            }
        }
    }
}
