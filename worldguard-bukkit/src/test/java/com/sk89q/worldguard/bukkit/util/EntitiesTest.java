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

package com.sk89q.worldguard.bukkit.util;

import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Slime;
import org.bukkit.entity.SulfurCube;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitiesTest {
    @Test
    void targetHierarchyPreservesHostileAndPassiveProtection() {
        assertTrue(Entities.isHostile(entity(MagmaCube.class, false)));
        assertTrue(Entities.isHostile(entity(Slime.class, false)));
        assertTrue(Entities.isHostile(entity(Ghast.class, false)));
        assertTrue(Entities.isHostile(entity(EnderDragon.class, false)));
        assertTrue(Entities.isHostile(entity(Shulker.class, false)));
        assertFalse(Entities.isHostile(entity(Cow.class, false)));
        assertFalse(Entities.isHostile(entity(SulfurCube.class, false)));
        assertTrue(Entities.isNonHostile(entity(SulfurCube.class, false)));
        assertFalse(Entities.isNonHostile(entity(MagmaCube.class, false)));
    }

    @Test
    void sulfurExplosionUsesExistingTntProtection() {
        assertTrue(Entities.isTNTBased(entity(SulfurCube.class, true)));
        assertFalse(Entities.isTNTBased(entity(SulfurCube.class, false)));
        assertSame(Flags.TNT, Entities.getExplosionFlag(entity(SulfurCube.class, true)));
        assertSame(Flags.OTHER_EXPLOSION, Entities.getExplosionFlag(entity(MagmaCube.class, false)));
    }

    private static Entity entity(Class<? extends Entity> type, boolean canExplode) {
        return (Entity) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("canExplode")) {
                return canExplode;
            }
            throw new AssertionError("Unexpected entity access: " + method);
        });
    }
}
