import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:captionforge_mobile/main.dart';
import 'package:captionforge_mobile/features/editor/editor_screen.dart';
import 'package:captionforge_mobile/features/editor/widgets/subtitle_overlay.dart';
import 'package:captionforge_mobile/features/editor/widgets/timeline_view.dart';
import 'package:captionforge_mobile/models/project.dart';

void main() {
  testWidgets('CaptionForge app loads HomeScreen with brand header and New Project button', (tester) async {
    await tester.pumpWidget(const CaptionForgeApp());
    await tester.pumpAndSettle();

    expect(find.text('CaptionForge'), findsOneWidget);
    expect(find.text('New Project'), findsOneWidget);
    expect(find.text('Recent Projects'), findsOneWidget);
  });

  testWidgets('EditorScreen renders preview canvas, timeline, and bottom tools', (tester) async {
    final now = DateTime.now().toUtc().toIso8601String();
    final testProject = Project(
      id: 'test_project_1',
      name: 'Test Project',
      source: 'test.mp4',
      duration: 5.0,
      width: 720,
      height: 1280,
      hasAudio: true,
      trim: const VideoTrim(start: 0.0, end: 5.0),
      createdAt: now,
      updatedAt: now,
      tracks: [
        const Track(
          id: 'tr_1',
          name: 'Track 1',
          style: SubtitleStyle(preset: 'Bold', textColor: '#FFFFFF'),
          captions: [
            CaptionCue(
              id: 'c1',
              start: 0.0,
              end: 2.0,
              text: 'Active Subtitle Text',
            ),
            CaptionCue(
              id: 'c2',
              start: 2.2,
              end: 4.0,
              text: 'Second Subtitle Text',
            ),
          ],
        ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: EditorScreen(initialProject: testProject),
      ),
    );
    await tester.pumpAndSettle();

    // Verify header elements
    expect(find.text('Test Project'), findsOneWidget);
    expect(find.text('Export'), findsOneWidget);
    expect(find.byIcon(Icons.undo), findsOneWidget);
    expect(find.byIcon(Icons.redo), findsOneWidget);

    // Verify TimelineView
    expect(find.byType(TimelineView), findsOneWidget);

    // Verify Tools tabs
    expect(find.text('Style'), findsWidgets);
    expect(find.text('Format'), findsOneWidget);
    expect(find.text('Font'), findsOneWidget);
    expect(find.text('Color'), findsOneWidget);
    expect(find.text('Animation'), findsOneWidget);
    expect(find.text('Translate'), findsOneWidget);
    expect(find.text('SFX'), findsOneWidget);

    // Verify Cue selection shows action bar with Merge and Split
    final cueFinder = find.descendant(of: find.byType(TimelineView), matching: find.text('Active Subtitle Text'));
    await tester.tap(cueFinder);
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.merge_type), findsOneWidget);
    expect(find.byIcon(Icons.content_cut), findsOneWidget);
    expect(find.byIcon(Icons.edit), findsOneWidget);

    // Tap merge and verify cues merge across both timeline and preview overlay
    await tester.tap(find.byIcon(Icons.merge_type));
    await tester.pumpAndSettle();

    expect(find.text('Active Subtitle Text Second Subtitle Text'), findsNWidgets(2));
  });


  testWidgets('SubtitleOverlay displays active caption and supports bidirectional text', (tester) async {
    final tracks = [
      const Track(
        id: 't1',
        name: 'Arabic Script Track',
        style: SubtitleStyle(textColor: '#FFFFFF', yPos: 0.76),
        captions: [
          CaptionCue(
            id: 'c1',
            start: 1.0,
            end: 3.0,
            text: 'سلام به همه کاربران',
          ),
        ],
      ),
    ];

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SubtitleOverlay(
            tracks: tracks,
            currentTime: 2.0,
            canvasSize: const Size(360, 640),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.text('سلام به همه کاربران'), findsOneWidget);
  });

  testWidgets('EditorScreen displays Clear Voice badge and applies ColorFiltered effects', (tester) async {
    final now = DateTime.now().toUtc().toIso8601String();
    final testProject = Project(
      id: 'test_project_fx',
      name: 'FX Test Project',
      source: 'test.mp4',
      duration: 5.0,
      width: 720,
      height: 1280,
      hasAudio: true,
      trim: const VideoTrim(start: 0.0, end: 5.0),
      createdAt: now,
      updatedAt: now,
      effects: const VideoEffects(
        brightness: 0.1,
        contrast: 1.2,
        saturation: 1.3,
        clearVoice: 'Balanced',
        sfx: 'Pop',
      ),
      tracks: [
        const Track(
          id: 'tr_fx',
          name: 'Track 1',
          style: SubtitleStyle(textColor: '#FFFFFF'),
          captions: [],
        ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: EditorScreen(initialProject: testProject),
      ),
    );
    await tester.pumpAndSettle();

    // Verify ColorFiltered widget exists in the widget tree
    expect(find.byType(ColorFiltered), findsWidgets);

    // Verify Clear Voice badge is displayed
    expect(find.text('Clear Voice: Balanced'), findsOneWidget);
    expect(find.text('SFX: Pop'), findsOneWidget);
  });

  testWidgets('SubtitleOverlay renders Persian, Pashto and Urdu simultaneously with proper directionality', (tester) async {
    final tracks = [
      const Track(
        id: 't_fa',
        name: 'Persian Track',
        style: SubtitleStyle(textColor: '#FFFFFF', yPos: 0.65),
        captions: [
          CaptionCue(
            id: 'c_fa',
            start: 1.0,
            end: 4.0,
            text: 'به برنامه زیرنویس خوش آمدید',
          ),
        ],
      ),
      const Track(
        id: 't_ps',
        name: 'Pashto Track',
        style: SubtitleStyle(textColor: '#33C6BD', yPos: 0.76),
        captions: [
          CaptionCue(
            id: 'c_ps',
            start: 1.0,
            end: 4.0,
            text: 'د ویډیو فرعي سرلیکونو اپلیکیشن ته ښه راغلاست',
          ),
        ],
      ),
      const Track(
        id: 't_ur',
        name: 'Urdu Track',
        style: SubtitleStyle(textColor: '#F59E0B', yPos: 0.87),
        captions: [
          CaptionCue(
            id: 'c_ur',
            start: 1.0,
            end: 4.0,
            text: 'کپشن فورج اے آئی ویڈیو سب ٹائٹل ایپ میں خوش آمدید',
          ),
        ],
      ),
    ];

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SubtitleOverlay(
            tracks: tracks,
            currentTime: 2.5,
            canvasSize: const Size(360, 640),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.text('به برنامه زیرنویس خوش آمدید'), findsOneWidget);
    expect(find.text('د ویډیو فرعي سرلیکونو اپلیکیشن ته ښه راغلاست'), findsOneWidget);
    expect(find.text('کپشن فورج اے آئی ویڈیو سب ٹائٹل ایپ میں خوش آمدید'), findsOneWidget);
  });

  testWidgets('SubtitleOverlay renders foreground and background graphic overlays without layout overflow', (tester) async {
    final overlays = [
      const GraphicOverlay(
        id: 'ov_holo',
        name: 'Hand Hologram',
        type: 'glow_hologram',
        content: '◈ HOLOGRAM ◈',
        start: 1.0,
        end: 4.0,
        xPos: 0.5,
        yPos: 0.35,
      ),
      const GraphicOverlay(
        id: 'ov_banner',
        name: 'Metallic Australia',
        type: 'metallic_banner',
        content: 'AUSTRALIA',
        start: 1.0,
        end: 4.0,
        xPos: 0.5,
        yPos: 0.55,
      ),
      const GraphicOverlay(
        id: 'ov_bg',
        name: 'Soft Words',
        type: 'soft_bg_text',
        content: 'کپشن فورج شناور',
        start: 1.0,
        end: 4.0,
        xPos: 0.3,
        yPos: 0.3,
      ),
      const GraphicOverlay(
        id: 'ov_accent',
        name: 'End Accent',
        type: 'end_accent',
        content: 'CaptionForge Studio',
        start: 1.0,
        end: 4.0,
        xPos: 0.5,
        yPos: 0.88,
      ),
    ];

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SubtitleOverlay(
            tracks: const [],
            overlays: overlays,
            currentTime: 2.0,
            canvasSize: const Size(400, 700),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.text('◈ HOLOGRAM ◈'), findsOneWidget);
    expect(find.text('AUSTRALIA'), findsOneWidget);
    expect(find.text('کپشن فورج شناور'), findsOneWidget);
    expect(find.text('CaptionForge Studio'), findsOneWidget);
  });

  testWidgets('TimelineView renders Overlays lane when project has graphic overlays', (tester) async {
    final now = DateTime.now().toIso8601String();
    GraphicOverlay? selectedOverlay;

    final testProject = Project(
      id: 'p_ov_test',
      name: 'Overlay Timeline Test',
      source: 'test.mp4',
      duration: 10.0,
      width: 720,
      height: 1280,
      hasAudio: true,
      trim: const VideoTrim(start: 0.0, end: 10.0),
      createdAt: now,
      updatedAt: now,
      overlays: const [
        GraphicOverlay(
          id: 'ov_lane_1',
          name: 'Hand Hologram',
          type: 'glow_hologram',
          start: 2.0,
          end: 6.0,
          color: '#38BDF8',
        ),
      ],
      tracks: [
        const Track(id: 'tr1', name: 'Track 1', captions: []),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SizedBox(
            height: 200,
            child: TimelineView(
              project: testProject,
              currentTime: 3.0,
              activeTrackIndex: 0,
              selectedCueId: null,
              onSeek: (_) {},
              onSelectTrack: (_) {},
              onSelectCue: (_) {},
              onEditCue: (_) {},
              onDeleteCue: (_) {},
              onSplitCue: (_, __) {},
              onSelectOverlay: (ov) => selectedOverlay = ov,
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('LAYERS (1)'), findsOneWidget);
    expect(find.text('Hand Hologram'), findsOneWidget);

    await tester.tap(find.text('Hand Hologram'));
    expect(selectedOverlay?.id, 'ov_lane_1');
  });
}

