package io.github.leadfreecandy.metalforge.metal;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLVideo;

/** Native Metal rendering for Minecraft's renderpearl layer, next to Mojang's OpenGL and Vulkan (MoltenVK) backends. */
public final class MetalBackend implements GpuBackend {
	static {
		// METALFORGE_METAL_PROPS="k=v;k=v": backend tuning switches (metalforge.metal.*) from the environment, for harness runs.
		String env = System.getenv("METALFORGE_METAL_PROPS");
		if (env != null) {
			for (String kv : env.split(";")) {
				int eq = kv.indexOf('=');
				if (eq > 0) System.setProperty(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
			}
		}
	}

	@Override
	public String getName() {
		return "Metal (MetalForge)";
	}

	@Override
	public void loadLibrary() {
	}

	@Override
	public void unloadLibrary() {
	}

	@Override
	public long createWindow(@Nullable String title, int width, int height, long flags) {
		return SDLVideo.SDL_CreateWindow(title, width, height, SDLVideo.SDL_WINDOW_METAL | flags);
	}

	@Override
	public GpuDevice createDevice(GpuDebugOptions debugOptions) throws BackendCreationException {
		return new FrontendGpuDevice(new MetalDevice());
	}
}
