// Ported from mcopt (https://github.com/noahdunnagan/mcopt), Apache-2.0, Copyright 2026 Noah Dunnagan: the SDL Metal layer,
// late drawable acquire and present pacing. MetalForge adds smooth pacing (MetalPacer), the [shown] log and the frame log.
package io.github.leadfreecandy.metalforge.metal;

import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLMetal;

/**
 * A CAMetalLayer on the SDL window. The drawable is fetched at blit time rather than frame start: holding it for
 * the whole frame only adds latency and can stall on a drawable the display hasn't released yet.
 */
final class MetalSurface implements GpuSurfaceBackend {
	/** With vsync off, present only the last frame that makes each refresh (see mc_pace); -Dmetalforge.metal.pace=false presents every frame. */
	private static final boolean PACE = Boolean.parseBoolean(System.getProperty("metalforge.metal.pace", "true"));
	/** -Dmetalforge.metal.paceMarginMs: how long before a refresh the paced present must be done (the compositor's latch); default 2. */
	private static final double PACE_MARGIN_S = Double.parseDouble(System.getProperty("metalforge.metal.paceMarginMs", "2")) / 1000;
	/** -Dmetalforge.metal.drawables=2|3: the layer's maximumDrawableCount (default 3). */
	private static final int DRAWABLES = Integer.getInteger("metalforge.metal.drawables", 0);
	/**
	 * -Dmetalforge.metal.paceSync=true: the paced presents (vsync off) go out with display sync on, so each latches at a vblank. On
	 * the laptop's 120 Hz 5K display at ~1800 fps, paced presents without display sync reach the screen in pairs ~1.9 ms apart
	 * every 16.7 ms (a 60 Hz cadence); with vsync the grid is a clean 8.33 ms.
	 */
	private static final boolean PACE_SYNC = Boolean.getBoolean("metalforge.metal.paceSync");
	/**
	 * -Dmetalforge.metal.paceAdapt (default on): the pacer's margin learns the compositor's latch from scanout times (mc_pace in
	 * mcmetal.m): a paced frame shown more than half a refresh after the refresh it was aimed at adds lead, a frame on time takes a
	 * little away.
	 */
	static final boolean PACE_ADAPT = Boolean.parseBoolean(System.getProperty("metalforge.metal.paceAdapt", "true"));
	/** -Dmetalforge.metal.frameLog=true logs the percentiles of frame time (time between two presents) every 5 seconds. */
	private static final boolean FRAME_LOG = Boolean.parseBoolean(System.getProperty("metalforge.metal.frameLog", "false"));

	/** The surface the game presents to, for Pacing (the frame limiter mixin). */
	static volatile @Nullable MetalSurface active;

	private boolean paced;
	/** Smooth pacing (MetalPacer): every frame is presented, at a scheduled refresh, and the next frame waits for its beat. */
	private boolean smooth = MetalPacer.enabled(false);
	private final MetalPacer pacer = new MetalPacer();
	private final long ctx;
	private final MetalEncoder encoder;
	private final long view;
	private final long layer;
	private int frames;

	MetalSurface(long ctx, MetalEncoder encoder, long window) {
		this.ctx = ctx;
		this.encoder = encoder;
		this.view = SDLMetal.SDL_Metal_CreateView(window);
		this.layer = SDLMetal.SDL_Metal_GetLayer(this.view);
		active = this;
	}

	boolean smooth() {
		return this.smooth;
	}

	@Override
	public void configure(GpuSurface.Configuration config) {
		boolean vsync = config.presentMode() == GpuSurface.PresentMode.FIFO;
		this.smooth = MetalPacer.enabled(vsync);
		this.pacer.reset();
		this.paced = !vsync && !this.smooth && PACE;
		if (PACE_ADAPT) Native.paceAdapt(this.paced ? 1 : 0);
		Native.layerConfigure(this.ctx, this.layer, config.width(), config.height(), (vsync || this.smooth || PACE_SYNC && this.paced ? 1 : 0) | DRAWABLES << 8);
	}

	@Override
	public boolean isSuboptimal() {
		return false;
	}

	@Override
	public void acquireNextTexture() {
	}

