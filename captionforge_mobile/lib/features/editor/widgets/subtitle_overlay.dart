import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import '../../../models/project.dart';

class SubtitleOverlay extends StatelessWidget {
  final List<Track> tracks;
  final List<GraphicOverlay> overlays;
  final double currentTime;
  final Size canvasSize;

  const SubtitleOverlay({
    super.key,
    required this.tracks,
    this.overlays = const [],
    required this.currentTime,
    required this.canvasSize,
  });

  bool _isRtl(String text) {
    return RegExp(r'[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]').hasMatch(text);
  }

  Color _parseColor(String hex, [int opacity = 100]) {
    String cleanHex = hex.replaceAll('#', '');
    if (cleanHex.length == 6) {
      cleanHex = 'FF$cleanHex';
    }
    int val = int.tryParse(cleanHex, radix: 16) ?? 0xFFFFFFFF;
    return Color(val).withOpacity((opacity / 100.0).clamp(0.0, 1.0));
  }

  double _evaluateEasing(double t, String easing) {
    t = t.clamp(0.0, 1.0);
    switch (easing) {
      case 'linear':
        return t;
      case 'easeIn':
        return t * t;
      case 'easeOut':
        return t * (2.0 - t);
      case 'easeInOut':
        return 0.5 * (1.0 - math.cos(math.pi * t));
      case 'bounce':
        if (t < (1 / 2.75)) {
          return 7.5625 * t * t;
        } else if (t < (2 / 2.75)) {
          t -= (1.5 / 2.75);
          return 7.5625 * t * t + 0.75;
        } else if (t < (2.5 / 2.75)) {
          t -= (2.25 / 2.75);
          return 7.5625 * t * t + 0.9375;
        } else {
          t -= (2.625 / 2.75);
          return 7.5625 * t * t + 0.984375;
        }
      default:
        return t;
    }
  }

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: Stack(
        children: [
          // Depth Layer 0: Background Overlays occluded behind speaker
          ...overlays
              .where((ov) => ov.visible && (ov.isBackground || ov.depthLayer == 0 || ov.type == 'soft_bg_text') &&
                  currentTime >= ov.start && currentTime <= ov.end)
              .map(_buildGraphicOverlay),

          // Subtitle Tracks
          ...tracks.where((t) => t.visible && t.captions.isNotEmpty).map((track) {
            final style = track.style;
            final earlyOffset = style.early;
            final adjustedTime = currentTime + earlyOffset;

            CaptionCue? activeCue;
            for (final cue in track.captions) {
              if (adjustedTime >= cue.start && adjustedTime <= cue.end) {
                activeCue = cue;
                break;
              }
            }

            if (activeCue == null || activeCue.text.trim().isEmpty) {
              return const SizedBox.shrink();
            }

            final elapsed = adjustedTime - activeCue.start;
            final isRtl = _isRtl(activeCue.text);

            double scale = 1.0;
            double opacity = style.opacity / 100.0;
            double translateY = 0.0;

            switch (style.animation) {
              case 'Pop':
                if (elapsed < 0.12) {
                  scale = 0.75 + (elapsed / 0.12) * 0.40;
                } else if (elapsed < 0.22) {
                  scale = 1.15 - ((elapsed - 0.12) / 0.10) * 0.15;
                }
                break;
              case 'Bounce':
                if (elapsed < 0.10) {
                  translateY = -15 * (1 - elapsed / 0.10);
                  scale = 0.85 + (elapsed / 0.10) * 0.35;
                } else if (elapsed < 0.24) {
                  translateY = 5 * (1 - (elapsed - 0.10) / 0.14);
                  scale = 1.20 - ((elapsed - 0.10) / 0.14) * 0.20;
                }
                break;
              case 'Slide up':
                if (elapsed < 0.18) {
                  translateY = 25 * (1 - elapsed / 0.18);
                }
                break;
              case 'Slide down':
                if (elapsed < 0.18) {
                  translateY = -25 * (1 - elapsed / 0.18);
                }
                break;
              case 'Zoom':
                if (elapsed < 0.20) {
                  scale = 0.3 + (elapsed / 0.20) * 0.7;
                }
                break;
              case 'Fade':
                if (elapsed < 0.15) {
                  opacity *= (elapsed / 0.15).clamp(0.0, 1.0);
                } else if (activeCue.end - adjustedTime < 0.15) {
                  opacity *= ((activeCue.end - adjustedTime) / 0.15).clamp(0.0, 1.0);
                }
                break;
            }

            final textColor = _parseColor(style.textColor);
            final bgColor = _parseColor(style.bgColor, 85);
            final fontSize = (style.fontSize * (canvasSize.width / 360)).clamp(12.0, 60.0);

            TextAlign textAlign;
            Alignment stackAlignment;
            switch (style.align) {
              case 'left':
                textAlign = TextAlign.left;
                stackAlignment = Alignment.centerLeft;
                break;
              case 'right':
                textAlign = TextAlign.right;
                stackAlignment = Alignment.centerRight;
                break;
              default:
                textAlign = TextAlign.center;
                stackAlignment = Alignment.center;
            }

            final topPosition = (style.yPos * canvasSize.height).clamp(30.0, canvasSize.height - 80.0);

            Color containerBg;
            final bool hasBg = style.overlay != 'None' || style.bgEnabled;
            if (style.overlay == 'Solid') {
              containerBg = const Color(0xEE0D0B14);
            } else if (style.overlay == 'Translucent') {
              containerBg = const Color(0x88000000);
            } else {
              containerBg = style.bgEnabled ? bgColor : Colors.transparent;
            }

            final displayText = style.oneLine
                ? activeCue.text.replaceAll('\n', ' ')
                : activeCue.text;

            // Shadows and glow
            List<Shadow> textShadows = [
              Shadow(
                color: Colors.black.withOpacity(0.9),
                offset: const Offset(1.5, 1.5),
                blurRadius: style.outline * 2,
              ),
            ];
            if (style.glow > 0) {
              textShadows.add(
                Shadow(
                  color: _parseColor(style.glowColor).withOpacity(0.85),
                  blurRadius: style.glow * 3,
                ),
              );
            }

            Widget textWidget;
            if (style.karaoke && activeCue.words.isNotEmpty) {
              textWidget = _buildKaraokeText(
                cue: activeCue,
                adjustedTime: adjustedTime,
                textColor: textColor,
                fontSize: fontSize,
                isRtl: isRtl,
                textAlign: textAlign,
                oneLine: style.oneLine,
                fontFamily: style.font,
                letterSpacing: style.letterSpacing,
              );
            } else if (style.gradient) {
              textWidget = ShaderMask(
                shaderCallback: (bounds) => LinearGradient(
                  colors: [textColor, _parseColor(style.secondaryColor)],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ).createShader(bounds),
                child: Text(
                  displayText,
                  textAlign: textAlign,
                  maxLines: style.oneLine ? 1 : null,
                  overflow: style.oneLine ? TextOverflow.ellipsis : TextOverflow.visible,
                  textDirection: isRtl ? TextDirection.rtl : TextDirection.ltr,
                  style: TextStyle(
                    fontFamily: style.font,
                    fontSize: fontSize,
                    fontWeight: FontWeight.w800,
                    color: Colors.white,
                    letterSpacing: style.letterSpacing,
                    height: style.lineSpacing,
                    shadows: textShadows,
                  ),
                ),
              );
            } else {
              textWidget = Text(
                displayText,
                textAlign: textAlign,
                maxLines: style.oneLine ? 1 : null,
                overflow: style.oneLine ? TextOverflow.ellipsis : TextOverflow.visible,
                textDirection: isRtl ? TextDirection.rtl : TextDirection.ltr,
                style: TextStyle(
                  fontFamily: style.font,
                  fontSize: fontSize,
                  fontWeight: FontWeight.w700,
                  color: textColor,
                  letterSpacing: style.letterSpacing,
                  height: style.lineSpacing,
                  shadows: textShadows,
                ),
              );
            }

            return Positioned(
              top: topPosition,
              left: 20,
              right: 20,
              child: Transform.translate(
                offset: Offset(0, translateY),
                child: Transform.scale(
                  scale: scale,
                  child: Opacity(
                    opacity: opacity.clamp(0.0, 1.0),
                    child: Align(
                      alignment: stackAlignment,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                        decoration: BoxDecoration(
                          color: containerBg,
                          borderRadius: BorderRadius.circular(10),
                          boxShadow: hasBg
                              ? [
                                  BoxShadow(
                                    color: Colors.black.withOpacity(0.35),
                                    blurRadius: 8,
                                    offset: const Offset(0, 3),
                                  )
                                ]
                              : null,
                        ),
                        child: textWidget,
                      ),
                    ),
                  ),
                ),
              ),
            );
          }),

          // Foreground Graphic Overlays (Layers 1-3)
          ...overlays
              .where((ov) => ov.visible && !(ov.isBackground || ov.depthLayer == 0 || ov.type == 'soft_bg_text') &&
                  currentTime >= ov.start && currentTime <= ov.end)
              .map(_buildGraphicOverlay),
        ],
      ),
    );
  }

  Widget _buildGraphicOverlay(GraphicOverlay ov) {
    final relTime = (currentTime - ov.start).clamp(0.0, ov.duration);

    // 1. Keyframe Interpolation
    double curX = ov.xPos;
    double curY = ov.yPos;
    double curScale = ov.scale;
    double curOpacity = ov.opacity;
    double curRotation = ov.rotation;
    double curBlur = ov.blur;
    double curGlow = ov.glow;
    Color curColor = _parseColor(ov.color);

    if (ov.keyframes.length >= 2) {
      final sorted = List<OverlayKeyframe>.from(ov.keyframes)
        ..sort((a, b) => a.time.compareTo(b.time));

      if (relTime <= sorted.first.time) {
        final k = sorted.first;
        curX = k.xPos; curY = k.yPos; curScale = k.scale; curOpacity = k.opacity;
        curRotation = k.rotation; curBlur = k.blur; curGlow = k.glow; curColor = _parseColor(k.color);
      } else if (relTime >= sorted.last.time) {
        final k = sorted.last;
        curX = k.xPos; curY = k.yPos; curScale = k.scale; curOpacity = k.opacity;
        curRotation = k.rotation; curBlur = k.blur; curGlow = k.glow; curColor = _parseColor(k.color);
      } else {
        OverlayKeyframe k1 = sorted.first;
        OverlayKeyframe k2 = sorted.last;
        for (int i = 0; i < sorted.length - 1; i++) {
          if (relTime >= sorted[i].time && relTime <= sorted[i + 1].time) {
            k1 = sorted[i];
            k2 = sorted[i + 1];
            break;
          }
        }
        final span = math.max(0.001, k2.time - k1.time);
        final rawProgress = (relTime - k1.time) / span;
        final alpha = _evaluateEasing(rawProgress, k2.easing);

        curX = k1.xPos + (k2.xPos - k1.xPos) * alpha;
        curY = k1.yPos + (k2.yPos - k1.yPos) * alpha;
        curScale = k1.scale + (k2.scale - k1.scale) * alpha;
        curOpacity = k1.opacity + (k2.opacity - k1.opacity) * alpha;
        curRotation = k1.rotation + (k2.rotation - k1.rotation) * alpha;
        curBlur = k1.blur + (k2.blur - k1.blur) * alpha;
        curGlow = k1.glow + (k2.glow - k1.glow) * alpha;
      }
    } else {
      // Standard entrance animation if no custom keyframes
      if (ov.animation == 'Fade') {
        if (relTime < 0.2) {
          curOpacity *= (relTime / 0.2).clamp(0.0, 1.0);
        } else if (ov.end - currentTime < 0.2) {
          curOpacity *= ((ov.end - currentTime) / 0.2).clamp(0.0, 1.0);
        }
      } else if (ov.animation == 'Pulse') {
        curScale *= 1.0 + 0.08 * (relTime * 3.5).abs().remainder(1.0);
      } else if (ov.animation == 'Pop') {
        if (relTime < 0.15) {
          curScale *= 0.75 + (relTime / 0.15) * 0.35;
        }
      }
    }

    final x = (curX * canvasSize.width).clamp(20.0, canvasSize.width - 20.0);
    final y = (curY * canvasSize.height).clamp(20.0, canvasSize.height - 20.0);

    Widget contentWidget;
    switch (ov.type) {
      case 'persian_orange_headline':
      case 'persian_orange':
        contentWidget = Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          decoration: BoxDecoration(
            boxShadow: [
              BoxShadow(
                color: const Color(0xFFFF6A00).withOpacity(0.55),
                blurRadius: 24,
                spreadRadius: 4,
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'ویزای',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  shadows: [Shadow(color: Colors.black, blurRadius: 4, offset: Offset(1, 1))],
                ),
              ),
              ShaderMask(
                shaderCallback: (bounds) => const LinearGradient(
                  colors: [Color(0xFFFFB800), Color(0xFFFF6A00)],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ).createShader(bounds),
                child: Text(
                  ov.content.isNotEmpty ? ov.content : 'فنلاند',
                  textDirection: TextDirection.rtl,
                  style: const TextStyle(
                    fontFamily: 'Vazirmatn',
                    fontSize: 42,
                    fontWeight: FontWeight.w900,
                    color: Colors.white,
                    shadows: [
                      Shadow(color: Color(0xFF1A1A1A), blurRadius: 6, offset: Offset(2.5, 2.5)),
                    ],
                  ),
                ),
              ),
              const Text(
                'تحصیلی',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  shadows: [Shadow(color: Colors.black, blurRadius: 4, offset: Offset(1, 1))],
                ),
              ),
            ],
          ),
        );
        break;

      case 'glow_hologram':
        contentWidget = Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            color: const Color(0x330284C7),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: const Color(0xFF00F0FF), width: 2),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF00F0FF).withOpacity(0.65),
                blurRadius: 22,
                spreadRadius: 4,
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: const [
                  Icon(Icons.location_city, color: Color(0xFF00F0FF), size: 32),
                  SizedBox(width: 4),
                  Icon(Icons.apartment, color: Color(0xFF00F0FF), size: 32),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                ov.content.isNotEmpty ? ov.content : '◈ HOLOGRAM OBJECT ◈',
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                  fontSize: 13,
                  letterSpacing: 2.0,
                ),
              ),
            ],
          ),
        );
        break;

      case 'metallic_banner':
        contentWidget = Container(
          width: canvasSize.width * 0.90,
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A)],
              begin: Alignment.centerLeft,
              end: Alignment.centerRight,
            ),
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: const Color(0xFFE2E8F0), width: 2.0),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.8),
                blurRadius: 14,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Center(
            child: Text(
              ov.content.isNotEmpty ? ov.content.toUpperCase() : 'AUSTRALIA',
              style: const TextStyle(
                color: Color(0xFFE2E8F0),
                fontWeight: FontWeight.w900,
                fontSize: 26,
                letterSpacing: 6.0,
                fontFamily: 'Impact',
                shadows: [
                  Shadow(color: Colors.black, blurRadius: 4, offset: Offset(2, 2)),
                ],
              ),
            ),
          ),
        );
        break;

      case 'light_leak':
        return Positioned.fill(
          child: IgnorePointer(
            child: Opacity(
              opacity: curOpacity.clamp(0.0, 1.0) * 0.75,
              child: Container(
                decoration: const BoxDecoration(
                  gradient: RadialGradient(
                    center: Alignment(0.65, -0.45),
                    radius: 1.3,
                    colors: [Color(0xD9D946EF), Color(0x669333EA), Colors.transparent],
                  ),
                ),
              ),
            ),
          ),
        );

      case 'flag_graphic':
      case 'waving_flag':
        contentWidget = Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          decoration: BoxDecoration(
            color: const Color(0xDD0F172A),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: const Color(0xFFF59E0B), width: 2),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFFF59E0B).withOpacity(0.4),
                blurRadius: 12,
              ),
            ],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.flag, color: Color(0xFFF59E0B), size: 22),
              const SizedBox(width: 8),
              Text(
                ov.content.isNotEmpty ? ov.content : '⚑ FLAG OVERLAY ⚑',
                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 13),
              ),
            ],
          ),
        );
        break;

      case 'soft_bg_text':
        contentWidget = Opacity(
          opacity: 0.40,
          child: Text(
            ov.content.isNotEmpty ? ov.content : 'برج‌های دوگانه بلند ترین',
            textDirection: TextDirection.rtl,
            style: const TextStyle(
              fontFamily: 'Vazirmatn',
              fontSize: 30,
              fontWeight: FontWeight.w700,
              color: Colors.white70,
              shadows: [
                Shadow(color: Colors.black, blurRadius: 10),
              ],
            ),
          ),
        );
        break;

      case 'end_accent':
        contentWidget = Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
          decoration: BoxDecoration(
            color: const Color(0xFFEF4444),
            borderRadius: BorderRadius.circular(6),
            boxShadow: const [
              BoxShadow(color: Color(0x88EF4444), blurRadius: 8),
            ],
          ),
          child: Text(
            ov.content.isNotEmpty ? ov.content : 'CaptionForge Studio',
            style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w800),
          ),
        );
        break;

      default:
        contentWidget = Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: Colors.black87,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: curColor),
          ),
          child: Text(
            ov.content,
            style: TextStyle(color: curColor, fontWeight: FontWeight.w700),
          ),
        );
    }

    if (curGlow > 0.05) {
      contentWidget = DecoratedBox(
        decoration: BoxDecoration(
          boxShadow: [
            BoxShadow(
              color: curColor.withOpacity(0.6),
              blurRadius: curGlow * 1.5,
              spreadRadius: curGlow * 0.5,
            ),
          ],
        ),
        child: contentWidget,
      );
    }

    if (curBlur > 0.05) {
      contentWidget = ImageFiltered(
        imageFilter: ui.ImageFilter.blur(sigmaX: curBlur, sigmaY: curBlur),
        child: contentWidget,
      );
    }

    return Positioned(
      left: x,
      top: y,
      child: FractionalTranslation(
        translation: const Offset(-0.5, -0.5),
        child: Transform.rotate(
          angle: curRotation * (math.pi / 180.0),
          child: Transform.scale(
            scale: curScale,
            child: Opacity(
              opacity: curOpacity.clamp(0.0, 1.0),
              child: contentWidget,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildKaraokeText({
    required CaptionCue cue,
    required double adjustedTime,
    required Color textColor,
    required double fontSize,
    required bool isRtl,
    required TextAlign textAlign,
    bool oneLine = false,
    String? fontFamily,
    double letterSpacing = 0.0,
  }) {
    return RichText(
      textAlign: textAlign,
      textDirection: isRtl ? TextDirection.rtl : TextDirection.ltr,
      maxLines: oneLine ? 1 : null,
      overflow: oneLine ? TextOverflow.ellipsis : TextOverflow.clip,
      text: TextSpan(
        children: cue.words.map((word) {
          final isActive = adjustedTime >= word.start && adjustedTime <= word.end;
          final isPast = adjustedTime > word.end;

          Color color = textColor;
          FontWeight weight = FontWeight.w600;

          if (isActive) {
            color = const Color(0xFFFACC15);
            weight = FontWeight.w900;
          } else if (isPast) {
            color = textColor;
          } else {
            color = textColor.withOpacity(0.65);
          }

          return TextSpan(
            text: '${word.text} ',
            style: TextStyle(
              fontFamily: fontFamily,
              fontSize: isActive ? fontSize * 1.08 : fontSize,
              fontWeight: weight,
              color: color,
              letterSpacing: letterSpacing,
              shadows: [
                Shadow(
                  color: Colors.black.withOpacity(0.9),
                  offset: const Offset(1.5, 1.5),
                  blurRadius: isActive ? 6 : 3,
                ),
              ],
            ),
          );
        }).toList(),
      ),
    );
  }
}
