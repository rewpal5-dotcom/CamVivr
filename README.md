# CamVivr — Your Camera, Your Control

Android app: gallery se photo/video chunein, preview karein, face animation (blink/mouth/head),
processing (zoom, rotate, flip, brightness, contrast) aur floating control panel.

**APK:** https://github.com/rewpal5-dotcom/CamVivr/releases/latest/download/app-release.apk

## Screens
| Tab | Kya hai |
|---|---|
| **Home** | Status (service/preview/source), Quick Start/Stop, recent files |
| **Camera** | Gallery se photo/video, preview, zoom/brightness/contrast/rotate/flip, video play/pause/stop/speed |
| **Control** | Eye (on/off, blink, auto), Mouth (on/off, open/close/smile), Head (up/down/left/right/center), Floating panel |
| **Streaming** | RTMP URL + key save (streaming is build mein nahi — saaf likha hai) |
| **Settings** | Animation speed, blink interval, about, privacy, reset |

## Imaandari se — kya nahi hai
- **External virtual camera** (doosre apps ko feed dena): Android normal app ko allow nahi karta.
  App khud check karke "not supported" dikhata hai — fake "supported" nahi.
- **RTMP streaming**: is build mein implement nahi hai; UI saaf batati hai.
- **Face animation**: 2D puppet (blink, mouth, head move) — 3D mesh nahi.
- **Video color**: tint-based; image par poora ColorMatrix.

## Tech
Java · MediaPlayer + TextureView · Canvas + ColorMatrix · FaceDetector · Overlay panel · minSdk 24

## Build
```bash
./gradlew assembleRelease
```
