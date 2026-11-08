# MetalForge

A NeoForge mod that replaces Minecraft's OpenGL renderer with a native Metal renderer on Apple Silicon Macs.

## Language

**Metal memory**:
GPU-visible memory MetalForge holds at a point in time, outside the Java heap. It is the number the memory caps are measured by.
_Avoid_: VRAM, native memory

**Staging block**:
A 4 MiB shared-memory block that texture and buffer uploads are written into before the GPU copies them to their destination. Finished blocks are reused up to a limit and freed beyond it.
_Avoid_: Upload buffer, scratch buffer

**Rename**:
Moving a buffer onto other memory when a write replaces all of its contents while the GPU may still read the old contents, so earlier commands see the old data.
_Avoid_: Orphan, double-buffer

**Spare**:
Memory a buffer left behind in a rename, kept after the GPU finishes with it so the next rename can reuse it.
_Avoid_: Backup buffer, free buffer

**Atlas stitch**:
Uploading every sprite of a texture atlas in one submit, which is the largest staging peak of a session. It happens at startup and on every resource reload (F3+T).
_Avoid_: Atlas load

**OpenGL fallback**:
Running the stock OpenGL renderer instead of Metal. It is chosen when Iris is installed, on non-Apple-Silicon hardware, or with `-Dmetalforge.metal=false`.
_Avoid_: Compatibility mode

**Hitch**:
A displayed frame that appears three or more display refreshes after the previous one, counted in the `3x+` bucket of the `[shown]` log. Hitch share is the fraction of displayed frames that are hitches.
_Avoid_: Stutter, lag spike

**Frame time**:
The time between two consecutive frames leaving the render thread, in milliseconds. Unlike a hitch, it does not depend on the display's refresh rate.
_Avoid_: Frametime

**Scene**:
A fixed world state and camera position captured under both Metal and the OpenGL fallback, so the two screenshots can be compared.
_Avoid_: Test world, shot

**A/B check**:
Comparing the screenshots of a scene taken under Metal and under the OpenGL fallback to find rendering differences.
_Avoid_: Visual test, diff test
