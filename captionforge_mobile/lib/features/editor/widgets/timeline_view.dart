import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';
import '../../../models/project.dart';

class TimelineView extends StatefulWidget {
  final Project project;
  final double currentTime;
  final int activeTrackIndex;
  final String? selectedCueId;
  final ValueChanged<double> onSeek;
  final ValueChanged<int> onSelectTrack;
  final ValueChanged<CaptionCue?> onSelectCue;
  final Function(CaptionCue cue) onEditCue;
  final Function(CaptionCue cue) onDeleteCue;
  final Function(CaptionCue cue, double splitAt) onSplitCue;
  final Function(GraphicOverlay overlay)? onSelectOverlay;

  const TimelineView({
    super.key,
    required this.project,
    required this.currentTime,
    required this.activeTrackIndex,
    required this.selectedCueId,
    required this.onSeek,
    required this.onSelectTrack,
    required this.onSelectCue,
    required this.onEditCue,
    required this.onDeleteCue,
    required this.onSplitCue,
    this.onSelectOverlay,
  });

  @override
  State<TimelineView> createState() => _TimelineViewState();
}

class _TimelineViewState extends State<TimelineView> {
  double _pixelsPerSecond = 80.0;
  final ScrollController _scrollController = ScrollController();

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  void didUpdateWidget(covariant TimelineView oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Auto-scroll timeline to follow playhead if playhead approaches right edge
    if (widget.currentTime != oldWidget.currentTime && _scrollController.hasClients) {
      final playheadPx = widget.currentTime * _pixelsPerSecond;
      final viewportWidth = _scrollController.position.viewportDimension;
      final scrollOffset = _scrollController.offset;

      if (playheadPx > scrollOffset + viewportWidth - 60 || playheadPx < scrollOffset) {
        _scrollController.animateTo(
          (playheadPx - viewportWidth * 0.3).clamp(0.0, _scrollController.position.maxScrollExtent),
          duration: const Duration(milliseconds: 120),
          curve: Curves.easeOut,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final duration = widget.project.duration > 0 ? widget.project.duration : 10.0;
    final totalWidth = duration * _pixelsPerSecond + 200;

    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        border: Border(
          top: BorderSide(color: AppColors.surfaceBorder, width: 1),
          bottom: BorderSide(color: AppColors.surfaceBorder, width: 1),
        ),
      ),
      child: Column(
        children: [
          // Timeline Controls (Zoom & Active Track Selector)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            child: Row(
              children: [
                // Track tabs
                Expanded(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: List.generate(widget.project.tracks.length, (i) {
                        final track = widget.project.tracks[i];
                        final isSelected = i == widget.activeTrackIndex;
                        final color = AppColors.trackColors[i % AppColors.trackColors.length];

                        return Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: InkWell(
                            onTap: () => widget.onSelectTrack(i),
                            borderRadius: BorderRadius.circular(20),
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                              decoration: BoxDecoration(
                                color: isSelected ? color.withOpacity(0.2) : Colors.transparent,
                                border: Border.all(
                                  color: isSelected ? color : AppColors.surfaceBorder,
                                  width: 1.5,
                                ),
                                borderRadius: BorderRadius.circular(20),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Container(
                                    width: 8,
                                    height: 8,
                                    decoration: BoxDecoration(
                                      color: color,
                                      shape: BoxShape.circle,
                                    ),
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    track.name.isNotEmpty ? track.name : 'Track ${i + 1}',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                                      color: isSelected ? Colors.white : AppColors.textSecondary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      }),
                    ),
                  ),
                ),
                // Zoom Controls
                IconButton(
                  icon: const Icon(Icons.remove, size: 18),
                  visualDensity: VisualDensity.compact,
                  onPressed: () {
                    setState(() {
                      _pixelsPerSecond = (_pixelsPerSecond - 20).clamp(40.0, 240.0);
                    });
                  },
                ),
                Text(
                  '${_pixelsPerSecond.toInt()}px/s',
                  style: const TextStyle(fontSize: 11, color: AppColors.textMuted),
                ),
                IconButton(
                  icon: const Icon(Icons.add, size: 18),
                  visualDensity: VisualDensity.compact,
                  onPressed: () {
                    setState(() {
                      _pixelsPerSecond = (_pixelsPerSecond + 20).clamp(40.0, 240.0);
                    });
                  },
                ),
              ],
            ),
          ),
          const Divider(height: 1),

          // Horizontal Timeline Scrubber
          Expanded(
            child: LayoutBuilder(
              builder: (context, constraints) => SingleChildScrollView(
                controller: _scrollController,
                scrollDirection: Axis.horizontal,
                physics: const ClampingScrollPhysics(),
                child: SizedBox(
                  width: totalWidth,
                  height: constraints.maxHeight,
                  child: Stack(

                  children: [
                    // Background scrubber area for seeking
                    Positioned.fill(
                      child: GestureDetector(
                        behavior: HitTestBehavior.translucent,
                        onHorizontalDragUpdate: (details) {
                          final seekSec = (details.localPosition.dx / _pixelsPerSecond).clamp(0.0, duration);
                          widget.onSeek(seekSec);
                        },
                        onTapDown: (details) {
                          final seekSec = (details.localPosition.dx / _pixelsPerSecond).clamp(0.0, duration);
                          widget.onSeek(seekSec);
                        },
                      ),
                    ),
                    // Time ruler & background seconds grid
                    _buildTimeRuler(duration),

                    // Visual Trim Boundaries (dimmed non-active playback zones)
                    if (widget.project.trim.start > 0)
                      Positioned(
                        left: 0,
                        width: widget.project.trim.start * _pixelsPerSecond,
                        top: 24,
                        bottom: 0,
                        child: IgnorePointer(
                          child: Container(
                            color: Colors.black.withOpacity(0.55),
                            child: Align(
                              alignment: Alignment.centerRight,
                              child: Container(
                                width: 2.5,
                                color: AppColors.primaryPurpleLight,
                              ),
                            ),
                          ),
                        ),
                      ),
                    if (widget.project.trim.end < duration)
                      Positioned(
                        left: widget.project.trim.end * _pixelsPerSecond,
                        width: (duration - widget.project.trim.end) * _pixelsPerSecond + 200,
                        top: 24,
                        bottom: 0,
                        child: IgnorePointer(
                          child: Container(
                            color: Colors.black.withOpacity(0.55),
                            child: Align(
                              alignment: Alignment.centerLeft,
                              child: Container(
                                width: 2.5,
                                color: AppColors.primaryPurpleLight,
                              ),
                            ),
                          ),
                        ),
                      ),

                      // Tracks Lanes
                      Positioned(
                        top: 26,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        child: Column(
                          children: [
                            if (widget.project.overlays.isNotEmpty)
                              Expanded(
                                flex: 1,
                                child: Container(
                                  margin: const EdgeInsets.symmetric(vertical: 2),
                                  decoration: BoxDecoration(
                                    color: Colors.white.withOpacity(0.04),
                                    border: Border(
                                      bottom: BorderSide(
                                        color: AppColors.surfaceBorder.withOpacity(0.5),
                                        width: 0.5,
                                      ),
                                    ),
                                  ),
                                  child: Stack(
                                    children: [
                                      Positioned(
                                        left: 8,
                                        top: 2,
                                        child: Text(
                                          'LAYERS (${widget.project.overlays.length})',
                                          style: const TextStyle(
                                            fontSize: 9,
                                            fontWeight: FontWeight.w700,
                                            color: AppColors.textMuted,
                                            letterSpacing: 0.5,
                                          ),
                                        ),
                                      ),
                                      ...widget.project.overlays.map((ov) {
                                        final left = ov.start * _pixelsPerSecond;
                                        final width = ((ov.end - ov.start) * _pixelsPerSecond).clamp(24.0, totalWidth);
                                        final colorHex = ov.color.replaceAll('#', '0xFF');
                                        final ovColor = Color(int.tryParse(colorHex) ?? 0xFF38BDF8);

                                        return Positioned(
                                          left: left,
                                          width: width,
                                          top: 14,
                                          bottom: 3,
                                          child: GestureDetector(
                                            onTap: () {
                                              if (widget.onSelectOverlay != null) {
                                                widget.onSelectOverlay!(ov);
                                              }
                                            },
                                            child: Container(
                                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                                              decoration: BoxDecoration(
                                                color: ovColor.withOpacity(ov.visible ? 0.75 : 0.25),
                                                borderRadius: BorderRadius.circular(5),
                                                border: Border.all(
                                                  color: Colors.white.withOpacity(0.7),
                                                  width: 1,
                                                ),
                                              ),
                                              child: Row(
                                                mainAxisSize: MainAxisSize.min,
                                                children: [
                                                  Icon(
                                                    ov.visible ? Icons.layers : Icons.layers_clear,
                                                    size: 11,
                                                    color: Colors.white,
                                                  ),
                                                  const SizedBox(width: 4),
                                                  Flexible(
                                                    child: Text(
                                                      ov.name,
                                                      maxLines: 1,
                                                      overflow: TextOverflow.ellipsis,
                                                      style: const TextStyle(
                                                        fontSize: 10,
                                                        fontWeight: FontWeight.w600,
                                                        color: Colors.white,
                                                      ),
                                                    ),
                                                  ),
                                                  if (ov.keyframes.isNotEmpty) ...[
                                                    const SizedBox(width: 4),
                                                    ...ov.keyframes.take(4).map((_) => Container(
                                                          width: 5,
                                                          height: 5,
                                                          margin: const EdgeInsets.only(right: 2),
                                                          decoration: const BoxDecoration(
                                                            color: Colors.amberAccent,
                                                            shape: BoxShape.circle,
                                                          ),
                                                        )),
                                                  ],
                                                ],
                                              ),
                                            ),
                                          ),
                                        );
                                      }),
                                    ],
                                  ),
                                ),
                              ),
                            ...List.generate(widget.project.tracks.length, (trackIdx) {
                            final track = widget.project.tracks[trackIdx];
                            final isCurrent = trackIdx == widget.activeTrackIndex;
                            final color = AppColors.trackColors[trackIdx % AppColors.trackColors.length];

                            return Expanded(
                              child: Container(
                                margin: const EdgeInsets.symmetric(vertical: 2),
                                decoration: BoxDecoration(
                                  color: isCurrent ? AppColors.surfaceElevated : Colors.transparent,
                                  border: Border(
                                    bottom: BorderSide(
                                      color: AppColors.surfaceBorder.withOpacity(0.5),
                                      width: 0.5,
                                    ),
                                  ),
                                ),
                                child: Stack(
                                  children: track.captions.map((cue) {
                                    final isCueSelected = cue.id == widget.selectedCueId;
                                    final left = cue.start * _pixelsPerSecond;
                                    final width = (cue.end - cue.start) * _pixelsPerSecond;

                                    return Positioned(
                                      left: left,
                                      width: width.clamp(20.0, totalWidth),
                                      top: 4,
                                      bottom: 4,
                                      child: GestureDetector(
                                        behavior: HitTestBehavior.opaque,
                                        onTapDown: (_) {
                                          widget.onSelectTrack(trackIdx);
                                          widget.onSelectCue(cue);
                                        },
                                        onTap: () {
                                          widget.onSelectTrack(trackIdx);
                                          widget.onSelectCue(cue);
                                        },
                                        onDoubleTap: () => widget.onEditCue(cue),
                                        onLongPress: () {
                                          widget.onSelectTrack(trackIdx);
                                          widget.onSelectCue(cue);
                                          if (widget.currentTime > cue.start && widget.currentTime < cue.end) {
                                            widget.onSplitCue(cue, widget.currentTime);
                                          } else {
                                            widget.onEditCue(cue);
                                          }
                                        },
                                        child: Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                          decoration: BoxDecoration(
                                            color: color.withOpacity(isCueSelected ? 0.9 : 0.45),
                                            borderRadius: BorderRadius.circular(6),
                                            border: Border.all(
                                              color: isCueSelected ? Colors.white : color,
                                              width: isCueSelected ? 2 : 1,
                                            ),
                                            boxShadow: isCueSelected
                                                ? [
                                                    BoxShadow(
                                                      color: color.withOpacity(0.5),
                                                      blurRadius: 6,
                                                      spreadRadius: 1,
                                                    )
                                                  ]
                                                : null,
                                          ),
                                          child: Text(
                                            cue.text,
                                            overflow: TextOverflow.ellipsis,
                                            maxLines: 1,
                                            style: TextStyle(
                                              fontSize: 11,
                                              fontWeight: isCueSelected ? FontWeight.w700 : FontWeight.w500,
                                              color: Colors.white,
                                            ),
                                          ),
                                        ),
                                      ),
                                    );
                                  }).toList(),
                                ),
                              ),
                            );
                          }),
                          if (widget.project.audioClips.isNotEmpty)
                            Expanded(
                              flex: 1,
                              child: Container(
                                margin: const EdgeInsets.symmetric(vertical: 2),
                                decoration: BoxDecoration(
                                  color: const Color(0xFF10B981).withOpacity(0.06),
                                  border: Border(
                                    bottom: BorderSide(
                                      color: AppColors.surfaceBorder.withOpacity(0.5),
                                      width: 0.5,
                                    ),
                                  ),
                                ),
                                child: Stack(
                                  children: [
                                    Positioned(
                                      left: 8,
                                      top: 2,
                                      child: Text(
                                        'AUDIO / SFX ()',
                                        style: const TextStyle(
                                          fontSize: 9,
                                          fontWeight: FontWeight.w700,
                                          color: Color(0xFF34D399),
                                          letterSpacing: 0.5,
                                        ),
                                      ),
                                    ),
                                    ...widget.project.audioClips.map((clip) {
                                      final left = clip.start * _pixelsPerSecond;
                                      final width = (clip.duration * _pixelsPerSecond).clamp(24.0, totalWidth);
                                      return Positioned(
                                        left: left,
                                        width: width,
                                        top: 14,
                                        bottom: 3,
                                        child: Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                                          decoration: BoxDecoration(
                                            color: const Color(0xFF059669).withOpacity(0.8),
                                            borderRadius: BorderRadius.circular(5),
                                            border: Border.all(color: const Color(0xFF34D399), width: 1),
                                          ),
                                          child: Row(
                                            mainAxisSize: MainAxisSize.min,
                                            children: [
                                              const Icon(Icons.volume_up, size: 11, color: Colors.white),
                                              const SizedBox(width: 4),
                                              Flexible(
                                                child: Text(
                                                  clip.name,
                                                  maxLines: 1,
                                                  overflow: TextOverflow.ellipsis,
                                                  style: const TextStyle(
                                                    fontSize: 10,
                                                    fontWeight: FontWeight.w600,
                                                    color: Colors.white,
                                                  ),
                                                ),
                                              ),
                                            ],
                                          ),
                                        ),
                                      );
                                    }),
                                  ],
                                ),
                              ),
                            ),
                        ],
                      ),
                    ),

                      // Red Playhead Indicator
                      Positioned(
                        left: widget.currentTime * _pixelsPerSecond,
                        top: 0,
                        bottom: 0,
                        child: IgnorePointer(
                          child: Container(
                            width: 2,
                            color: AppColors.accentCoral,
                            child: Stack(
                              clipBehavior: Clip.none,
                              children: [
                                Positioned(
                                  top: 0,
                                  left: -5,
                                  child: Container(
                                    width: 12,
                                    height: 12,
                                    decoration: const BoxDecoration(
                                      color: AppColors.accentCoral,
                                      shape: BoxShape.circle,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),

                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );


  }

  Widget _buildTimeRuler(double totalDuration) {
    final secondCount = totalDuration.ceil() + 2;
    return SizedBox(
      height: 24,
      child: Stack(
        children: List.generate(secondCount, (s) {
          final x = s * _pixelsPerSecond;
          return Positioned(
            left: x,
            top: 0,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 1,
                  height: 8,
                  color: AppColors.textMuted,
                ),
                const SizedBox(height: 2),
                Text(
                  '${s}s',
                  style: const TextStyle(fontSize: 9, color: AppColors.textMuted),
                ),
              ],
            ),
          );
        }),
      ),
    );
  }
}
