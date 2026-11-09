package io.github.leadfreecandy.metalforge.metal;

/** -Dmetalforge.metal.residentAnim: see MetalDevice. The flag and the marker its mixin sets while a sprite animation's frames are created. */
public final class ResidentAnim {
	public static final boolean ON = Boolean.getBoolean("metalforge.metal.residentAnim");
	public static volatile boolean creating;

	private ResidentAnim() {
	}
}
