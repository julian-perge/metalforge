// MetalForge: building mcopt's mcmetal.m with the macOS 14.4 SDK (Command Line Tools) for macOS 15 at runtime.
// MTLResidencySet is macOS 15+: declare what mcmetal.m uses and look the descriptor class up at runtime (no link-time symbol).
#import <Metal/Metal.h>
#if !defined(MAC_OS_VERSION_15_0)
@protocol MTLAllocation <NSObject>
@end
@protocol MTLResidencySet <NSObject>
- (void)addAllocation:(id<MTLAllocation>)allocation;
- (void)removeAllocation:(id<MTLAllocation>)allocation;
- (void)commit;
- (void)requestResidency;
- (void)endResidency;
@end
@interface MTLResidencySetDescriptor : NSObject <NSCopying>
@property (copy, nonatomic) NSString *label;
@property (nonatomic) NSUInteger initialCapacity;
@end
@protocol MTLDeviceResidency
- (id<MTLResidencySet>)newResidencySetWithDescriptor:(MTLResidencySetDescriptor *)desc error:(NSError **)error;
@end
@protocol MTLCommandQueueResidency
- (void)addResidencySet:(id<MTLResidencySet>)residencySet;
- (void)removeResidencySet:(id<MTLResidencySet>)residencySet;
@end
#define METALFORGE_RESIDENCY_DESC() ((MTLResidencySetDescriptor *)[NSClassFromString(@"MTLResidencySetDescriptor") new])
#else
// With the macOS 15 SDK or newer, MTLDevice and MTLCommandQueue declare the residency methods themselves, so the casts in
// mcmetal.m name those protocols instead of the stand-ins above.
#define MTLDeviceResidency MTLDevice
#define MTLCommandQueueResidency MTLCommandQueue
#define METALFORGE_RESIDENCY_DESC() [MTLResidencySetDescriptor new]
#endif
