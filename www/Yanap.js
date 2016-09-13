var exec = require('cordova/exec');
var channel = require('cordova/channel');
var utils = cordova.require('cordova/utils');

var AUDIO_INSTANCE_STATUS = {
    ERROR: 'error',
    EMPTY: 'empty',
    LOADING: 'loading',
    LOADED: 'loaded',
    PLAYING: 'playing',
    LOOPING: 'looping',
    STOPPED: 'stopped',
    RELEASED: 'released'
};

var AUDIO_TYPE = {
    LOOP: 'loop',
    MUSIC: 'music',
    SOUND: 'sound'
};

var audioInstances = {};

var init = false;

function AudioInstance(audioType, onStatusUpdate) {
    if (!init) {
        init = true;
        channel.createSticky('onYanapPluginReady');
        channel.waitForInitialization('onYanapPluginReady');
        exec(onNativeMessage, null, 'Yanap', 'messageChannel', []);
        channel.initializationComplete('onYanapPluginReady');
    }

    this.uid = utils.createUUID();
    audioInstances[this.uid] = this;

    this.audioType = audioType;
    this.onStatusUpdate = onStatusUpdate;
    this.filePath = null;
    this.status = null;
    this.fileLength = -1;
    setStatus(this.uid, AUDIO_INSTANCE_STATUS.EMPTY);
}

AudioInstance.prototype.isAlive = function (method) {
    if (!audioInstances[this.uid]) {
        console.warn('Yanap (' + method + '): unknown audioInstance `' + this.uid + '`');
    } else if (this.status === AUDIO_INSTANCE_STATUS.RELEASED) {
        console.warn('Yanap (' + method + '): this object has already been released');
    } else if (this.status === AUDIO_INSTANCE_STATUS.ERROR) {
        console.warn('Yanap (' + method + '): this object is in an error state');
    } else {
        return true;
    }
    return false;
}

AudioInstance.prototype.load = function (filePath) {
    if (!this.isAlive('load')) { return; }
    if (this.status !== AUDIO_INSTANCE_STATUS.EMPTY) {
        return console.warn('Yanap (load): available only on an audio instances in `EMPTY` state');
    }
    this.filePath = filePath;
    exec(null, null, 'Yanap', 'createAudioInstance', [this.uid, this.audioType, filePath]);
};

AudioInstance.prototype.play = function () {
    if (!this.isAlive('play')) { return; }
    exec(null, null, 'Yanap', 'play', [this.uid]);
};

AudioInstance.prototype.stop = function () {
    if (!this.isAlive('stop')) { return; }
    exec(null, null, 'Yanap', 'stop', [this.uid]);
};

AudioInstance.prototype.release = function () {
    if (!this.isAlive('release')) { return; }
    exec(null, null, 'Yanap', 'release', [this.uid]);
};

AudioInstance.prototype.setVolume = function (v1, v2) {
    if (!this.isAlive('setVolume')) { return; }
    if (v2 === undefined || v2 === null) { v2 = v1; }
    exec(null, null, 'Yanap', 'setVolume', [this.uid, v1, v2]);
};

function cleanup(uid) {
    if (!audioInstances[uid]) {
        return console.warn('Yanap (cleanup): unknown audioInstance `' + uid + '`');
    }
    var ai = audioInstances[uid];
    delete audioInstances[uid];
    ai.audioType = null;
    ai.filePath = null;
}

function setStatus(uid, status, additionalInfo) {
    if (!audioInstances[uid]) {
        return console.warn('Yanap (setStatus, ' + status + ', ' + additionalInfo + '): unknown audioInstance `' + uid + '`');
    }
    var ai = audioInstances[uid];
    if (ai.status === status && status === AUDIO_INSTANCE_STATUS.ERROR) {
        return;
    }
    ai.status = status;
    if (status === AUDIO_INSTANCE_STATUS.ERROR) {
        console.warn('Yanap (setStatus, error): '+ additionalInfo);
    }
    if (ai.onStatusUpdate) {
        ai.onStatusUpdate(status, additionalInfo);
    }
};

function onNativeMessage(msg) {
    var audioUid = null;
    if (msg.msgType === 'statusUpdate') {
        audioUid = msg.audioUid;
        if (!audioInstances[audioUid]) {
            return console.warn('Yanap (statusUpdate, ' + msg.newStatus + '): unknown audioInstance `' + audioUid + '`');
        }
        if (!AUDIO_INSTANCE_STATUS[msg.newStatus]) {
            return console.error('unknown status code in native message: ' + JSON.stringify(msg));
        }
        setStatus(audioUid, AUDIO_INSTANCE_STATUS[msg.newStatus], msg.additionalInfo);
        if (AUDIO_INSTANCE_STATUS[msg.newStatus] === AUDIO_INSTANCE_STATUS.RELEASED) {
            cleanup(audioUid);
        }
    } else if (msg.msgType === 'fileLength') {
        audioUid = msg.audioUid;
        if (!audioInstances[audioUid]) {
            return console.warn('Yanap (fileLength message: unknown audioInstance `' + audioUid + '`');
        }
        audioInstances[audioUid].fileLength = msg.fileLength;
    } else {
        return console.error(new Error('Yanap (onNativeMessage): received an unknown native message: ' + JSON.stringify(msg)));
    }
}

function releaseAll() {
    for (var uid in audioInstances) {
        audioInstances[uid].release();
    }
}

// -----------------------------
// ---------- Exposed ----------
// -----------------------------

// constants
exports.AUDIO_INSTANCE_STATUS = AUDIO_INSTANCE_STATUS;
exports.AUDIO_TYPE = AUDIO_TYPE;

// class
exports.AudioInstance = AudioInstance;

// instances list
exports.audioInstances = audioInstances;

// global cleaning
exports.releaseAll = releaseAll;