// MetalForge additions to mcopt's native Metal backend (mcmetal.m) that need AppKit or NeoForge's render pass API.
// SDL creates the window's CAMetalLayer (SDL_Metal_CreateView), so nothing here sets up the window.
#import <Cocoa/Cocoa.h>
#import "sdk14compat.h"
#import "mcmetal.h"

// NeoForge's RenderPass.setViewport. Metal's origin is top left, like Vulkan's, so the rectangle goes through as given.
void evo_r_viewport(Enc *enc, int x, int y, int w, int h) {
	if (!enc->render) return;
	[enc->render setViewport:(MTLViewport) {x, y, w, h, 0.0, 1.0}];
}

// The CGDirectDisplayID of the screen the view's window is on (0 if unknown), for the pacer's display link.
unsigned evo_view_display(long nsviewPtr) {
	NSView *view = (NSView *) nsviewPtr;
	NSScreen *screen = view.window.screen;
	if (screen == nil) return 0;
	NSNumber *n = screen.deviceDescription[@"NSScreenNumber"];
	return n ? n.unsignedIntValue : 0;
}
