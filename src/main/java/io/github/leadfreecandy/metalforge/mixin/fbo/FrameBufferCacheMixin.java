package io.github.leadfreecandy.metalforge.mixin.fbo;

import com.mojang.renderpearl.backend.opengl.DirectStateAccess;
import com.mojang.renderpearl.backend.opengl.FrameBufferAttachment;
import com.mojang.renderpearl.backend.opengl.FrameBufferCache;
import io.github.leadfreecandy.metalforge.opt.FboTracker;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Remembers which FBO carries which depth texture at mip level 0, so depth clears can happen in that FBO (see
 * GlCommandEncoderMixin). Every FBO the GL backend makes comes from this cache, and a destroyed texture destroys its
 * FBOs here, so a recycled GL name never keeps a stale entry.
 */
@Mixin(value = FrameBufferCache.class, remap = false)
public abstract class FrameBufferCacheMixin {
    @Shadow @Final private Object2IntMap<FrameBufferCache.CacheKey> cache;

    @Inject(method = "getFbo(Lcom/mojang/renderpearl/backend/opengl/DirectStateAccess;Ljava/util/List;Lcom/mojang/renderpearl/backend/opengl/FrameBufferAttachment;I)I",
        at = @At("RETURN"))
    private void metalforge$track(DirectStateAccess dsa, List<@Nullable FrameBufferAttachment> colorTextures, @Nullable FrameBufferAttachment depthTexture,
                                  int mipOffset, CallbackInfoReturnable<Integer> cir) {
        if (depthTexture != null && depthTexture.fboMipLevel() == 0) FboTracker.record(cir.getReturnValueI(), depthTexture.glId());
    }

    @Inject(method = "destroyFbo", at = @At("HEAD"))
    private void metalforge$forget(FrameBufferCache.CacheKey key, CallbackInfo ci) {
        if (this.cache.containsKey(key)) FboTracker.forget(this.cache.getInt(key));
    }
}
