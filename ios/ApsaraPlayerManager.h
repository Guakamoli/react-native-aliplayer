#import <React/RCTViewManager.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import "ApsaraPlayerView.h"

@interface ApsaraPlayerManager : RCTViewManager  <RCTBridgeModule>


@end

@interface ApsaraMediaManager : RCTEventEmitter  <AliVodMediaLoaderStatusDelegate, RCTBridgeModule>


@end

