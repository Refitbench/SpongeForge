package org.spongepowered.mod.mixin.nucleus;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "io.github.nucleuspowered.nucleus.services.impl.messageprovider.repository.AbstractMessageRepository", remap = false)
public abstract class AbstractMessageRepositoryMixin {

    @Shadow
    protected abstract TextTemplate getTextTemplate(String key);

    /**
     * @author Rongmario
     * @reason HashMap CME
     */
    @Overwrite
    public Text getText(String key) {
        return this.getTextTemplate(key).toText();
    }

}
