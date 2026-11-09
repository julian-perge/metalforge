package io.github.leadfreecandy.metalforge.mixin.infra;

import com.google.common.collect.ImmutableMap;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import io.github.leadfreecandy.metalforge.shader.ShaderPatches;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Infrastructure (always applied with Sodium): routes every shader source vanilla loads through {@link ShaderPatches}. Sodium
 * 0.9.3 loads its terrain shaders through vanilla's ShaderManager, so this is where its GLSL can be patched.
 */
@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
    @Redirect(method = "loadShader", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"))
    private static String metalforge$patch(Resource resource, Identifier location, Resource sameResource, ShaderType type,
                                           ImmutableMap.Builder<?, String> output) throws IOException {
        return ShaderPatches.apply(location.toString(), resource.readAllAsString());
    }
}
