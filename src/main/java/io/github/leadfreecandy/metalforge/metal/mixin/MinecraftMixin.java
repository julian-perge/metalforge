package io.github.leadfreecandy.metalforge.metal.mixin;

import com.mojang.renderpearl.api.device.GpuBackend;
import io.github.leadfreecandy.metalforge.metal.MetalBackend;
import net.minecraft.client.PreferredGraphicsApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the Metal backend in front of OpenGL and Vulkan in every list Minecraft tries, whatever the graphics API option says.
 * -Dmetalforge.metal=false keeps the vanilla list, and so does an installed Iris, which only works with OpenGL.
 */
@Mixin(PreferredGraphicsApi.class)
public abstract class MinecraftMixin {
	@Inject(method = "getBackendsToTry", at = @At("RETURN"), cancellable = true)
	private void metalforge$preferMetal(CallbackInfoReturnable<GpuBackend[]> cir) {
		// The native backend is built for Apple Silicon only.
		if (!Boolean.parseBoolean(System.getProperty("metalforge.metal", "true")) || !System.getProperty("os.name", "").startsWith("Mac")
			|| !"aarch64".equals(System.getProperty("os.arch"))) return;
		try {
			if (net.neoforged.fml.loading.FMLLoader.getCurrent().getLoadingModList().getModFileById("iris") != null) {
				com.mojang.logging.LogUtils.getLogger().warn("MetalForge Metal: Iris is installed (OpenGL only); using OpenGL. Remove Iris to use Metal.");
				return;
			}
		} catch (Throwable ignored) {
		}
		GpuBackend[] fallbacks = cir.getReturnValue();
		GpuBackend[] backends = new GpuBackend[fallbacks.length + 1];
		backends[0] = new MetalBackend();
		System.arraycopy(fallbacks, 0, backends, 1, fallbacks.length);
		cir.setReturnValue(backends);
	}
}
