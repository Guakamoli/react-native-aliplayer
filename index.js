import React from 'react';
import {
  View,
  Platform,
  StyleSheet,
  NativeModules,
  findNodeHandle,
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
    this.state= {
      source: props.source,
      paused: props.paused,
      muted: props.muted,
      positionMillis: props.positionMillis
    }
  }

  componentWillUnmount() {
    try {
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
    console.info('加载', source)

    this.setState({
      source,
      paused: !options.shouldPlay,
      muted: options.muted
    })
  }
  playAsync =()=> {
    console.info('播放了', this.state.source)
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
    console.info('停止了', this.state.source)
    this.setState({
      paused: true
    })
  }
  unloadAsync =()=>{
    console.info('卸载掉', this.state.source)

    this.setState({
      paused: false,
      source: null
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
    return (
      <View style={style}>
        {this.state.source ? (
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
  repeat: false,
  cacheEnable: false,
  cacheMaxDuration: 100,
  cacheMaxSizeMB: 200,
  startBufferDuration: 500,
  highBufferDuration: 3000,
  maxBufferDuration: 50000,
  progressUpdateIntervalMillis: 30,
  resizeMode: 'contain'
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
 ? NativeModules.ApsaraPlayerManager
 : NativeModules.ApsaraPlayerModule;
}
const setGlobalSettings =()=> {
  const _module = getModule()
  try {
      _module?.setGlobalSettings();
  } catch (e) {
    
  }
}
const preLoadUrl = (url) =>{
  const _module = getModule()
  try {
      _module?.preLoadUrl(url);
  } catch (e) {
    
  }
}
export {
  setGlobalSettings,
  preLoadUrl
}
