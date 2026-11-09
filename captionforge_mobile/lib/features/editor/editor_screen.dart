import 'dart:async';
import 'dart:io' show File;
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import '../../core/constants/api_constants.dart';
import '../../core/theme/app_theme.dart';
import '../../core/services/api_service.dart';
import '../../models/project.dart';
import 'widgets/subtitle_overlay.dart';
import 'widgets/timeline_view.dart';
import 'widgets/tools_trays.dart';
import '../export/export_dialog.dart';

class EditorScreen extends StatefulWidget {
  final Project initialProject;
  final ApiService? apiService;

  const EditorScreen({
    super.key,
    required this.initialProject,
    this.apiService,
  });

  @override
  State<EditorScreen> createState() => _EditorScreenState();
}

class _EditorScreenState extends State<EditorScreen> {
  late Project _project;
  late final ApiService _api;

  final List<Project> _undoStack = [];
  final List<Project> _redoStack = [];

  double _currentTime = 0.0;
  bool _isPlaying = false;
  Timer? _playbackTimer;

  VideoPlayerController? _videoController;
  bool _isVideoInitialized = false;
  bool _isVideoLoading = false;

  bool _isSeeking = false;
  DateTime? _lastSeekTime;
  bool _isMuted = false;

  void _toggleMute() {
    setState(() => _isMuted = !_isMuted);
    _videoController?.setVolume(_isMuted ? 0.0 : 1.0);
  }

  int _activeTrackIndex = 0;
  String? _selectedCueId;
  EditorTool _activeTool = EditorTool.styles;

  bool _isSaving = false;
  double _aspectRatio = 9 / 16; // 9:16 portrait default, 1:1 square, 16:9 landscape
  bool _sfxActivePulse = false;

  @override
  void initState() {
    super.initState();
    _project = widget.initialProject;
    _api = widget.apiService ?? ApiService();
    _currentTime = _project.trim.start;
    _updateAspectRatioFromProject();
    _initVideoPlayer();
  }

  void _updateAspectRatioFromProject() {
    if (_project.aspectRatio == '16:9') {
      _aspectRatio = 16 / 9;
    } else if (_project.aspectRatio == '1:1') {
      _aspectRatio = 1.0;
    } else {
      _aspectRatio = 9 / 16;
    }
  }

  Future<void> _initVideoPlayer() async {
    setState(() {
      _isVideoLoading = true;
    });

    try {
      final source = _project.source;
      if (source.startsWith('http://') || source.startsWith('https://')) {
        _videoController = VideoPlayerController.networkUrl(Uri.parse(source));
      } else {
        final localFile = File(source);
        if (localFile.existsSync()) {
          _videoController = VideoPlayerController.file(localFile);
        } else if (_project.id.isNotEmpty) {
          final videoUrl = ApiConstants.projectVideo(_project.id);
          _videoController = VideoPlayerController.networkUrl(Uri.parse(videoUrl));
        }
      }

      if (_videoController != null) {
        await _videoController!.initialize();
        _videoController!.addListener(_onVideoPlayerUpdate);
        _videoController!.setVolume(_isMuted ? 0.0 : 1.0);

        // Seek to trim.start position
        final initialMs = (_currentTime * 1000).round();
        if (initialMs > 0) {
          await _videoController!.seekTo(Duration(milliseconds: initialMs));
        }

        if (mounted) {
          setState(() {
            _isVideoInitialized = true;
            _isVideoLoading = false;
          });
        }
      } else {
        if (mounted) setState(() => _isVideoLoading = false);
      }
    } catch (e) {
      try {
        _videoController?.removeListener(_onVideoPlayerUpdate);
        _videoController?.dispose();
      } catch (_) {}
      _videoController = null;
      // Graceful fallback to timer playhead in headless tests or when codec/network unavailable
      if (mounted) {
        setState(() {
          _isVideoInitialized = false;
          _isVideoLoading = false;
        });
      }
    }
  }

  void _onVideoPlayerUpdate() {
    if (!mounted || _videoController == null || _isSeeking) return;
    final val = _videoController!.value;
    if (!val.isInitialized) return;

    final posSec = val.position.inMilliseconds / 1000.0;
    final isPlaying = val.isPlaying;

    // Enforce trim boundary
    if (isPlaying) {
      if (posSec >= _project.trim.end) {
        _videoController!.pause();
        _videoController!.seekTo(Duration(milliseconds: (_project.trim.start * 1000).round()));
        setState(() {
          _currentTime = _project.trim.start;
          _isPlaying = false;
          _updateSfxPulse(_currentTime);
        });
        return;
      }
      if (posSec < _project.trim.start - 0.1) {
        _videoController!.seekTo(Duration(milliseconds: (_project.trim.start * 1000).round()));
        return;
      }
    }

    if (isPlaying != _isPlaying || (posSec - _currentTime).abs() > 0.04) {
      setState(() {
        _currentTime = posSec.clamp(_project.trim.start, _project.trim.end);
        _isPlaying = isPlaying;
        _updateSfxPulse(_currentTime);
      });
    }
  }

  void _updateSfxPulse(double time) {
    final sfx = _project.effects.sfx;
    if (sfx != 'None') {
      final track = _activeTrackIndex < _project.tracks.length
          ? _project.tracks[_activeTrackIndex]
          : _project.tracks.first;
      bool hit = false;
      for (final cue in track.captions) {
        if ((time - cue.start).abs() < 0.05) {
          hit = true;
          break;
        }
      }
      _sfxActivePulse = hit;
    } else {
      _sfxActivePulse = false;
    }
  }

  @override
  void dispose() {
    _playbackTimer?.cancel();
    _videoController?.removeListener(_onVideoPlayerUpdate);
    _videoController?.dispose();
    super.dispose();
  }

