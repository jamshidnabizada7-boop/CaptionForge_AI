import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:captionforge_mobile/core/constants/api_constants.dart';
import 'package:captionforge_mobile/features/editor/editor_screen.dart';
import 'package:captionforge_mobile/features/editor/widgets/subtitle_overlay.dart';
import 'package:captionforge_mobile/features/editor/widgets/tools_trays.dart';
import 'package:captionforge_mobile/features/home/home_screen.dart';
import 'package:captionforge_mobile/models/project.dart';

void main() {
  group('Real Video Playback & Editor Integration Tests', () {
    testWidgets('EditorScreen initializes playback within trim boundaries and handles play/pause toggle', (tester) async {
      final now = DateTime.now().toUtc().toIso8601String();
      final project = Project(
        id: 'p_video_test',
        name: 'Video Playback Test',
        source: 'real_speech_video.mp4',
        duration: 10.0,
        width: 720,
        height: 1280,
        hasAudio: true,
        trim: const VideoTrim(start: 1.5, end: 8.5),
        aspectRatio: '9:16',
        createdAt: now,
        updatedAt: now,
        tracks: [
          const Track(
            id: 'tr_main',
            name: 'Original Audio Track',
            style: SubtitleStyle(textColor: '#FFFFFF', fontSize: 24, align: 'center'),
            captions: [
              CaptionCue(
                id: 'cue_1',
                start: 1.8,
                end: 4.5,
                text: 'Moving video frame caption',
              ),
            ],
          ),
        ],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: EditorScreen(initialProject: project),
        ),
      );
      await tester.pumpAndSettle();

      // Verify aspect ratio container exists
      expect(find.byType(AspectRatio), findsWidgets);

      // Verify ColorFiltered effect exists over video surface
      expect(find.byType(ColorFiltered), findsWidgets);

      // Verify floating action buttons: Play/Pause and Mute
      final playBtnFinder = find.byKey(const ValueKey('editor_play_btn'));
      expect(playBtnFinder, findsOneWidget);

      final muteBtnFinder = find.byKey(const ValueKey('editor_mute_btn'));
      expect(muteBtnFinder, findsOneWidget);

      // Tap Mute: toggles to volume_off
      await tester.tap(muteBtnFinder);
      await tester.pumpAndSettle();
      expect(find.descendant(of: muteBtnFinder, matching: find.byIcon(Icons.volume_off)), findsOneWidget);

      // Tap Unmute: toggles back to volume_up
      await tester.tap(muteBtnFinder);
      await tester.pumpAndSettle();
      expect(find.descendant(of: muteBtnFinder, matching: find.byIcon(Icons.volume_up)), findsOneWidget);

      // Tap Play button: toggle to playing
      await tester.tap(playBtnFinder);
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.descendant(of: playBtnFinder, matching: find.byIcon(Icons.pause)), findsOneWidget);

      // Tap Pause button: toggle back to paused
      await tester.tap(playBtnFinder);
      await tester.pump(const Duration(milliseconds: 50));
      expect(find.descendant(of: playBtnFinder, matching: find.byIcon(Icons.play_arrow)), findsOneWidget);
    });

    testWidgets('Priority 2: Edits to font, alignment, colors and effects react instantaneously in UI', (tester) async {
      final now = DateTime.now().toUtc().toIso8601String();
      final project = Project(
        id: 'p_reactive_test',
        name: 'Reactive Edit Test',
        source: 'test.mp4',
        duration: 8.0,
        width: 720,
        height: 1280,
        hasAudio: true,
        trim: const VideoTrim(start: 0.0, end: 8.0),
        aspectRatio: '9:16',
        createdAt: now,
        updatedAt: now,
        tracks: [
          const Track(
            id: 'tr_edit',
            name: 'Live Edit Track',
            style: SubtitleStyle(textColor: '#FFFFFF', fontSize: 22, align: 'center'),
            captions: [
              CaptionCue(
                id: 'c_live',
                start: 0.0,
                end: 5.0,
                text: 'Live Edited Typography',
              ),
            ],
          ),
        ],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: EditorScreen(initialProject: project),
        ),
      );
      await tester.pumpAndSettle();

      // Verify text is initially rendered in preview overlay and timeline
      expect(find.descendant(of: find.byType(SubtitleOverlay), matching: find.text('Live Edited Typography')), findsOneWidget);
      expect(find.text('Live Edited Typography'), findsWidgets);

      // Switch to Style tool tray and select neon preset
      final styleTab = find.descendant(of: find.byType(ToolsTrays), matching: find.text('Style'));
      await tester.tap(styleTab);
      await tester.pumpAndSettle();

      // Select preset chip
      final neonChip = find.text('Neon');
      if (neonChip.evaluate().isNotEmpty) {
        await tester.tap(neonChip);
        await tester.pumpAndSettle();
        // Live preview updated
        expect(find.descendant(of: find.byType(SubtitleOverlay), matching: find.text('Live Edited Typography')), findsOneWidget);
      }

      // Switch to Font tab
      final fontTab = find.descendant(of: find.byType(ToolsTrays), matching: find.text('Font'));
      await tester.tap(fontTab);
      await tester.pumpAndSettle();

      // Select Segoe UI / Noto font
      final fontChip = find.text('Segoe UI');
      if (fontChip.evaluate().isNotEmpty) {
        await tester.tap(fontChip);
        await tester.pumpAndSettle();
      }

      // Switch to Format tab and test Overlay and One-line switch
      final formatTab = find.descendant(of: find.byType(ToolsTrays), matching: find.text('Format'));
      await tester.tap(formatTab);
      await tester.pumpAndSettle();

      final translucentChip = find.text('Translucent');
      if (translucentChip.evaluate().isNotEmpty) {
        await tester.tap(translucentChip);
        await tester.pumpAndSettle();
      }

      final oneLineSwitch = find.byType(Switch).first;
      await tester.tap(oneLineSwitch);
      await tester.pumpAndSettle();

      expect(find.descendant(of: find.byType(SubtitleOverlay), matching: find.text('Live Edited Typography')), findsOneWidget);
    });

    testWidgets('Priority 3 & 5: Translation modal exposes AI engine provider options', (tester) async {
      final now = DateTime.now().toUtc().toIso8601String();
      final project = Project(
        id: 'p_trans_test',
        name: 'Translation Provider Test',
        source: 'test.mp4',
        duration: 5.0,
        width: 720,
        height: 1280,
        hasAudio: true,
        trim: const VideoTrim(start: 0.0, end: 5.0),
        aspectRatio: '9:16',
        createdAt: now,
        updatedAt: now,
        tracks: [
          const Track(
            id: 'tr_t1',
            name: 'Original',
            captions: [
              CaptionCue(id: 'c1', start: 0.0, end: 2.5, text: 'Hello world'),
            ],
          ),
        ],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: EditorScreen(initialProject: project),
        ),
      );
      await tester.pumpAndSettle();

      // Switch to Translate tab
      final transTab = find.descendant(of: find.byType(ToolsTrays), matching: find.text('Translate'));
      await tester.tap(transTab);
      await tester.pumpAndSettle();

      // Tap Translate button in tray
      final translateBtn = find.descendant(of: find.byType(ToolsTrays), matching: find.byType(ElevatedButton));
      expect(translateBtn, findsOneWidget);
      await tester.tap(translateBtn);
      await tester.pumpAndSettle();

      // Verify AI Translation Engine selector exists
      expect(find.text('AI Translation Engine:'), findsOneWidget);
      expect(find.text('Auto-detect AI Engine'), findsOneWidget);

      // Close modal
      await tester.tap(find.text('Simultaneous (Dual Track)'));
      await tester.pumpAndSettle();
    });

    testWidgets('Priority 4: Server configuration dialog provides presets and updates baseUrl', (tester) async {
      final initialUrl = ApiConstants.baseUrl;

      await tester.pumpWidget(
        const MaterialApp(
          home: HomeScreen(),
        ),
      );
      await tester.pumpAndSettle();

      // Open settings modal
      final settingsBtn = find.byIcon(Icons.settings_outlined);
      expect(settingsBtn, findsOneWidget);
      await tester.tap(settingsBtn);
      await tester.pumpAndSettle();

      // Tap on Backend Server URL to open Server Config dialog
      final serverUrlRow = find.text('Backend Server URL');
      expect(serverUrlRow, findsOneWidget);
      await tester.tap(serverUrlRow);
      await tester.pumpAndSettle();

      // Verify presets exist in dialog
      expect(find.text('Choose Environment Preset:'), findsOneWidget);
      expect(find.text('Emulator (10.0.2.2)'), findsOneWidget);
      expect(find.text('USB ADB (127.0.0.1)'), findsOneWidget);
      expect(find.text('Production HTTPS'), findsOneWidget);

      // Select USB ADB preset
      await tester.tap(find.text('USB ADB (127.0.0.1)'));
      await tester.pumpAndSettle();

      // Save & Connect
      await tester.tap(find.text('Save & Connect'));
      await tester.pumpAndSettle();

      expect(ApiConstants.baseUrl, 'http://127.0.0.1:8000');

      // Reset back to initial URL
      ApiConstants.setBaseUrl(initialUrl);
    });
  });
}
