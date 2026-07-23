/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.registry.type.event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.EventContextKey;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.dismount.DismountType;
import org.spongepowered.api.event.cause.entity.spawn.SpawnType;
import org.spongepowered.api.event.cause.entity.teleport.TeleportType;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.registry.AdditionalCatalogRegistryModule;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.event.SpongeEventContextKey;
import org.spongepowered.common.registry.type.AbstractPrefixAlternateCatalogTypeRegistryModule;

import java.util.Locale;

@SuppressWarnings("rawtypes")
@RegisterCatalog(EventContextKeys.class)
public final class EventContextKeysModule
    extends AbstractPrefixAlternateCatalogTypeRegistryModule<EventContextKey>
    implements AdditionalCatalogRegistryModule<EventContextKey> {

    private static final EventContextKeysModule INSTANCE = new EventContextKeysModule();

    public static EventContextKeysModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void registerAdditionalCatalog(EventContextKey extraCatalog) {
        final String id = checkNotNull(extraCatalog).getId();
        final String key = id.toLowerCase(Locale.ENGLISH);
        checkArgument(!key.contains(SpongeImpl.ECOSYSTEM_ID + ":"), "Cannot register spoofed event context key!");
        checkArgument(!key.contains(SpongeImpl.GAME_ID + ":"), "Cannot register spoofed event context key!");
        checkArgument(!this.catalogTypeMap.containsKey(key), "Cannot register an already registered EventContextKey: %s", key);
        this.catalogTypeMap.put(key, extraCatalog);

    }

    @Override
    public void registerDefaults() {
        this.createKey("block_event_queue", "Block Event Queue", LocatableBlock.class);
        this.createKey("block_event_process", "Block Event Process", LocatableBlock.class);
        this.createKey("creator", "Creator", User.class);
        this.createKey("damage_type", "Damage Type", DamageType.class);
        this.createKey("dismount_type", "Dimension Type", DismountType.class);
        this.createKey("igniter", "Igniter", User.class);
        this.createKey("last_damage_source", "Last Damage Source", DamageSource.class);
        this.createKey("liquid_break", "Liquid Break", World.class);
        this.createKey("liquid_flow", "Liquid Flow", World.class);
        this.createKey("liquid_mix", "Liquid Mix", World.class);
        this.createKey("neighbor_notify_source", "Neighbor Notify Source", BlockSnapshot.class);
        this.createKey("notifier", "Notifier", User.class);
        this.createKey("owner", "Owner", User.class);
        this.createKey("player", "Player", Player.class);
        this.createKey("player_simulated", "Game Profile", GameProfile.class);
        this.createKey("projectile_source", "Projectile Source", ProjectileSource.class);
        this.createKey("service_manager", "Service Manager", ServiceManager.class);
        this.createKey("spawn_type", "Spawn Type", SpawnType.class);
        this.createKey("teleport_type", "Teleport Type", TeleportType.class);
        this.createKey("thrower", "Thrower", User.class);
        this.createKey("weapon", "Weapon", ItemStackSnapshot.class);
        this.createKey("fake_player", "Fake Player", Player.class);
        this.createKey("player_break", "Player Break", World.class);
        this.createKey("player_place", "Player Place", World.class);
        this.createKey("fire_spread", "Fire Spread", World.class);
        this.createKey("leaves_decay", "Leaves Decay", World.class);
        this.createKey("piston_retract", "Piston Retract", World.class);
        this.createKey("piston_extend", "Piston Extend", World.class);
        this.createKey("block_hit", "Block Hit", BlockSnapshot.class);
        this.createKey("entity_hit", "Entity Hit", BlockSnapshot.class);
        this.createKey("used_item", "Used Item", ItemStackSnapshot.class);
        this.createKey("used_hand", "Used Hand", HandType.class);
        this.createKey("plugin", "Plugin", PluginContainer.class);
        this.createKey("break_event", "Break Event", ChangeBlockEvent.Break.class);
        this.createKey("place_event", "Place Event", ChangeBlockEvent.Place.class);
        this.createKey("modify_event", "Modify Event", ChangeBlockEvent.Modify.class);
        this.createKey("decay_event", "Decay Event", ChangeBlockEvent.Decay.class);
        this.createKey("grow_event", "Decay Event", ChangeBlockEvent.Grow.class);
        this.createKey("growth_origin", "Growth Origin", BlockSnapshot.class);
    }

    private void createKey(String id, String name, Class<?> usedClass) {
        id = SpongeImpl.ECOSYSTEM_ID + ":" + id;
        this.catalogTypeMap.put(id, new SpongeEventContextKey<>(id, name, usedClass));
    }

    private EventContextKeysModule() {
        super(SpongeImpl.ECOSYSTEM_ID);
    }
}
