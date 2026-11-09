package io.github.leadfreecandy.metalforge.mixin.toro_light;

import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Carrier only: MixinPlugin.postApply rewrites ToroHealth's merged LevelExtractor handler (opt/ToroLight). Priority 2000 so
 * it applies after ToroHealth's mixin (default 1000) and the handler exists.
 */
@Mixin(value = LevelExtractor.class, priority = 2000)
public abstract class LevelExtractorToroMixin {
}
