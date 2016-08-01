# Yanap (yet another native audio plugin)

This Cordova's plugin is an alternative to the `cordova-plugin-media` and `cordova-plugin-nativeaudio`/`cordova-plugin-lowlatencyaudio` plugins to play audio from native on Android. It may support iOS in the future.

Main features we have in mind while developing it are:
- gapless audio loops
- low latency playback for short sounds

## Basic usage

```javascript
var Yanap = cordova.plugins.Yanap;

var myBackgroundLoop = new Yanap.AudioInstance(Yanap.AUDIO_TYPE.LOOP);

myBackgroundLoop.load('audio/bg.mp3', function (err) {
	if (err) {
		return console.error(err);
	}
	myBackgroundLoop.play();
});

// and when it is not needed anymore...
// myBackgroundLoop.stop();
// myBackgroundLoop.release();
```

## API

### Yanap.AUDIO_TYPE

Enum of available audio instance types:

```javascript
Yanap.AUDIO_TYPE.LOOP  // for -relatively- long audio tracks that need to loop
Yanap.AUDIO_TYPE.MUSIC // for -relatively- long audio tracks
Yanap.AUDIO_TYPE.SOUND // for short audio files (fx, ui...)
```

### Yanap.AUDIO_INSTANCE_STATUS

Enum of possible audio instance status:
```javascript
Yanap.AUDIO_INSTANCE_STATUS.ERROR    // something bad happened, instance cannot be used anymore
Yanap.AUDIO_INSTANCE_STATUS.EMPTY    // instance has been initialised
Yanap.AUDIO_INSTANCE_STATUS.LOADING  // currently loading
Yanap.AUDIO_INSTANCE_STATUS.LOADED   // loaded successfully
Yanap.AUDIO_INSTANCE_STATUS.PLAYING  // currently playing
Yanap.AUDIO_INSTANCE_STATUS.LOOPING  // set when looping the first time and re-triggered at each consecutive loop
Yanap.AUDIO_INSTANCE_STATUS.STOPPED  // playback reached the end or has been manually stopped
Yanap.AUDIO_INSTANCE_STATUS.RELEASED // all resources for this instance have been released
```

NB:
- `LOOPING` is available only for audio types `LOOP`
- `PLAYING` and `STOPPED` are not available for audio types `SOUND`

### Yanap.AudioInstance(audioType, onStatusUpdate)

Audio instance constructor.
- `audioType` can be any value present in `Yanap.AUDIO_TYPE`.
- `onStatusUpdate` is an optional method that will receive a `Yanap.AUDIO_INSTANCE_STATUS` as first parameter and a string containing additional information as second parameter (mostly used when an error happened).

```javascript
var mySong = new Yanap.AudioInstance(
	Yanap.AUDIO_TYPE.MUSIC,
	function (status, additionalInfo) {
		if (status === Yanap.AUDIO_INSTANCE_STATUS.ERROR) {
			return console.error('error: ' + additionalInfo);
			console.log('new status: ' + status);
		}
	}
);
```

This creation is successful even if audio type is invalid (it would fail during the loading).

### Yanap.AudioInstance.load(filePath)

Used to load a file.
- `filePath` is a string relative to the `cache` directory.

```javascript
// example 1: if you want to be sure that audio is ready before trying to play
var myLaserFx = new Yanap.AudioInstance(Yanap.AUDIO_TYPE.SOUND, function (status) {
	if (status === Yanap.AUDIO_INSTANCE_STATUS.LOADED) {
		myLaserFx.play();
	}
});
myLaserFx.load('audio/laser.mp3');

// example 2: if you want to play automatically when loaded
var myLaserFx = new Yanap.AudioInstance(Yanap.AUDIO_TYPE.SOUND);
myLaserFx.load('audio/laser.mp3');
myLaserFx.play();
```

### Yanap.AudioInstance.play()

### Yanap.AudioInstance.stop()

### Yanap.AudioInstance.setVolume(channel1, channel2)

### Yanap.AudioInstance.release()

### Yanap.releaseAll()