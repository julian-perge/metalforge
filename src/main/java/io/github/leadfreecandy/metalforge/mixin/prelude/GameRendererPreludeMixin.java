package io.github.leadfreecandy.metalforge.mixin.prelude;

import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.GpuTexture;
import io.github.leadfreecandy.metalforge.opt.IrisCompat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * GameRenderer.render clears the main target, then (when the lightmap needs an update) renders the lightmap into its own
 * texture, then the level's frame graph clears the main target again, fully, with the fog colour. When the level is
 * rendered the first clear is dead: drop it. That removes a full-screen clear and, on lightmap frames, a main -> lightmap
 * -> main render-target round trip. Kept whenever the level isn't rendered (menus, loading) or an Iris shader pack is active.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererPreludeMixin {
    @Shadow @Final private GameRenderState gameRenderState;

    @Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/renderpearl/api/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/renderpearl/api/textures/GpuTexture;D)V"))
    private void metalforge$skipDeadClear(CommandEncoder encoder, GpuTexture color, Vector4fc clearColor, GpuTexture depth, double clearDepth) {
        boolean levelWillClear = this.gameRenderState.shouldRenderLevel && !IrisCompat.shaderPackInUse();
        if (!levelWillClear) encoder.clearColorAndDepthTextures(color, clearColor, depth, clearDepth);
    }
}
