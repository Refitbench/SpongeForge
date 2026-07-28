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
package org.spongepowered.mod.plugin;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.SpongePlatform;
import org.spongepowered.common.plugin.MinecraftPluginContainer;
import org.spongepowered.mod.SpongeModMetadata;
import org.spongepowered.plugin.meta.PluginDependency;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@NonnullByDefault
@Singleton
public class SpongeModPluginManager implements PluginManager {

    private Map<String, PluginContainer> getContainers() {
        Map<String, PluginContainer> containers = new HashMap<>();

        // These can be overridden later
        containers.put(SpongeImplHooks.getImplementationId(), new ModPluginContainer(new DummyModContainer(SpongeModMetadata.SPONGE_FORGE_METADATA)));
        containers.put(SpongePlatform.API_ID, new ModPluginContainer(new DummyModContainer(SpongeModMetadata.SPONGE_API_METADATA)));
        containers.put(SpongeImpl.ECOSYSTEM_ID, new ModPluginContainer(new DummyModContainer(SpongeModMetadata.SPONGE_COMMONS_METADATA)));

        for (ModContainer mod : Loader.instance().getModList()) {
            containers.put(mod.getModId(), wrap(mod));
        }

        containers.put(SpongeImpl.GAME_ID, MinecraftPluginContainer.INSTANCE);
        return containers;
    }

    private static PluginContainer wrap(ModContainer mod) {
        return SpongeImpl.GAME_ID.equals(mod.getModId()) ? MinecraftPluginContainer.INSTANCE : new ModPluginContainer(mod);
    }

    @Override
    public Optional<PluginContainer> getPlugin(String id) {
        return Optional.ofNullable(this.getContainers().get(checkNotNull(id, "id")));
    }

    @Override
    public Collection<PluginContainer> getPlugins() {
        return this.getContainers().values();
    }

    @Override
    public Optional<PluginContainer> fromInstance(Object instance) {
        checkNotNull(instance, "instance");
        if (instance instanceof PluginContainer) {
            return Optional.of((PluginContainer) instance);
        }
        if (instance instanceof ModContainer) {
            return Optional.of(wrap((ModContainer) instance));
        }
        ModContainer container = Loader.instance().getReversedModObjectList().get(instance);
        if (container != null) {
            return Optional.of(wrap(container));
        }
        return Optional.empty();
    }

    @Override
    public boolean isLoaded(String id) {
        return Loader.isModLoaded(checkNotNull(id, "id"));
    }

    private static class ModPluginContainer implements PluginContainer {

        private final ModContainer modContainer;

        private ModPluginContainer(ModContainer modContainer) {
            this.modContainer = modContainer;
        }

        @Override
        public String getId() {
            return checkNotNull(emptyToNull(this.modContainer.getModId()), "modid");
        }

        @Override
        public String getName() {
            return checkNotNull(emptyToNull(this.modContainer.getName()), "name");
        }

        @Override
        public Optional<String> getVersion() {
            String version = emptyToNull(this.modContainer.getVersion());
            if ("unknown".equalsIgnoreCase(version) || "dev".equalsIgnoreCase(version)) {
                version = null;
            }
            return Optional.ofNullable(version);
        }

        @Override
        public Optional<String> getDescription() {
            final ModMetadata meta = this.modContainer.getMetadata();
            return meta != null ? Optional.ofNullable(emptyToNull(meta.description)) : Optional.empty();
        }

        @Override
        public Optional<String> getUrl() {
            final ModMetadata meta = this.modContainer.getMetadata();
            return meta != null ? Optional.ofNullable(emptyToNull(meta.url)) : Optional.empty();
        }

        @Override
        public List<String> getAuthors() {
            final ModMetadata meta = this.modContainer.getMetadata();
            return meta != null ? ImmutableList.copyOf(meta.authorList) : ImmutableList.of();
        }

        @Override
        public Set<PluginDependency> getDependencies() {
            return DependencyHandler.collectDependencies(this.modContainer);
        }

        @Override
        public Optional<PluginDependency> getDependency(String id) {
            return Optional.ofNullable(DependencyHandler.findDependency(this.modContainer, id));
        }

        @Override
        public Optional<Path> getSource() {
            final File source = this.modContainer.getSource();
            if (source != null) {
                return Optional.of(source.toPath());
            }
            return Optional.empty();
        }

        @Override
        public Optional<?> getInstance() {
            return Optional.ofNullable(this.modContainer.getMod());
        }

    }

}
