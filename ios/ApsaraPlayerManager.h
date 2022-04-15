#import <React/RCTViewManager.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import "ApsaraPlayerView.h"

@interface ApsaraPlayerManager : RCTViewManager  <RCTBridgeModule>

@property (nonatomic, strong) ApsaraPlayerView *playerView;

@end

@interface ApsaraMediaManager : RCTEventEmitter  <RCTBridgeModule>


@end

