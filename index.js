import React from 'react';
import {
  View,
  Platform,
  StyleSheet,
  NativeModules,
  NativeEventEmitter,
  findNodeHandle,
  AppState,
  requireNativeComponent,
} from 'react-native';
import PropTypes from 'prop-types';

export default class ApsaraPlayer extends React.Component {
  get _module() {
    return Platform.OS === 'ios'
      ? NativeModules.ApsaraPlayerManager
      : NativeModules.ApsaraPlayerModule;
  }
  constructor (props){
    super(props)
    this.paused = props.paused
    this.state= {
      show: !!props.source,
      source: {
        cacheEnable: props.cacheEnable,
        cachePath: props.cachePath,
        maxVideoNum: props.maxVideoNum || 5,
        muted: props.muted,
        repeat: props.repeat,
        ...props.source, 
      },
      paused: props.paused,
      muted: props.muted,
      positionMillis: props.positionMillis
    }
  }
  componentDidMount (){
    AppState.addEventListener('change', this._handleAppStateChange);
  }
  _handleAppStateChange = (state)=> {
    if (state !== 'active') {
      this.setState({
        paused: true
      })
    } else if (!this.paused){
        this.setState({
          paused: false
        })
    }
  }
  componentWillUnmount() {
    try {
      AppState.removeEventListener('change', this._handleAppStateChange);

      if (this.t) {
        clearTimeout(this.t)
      }
      if (findNodeHandle(this._player)) {
        this._module?.destroy?.(findNodeHandle(this._player));
      }
    } catch (e) {
      
    }

  }

  setNativeProps(nativeProps) {
    this._player.setNativeProps(nativeProps);
  }
  loadAsync =(source, options)=> {
    this.paused = !options.shouldPlay
    this.setState({
      paused: !options.shouldPlay,
      muted: options.muted,
      show: true,
      source: {
        ...source,
        cacheEnable: this.props.cacheEnable,
        cachePath: this.props.cachePath,
        maxVideoNum: this.props.maxVideoNum || 5,
        muted: options.muted,
        repeat: this.props.repeat
      },
    })
  }
  playAsync =()=> {
    this.paused = false
    this.setState({
      paused: false
    })
  }
  setIsMutedAsync =(muted) =>{
    this.setState({
      muted: muted
    })
  }
  pauseAsync =()=>{
    this.paused  = true
    this.setState({
      paused: true
    })
  }
  unloadAsync =()=>{
    if (this.t) {
      clearTimeout(this.t)
    }
    try {
      if (this.t) {
        clearTimeout(this.t)
      }
      if (findNodeHandle(this._player)) {
        this._module?.destroy?.(findNodeHandle(this._player));
      }
    } catch (e) {
      
    }
    this.paused  = false
    this.setState({
      paused: false,
      // source: null,
      show: false
    })

  }

  playFromPositionAsync = time => {
    if (isNaN(time)) throw new Error('Specified time is not a number');
    this.setNativeProps({seek: time});
  };

  _onLoad = event => {
    if (this.props.onLoad) {
      this.props.onLoad({durationMillis: event.nativeEvent?.duration});
    }
  };

  _onError = event => {
    if (this.props.onError) {
      this.props.onError(event.nativeEvent);
    }
  };

  _onProgress = event => {
    if (this.props.onPlaybackStatusUpdate) {
      this.props.onPlaybackStatusUpdate({positionMillis: event.nativeEvent?.currentTime});
    }
  };
  _onVideoFirstRenderedStart = event => {
    if (this.props.onVideoFirstRenderedStart) {
      this.props.onVideoFirstRenderedStart(event.nativeEvent);
    }
  };
  _onSeek = event => {
    if (this.props.onSeek) {
      this.props.onSeek(event.nativeEvent);
    }
  };

  save = options => {
    return this._module.save(options, findNodeHandle(this._player));
  };

