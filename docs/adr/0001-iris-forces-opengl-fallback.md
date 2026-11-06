# Iris installed means OpenGL fallback

Iris draws with OpenGL directly and casts the graphics device to the OpenGL one at startup, so it cannot run on the Metal backend. When Iris is installed MetalForge disables the Metal renderer and logs a warning, instead of trying to emulate OpenGL calls for Iris. Players who want Metal must remove Iris.
