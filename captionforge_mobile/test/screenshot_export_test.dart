import 'dart:io';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:captionforge_mobile/core/services/api_service.dart';
import 'package:captionforge_mobile/features/home/home_screen.dart';
import 'package:captionforge_mobile/features/editor/editor_screen.dart';
import 'package:captionforge_mobile/features/editor/widgets/subtitle_overlay.dart';
import 'package:captionforge_mobile/models/project.dart';

void main() {
  final mockClient = MockClient((request) async {
    if (request.url.path == '/api/config') {
      return http.Response(
        '{"transcription": "faster-whisper", "translation": "ready (offline built-in)", "render": true, "faceTracking": true}',
        200,
      );
    }
    if (request.url.path == '/api/projects') {
      return http.Response(
        '[{"id": "p1", "name": "Persian Reel Demo", "duration": 6.0, "updatedAt": "2026-11-09T10:00:00Z"}]',
        200,
      );
    }
    return http.Response('{"ok": true}', 200);
  });
  final mockApi = ApiService(client: mockClient);

  testWidgets('Capture HomeScreen real UI render', (WidgetTester tester) async {
    final boundaryKey = GlobalKey();

    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData.dark(),
        home: Scaffold(
          body: Center(
            child: SizedBox(
              width: 390,
              height: 844,
              child: RepaintBoundary(
                key: boundaryKey,
                child: HomeScreen(apiService: mockApi),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.runAsync(() async {
      final RenderRepaintBoundary homeBoundary =
          boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final ui.Image homeImage = await homeBoundary.toImage(pixelRatio: 2.0);
      final homeByteData = await homeImage.toByteData(format: ui.ImageByteFormat.png);
      if (homeByteData != null) {
        File('../screenshot_mobile_home.png').writeAsBytesSync(homeByteData.buffer.asUint8List());
        // ignore: avoid_print
        print('Saved screenshot_mobile_home.png');
      }
      homeImage.dispose();
    });
  });

  testWidgets('Capture EditorScreen real UI render', (WidgetTester tester) async {
    final boundaryKey = GlobalKey();
    final now = DateTime.now().toUtc().toIso8601String();

    final sampleProject = Project(
      id: 'demo_pid',
      name: 'CaptionForge Demo',
      source: 'sample.mp4',
      duration: 6.0,
      width: 720,
      height: 1280,
      hasAudio: true,
      trim: const VideoTrim(start: 0.2, end: 6.0),
      createdAt: now,
      updatedAt: now,
      effects: const VideoEffects(
        sfx: 'Pop',
        sfxMode: 'Word',
        clearVoice: 'Balanced',
      ),
      tracks: [
        const Track(
          id: 't1',
          name: 'English (Original)',
          language: 'en',
          color: '#7352FF',
          style: SubtitleStyle(
            preset: 'Neon',
            textColor: '#FFFFFF',
            yPos: 0.74,
            animation: 'Pop',
            karaoke: true,
          ),
          captions: [
            CaptionCue(
              id: 'c1',
              start: 0.0,
              end: 3.0,
              text: 'Welcome to CaptionForge: AI Video Subtitles',
            ),
          ],
        ),
        const Track(
          id: 't2',
          name: 'Persian / Dari (Simultaneous)',
          language: 'fa',
          color: '#FFE844',
          style: SubtitleStyle(
            preset: 'Vazir',
            font: 'Tahoma',
            textColor: '#FFE844',
            yPos: 0.85,
            animation: 'Fade',
          ),
          captions: [
            CaptionCue(
              id: 'c2',
              start: 0.0,
              end: 3.0,
              text: 'به برنامه زیرنویس هوش مصنوعی خوش آمدید',
            ),
          ],
        ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData.dark(),
        home: Scaffold(
          body: Center(
            child: SizedBox(
              width: 390,
              height: 844,
              child: RepaintBoundary(
                key: boundaryKey,
                child: EditorScreen(
                  initialProject: sampleProject,
                  apiService: mockApi,
                ),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.runAsync(() async {
      final RenderRepaintBoundary editorBoundary =
          boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final ui.Image editorImage = await editorBoundary.toImage(pixelRatio: 2.0);
      final editorByteData = await editorImage.toByteData(format: ui.ImageByteFormat.png);
      if (editorByteData != null) {
        File('../screenshot_mobile_editor.png').writeAsBytesSync(editorByteData.buffer.asUint8List());
        // ignore: avoid_print
        print('Saved screenshot_mobile_editor.png');
      }
      editorImage.dispose();
    });
  });

  testWidgets('Capture Cinematic Overlay real UI render', (WidgetTester tester) async {
    final boundaryKey = GlobalKey();

    final testOverlays = [
      const GraphicOverlay(
        id: 'ov_persian',
        name: 'Orange Persian Typography',
        type: 'persian_orange_headline',
        content: 'فنلاند',
        start: 0.0,
        end: 5.0,
        xPos: 0.5,
        yPos: 0.25,
        scale: 1.2,
      ),
      const GraphicOverlay(
        id: 'ov_holo',
        name: 'Illuminated Hologram',
        type: 'glow_hologram',
        content: '◈ PETRONAS TOWERS ◈',
        start: 0.0,
        end: 5.0,
        xPos: 0.5,
        yPos: 0.45,
        scale: 1.1,
      ),
      const GraphicOverlay(
        id: 'ov_flag',
        name: 'Malaysian Flag',
        type: 'waving_flag',
        content: '⚑ MALAYSIA ⚑',
        start: 0.0,
        end: 5.0,
        xPos: 0.5,
        yPos: 0.62,
        scale: 1.0,
      ),
      const GraphicOverlay(
        id: 'ov_metal',
        name: 'Metallic Banner',
        type: 'metallic_banner',
        content: 'AUSTRALIA',
        start: 0.0,
        end: 5.0,
        xPos: 0.5,
        yPos: 0.80,
        scale: 1.25,
      ),
    ];

    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData.dark(),
        home: Scaffold(
          backgroundColor: const Color(0xFF0F172A),
          body: Center(
            child: SizedBox(
              width: 390,
              height: 700,
              child: RepaintBoundary(
                key: boundaryKey,
                child: Stack(
                  children: [
                    Container(color: const Color(0xFF0F172A)),
                    SubtitleOverlay(
                      tracks: const [],
                      overlays: testOverlays,
                      currentTime: 1.5,
                      canvasSize: const Size(390, 700),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.runAsync(() async {
      final RenderRepaintBoundary overlayBoundary =
          boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final ui.Image overlayImage = await overlayBoundary.toImage(pixelRatio: 2.0);
      final overlayByteData = await overlayImage.toByteData(format: ui.ImageByteFormat.png);
      if (overlayByteData != null) {
        File('../screenshot_cinematic_overlay.png').writeAsBytesSync(overlayByteData.buffer.asUint8List());
        // ignore: avoid_print
        print('Saved screenshot_cinematic_overlay.png');
      }
      overlayImage.dispose();
    });
  });
}
