#import <React/RCTUIManager.h>
#import <React/RCTBridge.h>
#import "ApsaraPlayerManager.h"

@implementation ApsaraMediaManager
RCT_EXPORT_MODULE()
- (NSArray<NSString *> *)supportedEvents {
    return @[@"onError", @"onCompleted", @"onCanceled"];
}
RCT_REMAP_METHOD(setGlobalSettings, options:(NSDictionary *)options
    resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    int64_t maxBufferMemoryKB = [options[@"maxBufferMemoryKB"] longLongValue]; // 1024 * 10;
    NSString *localCacheDir = options[@"localCacheDir"]; // @"";
    int64_t expireMin = [options[@"expireMin"] longLongValue]; // 24 * 60 * 2;
    int64_t maxCapacityMB = [options[@"maxCapacityMB"] longLongValue]; // 1024 * 5;
    int64_t freeStorageMB = [options[@"freeStorageMB"] longLongValue]; // 1024 * 5

    [AliPlayerGlobalSettings enableLocalCache:true maxBufferMemoryKB: maxBufferMemoryKB localCacheDir: localCacheDir];
    [AliPlayerGlobalSettings setCacheFileClearConfig: expireMin maxCapacityMB: maxCapacityMB freeStorageMB: freeStorageMB];
    [[AliMediaLoader shareInstance] setAliMediaLoaderStatusDelegate:self];

}

RCT_REMAP_METHOD(preLoadUrl, url:(NSString *)url duration:(nonnull NSNumber *)duration  resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    int64_t durationtrans = [duration longLongValue];
  [[AliMediaLoader shareInstance] load: url duration: durationtrans];
}

RCT_REMAP_METHOD(cancelPreLoadUrl, url:(NSString *)url resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  [[AliMediaLoader shareInstance] cancel: url];
}
/**
 @brief 错误回调
 @param url 加载url
 @param code 错误码
 @param msg 错误描述
 */
- (void)onError:(NSString *)url code:(int64_t)code msg:(NSString *)msg {
    [self sendEventWithName:@"onError" body:@{@"url":url, @"msg": msg}];
};

/**
 @brief 完成回调
 @param url 加载url
 */
- (void)onCompleted:(NSString *)url {
    [self sendEventWithName:@"onCompleted" body:@{@"url":url}];
};

/**
 @brief 取消回调
 @param url 加载url
 */
- (void)onCanceled:(NSString *)url {
    [self sendEventWithName:@"onCanceled" body:@{@"url":url}];
};


@end

@implementation ApsaraPlayerManager

RCT_EXPORT_MODULE()

- (UIView *)view {
  return [ApsaraPlayerView new];;
}

- (dispatch_queue_t)methodQueue{
  return self.bridge.uiManager.methodQueue;
}

RCT_EXPORT_VIEW_PROPERTY(repeat, BOOL)
RCT_EXPORT_VIEW_PROPERTY(paused, BOOL)
RCT_EXPORT_VIEW_PROPERTY(muted, BOOL)
RCT_EXPORT_VIEW_PROPERTY(volume, float)
RCT_EXPORT_VIEW_PROPERTY(seek, float)
RCT_EXPORT_VIEW_PROPERTY(positionTimerIntervalMs, int)

RCT_EXPORT_VIEW_PROPERTY(maxBufferDuration, int)
RCT_EXPORT_VIEW_PROPERTY(highBufferDuration, int)
RCT_EXPORT_VIEW_PROPERTY(startBufferDuration, int)

RCT_EXPORT_VIEW_PROPERTY(cacheEnable, BOOL)
RCT_EXPORT_VIEW_PROPERTY(spaceMaxVideoNum, int)

RCT_EXPORT_VIEW_PROPERTY(nameSapce, NSString)

RCT_EXPORT_VIEW_PROPERTY(resizeMode, NSString);
// RCT_EXPORT_VIEW_PROPERTY(cacheMaxSizeMB, int)


RCT_EXPORT_VIEW_PROPERTY(source, NSDictionary)

RCT_EXPORT_VIEW_PROPERTY(onVideoLoad, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onVideoSeek, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onVideoFirstRenderedStart, RCTDirectEventBlock)

RCT_EXPORT_VIEW_PROPERTY(onVideoEnd, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onVideoError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onVideoProgress, RCTDirectEventBlock)

RCT_REMAP_METHOD(save,
    options:(NSDictionary *)options
    reactTag:(nonnull NSNumber *)reactTag
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject) {
  [self.bridge.uiManager prependUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, ApsaraPlayerView *> *viewRegistry) {
    ApsaraPlayerView *view = viewRegistry[reactTag];
    if (![view isKindOfClass:[ApsaraPlayerView class]]) {
      RCTLogError(@"Invalid view returned from registry, expecting ApsaraPlayerView, got: %@", view);
    } else {
      [view save:options resolve:resolve reject:reject];
    }
  }];
}

static NSString *CaheUrlHashHandle(NSString *url) {
    return @"xxx";
}


RCT_REMAP_METHOD(destroy, reactTag:(nonnull NSNumber *)reactTag) {
  [self.bridge.uiManager prependUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, ApsaraPlayerView *> *viewRegistry) {
    ApsaraPlayerView *view = viewRegistry[reactTag];
    if (![view isKindOfClass:[ApsaraPlayerView class]]) {
      RCTLogError(@"Invalid view returned from registry, expecting ApsaraPlayerView, got: %@", view);
    } else {
      [view destroy];
    }
  }];
}

+ (BOOL)requiresMainQueueSetup {
  return YES;
}
@end