  void _pushUndoState() {
    _undoStack.add(_project.copyWith());
    _redoStack.clear();
    if (_undoStack.length > 30) {
      _undoStack.removeAt(0);
    }
  }

  void _undo() {
    if (_undoStack.isEmpty) return;
    _redoStack.add(_project.copyWith());
    setState(() {
      _project = _undoStack.removeLast();
    });
    _autosave();
  }

  void _redo() {
    if (_redoStack.isEmpty) return;
    _undoStack.add(_project.copyWith());
    setState(() {
      _project = _redoStack.removeLast();
    });
    _autosave();
  }

  void _togglePlayback() {
    if (_isVideoInitialized && _videoController != null) {
      if (_videoController!.value.isPlaying) {
        _videoController!.pause();
        setState(() => _isPlaying = false);
      } else {
        if (_currentTime >= _project.trim.end || _currentTime < _project.trim.start) {
          _currentTime = _project.trim.start;
          _videoController!.seekTo(Duration(milliseconds: (_currentTime * 1000).round()));
        }
        _videoController!.play();
        setState(() => _isPlaying = true);
      }
      return;
    }

    // Headless test fallback playhead
    setState(() {
      _isPlaying = !_isPlaying;
    });

    if (_isPlaying) {
      _playbackTimer?.cancel();
      _playbackTimer = Timer.periodic(const Duration(milliseconds: 33), (timer) {
        setState(() {
          _currentTime += 0.033;
          _updateSfxPulse(_currentTime);

          if (_currentTime >= _project.trim.end) {
            _currentTime = _project.trim.start;
            _isPlaying = false;
            _sfxActivePulse = false;
            timer.cancel();
          }
        });
      });
    } else {
      _playbackTimer?.cancel();
      setState(() => _sfxActivePulse = false);
    }
  }

  void _seekTo(double sec) {
    final clamped = sec.clamp(_project.trim.start, _project.trim.end);
    _isSeeking = true;
    _lastSeekTime = DateTime.now();
    setState(() {
      _currentTime = clamped;
      _updateSfxPulse(_currentTime);
    });
    if (_isVideoInitialized && _videoController != null) {
      _videoController!.seekTo(Duration(milliseconds: (clamped * 1000).round())).then((_) {
        Future.delayed(const Duration(milliseconds: 140), () {
          if (mounted && DateTime.now().difference(_lastSeekTime!).inMilliseconds >= 130) {
            setState(() => _isSeeking = false);
          }
        });
      });
    } else {
      _isSeeking = false;
    }
  }

  Future<void> _autosave() async {
    if (_project.id.isEmpty) return;
    setState(() => _isSaving = true);
    try {
      await _api.updateProject(_project.id, _project);
    } catch (_) {}
    if (mounted) {
      setState(() => _isSaving = false);
    }
  }

  void _updateActiveTrackStyle(SubtitleStyle newStyle) {
    _pushUndoState();
    final updatedTracks = List<Track>.from(_project.tracks);
    if (_activeTrackIndex < updatedTracks.length) {
      updatedTracks[_activeTrackIndex] = updatedTracks[_activeTrackIndex].copyWith(style: newStyle);
      setState(() {
        _project = _project.copyWith(tracks: updatedTracks);
      });
      _autosave();
    }
  }

  void _updateVideoEffects(VideoEffects newEffects) {
    _pushUndoState();
    setState(() {
      _project = _project.copyWith(effects: newEffects);
    });
    _autosave();
  }

