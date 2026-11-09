import 'package:flutter/material.dart';

class WordTiming {
  final double start;
  final double end;
  final String text;

  const WordTiming({
    required this.start,
    required this.end,
    required this.text,
  });

  Map<String, dynamic> toJson() => {
        'start': start,
        'end': end,
        'text': text,
      };

  factory WordTiming.fromJson(Map<String, dynamic> json) {
    return WordTiming(
      start: (json['start'] as num?)?.toDouble() ?? 0.0,
      end: (json['end'] as num?)?.toDouble() ?? 0.0,
      text: json['text'] as String? ?? '',
    );
  }

  WordTiming copyWith({double? start, double? end, String? text}) {
    return WordTiming(
      start: start ?? this.start,
      end: end ?? this.end,
      text: text ?? this.text,
    );
  }
}

class CaptionCue {
  final String id;
  final double start;
  final double end;
  final String text;
  final List<WordTiming> words;

  const CaptionCue({
    required this.id,
    required this.start,
    required this.end,
    required this.text,
    this.words = const [],
  });

  double get duration => end - start;

  Map<String, dynamic> toJson() => {
        'id': id,
        'start': start,
        'end': end,
        'text': text,
        'words': words.map((w) => w.toJson()).toList(),
      };

  factory CaptionCue.fromJson(Map<String, dynamic> json) {
    return CaptionCue(
      id: json['id']?.toString() ?? UniqueKey().toString(),
      start: (json['start'] as num?)?.toDouble() ?? 0.0,
      end: (json['end'] as num?)?.toDouble() ?? 1.0,
      text: json['text'] as String? ?? '',
      words: (json['words'] as List<dynamic>?)
              ?.map((w) => WordTiming.fromJson(w as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }

  CaptionCue copyWith({
    String? id,
    double? start,
    double? end,
    String? text,
    List<WordTiming>? words,
  }) {
    return CaptionCue(
      id: id ?? this.id,
      start: start ?? this.start,
      end: end ?? this.end,
      text: text ?? this.text,
      words: words ?? this.words,
    );
  }
}

class SubtitleStyle {
  final String preset;
  final String font;
  final double fontSize;
  final String textColor;
  final String bgColor;
  final bool bgEnabled;
  final double outline;
  final double yPos; // 0.0 top to 1.0 bottom
  final String align; // 'left', 'center', 'right'
  final String animation; // 'None', 'Fade', 'Pop', 'Bounce', 'Slide up', 'Slide down', 'Zoom'
  final double early; // 0.0s to 1.0s
  final bool oneLine;
  final String overlay; // 'None', 'Translucent', 'Solid'
  final bool karaoke;
  final bool followFace;
  final int opacity; // 0 to 100
  final double letterSpacing;
  final double lineSpacing;
  final double glow;
  final String glowColor;
  final bool gradient;
  final String secondaryColor;

  const SubtitleStyle({
    this.preset = 'Bold',
    this.font = 'Noto Sans',
    this.fontSize = 25.0,
    this.textColor = '#ffffff',
    this.bgColor = '#181124',
    this.bgEnabled = true,
    this.outline = 2.0,
    this.yPos = 0.76,
    this.align = 'center',
    this.animation = 'Pop',
    this.early = 0.0,
    this.oneLine = false,
    this.overlay = 'None',
    this.karaoke = false,
    this.followFace = false,
    this.opacity = 100,
    this.letterSpacing = 0.0,
    this.lineSpacing = 1.0,
    this.glow = 0.0,
    this.glowColor = '#00F0FF',
    this.gradient = false,
    this.secondaryColor = '#FF6A00',
  });

  Map<String, dynamic> toJson() => {
        'preset': preset,
        'font': font,
        'fontSize': fontSize,
        'textColor': textColor,
        'bgColor': bgColor,
        'bgEnabled': bgEnabled,
        'outline': outline,
        'yPos': yPos,
        'align': align,
        'animation': animation,
        'early': early,
        'oneLine': oneLine,
        'overlay': overlay,
        'karaoke': karaoke,
        'followFace': followFace,
        'opacity': opacity,
        'letterSpacing': letterSpacing,
        'lineSpacing': lineSpacing,
        'glow': glow,
        'glowColor': glowColor,
        'gradient': gradient,
        'secondaryColor': secondaryColor,
      };

  factory SubtitleStyle.fromJson(Map<String, dynamic> json) {
    return SubtitleStyle(
      preset: json['preset'] as String? ?? 'Bold',
      font: json['font'] as String? ?? 'Noto Sans',
      fontSize: (json['fontSize'] as num?)?.toDouble() ?? 25.0,
      textColor: json['textColor'] as String? ?? '#ffffff',
      bgColor: json['bgColor'] as String? ?? '#181124',
      bgEnabled: json['bgEnabled'] as bool? ?? true,
      outline: (json['outline'] as num?)?.toDouble() ?? 2.0,
      yPos: (json['yPos'] as num?)?.toDouble() ?? 0.76,
      align: json['align'] as String? ?? 'center',
      animation: json['animation'] as String? ?? 'Pop',
      early: (json['early'] as num?)?.toDouble() ?? 0.0,
      oneLine: json['oneLine'] as bool? ?? false,
      overlay: json['overlay'] as String? ?? 'None',
      karaoke: json['karaoke'] as bool? ?? false,
      followFace: json['followFace'] as bool? ?? false,
      opacity: (json['opacity'] as num?)?.toInt() ?? 100,
      letterSpacing: (json['letterSpacing'] as num?)?.toDouble() ?? 0.0,
      lineSpacing: (json['lineSpacing'] as num?)?.toDouble() ?? 1.0,
      glow: (json['glow'] as num?)?.toDouble() ?? 0.0,
      glowColor: json['glowColor'] as String? ?? '#00F0FF',
      gradient: json['gradient'] as bool? ?? false,
      secondaryColor: json['secondaryColor'] as String? ?? '#FF6A00',
    );
  }

  SubtitleStyle copyWith({
    String? preset,
    String? font,
    double? fontSize,
    String? textColor,
    String? bgColor,
    bool? bgEnabled,
    double? outline,
    double? yPos,
    String? align,
    String? animation,
    double? early,
    bool? oneLine,
    String? overlay,
    bool? karaoke,
    bool? followFace,
    int? opacity,
    double? letterSpacing,
    double? lineSpacing,
    double? glow,
    String? glowColor,
    bool? gradient,
    String? secondaryColor,
  }) {
    return SubtitleStyle(
      preset: preset ?? this.preset,
      font: font ?? this.font,
      fontSize: fontSize ?? this.fontSize,
      textColor: textColor ?? this.textColor,
      bgColor: bgColor ?? this.bgColor,
      bgEnabled: bgEnabled ?? this.bgEnabled,
      outline: outline ?? this.outline,
      yPos: yPos ?? this.yPos,
      align: align ?? this.align,
      animation: animation ?? this.animation,
      early: early ?? this.early,
      oneLine: oneLine ?? this.oneLine,
      overlay: overlay ?? this.overlay,
      karaoke: karaoke ?? this.karaoke,
      followFace: followFace ?? this.followFace,
      opacity: opacity ?? this.opacity,
      letterSpacing: letterSpacing ?? this.letterSpacing,
      lineSpacing: lineSpacing ?? this.lineSpacing,
      glow: glow ?? this.glow,
      glowColor: glowColor ?? this.glowColor,
      gradient: gradient ?? this.gradient,
      secondaryColor: secondaryColor ?? this.secondaryColor,
    );
  }
}

class Track {
  final String id;
  final String name;
  final String language;
  final String color;
  final bool visible;
  final bool locked;
  final SubtitleStyle style;
  final List<CaptionCue> captions;

  const Track({
    required this.id,
    this.name = 'Original',
    this.language = 'auto',
    this.color = '#7352ff',
    this.visible = true,
    this.locked = false,
    this.style = const SubtitleStyle(),
    this.captions = const [],
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'language': language,
        'color': color,
        'visible': visible,
        'locked': locked,
        'style': style.toJson(),
        'captions': captions.map((c) => c.toJson()).toList(),
      };

  factory Track.fromJson(Map<String, dynamic> json) {
    return Track(
      id: json['id'] as String? ?? UniqueKey().toString(),
      name: json['name'] as String? ?? 'Track',
      language: json['language'] as String? ?? 'auto',
      color: json['color'] as String? ?? '#7352ff',
      visible: json['visible'] as bool? ?? true,
      locked: json['locked'] as bool? ?? false,
      style: json['style'] != null
          ? SubtitleStyle.fromJson(json['style'] as Map<String, dynamic>)
          : const SubtitleStyle(),
      captions: (json['captions'] as List<dynamic>?)
              ?.map((c) => CaptionCue.fromJson(c as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }

  Track copyWith({
    String? id,
    String? name,
    String? language,
    String? color,
    bool? visible,
    bool? locked,
    SubtitleStyle? style,
    List<CaptionCue>? captions,
  }) {
    return Track(
      id: id ?? this.id,
      name: name ?? this.name,
      language: language ?? this.language,
      color: color ?? this.color,
      visible: visible ?? this.visible,
      locked: locked ?? this.locked,
      style: style ?? this.style,
      captions: captions ?? this.captions,
    );
  }
}

class VideoTrim {
  final double start;
  final double end;

  const VideoTrim({required this.start, required this.end});

  double get duration => end - start;

  Map<String, dynamic> toJson() => {'start': start, 'end': end};

  factory VideoTrim.fromJson(Map<String, dynamic> json) {
    return VideoTrim(
      start: (json['start'] as num?)?.toDouble() ?? 0.0,
      end: (json['end'] as num?)?.toDouble() ?? 0.0,
    );
  }

  VideoTrim copyWith({double? start, double? end}) {
    return VideoTrim(
      start: start ?? this.start,
      end: end ?? this.end,
    );
  }
}

class OverlayKeyframe {
  final double time; // seconds offset from overlay start
  final double xPos; // 0.0 to 1.0
  final double yPos; // 0.0 to 1.0
  final double scale;
  final double rotation; // degrees
  final double opacity; // 0.0 to 1.0
  final double blur;
  final double glow;
  final String easing; // 'linear', 'easeIn', 'easeOut', 'easeInOut', 'bounce', 'spring'
  final String color;

  const OverlayKeyframe({
    required this.time,
    this.xPos = 0.5,
    this.yPos = 0.5,
    this.scale = 1.0,
    this.rotation = 0.0,
    this.opacity = 1.0,
    this.blur = 0.0,
    this.glow = 0.0,
    this.easing = 'easeInOut',
    this.color = '#FF7A00',
  });

  Map<String, dynamic> toJson() => {
        'time': time,
        'xPos': xPos,
        'yPos': yPos,
        'scale': scale,
        'rotation': rotation,
        'opacity': opacity,
        'blur': blur,
        'glow': glow,
        'easing': easing,
        'color': color,
      };

  factory OverlayKeyframe.fromJson(Map<String, dynamic> json) {
    return OverlayKeyframe(
      time: (json['time'] as num?)?.toDouble() ?? 0.0,
      xPos: (json['xPos'] as num?)?.toDouble() ?? 0.5,
      yPos: (json['yPos'] as num?)?.toDouble() ?? 0.5,
      scale: (json['scale'] as num?)?.toDouble() ?? 1.0,
      rotation: (json['rotation'] as num?)?.toDouble() ?? 0.0,
      opacity: (json['opacity'] as num?)?.toDouble() ?? 1.0,
      blur: (json['blur'] as num?)?.toDouble() ?? 0.0,
      glow: (json['glow'] as num?)?.toDouble() ?? 0.0,
      easing: json['easing'] as String? ?? 'easeInOut',
      color: json['color'] as String? ?? '#FF7A00',
    );
  }

  OverlayKeyframe copyWith({
    double? time,
    double? xPos,
    double? yPos,
    double? scale,
    double? rotation,
    double? opacity,
    double? blur,
    double? glow,
    String? easing,
    String? color,
  }) {
    return OverlayKeyframe(
      time: time ?? this.time,
      xPos: xPos ?? this.xPos,
      yPos: yPos ?? this.yPos,
      scale: scale ?? this.scale,
      rotation: rotation ?? this.rotation,
      opacity: opacity ?? this.opacity,
      blur: blur ?? this.blur,
      glow: glow ?? this.glow,
      easing: easing ?? this.easing,
      color: color ?? this.color,
    );
  }
}

class GraphicOverlay {
  final String id;
  final String name;
  final String type; // 'template', 'persian_orange_headline', 'light_leak', 'glow_hologram', 'flag_graphic', 'waving_flag', 'metallic_banner', 'soft_bg_text', 'end_accent', 'image'
  final String content;
  final double start;
  final double end;
  final double xPos; // 0.0 to 1.0 (relative canvas x)
  final double yPos; // 0.0 to 1.0 (relative canvas y)
  final double scale; // 0.5 to 2.5
  final double opacity; // 0.0 to 1.0
  final double rotation; // degrees -180 to 180
  final double blur;
  final double glow;
  final String glowColor;
  final String blendMode; // 'normal', 'screen', 'add', 'overlay'
  final String color; // hex color e.g. '#FF7A00', '#D946EF'
  final String animation; // 'None', 'Fade', 'Pop', 'Glitch', 'Pulse', 'Slide'
  final bool visible;
  final bool isBackground; // depth occlusion: occluded behind speaker
  final int depthLayer; // 0 = behind speaker, 1 = main, 2 = overlays, 3 = foreground
  final List<OverlayKeyframe> keyframes;

  const GraphicOverlay({
    required this.id,
    required this.name,
    this.type = 'template',
    this.content = '',
    required this.start,
    required this.end,
    this.xPos = 0.5,
    this.yPos = 0.5,
    this.scale = 1.0,
    this.opacity = 1.0,
    this.rotation = 0.0,
    this.blur = 0.0,
    this.glow = 0.0,
    this.glowColor = '#00F0FF',
    this.blendMode = 'normal',
    this.color = '#FF7A00',
    this.animation = 'Fade',
    this.visible = true,
    this.isBackground = false,
    this.depthLayer = 2,
    this.keyframes = const [],
  });

  double get duration => end - start;

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'type': type,
        'content': content,
        'start': start,
        'end': end,
        'xPos': xPos,
        'yPos': yPos,
        'scale': scale,
        'opacity': opacity,
        'rotation': rotation,
        'blur': blur,
        'glow': glow,
        'glowColor': glowColor,
        'blendMode': blendMode,
        'color': color,
        'animation': animation,
        'visible': visible,
        'isBackground': isBackground,
        'depthLayer': depthLayer,
        'keyframes': keyframes.map((k) => k.toJson()).toList(),
      };

  factory GraphicOverlay.fromJson(Map<String, dynamic> json) {
    return GraphicOverlay(
      id: json['id'] as String? ?? UniqueKey().toString(),
      name: json['name'] as String? ?? 'Overlay',
      type: json['type'] as String? ?? 'template',
      content: json['content'] as String? ?? '',
      start: (json['start'] as num?)?.toDouble() ?? 0.0,
      end: (json['end'] as num?)?.toDouble() ?? 3.0,
      xPos: (json['xPos'] as num?)?.toDouble() ?? 0.5,
      yPos: (json['yPos'] as num?)?.toDouble() ?? 0.5,
      scale: (json['scale'] as num?)?.toDouble() ?? 1.0,
      opacity: (json['opacity'] as num?)?.toDouble() ?? 1.0,
      rotation: (json['rotation'] as num?)?.toDouble() ?? 0.0,
      blur: (json['blur'] as num?)?.toDouble() ?? 0.0,
      glow: (json['glow'] as num?)?.toDouble() ?? 0.0,
      glowColor: json['glowColor'] as String? ?? '#00F0FF',
      blendMode: json['blendMode'] as String? ?? 'normal',
      color: json['color'] as String? ?? '#FF7A00',
      animation: json['animation'] as String? ?? 'Fade',
      visible: json['visible'] as bool? ?? true,
      isBackground: json['isBackground'] as bool? ?? false,
      depthLayer: (json['depthLayer'] as num?)?.toInt() ?? 2,
      keyframes: (json['keyframes'] as List<dynamic>?)
              ?.map((k) => OverlayKeyframe.fromJson(k as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }

  GraphicOverlay copyWith({
    String? id,
    String? name,
    String? type,
    String? content,
    double? start,
    double? end,
    double? xPos,
    double? yPos,
    double? scale,
    double? opacity,
    double? rotation,
    double? blur,
    double? glow,
    String? glowColor,
    String? blendMode,
    String? color,
    String? animation,
    bool? visible,
    bool? isBackground,
    int? depthLayer,
    List<OverlayKeyframe>? keyframes,
  }) {
    return GraphicOverlay(
      id: id ?? this.id,
      name: name ?? this.name,
      type: type ?? this.type,
      content: content ?? this.content,
      start: start ?? this.start,
      end: end ?? this.end,
      xPos: xPos ?? this.xPos,
      yPos: yPos ?? this.yPos,
      scale: scale ?? this.scale,
      opacity: opacity ?? this.opacity,
      rotation: rotation ?? this.rotation,
      blur: blur ?? this.blur,
      glow: glow ?? this.glow,
      glowColor: glowColor ?? this.glowColor,
      blendMode: blendMode ?? this.blendMode,
      color: color ?? this.color,
      animation: animation ?? this.animation,
      visible: visible ?? this.visible,
      isBackground: isBackground ?? this.isBackground,
      depthLayer: depthLayer ?? this.depthLayer,
      keyframes: keyframes ?? this.keyframes,
    );
  }
}

class AudioClip {
  final String id;
  final String name;
  final String category; // 'Pop', 'Click', 'Whoosh', 'Shimmer', 'Impact', 'Riser', 'Bass', 'Glitch', 'Light Sweep', 'Transition', 'Ambient', 'Music', 'Voiceover'
  final String source;
  final double start;
  final double duration;
  final double trimStart;
  final double volume; // 0.0 to 2.0
  final double fadeIn;
  final double fadeOut;
  final bool ducking;

  const AudioClip({
    required this.id,
    required this.name,
    this.category = 'sfx',
    required this.source,
    required this.start,
    required this.duration,
    this.trimStart = 0.0,
    this.volume = 1.0,
    this.fadeIn = 0.0,
    this.fadeOut = 0.0,
    this.ducking = false,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'category': category,
        'source': source,
        'start': start,
        'duration': duration,
        'trimStart': trimStart,
        'volume': volume,
        'fadeIn': fadeIn,
        'fadeOut': fadeOut,
        'ducking': ducking,
      };

  factory AudioClip.fromJson(Map<String, dynamic> json) {
    return AudioClip(
      id: json['id'] as String? ?? UniqueKey().toString(),
      name: json['name'] as String? ?? 'Audio Clip',
      category: json['category'] as String? ?? 'sfx',
      source: json['source'] as String? ?? '',
      start: (json['start'] as num?)?.toDouble() ?? 0.0,
      duration: (json['duration'] as num?)?.toDouble() ?? 1.0,
      trimStart: (json['trimStart'] as num?)?.toDouble() ?? 0.0,
      volume: (json['volume'] as num?)?.toDouble() ?? 1.0,
      fadeIn: (json['fadeIn'] as num?)?.toDouble() ?? 0.0,
      fadeOut: (json['fadeOut'] as num?)?.toDouble() ?? 0.0,
      ducking: json['ducking'] as bool? ?? false,
    );
  }

  AudioClip copyWith({
    String? id,
    String? name,
    String? category,
    String? source,
    double? start,
    double? duration,
    double? trimStart,
    double? volume,
    double? fadeIn,
    double? fadeOut,
    bool? ducking,
  }) {
    return AudioClip(
      id: id ?? this.id,
      name: name ?? this.name,
      category: category ?? this.category,
      source: source ?? this.source,
      start: start ?? this.start,
      duration: duration ?? this.duration,
      trimStart: trimStart ?? this.trimStart,
      volume: volume ?? this.volume,
      fadeIn: fadeIn ?? this.fadeIn,
      fadeOut: fadeOut ?? this.fadeOut,
      ducking: ducking ?? this.ducking,
    );
  }
}

class VideoEffects {
  final double brightness; // -0.8 to 0.8
  final double contrast; // 0.5 to 2.0
  final double saturation; // 0.0 to 3.0
  final double sharpness; // 0.0 to 2.0
  final String clearVoice; // 'Off', 'Light', 'Balanced', 'Strong'
  final String sfx; // 'None', 'Pop', 'Click', 'Whoosh', 'Shimmer', 'Impact', 'Riser', 'Bass', 'Glitch', 'Light Sweep', 'Transition', 'Ambient'
  final String sfxMode; // 'Sentence', 'Word'
  final int sfxVolume; // 0 to 100

  const VideoEffects({
    this.brightness = 0.0,
    this.contrast = 1.0,
    this.saturation = 1.0,
    this.sharpness = 0.0,
    this.clearVoice = 'Off',
    this.sfx = 'None',
    this.sfxMode = 'Sentence',
    this.sfxVolume = 40,
  });

  Map<String, dynamic> toJson() => {
        'brightness': brightness,
        'contrast': contrast,
        'saturation': saturation,
        'sharpness': sharpness,
        'clearVoice': clearVoice,
        'sfx': sfx,
        'sfxMode': sfxMode,
        'sfxVolume': sfxVolume,
      };

  factory VideoEffects.fromJson(Map<String, dynamic> json) {
    return VideoEffects(
      brightness: (json['brightness'] as num?)?.toDouble() ?? 0.0,
      contrast: (json['contrast'] as num?)?.toDouble() ?? 1.0,
      saturation: (json['saturation'] as num?)?.toDouble() ?? 1.0,
      sharpness: (json['sharpness'] as num?)?.toDouble() ?? 0.0,
      clearVoice: json['clearVoice'] as String? ?? 'Off',
      sfx: json['sfx'] as String? ?? 'None',
      sfxMode: json['sfxMode'] as String? ?? 'Sentence',
      sfxVolume: (json['sfxVolume'] as num?)?.toInt() ?? 40,
    );
  }

  VideoEffects copyWith({
    double? brightness,
    double? contrast,
    double? saturation,
    double? sharpness,
    String? clearVoice,
    String? sfx,
    String? sfxMode,
    int? sfxVolume,
  }) {
    return VideoEffects(
      brightness: brightness ?? this.brightness,
      contrast: contrast ?? this.contrast,
      saturation: saturation ?? this.saturation,
      sharpness: sharpness ?? this.sharpness,
      clearVoice: clearVoice ?? this.clearVoice,
      sfx: sfx ?? this.sfx,
      sfxMode: sfxMode ?? this.sfxMode,
      sfxVolume: sfxVolume ?? this.sfxVolume,
    );
  }
}

class Project {
  final String id;
  final String name;
  final String source;
  final double duration;
  final int width;
  final int height;
  final bool hasAudio;
  final VideoTrim trim;
  final int resolution; // 480, 720, 1080
  final String aspectRatio; // '9:16', '1:1', '16:9'
  final VideoEffects effects;
  final List<Track> tracks;
  final List<GraphicOverlay> overlays;
  final List<AudioClip> audioClips;
  final List<String> customFonts;
  final String createdAt;
  final String updatedAt;

  const Project({
    required this.id,
    required this.name,
    required this.source,
    required this.duration,
    required this.width,
    required this.height,
    required this.hasAudio,
    required this.trim,
    this.resolution = 720,
    this.aspectRatio = '9:16',
    this.effects = const VideoEffects(),
    this.tracks = const [],
    this.overlays = const [],
    this.audioClips = const [],
    this.customFonts = const [],
    required this.createdAt,
    required this.updatedAt,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'source': source,
        'duration': duration,
        'width': width,
        'height': height,
        'hasAudio': hasAudio,
        'trim': trim.toJson(),
        'resolution': resolution,
        'aspectRatio': aspectRatio,
        'effects': effects.toJson(),
        'tracks': tracks.map((t) => t.toJson()).toList(),
        'overlays': overlays.map((o) => o.toJson()).toList(),
        'audioClips': audioClips.map((a) => a.toJson()).toList(),
        'customFonts': customFonts,
        'createdAt': createdAt,
        'updatedAt': updatedAt,
      };

  factory Project.fromJson(Map<String, dynamic> json) {
    return Project(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? 'Untitled Project',
      source: json['source'] as String? ?? '',
      duration: (json['duration'] as num?)?.toDouble() ?? 0.0,
      width: (json['width'] as num?)?.toInt() ?? 1080,
      height: (json['height'] as num?)?.toInt() ?? 1920,
      hasAudio: json['hasAudio'] as bool? ?? true,
      trim: json['trim'] != null
          ? VideoTrim.fromJson(json['trim'] as Map<String, dynamic>)
          : VideoTrim(start: 0, end: (json['duration'] as num?)?.toDouble() ?? 0.0),
      resolution: (json['resolution'] as num?)?.toInt() ?? 720,
      aspectRatio: json['aspectRatio'] as String? ?? '9:16',
      effects: json['effects'] != null
          ? VideoEffects.fromJson(json['effects'] as Map<String, dynamic>)
          : const VideoEffects(),
      tracks: (json['tracks'] as List<dynamic>?)
              ?.map((t) => Track.fromJson(t as Map<String, dynamic>))
              .toList() ??
          [],
      overlays: (json['overlays'] as List<dynamic>?)
              ?.map((o) => GraphicOverlay.fromJson(o as Map<String, dynamic>))
              .toList() ??
          [],
      audioClips: (json['audioClips'] as List<dynamic>?)
              ?.map((a) => AudioClip.fromJson(a as Map<String, dynamic>))
              .toList() ??
          [],
      customFonts: (json['customFonts'] as List<dynamic>?)
              ?.map((f) => f.toString())
              .toList() ??
          [],
      createdAt: json['createdAt'] as String? ?? '',
      updatedAt: json['updatedAt'] as String? ?? '',
    );
  }

  Project copyWith({
    String? id,
    String? name,
    String? source,
    double? duration,
    int? width,
    int? height,
    bool? hasAudio,
    VideoTrim? trim,
    int? resolution,
    String? aspectRatio,
    VideoEffects? effects,
    List<Track>? tracks,
    List<GraphicOverlay>? overlays,
    List<AudioClip>? audioClips,
    List<String>? customFonts,
    String? createdAt,
    String? updatedAt,
  }) {
    return Project(
      id: id ?? this.id,
      name: name ?? this.name,
      source: source ?? this.source,
      duration: duration ?? this.duration,
      width: width ?? this.width,
      height: height ?? this.height,
      hasAudio: hasAudio ?? this.hasAudio,
      trim: trim ?? this.trim,
      resolution: resolution ?? this.resolution,
      aspectRatio: aspectRatio ?? this.aspectRatio,
      effects: effects ?? this.effects,
      tracks: tracks ?? this.tracks,
      overlays: overlays ?? this.overlays,
      audioClips: audioClips ?? this.audioClips,
      customFonts: customFonts ?? this.customFonts,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
