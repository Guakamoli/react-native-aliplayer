#import "ApsaraPlayerView.h"
static NSMutableArray *videos;
@interface ApsaraPlayerView ()
@property (nonatomic, strong) UIView *playerView;
@end


@implementation ApsaraPlayerView
{
    NSDictionary *_src;
    BOOL _paused;
    BOOL _muted;
    BOOL _repeat;
    BOOL _prepared;
    BOOL _cacheEnable;
    float _seek;
    AVPScalingMode _resizeMode;
    int _positionTimerIntervalMs;
    int _maxBufferDuration;
    int _highBufferDuration;
    int _startBufferDuration;
    NSMutableDictionary * _videoItem;
    
    AliMediaDownloader *_downloader;
    RCTPromiseResolveBlock _downloaderResolver;
    RCTPromiseRejectBlock _downloaderRejector;
}

- (void) layoutSubviews {
    [super layoutSubviews];
    for (UIView* view in self.subviews) {
        [view setFrame: self.bounds];
    }
}

- (void) dealloc {
    [self destroy];
}
- (void)removeFromSuperview
{
    [self destroy];
    [super removeFromSuperview];
    
}
- (AliPlayer *)player {
    _player.autoPlay = NO;
    _player.scalingMode = _resizeMode;
    _player.rate = 1;
    _player.delegate = self;
    _player.playerView = self.playerView;
    
    return _player;
}

- (UIView *)playerView {
    if (!_playerView) {
        _playerView = [[UIView alloc] init];
    }
    return _playerView;
}

- (AVPVidStsSource *) stsSource:(NSDictionary *)opts {
    AVPVidStsSource *source = [[AVPVidStsSource alloc] init];
    source.vid = opts[@"vid"];
    source.region = opts[@"region"];
    source.securityToken = opts[@"securityToken"];
    source.accessKeyId = opts[@"accessKeyId"];
    source.accessKeySecret = opts[@"accessKeySecret"];
    return source;
}

- (AVPVidAuthSource *) authSource:(NSDictionary *)opts {
    AVPVidAuthSource *source = [[AVPVidAuthSource alloc] init];
    source.vid = opts[@"vid"];
    source.region = opts[@"region"];
    source.playAuth = opts[@"playAuth"];
    return source;
}
- (AliPlayer*)getORcreateVideo {
    AliPlayer * video;
    for (int i = 0; i < [videos count]; i++) {
        NSMutableDictionary *item = videos[i];
        if (item[@"isUsed"] == @NO) {
            _videoItem = item;
            video = item[@"video"];
            item[@"isUsed"] = @YES;
            [item setObject:[NSNumber numberWithInteger:[item[@"usedCount"] intValue] + 1] forKey:@"usedCount"];
            break;
        }
    }
    if (video) {
        [video stop];
    } else {
        video = [self createVideo];
    }
    return video;
}

- (AliPlayer*)createVideo {
    AliPlayer * video =  [[AliPlayer alloc] init];
    NSMutableDictionary* item = [NSMutableDictionary new];
    [item setObject: video forKey:@"video"];
    [item setObject: [NSNumber numberWithInteger: 1] forKey:@"usedCount"];
    [item setObject: @YES forKey:@"isUsed"];
    _videoItem = item;
    [videos addObject: item];
    return video;
}
- (void)setSource: (NSDictionary *)source {
    _src = source;
    if (!_src[@"uri"]) {
        return;
    }
    
    int maxVideoNum = [_src[@"maxVideoNum"] intValue];
    if (!maxVideoNum) {
        maxVideoNum = 10;
    }
    AliPlayer *video;
    if (!videos) {
        videos = [NSMutableArray new];
    }
    if ([videos count] < maxVideoNum) {
        video = [self createVideo];
    } else {
        video = [self getORcreateVideo];
    }
    if (_src[@"uri"] && ![(NSString *)_src[@"uri"] isEqualToString:@""]) {
        [video setUrlSource:[[AVPUrlSource alloc] urlWithString:_src[@"uri"]]];
    } else if (_src[@"sts"] && _src[@"sts"][@"vid"]) {
        [video setStsSource: [self stsSource:_src[@"sts"]]];
    } else if (_src[@"auth"] && _src[@"auth"][@"vid"]) {
        [video setAuthSource: [self authSource:_src[@"auth"]]];
    }
    
    _player = video;
    
    [self addSubview: self.player.playerView];
    //先获取配置
    AVPConfig *config = [_player getConfig];
    BOOL cacheEnable = _src[@"cacheEnable"];
    NSString *cachePath = _src[@"cachePath"];
    BOOL muted = _src[@"muted"];
    BOOL repeat = _src[@"repeat"];

    // // 最大缓冲区时长。单位ms。播放器每次最多加载这么长时间的缓冲数据。
    config.maxBufferDuration = 10000;
    // //高缓冲时长。单位ms。当网络不好导致加载数据时，如果加载的缓冲时长到达这个值，结束加载状态。
    config.highBufferDuration = 3000;
    // // 起播缓冲区时长。单位ms。这个时间设置越短，起播越快。也可能会导致播放之后很快就会进入加载状态。
    config.startBufferDuration = 500;
    config.positionTimerIntervalMs = _positionTimerIntervalMs;
    //其他设置
    //设置配置给播放器
    [_player setConfig:config];
     AVPCacheConfig *cacheConfig = [[AVPCacheConfig alloc] init];
     //开启缓存功能
     cacheConfig.enable = cacheEnable;
     //能够缓存的单个文件最大时长。超过此长度则不缓存
     cacheConfig.maxDuration = 300;
     //缓存目录的位置，需替换成app期望的路径
     if (cachePath) {
       cacheConfig.path = cachePath;
     }
     //缓存目录的最大大小。超过此大小，将会删除最旧的缓存文件
     cacheConfig.maxSizeMB = 20 * 1024;
     //设置缓存配置给到播放器
     [_player setCacheConfig:cacheConfig];
  [_player prepare];
  _prepared = YES;
  if (!_paused) {
    _player.autoPlay = YES;
  }
  _player.muted = muted;
  _player.loop = repeat;
}
- (void)didMoveToSuperview
{
    [super didMoveToSuperview];
    if (!self.superview) {
        [self destroy];
    }
}
- (void)setPaused:(BOOL)paused {
    if (paused) {
        _player.autoPlay = NO;
        if (_player) {
            [_player pause];
        }
        
    } else {
        _player.autoPlay = YES;
        if (_player) {
            [_player start];
        }
        
    }
    
    _paused = paused;
}

