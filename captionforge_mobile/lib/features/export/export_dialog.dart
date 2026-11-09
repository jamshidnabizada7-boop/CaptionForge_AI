import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/services/api_service.dart';
import '../../models/project.dart';

class ExportDialog extends StatefulWidget {
  final Project project;
  final ApiService apiService;

  const ExportDialog({
    super.key,
    required this.project,
    required this.apiService,
  });

  @override
  State<ExportDialog> createState() => _ExportDialogState();
}

class _ExportDialogState extends State<ExportDialog> {
  int _selectedResolution = 720;
  bool _isExporting = false;
  String? _statusMessage;
  String? _downloadUrl;
  String? _error;

  @override
  void initState() {
    super.initState();
    _selectedResolution = widget.project.resolution;
  }

  Future<void> _startExport() async {
    setState(() {
      _isExporting = true;
      _statusMessage = 'Queuing FFmpeg render...';
      _error = null;
      _downloadUrl = null;
    });

    try {
      final jobId = await widget.apiService.exportVideo(widget.project.id, _selectedResolution, widget.project.aspectRatio);
      setState(() => _statusMessage = 'Encoding ${widget.project.aspectRatio} MP4 with ASS subtitles...');

      final result = await widget.apiService.pollJob(
        jobId,
        onProgress: (msg) => setState(() => _statusMessage = msg),
      );

      final downloadPath = result['result']?['download'] as String?;
      setState(() {
        _isExporting = false;
        _downloadUrl = downloadPath != null
            ? widget.apiService.getDownloadUrl(widget.project.id)
            : null;
        _statusMessage = 'Export complete! Your captioned video is ready.';
      });
    } catch (e) {
      setState(() {
        _isExporting = false;
        _error = e.toString();
        _statusMessage = null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        color: AppColors.surfaceElevated,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Export Captioned Video',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
              ),
              IconButton(
                icon: const Icon(Icons.close),
                onPressed: () => Navigator.pop(context),
              ),
            ],
          ),
          const SizedBox(height: 16),

          const Text('Select Resolution:', style: TextStyle(color: AppColors.textSecondary, fontSize: 13)),
          const SizedBox(height: 8),
          Row(
            children: [480, 720, 1080].map((res) {
              final isSelected = _selectedResolution == res;
              return Padding(
                padding: const EdgeInsets.only(right: 12),
                child: ChoiceChip(
                  label: Text('${res}p', style: const TextStyle(fontWeight: FontWeight.w600)),
                  selected: isSelected,
                  selectedColor: AppColors.primaryPurple,
                  onSelected: _isExporting ? null : (val) => setState(() => _selectedResolution = res),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 20),

          // Status & Progress indicator
          if (_isExporting) ...[
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.surfaceBorder),
              ),
              child: Column(
                children: [
                  const LinearProgressIndicator(
                    color: AppColors.primaryPurple,
                    backgroundColor: AppColors.surfaceBorder,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    _statusMessage ?? 'Processing...',
                    style: const TextStyle(fontSize: 12, color: AppColors.primaryPurpleLight),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
          ],

          if (_error != null) ...[
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.accentCoral.withOpacity(0.15),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.accentCoral),
              ),
              child: Row(
                children: [
                  const Icon(Icons.error_outline, color: AppColors.accentCoral, size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _error!,
                      style: const TextStyle(color: AppColors.accentCoral, fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
          ],

          if (_downloadUrl != null) ...[
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.accentGreen.withOpacity(0.15),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.accentGreen),
              ),
              child: Row(
                children: [
                  const Icon(Icons.check_circle, color: AppColors.accentGreen, size: 24),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: const [
                        Text(
                          'Render Complete',
                          style: TextStyle(fontWeight: FontWeight.w700, color: Colors.white, fontSize: 14),
                        ),
                        Text(
                          'Captions burned in with custom fonts & animations',
                          style: TextStyle(color: AppColors.textSecondary, fontSize: 11),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
          ],

          // Action Buttons
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () async {
                    try {
                      final srt = await widget.apiService.exportSrt(widget.project.id);
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('Exported SRT with ${srt.split('\n\n').length} cues')),
                        );
                      }
                    } catch (e) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('SRT export error: $e')),
                        );
                      }
                    }
                  },
                  icon: const Icon(Icons.subtitles_outlined, size: 18),
                  label: const Text('Export SRT'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                flex: 2,
                child: ElevatedButton.icon(
                  onPressed: _isExporting ? null : _startExport,
                  icon: const Icon(Icons.video_library),
                  label: Text(_isExporting ? 'Exporting...' : 'Render MP4'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
