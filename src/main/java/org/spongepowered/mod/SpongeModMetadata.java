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
package org.spongepowered.mod;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.client.FMLFileResourcePack;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.ModMetadata;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongePlatform;
import org.spongepowered.spongeforge.Tags;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public final class SpongeModMetadata {

    public static final ModMetadata SPONGE_FORGE_METADATA, SPONGE_API_METADATA, SPONGE_COMMONS_METADATA;

    static {
        ModMetadata[] metadata = getSpongeForgeMetadata();
        SPONGE_FORGE_METADATA = metadata[0];
        SPONGE_API_METADATA = metadata[1];
        SPONGE_COMMONS_METADATA = metadata[2];
    }

    private static ModMetadata[] getSpongeForgeMetadata() {
        try (JarFile jar = new JarFile(SpongeCoremod.modFile)) {
            ZipEntry modInfo = jar.getEntry("mcmod.info");
            try (InputStream inputStream = jar.getInputStream(modInfo)) {
                MetadataCollection metadataCollection = MetadataCollection.from(inputStream, Tags.MOD_ID);

                ModMetadata spongeForge = metadataCollection.getMetadataForId(Tags.MOD_ID, Collections.emptyMap());
                ModMetadata spongeApi = metadataCollection.getMetadataForId(SpongePlatform.API_ID, Collections.emptyMap());
                ModMetadata spongeCommons = metadataCollection.getMetadataForId(SpongeImpl.ECOSYSTEM_ID, Collections.emptyMap());

                return new ModMetadata[] { spongeForge, spongeApi, spongeCommons };
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to process metadata", e);
        }
    }

    public static class SpongeApiMod extends DummyModContainer {

        public SpongeApiMod() {
            super(SPONGE_API_METADATA);
        }

        @Override
        public boolean registerBus(EventBus bus, LoadController controller) {
            return true;
        }

        @Override
        public File getSource() {
            return SpongeCoremod.modFile;
        }

        @Override
        public Class<?> getCustomResourcePackClass() {
            return FMLFileResourcePack.class;
        }

    }

    public static class SpongeCommonsMod extends DummyModContainer {

        public SpongeCommonsMod() {
            super(SPONGE_COMMONS_METADATA);
        }

        @Override
        public boolean registerBus(EventBus bus, LoadController controller) {
            return true;
        }

        @Override
        public File getSource() {
            return SpongeCoremod.modFile;
        }

        @Override
        public Class<?> getCustomResourcePackClass() {
            return FMLFileResourcePack.class;
        }

    }

}