- (void)setResizeMode:(NSString*)mode
{
    if ([mode isEqual: @"contain"]) {
        if (_player) {
            _player.scalingMode = AVP_SCALINGMODE_SCALEASPECTFIT;
            
        }
        _resizeMode = AVP_SCALINGMODE_SCALEASPECTFIT;
        
    } else if ([mode isEqual: @"cover"]) {
        if (_player) {
            _player.scalingMode = AVP_SCALINGMODE_SCALEASPECTFILL;
        }
        _resizeMode = AVP_SCALINGMODE_SCALEASPECTFILL;
        
    }
}

- (void)setSeek: (float)seek {
    _seek = seek;
    if (_player) {
        [_player seekToTime:seek seekMode:AVP_SEEKMODE_ACCURATE];
    }
    
}

- (void)setMaxBufferDuration: (int)maxBufferDuration {
    _maxBufferDuration = maxBufferDuration;
    if (_player) {
        AVPConfig *config = [_player getConfig];
        config.maxBufferDuration = _maxBufferDuration;
        [_player setConfig:config];
    }
    
}
- (void)setHighBufferDuration: (int)highBufferDuration {
    _highBufferDuration = highBufferDuration;
    if (_player) {
        AVPConfig *config = [_player getConfig];
        config.highBufferDuration = _highBufferDuration;
        [_player setConfig:config];
    }
    
}
- (void)setStartBufferDuration: (int)startBufferDuration {
    _startBufferDuration = startBufferDuration;
    if (_player) {
        AVPConfig *config = [_player getConfig];
        config.startBufferDuration = _startBufferDuration;
        [_player setConfig:config];
    }
    
}
- (void)setPositionTimerIntervalMs: (int)positionTimerIntervalMs {
    _positionTimerIntervalMs = positionTimerIntervalMs;
    if (_player) {
        AVPConfig *config = [_player getConfig];
        config.positionTimerIntervalMs = _positionTimerIntervalMs;
        [_player setConfig:config];
    }
    
}

- (void)setMuted: (bool)muted {
   if (_player) {
       _player.muted = muted;
   }

   _muted = muted;
}

- (void)setVolume: (float)volume {
    if (_player) {
        _player.volume = volume;
    }
    
}
- (void)setCacheEnable: (bool)cacheEnable {
    _cacheEnable = cacheEnable;
}

