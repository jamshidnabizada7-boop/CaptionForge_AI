import 'package:flutter_test/flutter_test.dart';
import 'package:captionforge_mobile/models/project.dart';

void main() {
  group('Project Model Serialization & Immutability', () {
    test('CaptionCue and WordTiming serialization preserves timing and text', () {
      const cue = CaptionCue(
        id: 'c_test_1',
        start: 0.5,
        end: 2.1,
        text: 'CaptionForge Test',
        words: [
          WordTiming(start: 0.5, end: 1.2, text: 'CaptionForge'),
          WordTiming(start: 1.3, end: 2.1, text: 'Test'),
        ],
      );

      final json = cue.toJson();
      expect(json['id'], 'c_test_1');
      expect(json['start'], 0.5);
      expect(json['end'], 2.1);
      expect((json['words'] as List).length, 2);

      final reconstructed = CaptionCue.fromJson(json);
      expect(reconstructed.id, cue.id);
      expect(reconstructed.start, cue.start);
      expect(reconstructed.end, cue.end);
      expect(reconstructed.duration, closeTo(1.6, 0.001));
      expect(reconstructed.words.first.text, 'CaptionForge');
    });

    test('SubtitleStyle copyWith retains defaults and updates specific attributes', () {
      const style = SubtitleStyle();
      expect(style.preset, 'Bold');
      expect(style.animation, 'Pop');
      expect(style.align, 'center');
      expect(style.yPos, 0.76);

      final custom = style.copyWith(
        font: 'Vazirmatn',
        textColor: '#FACC15',
        animation: 'Bounce',
        karaoke: true,
      );

      expect(custom.font, 'Vazirmatn');
      expect(custom.textColor, '#FACC15');
      expect(custom.animation, 'Bounce');
      expect(custom.karaoke, true);
      expect(custom.align, 'center'); // Retains default
    });

    test('Complete Project round-trip serialization matches backend JSON format', () {
      final project = Project(
        id: 'proj_abc123',
        name: 'Persian Subtitle Reel',
        source: 'original.mp4',
        duration: 4.8,
        width: 1080,
        height: 1920,
        hasAudio: true,
        trim: const VideoTrim(start: 0.2, end: 4.5),
        resolution: 1080,
        effects: const VideoEffects(
          clearVoice: 'Balanced',
          sfx: 'Pop',
          sfxMode: 'Word',
          sfxVolume: 60,
        ),
        tracks: [
          const Track(
            id: 'tr_1',
            name: 'Original',
            language: 'fa',
            captions: [
              CaptionCue(
                id: 'cue_fa_1',
                start: 0.5,
                end: 2.5,
                text: 'سلام به اپلیکیشن کپشن فورج',
              ),
            ],
          ),
          const Track(
            id: 'tr_2',
            name: 'English Translation',
            language: 'en',
            style: SubtitleStyle(textColor: '#A78BFA', yPos: 0.85),
            captions: [
              CaptionCue(
                id: 'cue_en_1',
                start: 0.5,
                end: 2.5,
                text: 'Hello to CaptionForge App',
              ),
            ],
          ),
        ],
        createdAt: '2026-11-09T10:00:00Z',
        updatedAt: '2026-11-09T10:05:00Z',
      );

      final json = project.toJson();
      expect(json['id'], 'proj_abc123');
      expect(json['resolution'], 1080);
      expect(json['aspectRatio'], '9:16');
      expect((json['tracks'] as List).length, 2);

      final reconstructed = Project.fromJson(json);
      expect(reconstructed.id, project.id);
      expect(reconstructed.aspectRatio, '9:16');
      expect(reconstructed.tracks[0].captions[0].text, 'سلام به اپلیکیشن کپشن فورج');
      expect(reconstructed.tracks[1].language, 'en');
      expect(reconstructed.effects.clearVoice, 'Balanced');
    });

    test('GraphicOverlay serialization and Project overlays round-trip', () {
      const overlay = GraphicOverlay(
        id: 'ov_test_1',
        name: 'Hand Glow Hologram',
        type: 'glow_hologram',
        content: 'Holographic Icon',
        start: 1.0,
        end: 4.0,
        xPos: 0.65,
        yPos: 0.40,
        scale: 1.25,
        opacity: 0.95,
        blendMode: 'screen',
        color: '#38BDF8',
        animation: 'Pulse',
        visible: true,
      );

      final json = overlay.toJson();
      expect(json['id'], 'ov_test_1');
      expect(json['type'], 'glow_hologram');
      expect(json['color'], '#38BDF8');
      expect(json['scale'], 1.25);

      final reconstructed = GraphicOverlay.fromJson(json);
      expect(reconstructed.id, 'ov_test_1');
      expect(reconstructed.name, 'Hand Glow Hologram');
      expect(reconstructed.type, 'glow_hologram');
      expect(reconstructed.duration, closeTo(3.0, 0.001));

      final projectWithOverlays = Project(
        id: 'p_ov',
        name: 'Overlay Project',
        source: 'test.mp4',
        duration: 5.0,
        width: 720,
        height: 1280,
        hasAudio: true,
        trim: const VideoTrim(start: 0, end: 5.0),
        overlays: [reconstructed],
        createdAt: '2026-11-09T12:00:00Z',
        updatedAt: '2026-11-09T12:00:00Z',
      );

      final pJson = projectWithOverlays.toJson();
      expect((pJson['overlays'] as List).length, 1);
      final pRecon = Project.fromJson(pJson);
      expect(pRecon.overlays.length, 1);
      expect(pRecon.overlays.first.type, 'glow_hologram');
      expect(pRecon.overlays.first.color, '#38BDF8');
    });

    test('OverlayKeyframe and keyframed GraphicOverlay round-trip serialization', () {
      const keyframe = OverlayKeyframe(
        time: 1.5,
        xPos: 0.35,
        yPos: 0.45,
        scale: 1.4,
        rotation: 15.0,
        opacity: 0.9,
        blur: 4.0,
        glow: 8.0,
        easing: 'bounce',
        color: '#00F0FF',
      );

      final kfJson = keyframe.toJson();
      expect(kfJson['time'], 1.5);
      expect(kfJson['easing'], 'bounce');

      final reconstructedKf = OverlayKeyframe.fromJson(kfJson);
      expect(reconstructedKf.time, 1.5);
      expect(reconstructedKf.rotation, 15.0);
      expect(reconstructedKf.glow, 8.0);

      const overlayWithKf = GraphicOverlay(
        id: 'ov_kf_1',
        name: 'Animated Towers',
        type: 'glow_hologram',
        start: 0.0,
        end: 4.0,
        isBackground: true,
        depthLayer: 0,
        keyframes: [keyframe],
      );

      final ovJson = overlayWithKf.toJson();
      expect(ovJson['isBackground'], true);
      expect(ovJson['depthLayer'], 0);
      expect((ovJson['keyframes'] as List).length, 1);

      final reconstructedOv = GraphicOverlay.fromJson(ovJson);
      expect(reconstructedOv.isBackground, true);
      expect(reconstructedOv.keyframes.length, 1);
      expect(reconstructedOv.keyframes.first.easing, 'bounce');
    });

    test('AudioClip and Project multitrack audio clips round-trip', () {
      const clip = AudioClip(
        id: 'ac_test_1',
        name: 'Cinematic Sub Slam',
        category: 'Impact',
        source: 'Impact',
        start: 2.0,
        duration: 1.5,
        volume: 0.85,
        fadeIn: 0.1,
        fadeOut: 0.3,
        ducking: true,
      );

      final clipJson = clip.toJson();
      expect(clipJson['category'], 'Impact');
      expect(clipJson['ducking'], true);

      final reconstructed = AudioClip.fromJson(clipJson);
      expect(reconstructed.id, 'ac_test_1');
      expect(reconstructed.volume, 0.85);
      expect(reconstructed.ducking, true);

      final proj = Project(
        id: 'p_audio',
        name: 'Audio Project',
        source: 'video.mp4',
        duration: 10.0,
        width: 720,
        height: 1280,
        hasAudio: true,
        trim: const VideoTrim(start: 0, end: 10.0),
        audioClips: [reconstructed],
        customFonts: ['VazirmatnCustom.ttf'],
        createdAt: '2026-11-09T12:00:00Z',
        updatedAt: '2026-11-09T12:00:00Z',
      );

      final projJson = proj.toJson();
      expect((projJson['audioClips'] as List).length, 1);
      expect((projJson['customFonts'] as List).length, 1);

      final reconProj = Project.fromJson(projJson);
      expect(reconProj.audioClips.length, 1);
      expect(reconProj.audioClips.first.category, 'Impact');
      expect(reconProj.customFonts.first, 'VazirmatnCustom.ttf');
    });

    test('SubtitleStyle advanced typography properties round-trip', () {
      const style = SubtitleStyle(
        letterSpacing: 2.5,
        lineSpacing: 1.4,
        glow: 6.0,
        glowColor: '#00F0FF',
        gradient: true,
        secondaryColor: '#FF6A00',
      );

      final json = style.toJson();
      expect(json['letterSpacing'], 2.5);
      expect(json['lineSpacing'], 1.4);
      expect(json['glow'], 6.0);
      expect(json['gradient'], true);

      final recon = SubtitleStyle.fromJson(json);
      expect(recon.letterSpacing, 2.5);
      expect(recon.lineSpacing, 1.4);
      expect(recon.glow, 6.0);
      expect(recon.gradient, true);
      expect(recon.secondaryColor, '#FF6A00');
    });
  });
}