  void _applyTemplate(String templateKey) {
    _pushUndoState();
    final curTrack = _project.tracks[_activeTrackIndex];
    SubtitleStyle newStyle = curTrack.style;
    VideoEffects newEffects = _project.effects;
    List<GraphicOverlay> newOverlays = List<GraphicOverlay>.from(_project.overlays);

    final overlayId = 'ov_${DateTime.now().millisecondsSinceEpoch}';

    switch (templateKey) {
      case 'v1_orange_headline':
        newStyle = curTrack.style.copyWith(
          preset: 'Orange Headline',
          font: 'Vazirmatn',
          fontSize: 32.0,
          textColor: '#FF7A00',
          bgColor: '#110D1A',
          bgEnabled: true,
          outline: 3.5,
          animation: 'Pop',
          yPos: 0.72,
        );
        newEffects = newEffects.copyWith(sfx: 'Pop', sfxMode: 'Sentence', sfxVolume: 65);
        break;

      case 'v1_orange_white_explainer':
        newStyle = curTrack.style.copyWith(
          preset: 'Orange/White Explainer',
          font: 'Noto Sans Arabic',
          fontSize: 26.0,
          textColor: '#FFFFFF',
          bgColor: '#1E1B2E',
          bgEnabled: true,
          outline: 2.0,
          animation: 'Fade',
          yPos: 0.78,
        );
        newEffects = newEffects.copyWith(sfx: 'Soft', sfxMode: 'Sentence', sfxVolume: 50);
        break;

      case 'v1_purple_flare':
        newEffects = newEffects.copyWith(sfx: 'Whoosh', sfxVolume: 55);
        newOverlays.add(GraphicOverlay(
          id: overlayId,
          name: 'Purple Light Flare',
          type: 'light_leak',
          content: '✦ PURPLE FLARE ✦',
          color: '#D946EF',
          start: _currentTime,
          end: (_currentTime + 1.2).clamp(0.0, _project.duration),
          animation: 'Fade',
          opacity: 0.85,
        ));
        break;

      case 'v2_hand_hologram':
        newEffects = newEffects.copyWith(sfx: 'Shimmer', sfxVolume: 60);
        newOverlays.add(GraphicOverlay(
          id: overlayId,
          name: 'Hand Glow Hologram',
          type: 'glow_hologram',
          content: '◈ HOLOGRAM ◈',
          color: '#38BDF8',
          start: _currentTime,
          end: (_currentTime + 3.0).clamp(0.0, _project.duration),
          xPos: 0.65,
          yPos: 0.38,
          scale: 1.25,
          animation: 'Pulse',
        ));
        break;

      case 'v2_soft_bg_words':
        newEffects = newEffects.copyWith(sfx: 'Ambient', sfxVolume: 45);
        newOverlays.add(GraphicOverlay(
          id: overlayId,
          name: 'Soft Persian Words',
          type: 'soft_bg_text',
          content: 'کپشن فورج شناور',
          color: '#94A3B8',
          start: _currentTime,
          end: (_currentTime + 3.5).clamp(0.0, _project.duration),
          xPos: 0.30,
          yPos: 0.35,
          scale: 1.0,
          opacity: 0.40,
          animation: 'Fade',
        ));
        break;

      case 'v2_flag_subject':
        newEffects = newEffects.copyWith(sfx: 'Riser', sfxVolume: 55);
        newOverlays.add(GraphicOverlay(
          id: overlayId,
          name: 'Flag Graphic Overlay',
          type: 'flag_graphic',
          content: '⚑ MALAYSIAN FLAG ⚑',
          color: '#F59E0B',
          start: _currentTime,
          end: (_currentTime + 3.5).clamp(0.0, _project.duration),
          xPos: 0.32,
          yPos: 0.30,
          scale: 1.1,
          animation: 'Fade',
        ));
        break;

      case 'v2_metallic_australia':
        newEffects = newEffects.copyWith(sfx: 'Glitch', sfxVolume: 65);
        newOverlays.add(GraphicOverlay(
          id: overlayId,
          name: 'Metallic Australia Reveal',
          type: 'metallic_banner',
          content: 'AUSTRALIA',
          color: '#E2E8F0',
          start: _currentTime,
          end: (_currentTime + 3.0).clamp(0.0, _project.duration),
          xPos: 0.50,
          yPos: 0.55,
          scale: 1.0,
          animation: 'Pop',
        ));
        break;

      case 'v2_end_accent':
        newEffects = newEffects.copyWith(sfx: 'Click', sfxVolume: 45);
        newOverlays.add(GraphicOverlay(
          id: overlayId,
          name: 'End Accent Title',
          type: 'end_accent',
          content: 'CaptionForge Studio',
          color: '#EF4444',
          start: _currentTime,
          end: (_currentTime + 2.5).clamp(0.0, _project.duration),
          xPos: 0.50,
          yPos: 0.88,
          scale: 1.0,
          animation: 'Slide down',
        ));
        break;
    }

    final updatedTracks = List<Track>.from(_project.tracks);
    updatedTracks[_activeTrackIndex] = curTrack.copyWith(style: newStyle);

    setState(() {
      _project = _project.copyWith(
        tracks: updatedTracks,
        effects: newEffects,
        overlays: newOverlays,
      );
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Applied template: $templateKey'),
        duration: const Duration(seconds: 2),
      ),
    );
    _autosave();
  }

  void _toggleOverlayVisibility(GraphicOverlay overlay) {
    _pushUndoState();
    final updatedOverlays = _project.overlays.map((ov) {
      if (ov.id == overlay.id) {
        return ov.copyWith(visible: !ov.visible);
      }
      return ov;
    }).toList();

    setState(() {
      _project = _project.copyWith(overlays: updatedOverlays);
    });
    _autosave();
  }

