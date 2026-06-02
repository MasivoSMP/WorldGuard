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

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldguard.WorldGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class MessageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    public void loadsUserMessagesAndDefaultFallbacks() throws Exception {
        File userFile = write("lang.yml", "messages:\n  user: '<red>Hello <name></red>'\n  empty: ''\n");
        File defaultFile = write("defaults.yml", "messages:\n  fallback: '<yellow>Fallback</yellow>'\n");
        MessageService service = new MessageService(userFile, () -> open(defaultFile));

        service.load();

        assertThat(service.plain("messages.user", MessageContext.of("name", "Steve")), equalTo("\u00a7cHello Steve"));
        assertThat(service.plain("messages.fallback"), equalTo("\u00a7eFallback"));
        assertThat(service.plain("messages.empty"), equalTo(""));
    }

    @Test
    public void joinsListsAndEscapesPlaceholderTags() throws Exception {
        File userFile = write("lang.yml", "messages:\n  list:\n    - 'Line <value>'\n    - '&aSecond'\n");
        File defaultFile = write("defaults.yml", "{}\n");
        MessageService service = new MessageService(userFile, () -> open(defaultFile));

        service.load();

        assertThat(service.plain("messages.list", MessageContext.of("value", "<unsafe>")),
                equalTo("Line <unsafe>\n\u00a7aSecond"));
    }

    @Test
    public void localizedCommandExceptionRendersMessage() throws Exception {
        File userFile = write("lang.yml", "prefix: '[WG] '\ncommands:\n  error: 'Failure: <reason>'\n");
        File defaultFile = write("defaults.yml", "{}\n");
        MessageService service = new MessageService(userFile, () -> open(defaultFile));
        service.load();
        WorldGuard.getInstance().setMessageService(service);

        CommandException exception = service.commandException("commands.error", MessageContext.of("reason", "bad input"));

        assertThat(exception.getMessage(), containsString("[WG] Failure: bad input"));
    }

    @Test
    public void prefixesOutboundMessagesOnly() throws Exception {
        File userFile = write("lang.yml", "prefix: '<gray>[WorldGuard]</gray> '\nmessages:\n  line: '<red>Hello</red>'\n  multi: ['One', 'Two']\n");
        File defaultFile = write("defaults.yml", "{}\n");
        MessageService service = new MessageService(userFile, () -> open(defaultFile));

        service.load();

        assertThat(service.plain("messages.line"), equalTo("\u00a7cHello"));
        assertThat(service.prefixedPlain("messages.line"), equalTo("\u00a77[WorldGuard]\u00a7r \u00a7cHello"));
        assertThat(service.prefixedPlain("messages.multi"), equalTo("\u00a77[WorldGuard]\u00a7r One\n\u00a77[WorldGuard]\u00a7r Two"));
    }

    private File write(String name, String content) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path.toFile();
    }

    private static InputStream open(File file) {
        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
