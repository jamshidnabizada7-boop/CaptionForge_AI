import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';
import '../../../models/project.dart';

enum EditorTool {
  styles,
  templates,
  overlays,
  format,
  fonts,
  colors,
  animations,
  translate,
  sfx,
  timing,
  filters,
  clearVoice,
  follow,
}

class ToolsTrays extends StatelessWidget {
  final EditorTool activeTool;
  final Track currentTrack;
  final VideoEffects effects;
  final ValueChanged<EditorTool> onSelectTool;
  final ValueChanged<SubtitleStyle> onUpdateStyle;
  final ValueChanged<VideoEffects> onUpdateEffects;
  final VoidCallback onTriggerTranslate;
  final ValueChanged<String>? onApplyTemplate;
  final List<GraphicOverlay> overlays;
  final ValueChanged<GraphicOverlay>? onToggleOverlayVisibility;
  final ValueChanged<String>? onDeleteOverlay;
  final ValueChanged<GraphicOverlay>? onEditOverlay;
  final VoidCallback? onAddOverlay;

  const ToolsTrays({
    super.key,
    required this.activeTool,
    required this.currentTrack,
    required this.effects,
    required this.onSelectTool,
    required this.onUpdateStyle,
    required this.onUpdateEffects,
    required this.onTriggerTranslate,
    this.onApplyTemplate,
    this.overlays = const [],
    this.onToggleOverlayVisibility,
    this.onDeleteOverlay,
    this.onEditOverlay,
    this.onAddOverlay,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        border: Border(top: BorderSide(color: AppColors.surfaceBorder, width: 1)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Tool Content Tray
          SizedBox(
            height: 140,
            child: _buildToolContent(context),
          ),
          const Divider(height: 1),

          // Bottom Tool Category Bar
          SizedBox(
            height: 60,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 8),
              children: [
                _buildToolTab(EditorTool.styles, Icons.auto_awesome, 'Style'),
                _buildToolTab(EditorTool.templates, Icons.dashboard_customize, 'Templates'),
                _buildToolTab(EditorTool.overlays, Icons.layers, 'Layers'),
                _buildToolTab(EditorTool.format, Icons.format_shapes, 'Format'),
                _buildToolTab(EditorTool.fonts, Icons.font_download, 'Font'),
                _buildToolTab(EditorTool.colors, Icons.palette, 'Color'),
                _buildToolTab(EditorTool.animations, Icons.animation, 'Animation'),
                _buildToolTab(EditorTool.translate, Icons.translate, 'Translate'),
                _buildToolTab(EditorTool.sfx, Icons.volume_up, 'SFX'),
                _buildToolTab(EditorTool.timing, Icons.tune, 'Timing'),
                _buildToolTab(EditorTool.filters, Icons.filter_vintage, 'Filters'),
                _buildToolTab(EditorTool.clearVoice, Icons.record_voice_over, 'Voice'),
                _buildToolTab(EditorTool.follow, Icons.face, 'Follow'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildToolTab(EditorTool tool, IconData icon, String label) {
    final isSelected = activeTool == tool;
    return InkWell(
      onTap: () => onSelectTool(tool),
      child: Container(
        width: 64,
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              size: 20,
              color: isSelected ? AppColors.primaryPurple : AppColors.textSecondary,
            ),
            const SizedBox(height: 2),
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 10,
                fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                color: isSelected ? Colors.white : AppColors.textMuted,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildToolContent(BuildContext context) {
    final style = currentTrack.style;

    switch (activeTool) {
      case EditorTool.styles:
        return _buildStylesTray(style);
      case EditorTool.templates:
        return _buildTemplatesTray();
      case EditorTool.overlays:
        return _buildOverlaysTray(context);
      case EditorTool.format:
        return _buildFormatTray(style);
      case EditorTool.fonts:
        return _buildFontsTray(style);
      case EditorTool.colors:
        return _buildColorsTray(style);
      case EditorTool.animations:
        return _buildAnimationsTray(style);
      case EditorTool.translate:
        return _buildTranslateTray(context);
      case EditorTool.sfx:
        return _buildSfxTray();
      case EditorTool.timing:
        return _buildTimingTray(style);
      case EditorTool.filters:
        return _buildFiltersTray();
      case EditorTool.clearVoice:
        return _buildClearVoiceTray();
      case EditorTool.follow:
        return _buildFollowTray(style);
    }
  }

  Widget _buildFormatTray(SubtitleStyle style) {
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text('Align:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              const SizedBox(width: 8),
              ...[
                {'val': 'left', 'icon': Icons.format_align_left, 'tooltip': 'Left Align'},
                {'val': 'center', 'icon': Icons.format_align_center, 'tooltip': 'Center Align'},
                {'val': 'right', 'icon': Icons.format_align_right, 'tooltip': 'Right Align'},
              ].map((a) {
                final isSelected = style.align == a['val'];
                return Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: InkWell(
                    onTap: () => onUpdateStyle(style.copyWith(align: a['val'] as String)),
                    borderRadius: BorderRadius.circular(8),
                    child: Container(
                      padding: const EdgeInsets.all(6),
                      decoration: BoxDecoration(
                        color: isSelected ? AppColors.primaryPurple : AppColors.surfaceElevated,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: isSelected ? AppColors.primaryPurpleLight : AppColors.surfaceBorder,
                        ),
                      ),
                      child: Icon(a['icon'] as IconData, size: 18, color: Colors.white),
                    ),
                  ),
                );
              }),
              const Spacer(),
              const Text('One-line:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              Switch(
                value: style.oneLine,
                activeColor: AppColors.primaryPurple,
                onChanged: (val) => onUpdateStyle(style.copyWith(oneLine: val)),
              ),
            ],
          ),
          Row(
            children: [
              const Text('Size:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              Expanded(
                child: Slider(
                  value: style.fontSize.clamp(14.0, 48.0),
                  min: 14.0,
                  max: 48.0,
                  divisions: 34,
                  onChanged: (val) => onUpdateStyle(style.copyWith(fontSize: val)),
                ),
              ),
              Text('${style.fontSize.toInt()}pt', style: const TextStyle(fontSize: 11, color: AppColors.textMuted)),
              const SizedBox(width: 8),
              const Text('Outline:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              Expanded(
                child: Slider(
                  value: style.outline.clamp(0.0, 6.0),
                  min: 0.0,
                  max: 6.0,
                  divisions: 12,
                  onChanged: (val) => onUpdateStyle(style.copyWith(outline: val)),
                ),
              ),
              Text('${style.outline.toStringAsFixed(1)}px', style: const TextStyle(fontSize: 11, color: AppColors.textMuted)),
            ],
          ),
          Row(
            children: [
              const Text('Overlay:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              const SizedBox(width: 8),
              ...['None', 'Translucent', 'Solid'].map((ov) {
                final isSelected = style.overlay == ov;
                return Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: ChoiceChip(
                    label: Text(ov, style: const TextStyle(fontSize: 11)),
                    selected: isSelected,
                    selectedColor: AppColors.primaryPurple,
                    onSelected: (val) => onUpdateStyle(style.copyWith(overlay: ov)),
                  ),
                );
              }),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStylesTray(SubtitleStyle style) {
    final presets = [
      {'name': 'Bold', 'font': 'Noto Sans', 'color': '#FFFFFF', 'bg': '#181124', 'anim': 'Pop', 'karaoke': false},
      {'name': 'Karaoke', 'font': 'Montserrat', 'color': '#FFFFFF', 'bg': '#0D0B14', 'anim': 'None', 'karaoke': true},
      {'name': 'Neon', 'font': 'Inter', 'color': '#A78BFA', 'bg': '#2E1065', 'anim': 'Bounce', 'karaoke': false},
      {'name': 'Minimal', 'font': 'Roboto', 'color': '#F8FAFC', 'bg': '#000000', 'anim': 'Fade', 'karaoke': false},
      {'name': 'Vazir', 'font': 'Vazirmatn', 'color': '#FACC15', 'bg': '#1F2937', 'anim': 'Slide up', 'karaoke': false},
      {'name': 'Arabic Clean', 'font': 'Noto Sans Arabic', 'color': '#FFFFFF', 'bg': '#312E81', 'anim': 'Pop', 'karaoke': false},
    ];

    return ListView.builder(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      itemCount: presets.length,
      itemBuilder: (context, i) {
        final p = presets[i];
        final isSelected = style.preset == p['name'];
        return GestureDetector(
          onTap: () {
            onUpdateStyle(style.copyWith(
              preset: p['name'] as String,
              font: p['font'] as String,
              textColor: p['color'] as String,
              bgColor: p['bg'] as String,
              animation: p['anim'] as String,
              karaoke: p['karaoke'] as bool,
            ));
          },
          child: Container(
            width: 110,
            margin: const EdgeInsets.only(right: 12),
            decoration: BoxDecoration(
              color: AppColors.surfaceElevated,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: isSelected ? AppColors.primaryPurple : AppColors.surfaceBorder,
                width: isSelected ? 2 : 1,
              ),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Aa سلام',
                  style: TextStyle(
                    fontFamily: p['font'] as String,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: Color(int.parse((p['color'] as String).replaceAll('#', '0xFF'))),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  p['name'] as String,
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                    color: isSelected ? AppColors.primaryPurpleLight : AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildFontsTray(SubtitleStyle style) {
    final fonts = [
      {'family': 'Vazirmatn', 'lang': 'Persian / Dari / Pashto', 'sample': 'کپشن‌فورج متن فارسی'},
      {'family': 'Noto Sans Arabic', 'lang': 'Arabic / Persian / Urdu', 'sample': 'خط عربی و اردو'},
      {'family': 'Noto Nastaliq Urdu', 'lang': 'Urdu Nastaliq', 'sample': 'اردو نستعلیق ڈیزائن'},
      {'family': 'Lalezar', 'lang': 'Persian Display Bold', 'sample': 'عنوان سینمایی'},
      {'family': 'Amiri', 'lang': 'Classical Arabic', 'sample': 'الخط العربي الأصيل'},
      {'family': 'Impact', 'lang': 'English Cinematic', 'sample': 'AUSTRALIA TITLE'},
      {'family': 'Bebas Neue', 'lang': 'English Condensed', 'sample': 'CINEMATIC BANNER'},
      {'family': 'Segoe UI', 'lang': 'Universal Sans', 'sample': 'Modern Subtitles'},
      {'family': 'Tahoma', 'lang': 'Legacy RTL / Latin', 'sample': 'زیرنویس همزمان'},
      {'family': 'Roboto', 'lang': 'Clean Sans', 'sample': 'Minimal Clean Font'},
      {'family': 'Arial', 'lang': 'Standard Sans', 'sample': 'Standard Subtitle'},
    ];

    return Column(
      children: [
        Expanded(
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            itemCount: fonts.length,
            itemBuilder: (context, i) {
              final f = fonts[i];
              final font = f['family']!;
              final isSelected = style.font == font;
              return GestureDetector(
                onTap: () => onUpdateStyle(style.copyWith(font: font)),
                child: Container(
                  width: 140,
                  margin: const EdgeInsets.only(right: 10),
                  padding: const EdgeInsets.all(6),
                  decoration: BoxDecoration(
                    color: isSelected ? AppColors.primaryPurple.withOpacity(0.2) : AppColors.surfaceElevated,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(
                      color: isSelected ? AppColors.primaryPurple : AppColors.surfaceBorder,
                      width: isSelected ? 2 : 1,
                    ),
                  ),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        f['sample']!,
                        textAlign: TextAlign.center,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontFamily: font,
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: isSelected ? Colors.white : AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        font,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                          color: isSelected ? AppColors.primaryPurpleLight : AppColors.textSecondary,
                        ),
                      ),
                      Text(
                        f['lang']!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 8, color: AppColors.textMuted),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
        // Typography Fine Controls (Letter Spacing, Glow, Gradient)
        Container(
          height: 36,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          color: Colors.black.withOpacity(0.15),
          child: Row(
            children: [
              const Text('Spacing:', style: TextStyle(fontSize: 10, color: AppColors.textMuted)),
              Expanded(
                child: Slider(
                  value: style.letterSpacing.clamp(-2.0, 10.0),
                  min: -2.0,
                  max: 10.0,
                  onChanged: (val) => onUpdateStyle(style.copyWith(letterSpacing: val)),
                ),
              ),
              const Text('Glow:', style: TextStyle(fontSize: 10, color: AppColors.textMuted)),
              Expanded(
                child: Slider(
                  value: style.glow.clamp(0.0, 15.0),
                  min: 0.0,
                  max: 15.0,
                  onChanged: (val) => onUpdateStyle(style.copyWith(glow: val)),
                ),
              ),
              InkWell(
                onTap: () => onUpdateStyle(style.copyWith(gradient: !style.gradient)),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: style.gradient ? AppColors.primaryPurple : Colors.transparent,
                    borderRadius: BorderRadius.circular(4),
                    border: Border.all(color: AppColors.primaryPurple, width: 1),
                  ),
                  child: Text(
                    'Gradient',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: style.gradient ? Colors.white : AppColors.primaryPurpleLight,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildColorsTray(SubtitleStyle style) {
    final textColors = ['#FFFFFF', '#FACC15', '#A78BFA', '#38C5B3', '#FF5C5C', '#60A5FA', '#000000'];
    final bgColors = ['#181124', '#000000', '#2E1065', '#064E3B', '#1E1B4B', '#374151'];

    return Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text('Text:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              const SizedBox(width: 8),
              Expanded(
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: textColors.map((hex) {
                      final isSelected = style.textColor.toUpperCase() == hex;
                      return GestureDetector(
                        onTap: () => onUpdateStyle(style.copyWith(textColor: hex)),
                        child: Container(
                          width: 28,
                          height: 28,
                          margin: const EdgeInsets.only(right: 8),
                          decoration: BoxDecoration(
                            color: Color(int.parse(hex.replaceAll('#', '0xFF'))),
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: isSelected ? AppColors.primaryPurple : AppColors.surfaceBorder,
                              width: isSelected ? 3 : 1,
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              const Text('Box:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              const SizedBox(width: 10),
              Switch(
                value: style.bgEnabled,
                onChanged: (val) => onUpdateStyle(style.copyWith(bgEnabled: val)),
                activeColor: AppColors.primaryPurple,
              ),
              Expanded(
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: bgColors.map((hex) {
                      final isSelected = style.bgColor.toUpperCase() == hex;
                      return GestureDetector(
                        onTap: () => onUpdateStyle(style.copyWith(bgColor: hex, bgEnabled: true)),
                        child: Container(
                          width: 28,
                          height: 28,
                          margin: const EdgeInsets.only(right: 8),
                          decoration: BoxDecoration(
                            color: Color(int.parse(hex.replaceAll('#', '0xFF'))),
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(
                              color: isSelected ? AppColors.primaryPurple : AppColors.surfaceBorder,
                              width: isSelected ? 3 : 1,
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAnimationsTray(SubtitleStyle style) {
    final anims = ['Pop', 'Bounce', 'Fade', 'Slide up', 'Slide down', 'Zoom', 'None'];
    return ListView.builder(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      itemCount: anims.length,
      itemBuilder: (context, i) {
        final anim = anims[i];
        final isSelected = style.animation == anim;
        return GestureDetector(
          onTap: () => onUpdateStyle(style.copyWith(animation: anim)),
          child: Container(
            width: 100,
            margin: const EdgeInsets.only(right: 10),
            decoration: BoxDecoration(
              color: isSelected ? AppColors.primaryPurple.withOpacity(0.2) : AppColors.surfaceElevated,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: isSelected ? AppColors.primaryPurple : AppColors.surfaceBorder,
                width: isSelected ? 2 : 1,
              ),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  Icons.play_circle_fill,
                  size: 26,
                  color: isSelected ? AppColors.primaryPurple : AppColors.textSecondary,
                ),
                const SizedBox(height: 8),
                Text(
                  anim,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                    color: isSelected ? Colors.white : AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildTranslateTray(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: const [
                Text(
                  'AI Translation & Bilingual',
                  style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14, color: Colors.white),
                ),
                SizedBox(height: 4),
                Text(
                  'Generate simultaneous or translated subtitle track (Persian, Pashto, Urdu, Arabic, etc.)',
                  style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                ),
              ],
            ),
          ),
          ElevatedButton.icon(
            onPressed: onTriggerTranslate,
            icon: const Icon(Icons.g_translate, size: 18),
            label: const Text('Translate'),
          ),
        ],
      ),
    );
  }

  Widget _buildTemplatesTray() {
    final templates = [
      {
        'id': 'v1_orange_headline',
        'title': 'Orange Headline',
        'subtitle': 'Video 1 Persian Hero',
        'color': '#FF7A00',
        'icon': Icons.title,
      },
      {
        'id': 'v1_orange_white_explainer',
        'title': 'Orange/White Explainer',
        'subtitle': 'Dual-Track Layout',
        'color': '#FFFFFF',
        'icon': Icons.subtitles,
      },
      {
        'id': 'v1_purple_flare',
        'title': 'Purple Flare',
        'subtitle': 'Light Leak Transition',
        'color': '#D946EF',
        'icon': Icons.flare,
      },
      {
        'id': 'v2_hand_hologram',
        'title': 'Hand Hologram',
        'subtitle': 'Video 2 Glow Object',
        'color': '#38BDF8',
        'icon': Icons.architecture,
      },
      {
        'id': 'v2_soft_bg_words',
        'title': 'Soft Persian Words',
        'subtitle': 'Depth Background',
        'color': '#94A3B8',
        'icon': Icons.blur_on,
      },
      {
        'id': 'v2_flag_subject',
        'title': 'Flag with Subject',
        'subtitle': 'Flag Media Overlay',
        'color': '#F59E0B',
        'icon': Icons.flag,
      },
      {
        'id': 'v2_metallic_australia',
        'title': 'Metallic Reveal',
        'subtitle': 'Glitch Scanline Title',
        'color': '#E2E8F0',
        'icon': Icons.animation,
      },
      {
        'id': 'v2_end_accent',
        'title': 'End Accent Title',
        'subtitle': 'Bottom Red Accent',
        'color': '#EF4444',
        'icon': Icons.label_important,
      },
    ];

    return ListView.builder(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      itemCount: templates.length,
      itemBuilder: (context, i) {
        final t = templates[i];
        final colorHex = (t['color'] as String).replaceAll('#', '0xFF');
        final c = Color(int.parse(colorHex));

        return GestureDetector(
          onTap: () {
            if (onApplyTemplate != null) {
              onApplyTemplate!(t['id'] as String);
            }
          },
          child: Container(
            width: 140,
            margin: const EdgeInsets.only(right: 12),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: AppColors.surfaceElevated,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: c.withOpacity(0.5), width: 1.5),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(t['icon'] as IconData, color: c, size: 24),
                const SizedBox(height: 6),
                Text(
                  t['title'] as String,
                  textAlign: TextAlign.center,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: c,
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  t['subtitle'] as String,
                  textAlign: TextAlign.center,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: AppColors.textMuted,
                    fontSize: 10,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  IconData _overlayTypeIcon(String type) {
    switch (type) {
      case 'glow_hologram':
        return Icons.architecture;
      case 'metallic_banner':
        return Icons.title;
      case 'light_leak':
        return Icons.flare;
      case 'flag_graphic':
        return Icons.flag;
      case 'soft_bg_text':
        return Icons.blur_on;
      case 'end_accent':
        return Icons.label_important;
      default:
        return Icons.layers;
    }
  }

  Widget _buildOverlaysTray(BuildContext context) {
    if (overlays.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.layers_outlined, color: AppColors.textMuted, size: 28),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text(
                      'No Graphic Overlays',
                      style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 13),
                    ),
                    SizedBox(height: 2),
                    Text(
                      'Tap Templates or Add Layer to place VFX',
                      style: TextStyle(color: AppColors.textMuted, fontSize: 11),
                    ),
                  ],
                ),
              ),
              if (onAddOverlay != null)
                ElevatedButton.icon(
                  onPressed: onAddOverlay,
                  icon: const Icon(Icons.add, size: 16),
                  label: const Text('Add Layer', style: TextStyle(fontSize: 12)),
                ),
            ],
          ),
        ),
      );
    }

    return ListView.builder(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      itemCount: overlays.length + (onAddOverlay != null ? 1 : 0),
      itemBuilder: (context, i) {
        if (i == overlays.length) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.only(left: 4, right: 12),
              child: OutlinedButton.icon(
                onPressed: onAddOverlay,
                icon: const Icon(Icons.add, size: 16),
                label: const Text('Add Layer', style: TextStyle(fontSize: 12)),
              ),
            ),
          );
        }

        final ov = overlays[i];
        final colorHex = ov.color.replaceAll('#', '0xFF');
        final ovColor = Color(int.tryParse(colorHex) ?? 0xFF38BDF8);

        return Container(
          width: 170,
          margin: const EdgeInsets.only(right: 12),
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: AppColors.surfaceElevated,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: ov.visible ? ovColor.withOpacity(0.6) : AppColors.surfaceBorder,
              width: 1.5,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(_overlayTypeIcon(ov.type), color: ovColor, size: 18),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      ov.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 11, color: Colors.white),
                    ),
                  ),
                  InkWell(
                    onTap: () {
                      if (onToggleOverlayVisibility != null) {
                        onToggleOverlayVisibility!(ov);
                      }
                    },
                    child: Padding(
                      padding: const EdgeInsets.all(2),
                      child: Icon(
                        ov.visible ? Icons.visibility : Icons.visibility_off,
                        size: 16,
                        color: ov.visible ? ovColor : AppColors.textMuted,
                      ),
                    ),
                  ),
                ],
              ),
              Text(
                ov.content.isNotEmpty ? ov.content : ov.type,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 10, color: AppColors.textSecondary),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.06),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      '${ov.start.toStringAsFixed(1)}s - ${ov.end.toStringAsFixed(1)}s',
                      style: const TextStyle(fontSize: 9, color: AppColors.textMuted),
                    ),
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (onEditOverlay != null)
                        InkWell(
                          onTap: () => onEditOverlay!(ov),
                          child: const Padding(
                            padding: EdgeInsets.all(2),
                            child: Icon(Icons.edit, size: 14, color: AppColors.primaryPurpleLight),
                          ),
                        ),
                      if (onDeleteOverlay != null)
                        InkWell(
                          onTap: () => onDeleteOverlay!(ov.id),
                          child: const Padding(
                            padding: EdgeInsets.all(2),
                            child: Icon(Icons.delete_outline, size: 15, color: Colors.redAccent),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildSfxTray() {
    final sounds = [
      'None',
      'Pop',
      'Click',
      'Whoosh',
      'Shimmer',
      'Impact',
      'Riser',
      'Bass',
      'Glitch',
      'Light Sweep',
      'Transition',
      'Ambient',
    ];
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text('Sound:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              const SizedBox(width: 8),
              Expanded(
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: sounds.map((s) {
                      final isSelected = effects.sfx == s;
                      return Padding(
                        padding: const EdgeInsets.only(right: 6),
                        child: ChoiceChip(
                          avatar: isSelected && s != 'None' ? const Icon(Icons.graphic_eq, size: 14, color: Colors.white) : null,
                          label: Text(s, style: const TextStyle(fontSize: 11)),
                          selected: isSelected,
                          selectedColor: AppColors.primaryPurple,
                          onSelected: (val) => onUpdateEffects(effects.copyWith(sfx: s)),
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Row(
            children: [
              const Text('Trigger:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              const SizedBox(width: 8),
              ...['Sentence', 'Word'].map((m) {
                final isSelected = effects.sfxMode == m;
                return Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: ChoiceChip(
                    label: Text(m, style: const TextStyle(fontSize: 11)),
                    selected: isSelected,
                    selectedColor: AppColors.primaryPurple,
                    onSelected: (val) => onUpdateEffects(effects.copyWith(sfxMode: m)),
                  ),
                );
              }),
              const SizedBox(width: 8),
              const Text('Vol:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              Expanded(
                child: Slider(
                  value: effects.sfxVolume.toDouble(),
                  min: 0,
                  max: 100,
                  onChanged: (val) => onUpdateEffects(effects.copyWith(sfxVolume: val.toInt())),
                ),
              ),
              Text('${effects.sfxVolume}%', style: const TextStyle(fontSize: 11, color: AppColors.textMuted)),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: AppColors.accentGreen.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: AppColors.accentGreen, width: 0.8),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    Icon(Icons.auto_awesome, size: 10, color: AppColors.accentGreen),
                    SizedBox(width: 3),
                    Text('Ducking -12dB', style: TextStyle(fontSize: 9, color: AppColors.accentGreen, fontWeight: FontWeight.w700)),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTimingTray(SubtitleStyle style) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        children: [
          Row(
            children: [
              const Text('Vertical Position:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              Expanded(
                child: Slider(
                  value: style.yPos,
                  min: 0.1,
                  max: 0.9,
                  onChanged: (val) => onUpdateStyle(style.copyWith(yPos: val)),
                ),
              ),
              Text('${(style.yPos * 100).toInt()}%', style: const TextStyle(fontSize: 11, color: AppColors.textMuted)),
            ],
          ),
          Row(
            children: [
              const Text('Display Earlier:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              Expanded(
                child: Slider(
                  value: style.early,
                  min: 0.0,
                  max: 1.0,
                  divisions: 10,
                  onChanged: (val) => onUpdateStyle(style.copyWith(early: val)),
                ),
              ),
              Text('${style.early.toStringAsFixed(1)}s', style: const TextStyle(fontSize: 11, color: AppColors.textMuted)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildFiltersTray() {
    final filters = [
      {'name': 'Original', 'b': 0.0, 'c': 1.0, 's': 1.0},
      {'name': 'Vivid', 'b': 0.05, 'c': 1.2, 's': 1.3},
      {'name': 'Warm', 'b': 0.02, 'c': 1.05, 's': 1.1},
      {'name': 'Cool', 'b': -0.02, 'c': 1.05, 's': 1.05},
      {'name': 'B&W', 'b': 0.0, 'c': 1.15, 's': 0.0},
    ];

    return ListView.builder(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      itemCount: filters.length,
      itemBuilder: (context, i) {
        final f = filters[i];
        final isSelected = effects.saturation == f['s'] && effects.contrast == f['c'];
        return GestureDetector(
          onTap: () {
            onUpdateEffects(effects.copyWith(
              brightness: f['b'] as double,
              contrast: f['c'] as double,
              saturation: f['s'] as double,
            ));
          },
          child: Container(
            width: 90,
            margin: const EdgeInsets.only(right: 10),
            decoration: BoxDecoration(
              color: isSelected ? AppColors.primaryPurple.withOpacity(0.2) : AppColors.surfaceElevated,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: isSelected ? AppColors.primaryPurple : AppColors.surfaceBorder,
                width: isSelected ? 2 : 1,
              ),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  Icons.photo_filter,
                  size: 24,
                  color: isSelected ? AppColors.primaryPurple : AppColors.textSecondary,
                ),
                const SizedBox(height: 6),
                Text(
                  f['name'] as String,
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                    color: isSelected ? Colors.white : AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildClearVoiceTray() {
    final levels = ['Off', 'Light', 'Balanced', 'Strong'];
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Text(
            'Clear Voice (Audio Denoising & Vocal Clarity)',
            style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: Colors.white),
          ),
          const SizedBox(height: 10),
          Row(
            children: levels.map((lvl) {
              final isSelected = effects.clearVoice == lvl;
              return Padding(
                padding: const EdgeInsets.only(right: 10),
                child: ChoiceChip(
                  label: Text(lvl, style: const TextStyle(fontSize: 12)),
                  selected: isSelected,
                  selectedColor: AppColors.primaryPurple,
                  onSelected: (val) => onUpdateEffects(effects.copyWith(clearVoice: lvl)),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildFollowTray(SubtitleStyle style) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: const [
                Text(
                  'Face Follow Smart Placement',
                  style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14, color: Colors.white),
                ),
                SizedBox(height: 4),
                Text(
                  'Dynamically anchors captions above/below speaker face with smooth tracking',
                  style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                ),
              ],
            ),
          ),
          Switch(
            value: style.followFace,
            activeColor: AppColors.primaryPurple,
            onChanged: (val) => onUpdateStyle(style.copyWith(followFace: val)),
          ),
        ],
      ),
    );
  }
}