  void _deleteOverlay(String overlayId) {
    _pushUndoState();
    final updatedOverlays = _project.overlays.where((ov) => ov.id != overlayId).toList();
    setState(() {
      _project = _project.copyWith(overlays: updatedOverlays);
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Layer deleted'), duration: Duration(seconds: 2)),
    );
    _autosave();
  }

  void _editOverlay(GraphicOverlay overlay) {
    final nameCtrl = TextEditingController(text: overlay.name);
    final contentCtrl = TextEditingController(text: overlay.content);
    final startCtrl = TextEditingController(text: overlay.start.toStringAsFixed(2));
    final endCtrl = TextEditingController(text: overlay.end.toStringAsFixed(2));
    List<OverlayKeyframe> currentKeyframes = List<OverlayKeyframe>.from(overlay.keyframes);

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: AppColors.surfaceElevated,
          title: Text('Edit ', style: const TextStyle(fontWeight: FontWeight.w700)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(labelText: 'Layer Name', border: OutlineInputBorder()),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: contentCtrl,
                  decoration: const InputDecoration(labelText: 'Content / Text', border: OutlineInputBorder()),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: startCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: 'Start (s)', border: OutlineInputBorder()),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: endCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: 'End (s)', border: OutlineInputBorder()),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                const Divider(),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Keyframes ()',
                        style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: Colors.white)),
                    TextButton.icon(
                      icon: const Icon(Icons.add_circle_outline, size: 16),
                      label: const Text('Add at Playhead', style: TextStyle(fontSize: 11)),
                      onPressed: () {
                        final relT = (_currentTime - overlay.start).clamp(0.0, overlay.duration);
                        setDialogState(() {
                          currentKeyframes.add(OverlayKeyframe(
                            time: double.parse(relT.toStringAsFixed(2)),
                            xPos: overlay.xPos,
                            yPos: overlay.yPos,
                            scale: overlay.scale,
                            opacity: overlay.opacity,
                            rotation: overlay.rotation,
                            easing: 'easeInOut',
                          ));
                          currentKeyframes.sort((a, b) => a.time.compareTo(b.time));
                        });
                      },
                    ),
                  ],
                ),
                if (currentKeyframes.isNotEmpty)
                  Container(
                    constraints: const BoxConstraints(maxHeight: 120),
                    child: ListView.builder(
                      shrinkWrap: true,
                      itemCount: currentKeyframes.length,
                      itemBuilder: (context, idx) {
                        final kf = currentKeyframes[idx];
                        return Container(
                          margin: const EdgeInsets.symmetric(vertical: 2),
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.04),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                '${kf.time.toStringAsFixed(2)}s: pos(${kf.xPos.toStringAsFixed(2)}, ${kf.yPos.toStringAsFixed(2)}) sc(${kf.scale.toStringAsFixed(1)})',
                                style: const TextStyle(fontSize: 10, color: AppColors.textSecondary),
                              ),
                              InkWell(
                                onTap: () {
                                  setDialogState(() {
                                    currentKeyframes.removeAt(idx);
                                  });
                                },
                                child: const Icon(Icons.close, size: 14, color: Colors.redAccent),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
            ElevatedButton(
              onPressed: () {
                final newName = nameCtrl.text.trim();
                final newContent = contentCtrl.text.trim();
                final newStart = double.tryParse(startCtrl.text) ?? overlay.start;
                final newEnd = double.tryParse(endCtrl.text) ?? overlay.end;
                if (newEnd > newStart) {
                  _pushUndoState();
                  final updatedOverlays = _project.overlays.map((ov) {
                    if (ov.id == overlay.id) {
                      return ov.copyWith(
                        name: newName.isNotEmpty ? newName : ov.name,
                        content: newContent,
                        start: newStart,
                        end: newEnd,
                        keyframes: currentKeyframes,
                      );
                    }
                    return ov;
                  }).toList();
                  setState(() {
                    _project = _project.copyWith(overlays: updatedOverlays);
                  });
                  _autosave();
                }
                Navigator.pop(ctx);
              },
              child: const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }

  void _showAddOverlayModal() {
    final presets = [
      {'name': 'Glow Hologram', 'type': 'glow_hologram', 'content': '◈ HOLOGRAM ◈', 'color': '#38BDF8', 'icon': Icons.architecture},
      {'name': 'Metallic Australia', 'type': 'metallic_banner', 'content': 'AUSTRALIA', 'color': '#E2E8F0', 'icon': Icons.title},
      {'name': 'Purple Light Flare', 'type': 'light_leak', 'content': '✦ LIGHT FLARE ✦', 'color': '#D946EF', 'icon': Icons.flare},
      {'name': 'Floating Flag Graphic', 'type': 'flag_graphic', 'content': '⚑ MALAYSIA ⚑', 'color': '#F59E0B', 'icon': Icons.flag},
      {'name': 'Soft Background Words', 'type': 'soft_bg_text', 'content': 'کپشن فورج شناور', 'color': '#94A3B8', 'icon': Icons.blur_on},
      {'name': 'End Accent Title', 'type': 'end_accent', 'content': 'CaptionForge Studio', 'color': '#EF4444', 'icon': Icons.label_important},
    ];

    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surfaceElevated,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Add Graphic Overlay Layer', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: Colors.white)),
                const SizedBox(height: 12),
                ...presets.map((p) => ListTile(
                  leading: Icon(p['icon'] as IconData, color: Color(int.parse((p['color'] as String).replaceAll('#', '0xFF')))),
                  title: Text(p['name'] as String, style: const TextStyle(fontWeight: FontWeight.w600, color: Colors.white)),
                  subtitle: Text(p['content'] as String, style: const TextStyle(fontSize: 12, color: AppColors.textMuted)),
                  onTap: () {
                    Navigator.pop(ctx);
                    _pushUndoState();
                    final ovId = 'ov_${DateTime.now().millisecondsSinceEpoch}';
                    final newOv = GraphicOverlay(
                      id: ovId,
                      name: p['name'] as String,
                      type: p['type'] as String,
                      content: p['content'] as String,
                      color: p['color'] as String,
                      start: _currentTime,
                      end: (_currentTime + 3.0).clamp(0.0, _project.duration),
                      xPos: 0.50,
                      yPos: p['type'] == 'end_accent' ? 0.88 : (p['type'] == 'soft_bg_text' ? 0.35 : 0.50),
                    );
                    setState(() {
                      _project = _project.copyWith(overlays: [..._project.overlays, newOv]);
                      _activeTool = EditorTool.overlays;
                    });
                    _autosave();
                  },
                )),
              ],
            ),
          ),
        ),
      ),
    );
  }

  List<double> _buildColorMatrix() {
    final double b = _project.effects.brightness; // -0.8 to 0.8
    final double c = _project.effects.contrast;   // 0.5 to 2.0
    final double s = _project.effects.saturation; // 0.0 to 3.0

    final double invSat = 1.0 - s;
    final double r = 0.213 * invSat;
    final double g = 0.715 * invSat;
    final double bl = 0.072 * invSat;
    final double t = (1.0 - c) / 2.0 * 255.0 + (b * 255.0);

    return [
      (r + s) * c, g * c, bl * c, 0, t,
      r * c, (g + s) * c, bl * c, 0, t,
      r * c, g * c, (bl + s) * c, 0, t,
      0, 0, 0, 1, 0,
    ];
  }

  void _editCue(CaptionCue cue) {
    final textController = TextEditingController(text: cue.text);
    final startController = TextEditingController(text: cue.start.toStringAsFixed(2));
    final endController = TextEditingController(text: cue.end.toStringAsFixed(2));

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surfaceElevated,
        title: const Text('Edit Caption', style: TextStyle(fontWeight: FontWeight.w700)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: textController,
              maxLines: 3,
              decoration: const InputDecoration(
                labelText: 'Caption Text',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: startController,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: const InputDecoration(labelText: 'Start (s)', border: OutlineInputBorder()),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: TextField(
                    controller: endController,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: const InputDecoration(labelText: 'End (s)', border: OutlineInputBorder()),
                  ),
                ),
              ],
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              final newText = textController.text.trim();
              final newStart = double.tryParse(startController.text) ?? cue.start;
              final newEnd = double.tryParse(endController.text) ?? cue.end;

              if (newText.isNotEmpty && newEnd > newStart) {
                _pushUndoState();
                final currentTrack = _project.tracks[_activeTrackIndex];
                final updatedCaptions = currentTrack.captions.map((c) {
                  if (c.id == cue.id) {
                    return c.copyWith(text: newText, start: newStart, end: newEnd);
                  }
                  return c;
                }).toList();

                final updatedTracks = List<Track>.from(_project.tracks);
                updatedTracks[_activeTrackIndex] = currentTrack.copyWith(captions: updatedCaptions);

                setState(() {
                  _project = _project.copyWith(tracks: updatedTracks);
                });
                _autosave();
              }
              Navigator.pop(ctx);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _deleteCue(CaptionCue cue) {
    _pushUndoState();
    final currentTrack = _project.tracks[_activeTrackIndex];
    final updatedCaptions = currentTrack.captions.where((c) => c.id != cue.id).toList();
    final updatedTracks = List<Track>.from(_project.tracks);
    updatedTracks[_activeTrackIndex] = currentTrack.copyWith(captions: updatedCaptions);

    setState(() {
      _project = _project.copyWith(tracks: updatedTracks);
      if (_selectedCueId == cue.id) _selectedCueId = null;
    });
    _autosave();
  }

  void _splitCue(CaptionCue cue, double splitAt) {
    if (splitAt <= cue.start || splitAt >= cue.end) return;

    _pushUndoState();
    final currentTrack = _project.tracks[_activeTrackIndex];
    final words = cue.text.split(' ');
    final mid = (words.length / 2).ceil();
    final text1 = words.take(mid).join(' ');
    final text2 = words.skip(mid).join(' ');

    final cue1 = cue.copyWith(id: '${cue.id}_a', end: splitAt, text: text1);
    final cue2 = cue.copyWith(id: '${cue.id}_b', start: splitAt, text: text2);

    final updatedCaptions = <CaptionCue>[];
    for (final c in currentTrack.captions) {
      if (c.id == cue.id) {
        updatedCaptions.add(cue1);
        updatedCaptions.add(cue2);
      } else {
        updatedCaptions.add(c);
      }
    }

    final updatedTracks = List<Track>.from(_project.tracks);
    updatedTracks[_activeTrackIndex] = currentTrack.copyWith(captions: updatedCaptions);

    setState(() {
      _project = _project.copyWith(tracks: updatedTracks);
    });
    _autosave();
  }

  void _mergeCue(CaptionCue cue) {
    final currentTrack = _project.tracks[_activeTrackIndex];
    final idx = currentTrack.captions.indexWhere((c) => c.id == cue.id);
    if (idx == -1 || idx >= currentTrack.captions.length - 1) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Cannot merge: no subsequent subtitle cue in track')),
      );
      return;
    }

    final nextCue = currentTrack.captions[idx + 1];
    _pushUndoState();

    final merged = cue.copyWith(
      end: nextCue.end,
      text: '${cue.text} ${nextCue.text}'.trim(),
      words: [...cue.words, ...nextCue.words],
    );

    final updatedCaptions = List<CaptionCue>.from(currentTrack.captions);
    updatedCaptions[idx] = merged;
    updatedCaptions.removeAt(idx + 1);

    final updatedTracks = List<Track>.from(_project.tracks);
    updatedTracks[_activeTrackIndex] = currentTrack.copyWith(captions: updatedCaptions);

    setState(() {
      _project = _project.copyWith(tracks: updatedTracks);
      _selectedCueId = merged.id;
    });
    _autosave();
  }

  void _showTrackManagerModal() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surfaceElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setTrackSheetState) => Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Subtitle Tracks Manager', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
                  IconButton(
                    icon: const Icon(Icons.add_circle, color: AppColors.primaryPurpleLight),
                    tooltip: 'Add Subtitle Track',
                    onPressed: () {
                      _pushUndoState();
                      final newTr = Track(
                        id: 'tr_${DateTime.now().millisecondsSinceEpoch}',
                        name: 'Track ${_project.tracks.length + 1}',
                        language: 'en',
                        captions: [],
                      );
                      setState(() {
                        _project = _project.copyWith(tracks: [..._project.tracks, newTr]);
                        _activeTrackIndex = _project.tracks.length - 1;
                      });
                      setTrackSheetState(() {});
                      _autosave();
                    },
                  ),
                ],
              ),
              const SizedBox(height: 12),
              ...List.generate(_project.tracks.length, (idx) {
                final track = _project.tracks[idx];
                final isSelected = idx == _activeTrackIndex;

                return Card(
                  color: isSelected ? AppColors.surface : AppColors.surfaceElevated,
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    leading: Container(
                      width: 14,
                      height: 14,
                      decoration: BoxDecoration(
                        color: AppColors.trackColors[idx % AppColors.trackColors.length],
                        shape: BoxShape.circle,
                      ),
                    ),
                    title: Text('${track.name} (${track.language})', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    subtitle: Text('${track.captions.length} captions', style: const TextStyle(fontSize: 11, color: AppColors.textMuted)),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          icon: Icon(track.visible ? Icons.visibility : Icons.visibility_off, size: 20),
                          tooltip: 'Toggle Visibility',
                          onPressed: () {
                            _pushUndoState();
                            final updated = List<Track>.from(_project.tracks);
                            updated[idx] = track.copyWith(visible: !track.visible);
                            setState(() => _project = _project.copyWith(tracks: updated));
                            setTrackSheetState(() {});
                            _autosave();
                          },
                        ),
                        IconButton(
                          icon: Icon(track.locked ? Icons.lock : Icons.lock_open, size: 20),
                          tooltip: 'Toggle Lock',
                          onPressed: () {
                            _pushUndoState();
                            final updated = List<Track>.from(_project.tracks);
                            updated[idx] = track.copyWith(locked: !track.locked);
                            setState(() => _project = _project.copyWith(tracks: updated));
                            setTrackSheetState(() {});
                            _autosave();
                          },
                        ),
                        if (_project.tracks.length > 1)
                          IconButton(
                            icon: const Icon(Icons.delete_outline, color: AppColors.accentCoral, size: 20),
                            tooltip: 'Delete Track',
                            onPressed: () {
                              _pushUndoState();
                              final updated = List<Track>.from(_project.tracks)..removeAt(idx);
                              setState(() {
                                _project = _project.copyWith(tracks: updated);
                                if (_activeTrackIndex >= updated.length) {
                                  _activeTrackIndex = 0;
                                }
                              });
                              setTrackSheetState(() {});
                              _autosave();
                            },
                          ),
                      ],
                    ),
                    onTap: () {
                      setState(() => _activeTrackIndex = idx);
                      Navigator.pop(ctx);
                    },
                  ),
                );
              }),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _triggerAutoAiTranscription() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surfaceElevated,
        title: const Text('Auto AI Subtitles', style: TextStyle(fontWeight: FontWeight.w700)),
        content: const Text(
          'CaptionForge AI will analyze the video audio using Faster-Whisper, detect spoken language, and generate word-level synchronized captions.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.primaryPurple),
            onPressed: () => Navigator.pop(ctx, true),
            icon: const Icon(Icons.auto_awesome),
            label: const Text('Transcribe Video'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Starting AI Speech Transcription...')),
        );
      }
      try {
        final jobId = await _api.transcribe(_project.id, language: null, model: 'small');
        await _api.pollJob(jobId);
        final refreshed = await _api.getProject(_project.id);
        setState(() => _project = refreshed);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Transcription complete! ${refreshed.tracks.first.captions.length} captions generated.')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Transcription error: $e')),
          );
        }
      }
    }
  }

  void _showTranslateModal() {
    final languages = [
      {'code': 'fa', 'name': 'Persian (Dari / Farsi)'},
      {'code': 'ps', 'name': 'Pashto'},
      {'code': 'ur', 'name': 'Urdu'},
      {'code': 'ar', 'name': 'Arabic'},
      {'code': 'es', 'name': 'Spanish'},
      {'code': 'fr', 'name': 'French'},
      {'code': 'de', 'name': 'German'},
      {'code': 'hi', 'name': 'Hindi'},
      {'code': 'en', 'name': 'English'},
    ];

    final providers = [
      {'code': 'auto', 'name': 'Auto-detect AI Engine'},
      {'code': 'openai', 'name': 'OpenAI Neural (GPT-4o)'},
      {'code': 'anthropic', 'name': 'Anthropic Claude 3.5'},
      {'code': 'deepl', 'name': 'DeepL Neural MT'},
      {'code': 'local_transformer', 'name': 'Local NLLB / MarianMT'},
      {'code': 'offline', 'name': 'Offline Rule-Based (Built-in)'},
    ];

    String selectedLang = 'fa';
    String selectedMode = 'Simultaneous';
    String selectedProvider = 'auto';

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surfaceElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => SingleChildScrollView(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 20,
            top: 20,
            left: 20,
            right: 20,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'AI Subtitle Translation',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 16),
              const Text('Target Language:', style: TextStyle(color: AppColors.textSecondary, fontSize: 13)),
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                value: selectedLang,
                dropdownColor: AppColors.surface,
                items: languages.map((lang) {
                  return DropdownMenuItem(
                    value: lang['code']!,
                    child: Text(lang['name']!),
                  );
                }).toList(),
                onChanged: (val) {
                  if (val != null) setModalState(() => selectedLang = val);
                },
                decoration: const InputDecoration(border: OutlineInputBorder()),
              ),
              const SizedBox(height: 14),
              const Text('AI Translation Engine:', style: TextStyle(color: AppColors.textSecondary, fontSize: 13)),
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                value: selectedProvider,
                dropdownColor: AppColors.surface,
                items: providers.map((prov) {
                  return DropdownMenuItem(
                    value: prov['code']!,
                    child: Text(prov['name']!),
                  );
                }).toList(),
                onChanged: (val) {
                  if (val != null) setModalState(() => selectedProvider = val);
                },
                decoration: const InputDecoration(border: OutlineInputBorder()),
              ),
              const SizedBox(height: 14),
              const Text('Display Mode:', style: TextStyle(color: AppColors.textSecondary, fontSize: 13)),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: ChoiceChip(
                      label: const Text('Simultaneous (Dual Track)'),
                      selected: selectedMode == 'Simultaneous',
                      selectedColor: AppColors.primaryPurple,
                      onSelected: (val) => setModalState(() => selectedMode = 'Simultaneous'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ChoiceChip(
                      label: const Text('Replace Track'),
                      selected: selectedMode == 'Replace',
                      selectedColor: AppColors.primaryPurple,
                      onSelected: (val) => setModalState(() => selectedMode = 'Replace'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: () async {
                    Navigator.pop(ctx);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('Translating to $selectedLang ($selectedMode mode)...')),
                    );
                    try {
                      final track = _project.tracks[_activeTrackIndex];
                      final jobId = await _api.translate(
                        _project.id,
                        trackId: track.id,
                        language: selectedLang,
                        mode: selectedMode,
                        provider: selectedProvider == 'auto' ? null : selectedProvider,
                      );
                      await _api.pollJob(jobId);
                      final refreshed = await _api.getProject(_project.id);
                      setState(() => _project = refreshed);
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Translation completed successfully!')),
                        );
                      }
                    } catch (e) {
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('Translation error: $e')),
                        );
                      }
                    }
                  },
                  icon: const Icon(Icons.auto_fix_high),
                  label: const Text('Start AI Translation'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showExportSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => ExportDialog(
        project: _project,
        apiService: _api,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final currentTrack = _activeTrackIndex < _project.tracks.length
        ? _project.tracks[_activeTrackIndex]
        : _project.tracks.first;

    final selectedCue = _selectedCueId != null
        ? currentTrack.captions.where((c) => c.id == _selectedCueId).firstOrNull
        : null;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
        title: Row(
          children: [
            Expanded(
              child: Text(
                _project.name,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            if (_isSaving)
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 8),
                child: SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.undo),
            tooltip: 'Undo',
            onPressed: _undoStack.isNotEmpty ? _undo : null,
          ),
          IconButton(
            icon: const Icon(Icons.redo),
            tooltip: 'Redo',
            onPressed: _redoStack.isNotEmpty ? _redo : null,
          ),
          // Aspect Ratio Switcher (9:16, 1:1, 16:9)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: DropdownButton<String>(
              value: _project.aspectRatio,
              dropdownColor: AppColors.surfaceElevated,
              underline: const SizedBox.shrink(),
              items: const [
                DropdownMenuItem(value: '9:16', child: Text('9:16', style: TextStyle(fontSize: 12))),
                DropdownMenuItem(value: '1:1', child: Text('1:1', style: TextStyle(fontSize: 12))),
                DropdownMenuItem(value: '16:9', child: Text('16:9', style: TextStyle(fontSize: 12))),
              ],
              onChanged: (val) {
                if (val != null) {
                  setState(() {
                    _project = _project.copyWith(aspectRatio: val);
                    if (val == '16:9') {
                      _aspectRatio = 16 / 9;
                    } else if (val == '1:1') {
                      _aspectRatio = 1.0;
                    } else {
                      _aspectRatio = 9 / 16;
                    }
                  });
                  _autosave();
                }
              },
            ),
          ),
          // Resolution Selector
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: DropdownButton<int>(
              value: _project.resolution,
              dropdownColor: AppColors.surfaceElevated,
              underline: const SizedBox.shrink(),
              items: const [
                DropdownMenuItem(value: 480, child: Text('480p', style: TextStyle(fontSize: 12))),
                DropdownMenuItem(value: 720, child: Text('720p', style: TextStyle(fontSize: 12))),
                DropdownMenuItem(value: 1080, child: Text('1080p', style: TextStyle(fontSize: 12))),
              ],
              onChanged: (val) {
                if (val != null) {
                  setState(() => _project = _project.copyWith(resolution: val));
                  _autosave();
                }
              },
            ),
          ),
          // Export Button
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primaryPurple,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              ),
              onPressed: _showExportSheet,
              child: const Text('Export', style: TextStyle(fontWeight: FontWeight.w700)),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            // Center: Responsive Aspect Ratio Video Preview Canvas with Subtitle Overlays
            Expanded(
              flex: 5,
              child: Center(
                child: AspectRatio(
                  aspectRatio: _aspectRatio,
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.black,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                        color: _sfxActivePulse ? AppColors.accentCyan : AppColors.surfaceBorder,
                        width: _sfxActivePulse ? 2.5 : 1.5,
                      ),
                    ),
                    clipBehavior: Clip.antiAlias,
                    child: LayoutBuilder(
                      builder: (context, constraints) {
                        return Stack(
                          fit: StackFit.expand,
                          children: [
                            // Video Surface with Live Color Grading Filter
                            ColorFiltered(
                              colorFilter: ColorFilter.matrix(_buildColorMatrix()),
                              child: _isVideoInitialized && _videoController != null
                                  ? Center(
                                      child: AspectRatio(
                                        aspectRatio: _videoController!.value.aspectRatio > 0
                                            ? _videoController!.value.aspectRatio
                                            : _aspectRatio,
                                        child: VideoPlayer(_videoController!),
                                      ),
                                    )
                                  : Container(
                                      color: const Color(0xFF13101E),
                                      child: Center(
                                        child: Column(
                                          mainAxisAlignment: MainAxisAlignment.center,
                                          children: [
                                            Icon(
                                              Icons.movie_creation_outlined,
                                              size: 44,
                                              color: AppColors.primaryPurple.withOpacity(0.4),
                                            ),
                                            const SizedBox(height: 6),
                                            Text(
                                              '${_currentTime.toStringAsFixed(2)}s / ${_project.duration.toStringAsFixed(2)}s',
                                              style: const TextStyle(
                                                color: AppColors.textMuted,
                                                fontSize: 12,
                                                fontFamily: 'monospace',
                                              ),
                                            ),
                                            if (_isVideoLoading)
                                              const Padding(
                                                padding: EdgeInsets.only(top: 8),
                                                child: SizedBox(
                                                  width: 16,
                                                  height: 16,
                                                  child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.primaryPurpleLight),
                                                ),
                                              ),
                                            if (_project.effects.sfx != 'None')
                                              Padding(
                                                padding: const EdgeInsets.only(top: 4),
                                                child: Row(
                                                  mainAxisSize: MainAxisSize.min,
                                                  children: [
                                                    Icon(
                                                      Icons.volume_up,
                                                      size: 14,
                                                      color: _sfxActivePulse ? AppColors.accentCyan : AppColors.textMuted,
                                                    ),
                                                    const SizedBox(width: 4),
                                                    Flexible(
                                                      child: Text(
                                                        'SFX: ${_project.effects.sfx}',
                                                        maxLines: 1,
                                                        overflow: TextOverflow.ellipsis,
                                                        style: TextStyle(
                                                          fontSize: 10,
                                                          color: _sfxActivePulse ? AppColors.accentCyan : AppColors.textMuted,
                                                        ),
                                                      ),
                                                    ),
                                                  ],
                                                ),
                                              ),
                                            if (_project.effects.clearVoice != 'Off')
                                              Padding(
                                                padding: const EdgeInsets.only(top: 2),
                                                child: Row(
                                                  mainAxisSize: MainAxisSize.min,
                                                  children: [
                                                    const Icon(
                                                      Icons.record_voice_over,
                                                      size: 13,
                                                      color: AppColors.accentGreen,
                                                    ),
                                                    const SizedBox(width: 4),
                                                    Flexible(
                                                      child: Text(
                                                        'Clear Voice: ${_project.effects.clearVoice}',
                                                        maxLines: 1,
                                                        overflow: TextOverflow.ellipsis,
                                                        style: const TextStyle(
                                                          fontSize: 10,
                                                          fontWeight: FontWeight.w600,
                                                          color: AppColors.accentGreen,
                                                        ),
                                                      ),
                                                    ),
                                                  ],
                                                ),
                                              ),
                                          ],
                                        ),
                                      ),
                                    ),
                            ),

                            // Subtitle Overlay
                            SubtitleOverlay(
                              tracks: _project.tracks,
                              overlays: _project.overlays,
                              currentTime: _currentTime,
                              canvasSize: Size(constraints.maxWidth, constraints.maxHeight),
                            ),

                            // Floating Controls: Quick AI Subtitles CTA + Play/Pause
                            Positioned(
                              top: 10,
                              right: 10,
                              child: Row(
                                children: [
                                  InkWell(
                                    onTap: _showTrackManagerModal,
                                    child: Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                      decoration: BoxDecoration(
                                        color: AppColors.surfaceElevated.withOpacity(0.85),
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Row(
                                        children: [
                                          const Icon(Icons.layers, size: 14, color: AppColors.primaryPurpleLight),
                                          const SizedBox(width: 4),
                                          Text('${_project.tracks.length} Tracks', style: const TextStyle(fontSize: 11)),
                                        ],
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 6),
                                  InkWell(
                                    onTap: _triggerAutoAiTranscription,
                                    child: Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                      decoration: BoxDecoration(
                                        color: AppColors.primaryPurple.withOpacity(0.85),
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Row(
                                        children: const [
                                          Icon(Icons.auto_awesome, size: 14, color: Colors.white),
                                          SizedBox(width: 4),
                                          Text('AI Captions', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700)),
                                        ],
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),

                            Positioned(
                              bottom: 12,
                              right: 64,
                              child: FloatingActionButton.small(
                                key: const ValueKey('editor_mute_btn'),
                                heroTag: 'editor_mute_btn',
                                backgroundColor: AppColors.surfaceElevated.withOpacity(0.85),
                                foregroundColor: Colors.white,
                                tooltip: _isMuted ? 'Unmute' : 'Mute',
                                onPressed: _toggleMute,
                                child: Icon(_isMuted ? Icons.volume_off : Icons.volume_up, size: 18),
                              ),
                            ),

                            Positioned(
                              bottom: 12,
                              right: 12,
                              child: FloatingActionButton.small(
                                key: const ValueKey('editor_play_btn'),
                                heroTag: 'editor_play_btn',
                                backgroundColor: AppColors.surfaceElevated.withOpacity(0.85),
                                foregroundColor: Colors.white,
                                onPressed: _togglePlayback,
                                child: Icon(_isPlaying ? Icons.pause : Icons.play_arrow),
                              ),
                            ),
                          ],
                        );
                      },
                    ),
                  ),
                ),
              ),
            ),

            // Contextual Action Bar for Selected Cue (Edit, Split, Merge, Delete)
            if (selectedCue != null)
              Container(
                color: AppColors.surfaceElevated,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        'Cue: "${selectedCue.text}"',
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: Colors.white),
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.edit, size: 18),
                      tooltip: 'Edit Text & Timing',
                      onPressed: () => _editCue(selectedCue),
                    ),
                    IconButton(
                      icon: const Icon(Icons.content_cut, size: 18),
                      tooltip: 'Split Cue at Playhead',
                      onPressed: () => _splitCue(selectedCue, _currentTime),
                    ),
                    IconButton(
                      icon: const Icon(Icons.merge_type, size: 18),
                      tooltip: 'Merge with Next Cue',
                      onPressed: () => _mergeCue(selectedCue),
                    ),
                    IconButton(
                      icon: const Icon(Icons.delete_outline, color: AppColors.accentCoral, size: 18),
                      tooltip: 'Delete Cue',
                      onPressed: () => _deleteCue(selectedCue),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, size: 18),
                      tooltip: 'Deselect',
                      onPressed: () => setState(() => _selectedCueId = null),
                    ),
                  ],
                ),
              ),

            // Below: Multi-track Caption Timeline
            Expanded(
              flex: 4,
              child: TimelineView(
                project: _project,
                currentTime: _currentTime,
                activeTrackIndex: _activeTrackIndex,
                selectedCueId: _selectedCueId,
                onSeek: _seekTo,
                onSelectTrack: (idx) => setState(() => _activeTrackIndex = idx),
                onSelectCue: (cue) => setState(() => _selectedCueId = cue?.id),
                onEditCue: _editCue,
                onDeleteCue: _deleteCue,
                onSplitCue: _splitCue,
                onSelectOverlay: (ov) => setState(() => _activeTool = EditorTool.overlays),
              ),
            ),

            // Bottom Contextual Tools Tray (Styles, Fonts, Colors, Animations, SFX, Filters, etc.)
            ToolsTrays(
              activeTool: _activeTool,
              currentTrack: currentTrack,
              effects: _project.effects,
              onSelectTool: (t) => setState(() => _activeTool = t),
              onUpdateStyle: _updateActiveTrackStyle,
              onUpdateEffects: _updateVideoEffects,
              onTriggerTranslate: _showTranslateModal,
              onApplyTemplate: _applyTemplate,
              overlays: _project.overlays,
              onToggleOverlayVisibility: _toggleOverlayVisibility,
              onDeleteOverlay: _deleteOverlay,
              onEditOverlay: _editOverlay,
              onAddOverlay: _showAddOverlayModal,
            ),
          ],
        ),
      ),
    );
  }
}