  render() {
    const style = [styles.base, this.props.style];
    const {show} =this.state
    return (
      <View style={[style, {display: this.state.source ? "flex": "none"}]}>
        {show? (
          <RNApsaraPlayer
          ref={r => {
            this._player = r;
          }}
          style={StyleSheet.absoluteFill}
          source={this.state.source}
          paused={this.state.paused}
          repeat={this.props.repeat}
          volume={this.props.volume}
          positionTimerIntervalMs={this.props.progressUpdateIntervalMillis}
          muted={this.state.muted}
          seek={this.props.positionMillis}
          onVideoEnd={this.props.onEnd}
          resizeMode={this.props.resizeMode}
          onVideoLoad={this._onLoad}
          onVideoSeek={this._onSeek}
          onVideoError={this._onError}
          onVideoProgress={this._onProgress}
          onVideoFirstRenderedStart={this._onVideoFirstRenderedStart}
          cacheEnable={this.props.cacheEnable}
          cacheMaxDuration={this.props.cacheMaxDuration}
          cacheMaxSizeMB={this.props.cacheMaxSizeMB}
          startBufferDuration={this.props.startBufferDuration}
          highBufferDuration={this.props.highBufferDuration}
          maxBufferDuration={this.props.maxBufferDuration}
          />     
        ): null}
  
      </View>
    );
  }
}

ApsaraPlayer.defaultProps = {
  volume: 1,
  muted: false,
  paused: false,
  repeat: true,
  cacheEnable: false,
  cacheMaxDuration: 100,
  cacheMaxSizeMB: 200,
  startBufferDuration: 500,
  highBufferDuration: 3000,
  maxBufferDuration: 10000,
  progressUpdateIntervalMillis: 30,
  resizeMode: 'contain',
  maxVideoNum: 5,
}

ApsaraPlayer.propTypes = {
  repeat: PropTypes.bool,
  paused: PropTypes.bool,
  muted: PropTypes.bool,
  volume: PropTypes.number,
  source: PropTypes.shape({
    uri: PropTypes.string,
    sts: PropTypes.shape({
      vid: PropTypes.string,
      region: PropTypes.string,
      accessKeyId: PropTypes.string,
      accessKeySecret: PropTypes.string,
      securityToken: PropTypes.string,
    }),
    auth: PropTypes.shape({
      vid: PropTypes.string,
      region: PropTypes.string,
      playAuth: PropTypes.string,
    }),
  }),
  onEnd: PropTypes.func,
  onLoad: PropTypes.func,
  onSeek: PropTypes.func,
  onError: PropTypes.func,
  onPlaybackStatusUpdate: PropTypes.func,
  onVideoFirstRenderedStart: PropTypes.func,
  cacheEnable: PropTypes.bool,
  cacheMaxDuration: PropTypes.number,
  cacheMaxSizeMB: PropTypes.number,
  startBufferDuration: PropTypes.number,
  highBufferDuration: PropTypes.number,
  maxBufferDuration: PropTypes.number,
  progressUpdateIntervalMillis: PropTypes.number,
  resizeMode: PropTypes.string,
  path:  PropTypes.string,
};

const styles = StyleSheet.create({
  base: {
    overflow: 'hidden',
    backgroundColor: 'transparent'
  },
});

const RNApsaraPlayer = requireNativeComponent('ApsaraPlayer', ApsaraPlayer, {
  nativeOnly: {
    seek: true,
  },
});

const getModule = () => {
 return Platform.OS === 'ios'
 ? NativeModules.ApsaraMediaManager
 : NativeModules.ApsaraPlayerModule;
}
const setGlobalSettings =(options)=> {
  const _module = getModule()
  const defaultOptions = {
    maxBufferMemoryKB: 1024 * 10,
    localCacheDir: "",
    expireMin:  24 * 60 * 2,
    maxCapacityMB: 1024 * 5,
    freeStorageMB: 1024 * 5
  }
  options = {
    ...defaultOptions,
    ...options
  }
  try {
      _module?.setGlobalSettings(options);
  } catch (e) {
    
  }
}
const cancelPreLoadUrl = (url) =>{
  const _module = getModule()
  try {
      _module?.cancelPreLoadUrl?.(url);
  } catch (e) {
    
  }
}

const preLoadUrl = (url, duration=5000) =>{
  const _module = getModule()
  try {
      _module?.preLoadUrl?.(url, duration);
  } catch (e) {
    
  }
}
const ApsaraMediaManagerEmitter = new NativeEventEmitter(NativeModules.ApsaraMediaManager);

export {
  ApsaraMediaManagerEmitter,
  setGlobalSettings,
  preLoadUrl,
  cancelPreLoadUrl
}
