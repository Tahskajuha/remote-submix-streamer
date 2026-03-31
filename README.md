# RemoteSubmixStreamer

An Android audio output streamer designed for systems where remote_submix restrictions have been removed and audio policy routes output to remote_submix as soon as the submix pipe becomes active.

## Capture Specs

- **Source**: REMOTE_SUBMIX
- **Sample Rate**: 48kHz
- **Channels**: 2 (stereo)
- **Format**: PCM 16-bit

## Encoder Specs

- **Encoding**: OPUS
- **Application**: AUDIO
- **Channels**: 2
- **Frame Size**: 960 samples (20 ms)

## RTP Specs

- **Transport**: UDP
- **Clock Rate**: 48000
- **Timestamp Increment**: 960/packet
- One RTP packet per OPUS frame

## Payload Specs

- **RTP Header Version**: 2
- **Marker**: Disabled
- **CSRC**: Disabled
- **Payload Type**: 96 (dynamic, OPUS)
- **SSRC**: 32 bits
- **Timestamp**: 32 bits
- **Sequence Number**: 16 bits (increments by 1/packet; wraps at 65535)
- **Padding**: Disabled
- **Extension**: Disabled