- (void)setRepeat: (bool)repeat {
    _repeat = repeat;
    if (_player) {
        _player.loop = repeat;
    }
    
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}
-(void)onPlayerEvent:(AliPlayer*)player eventWithString:(AVPEventWithString)eventWithString description:(NSString *)description {
    if (eventWithString == EVENT_PLAYER_CACHE_SUCCESS) {
        //缓存成功事件。
        NSLog(@"%@", description);
        
    }else if (eventWithString == EVENT_PLAYER_CACHE_ERROR) {
        //缓存失败事件。
        NSLog(@"%@", description);
        
    }
}
-(void)onPlayerEvent:(AliPlayer*)player eventType:(AVPEventType)eventType {
    switch (eventType) {
        case AVPEventPrepareDone:
            if (self.onVideoLoad) {
                self.onVideoLoad(@{
                    @"duration": [NSNumber numberWithFloat:_player.duration],
                    @"currentTime": [NSNumber numberWithFloat:_player.currentPosition]});
            }
            if (_player) {
                if (_seek) {
                    [_player seekToTime:_seek seekMode:AVP_SEEKMODE_ACCURATE];
                }
                if (_paused) {
                    [_player pause];
                }
            }
            
            
            break;
        case AVPEventAutoPlayStart:
            if (self.onVideoLoad) {
                self.onVideoLoad(@{
                    @"duration": [NSNumber numberWithFloat:_player.duration],
                    @"currentTime": [NSNumber numberWithFloat:_player.currentPosition]});
            }
            if (_player) {
                if (_seek) {
                    [_player seekToTime:_seek seekMode:AVP_SEEKMODE_ACCURATE];
                }
                if (_paused) {
                    [_player pause];
                }
            }
            break;
        case AVPEventFirstRenderedStart:
            if (self.onVideoFirstRenderedStart) {
                self.onVideoFirstRenderedStart(@{
                    @"duration": [NSNumber numberWithFloat:_player.duration],
                    @"currentTime": [NSNumber numberWithFloat:_player.currentPosition]});
            }
            break;
        case AVPEventSeekEnd:
            if (self.onVideoSeek) {
                self.onVideoSeek(@{
                    @"currentTime": [NSNumber numberWithFloat:_player.currentPosition]});
            }
            break;
        case AVPEventCompletion:
            if (self.onVideoEnd) {
                self.onVideoEnd(@{});
            }
            break;
            // case ...
        default:
            break;
    }
}

- (void)onCurrentPositionUpdate:(AliPlayer*)player position:(int64_t)position {
    if (self.onVideoProgress) {
        self.onVideoProgress(@{@"currentTime": [NSNumber numberWithFloat:position]});
    }
}

- (void)onError:(id)instance errorModel:(AVPErrorModel *)errorModel {
    if ([instance isKindOfClass:[AliPlayer class]]) {
        [self destroyVideo];
        _videoItem = nil;
        _player = nil;
        [self removeFromSuperview];
        if (self.onVideoError) {
            self.onVideoError(@{
                @"message": errorModel.message,
                @"code": [NSNumber numberWithInteger: errorModel.code]
            });
        }
    } else if ([instance isKindOfClass:[AliMediaDownloader class]]) {
        NSLog(@"");
        _downloaderRejector(@"ERROR_SAVE_FAILED",
                            [NSString stringWithFormat:@"%@:%@", @(errorModel.code), errorModel.message],
                            nil);
        [self destroyDownloader];
    }
}

# pragma save
- (void)save:(NSDictionary *)options
     resolve:(RCTPromiseResolveBlock)resolve
      reject:(RCTPromiseRejectBlock)reject {
    _downloaderResolver = resolve;
    _downloaderRejector = reject;
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    
    _downloader = [[AliMediaDownloader alloc] init];
    [_downloader setDelegate:self];
    [_downloader setSaveDirectory: [paths firstObject]];
    
    NSDictionary *opts = options ? options : _src;
    if (opts[@"sts"] && opts[@"sts"][@"vid"]) {
        [_downloader prepareWithVid:[self stsSource:opts[@"sts"]]];
    } else if (opts[@"auth"] && opts[@"auth"][@"vid"]) {
        [_downloader prepareWithPlayAuth:[self authSource:opts[@"auth"]]];
    } else {
        reject(@"ERROR_SAVE_FAILED", @"invalid source", nil);
    }
}

- (void)destroyDownloader {
    [_downloader destroy];
    _downloader = nil;
}

-(void)onPrepared:(AliMediaDownloader *)downloader mediaInfo:(AVPMediaInfo *)info {
    NSArray<AVPTrackInfo*>* tracks = info.tracks;
    [downloader selectTrack:[tracks objectAtIndex:0].trackIndex];
    [downloader start];
}

-(void)onCompletion:(AliMediaDownloader *)downloader {
    _downloaderResolver(@{@"uri": downloader.downloadedFilePath});
    [self destroyDownloader];
}
- (void)destroyVideo {
    // 从队列中删除
    NSInteger count = [videos count];
    for (NSInteger index = (count - 1); index >= 0; index--) {
        NSMutableDictionary *item = videos[index];
        if ([item[@"video"] isEqual:_player]) {
            [_player destroy];
            [videos removeObjectAtIndex:index];
        }
    }
    
}
# pragma destroy
- (void)destroy {
    [_player stop];
    if (_player && _player.delegate == self) {
        _player.delegate = nil;
        _player.playerView = nil;
        _videoItem[@"isUsed"] = @NO;
        if ([_videoItem[@"usedCount"] intValue] > 50) {
            [self destroyVideo];
        }
        _player = nil;
        _videoItem = nil;
    }
    if (_downloader) {
        [_downloader destroy];
    }
}
@end