	@Override
	public void blitFromTexture(CommandEncoderBackend commandEncoder, GpuTextureView textureView) {
		// The pacer and the smooth schedule follow the display the window is on.
		if ((this.frames++ & 63) == 0) Native.paceDisplay(Native.viewDisplay(this.view));
		long t0 = WaitStats.ON ? System.nanoTime() : 0;
		if (this.paced && !Native.pace(PACE_MARGIN_S)) {
			if (WaitStats.ON) WaitStats.wait(WaitStats.PACE, System.nanoTime() - t0, 0);
			return;
		}
		long t1 = WaitStats.ON ? System.nanoTime() : 0;
		if (WaitStats.ON && this.paced) WaitStats.wait(WaitStats.PACE, t1 - t0, 0);
		if (this.smooth) Native.presentTarget(this.pacer.target());
		if (MetalEncoder.PRESENT_ACQUIRE) {
			this.encoder.presentAcquire(this.layer, textureView); // no nextDrawable here: the present side acquires it
			return;
		}
		MetalEvents.Operation event = MetalEvents.begin("nextDrawable", -1, 0);
		long drawable;
		try {
			drawable = Native.layerNext(this.layer);
		} finally {
			MetalEvents.end(event);
		}
		if (WaitStats.ON) WaitStats.wait(WaitStats.DRAWABLE, System.nanoTime() - t1, 0);
		if (drawable == 0) return; // no drawable (window hidden): skip the frame's present
		this.encoder.presentTexture(drawable, textureView);
		this.encoder.afterGpuFinishes(() -> Native.release(drawable));
	}

	/** Runs after the frame's submit: the present itself went out with the frame's command buffer (see blitFromTexture). */
	@Override
	public void present() {
		if (this.smooth) this.pacer.endFrame();
		long t = System.nanoTime();
		if (t - this.shownLoggedAt > 5_000_000_000L) {
			// on-screen spacing of displayed frames (native shownWatch): 1 refresh apart = smooth, 2 = a refresh repeated, 3+ = hitch
			if (this.shownLoggedAt != 0) org.slf4j.LoggerFactory.getLogger("MetalForge").info("[shown] 1x={} 2x={} 3x+={} untimed={} smooth={}",
				Native.shown(1), Native.shown(2), Native.shown(3), Native.shown(0), this.smooth);
			else for (int k = 0; k < 4; k++) Native.shown(k);
			this.shownLoggedAt = t;
		}
		if (FRAME_LOG) this.recordFrame(t);
	}

	private long shownLoggedAt;

	/** Frame times in 0.1 ms bins up to 100 ms, so percentiles are exact to 0.1 ms. The last bin counts frames of 100 ms or more. */
	private final int[] frameBins = new int[1001];
	private long frameCount, frameMaxNs, lastFrameAt, frameLoggedAt;

	private void recordFrame(long t) {
		if (this.lastFrameAt != 0) {
			long ns = t - this.lastFrameAt;
			this.frameBins[(int) Math.min(ns / 100_000L, 1000L)]++;
			this.frameCount++;
			if (ns > this.frameMaxNs) this.frameMaxNs = ns;
		}
		this.lastFrameAt = t;
		if (t - this.frameLoggedAt > 5_000_000_000L) {
			if (this.frameLoggedAt != 0 && this.frameCount > 0) {
				org.slf4j.LoggerFactory.getLogger("MetalForge").info("[frame] n={} p50={} p95={} p99={} max={} ms",
					this.frameCount, this.framePercentile(0.50), this.framePercentile(0.95), this.framePercentile(0.99), String.format("%.1f", this.frameMaxNs / 1e6));
			}
			java.util.Arrays.fill(this.frameBins, 0);
			this.frameCount = 0;
			this.frameMaxNs = 0;
			this.frameLoggedAt = t;
		}
	}

	/** The upper edge of the bin holding the given percentile, in ms. The overflow bin reports the true maximum. */
	private String framePercentile(double p) {
		long target = (long) Math.ceil(this.frameCount * p), seen = 0;
		for (int i = 0; i < this.frameBins.length; i++) {
			seen += this.frameBins[i];
			if (seen >= target) return i == 1000 ? String.format("%.1f", this.frameMaxNs / 1e6) : String.format("%.1f", (i + 1) / 10.0);
		}
		return "0.0";
	}

	@Override
	public Collection<GpuSurface.PresentMode> supportedPresentModes() {
		return List.of(GpuSurface.PresentMode.IMMEDIATE, GpuSurface.PresentMode.FIFO);
	}

	@Override
	public void close() {
		if (active == this) active = null;
		SDLMetal.SDL_Metal_DestroyView(this.view);
	}
}
