import 'dart:io';
import 'package:flutter/foundation.dart';

import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import '../../core/theme/app_theme.dart';
import '../../core/constants/api_constants.dart';
import '../../core/services/api_service.dart';
import '../../models/project.dart';
import '../editor/editor_screen.dart';

class HomeScreen extends StatefulWidget {
  final ApiService? apiService;

  const HomeScreen({super.key, this.apiService});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late final ApiService _api;
  List<Map<String, dynamic>> _projects = [];
  bool _isLoading = true;
  String? _errorMessage;
  Map<String, dynamic>? _backendConfig;

  @override
  void initState() {
    super.initState();
    _api = widget.apiService ?? ApiService();
    _loadProjects();
    _loadBackendConfig();
  }

  Future<void> _loadBackendConfig() async {
    try {
      final cfg = await _api.fetchConfig();
      if (mounted) setState(() => _backendConfig = cfg);
    } catch (_) {}
  }

  Future<void> _loadProjects() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final list = await _api.getProjects();
      if (mounted) {
        setState(() {
          _projects = list;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString();
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _openProject(String pid) async {
    try {
      final project = await _api.getProject(pid);
      if (mounted) {
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => EditorScreen(
              initialProject: project,
              apiService: _api,
            ),
          ),
        );
        _loadProjects();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Cannot open project: $e')),
        );
      }
    }
  }

  Future<void> _pickAndImportVideo() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.video,
        allowMultiple: false,
        withData: true,
      );

      if (result == null || result.files.isEmpty) return;

      final file = result.files.first;
      Uint8List? bytes = file.bytes;
      if (bytes == null && file.path != null && !kIsWeb) {
        bytes = await File(file.path!).readAsBytes();
      }

      if (bytes == null || bytes.isEmpty) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Could not read video file data')),
          );
        }
        return;
      }

      if (!mounted) return;

      // Show video trim & import confirmation sheet
      await _showTrimAndUploadSheet(bytes, file.name);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error selecting video: $e')),
        );
      }
    }
  }

  Future<void> _showTrimAndUploadSheet(Uint8List videoBytes, String filename) async {
    double trimStart = 0.0;
    double trimEnd = 30.0; // default estimated duration clamp
    bool isUploading = false;
    String statusText = 'Ready to import';

    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surfaceElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) {
          final sizeMb = (videoBytes.lengthInBytes / (1024 * 1024)).toStringAsFixed(1);

          return Padding(
            padding: EdgeInsets.only(
              bottom: MediaQuery.of(ctx).viewInsets.bottom + 20,
              top: 24,
              left: 20,
              right: 20,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Import & Trim Video',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close),
                      onPressed: () => Navigator.pop(ctx),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.surface,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.surfaceBorder),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.primaryPurple.withOpacity(0.2),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Icon(Icons.movie, color: AppColors.primaryPurpleLight),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              filename,
                              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                              overflow: TextOverflow.ellipsis,
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '$sizeMb MB • Selected segment: ${(trimEnd - trimStart).toStringAsFixed(1)}s',
                              style: const TextStyle(fontSize: 12, color: AppColors.textMuted),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                const Text('Trim Duration (seconds):', style: TextStyle(color: AppColors.textSecondary, fontSize: 13)),
                RangeSlider(
                  values: RangeValues(trimStart, trimEnd),
                  min: 0.0,
                  max: 60.0,
                  divisions: 60,
                  activeColor: AppColors.primaryPurple,
                  inactiveColor: AppColors.surfaceBorder,
                  labels: RangeLabels('${trimStart.toStringAsFixed(0)}s', '${trimEnd.toStringAsFixed(0)}s'),
                  onChanged: (RangeValues values) {
                    if (values.end - values.start >= 1.0) {
                      setSheetState(() {
                        trimStart = values.start;
                        trimEnd = values.end;
                      });
                    }
                  },
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Start: ${trimStart.toStringAsFixed(1)}s', style: const TextStyle(fontSize: 12, color: AppColors.textMuted)),
                    Text('End: ${trimEnd.toStringAsFixed(1)}s', style: const TextStyle(fontSize: 12, color: AppColors.textMuted)),
                  ],
                ),
                const SizedBox(height: 20),
                if (isUploading) ...[
                  LinearProgressIndicator(color: AppColors.primaryPurple, backgroundColor: AppColors.surfaceBorder),
                  const SizedBox(height: 8),
                  Center(child: Text(statusText, style: const TextStyle(fontSize: 12, color: AppColors.textSecondary))),
                  const SizedBox(height: 12),
                ],
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primaryPurple,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    onPressed: isUploading
                        ? null
                        : () async {
                            setSheetState(() {
                              isUploading = true;
                              statusText = 'Uploading video to CaptionForge AI backend...';
                            });
                            try {
                              final created = await _api.createProject(
                                bytes: videoBytes,
                                filename: filename,
                              );
                              // Update trim if customized
                              if (trimStart > 0 || trimEnd < created.duration) {
                                setSheetState(() => statusText = 'Applying trim parameters...');
                                await _api.updateProject(
                                  created.id,
                                  created.copyWith(
                                    trim: VideoTrim(
                                      start: trimStart,
                                      end: trimEnd.clamp(trimStart + 0.5, created.duration),
                                    ),
                                  ),
                                );
                              }
                              if (ctx.mounted) Navigator.pop(ctx);
                              _loadProjects();
                              _openProject(created.id);
                            } catch (e) {
                              setSheetState(() {
                                isUploading = false;
                                statusText = 'Upload failed: $e';
                              });
                            }
                          },
                    icon: const Icon(Icons.cloud_upload),
                    label: Text(
                      isUploading ? 'Uploading & Processing...' : 'Upload & Open in Editor',
                      style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Future<void> _createSampleProject() async {
    setState(() => _isLoading = true);
    try {
      if (_projects.isNotEmpty) {
        await _openProject(_projects.first['id'] as String);
      } else {
        final now = DateTime.now().toUtc().toIso8601String();
        final dummy = Project(
          id: 'sample_${DateTime.now().millisecondsSinceEpoch}',
          name: 'CaptionForge Demo',
          source: 'sample.mp4',
          duration: 3.5,
          width: 720,
          height: 1280,
          hasAudio: true,
          trim: const VideoTrim(start: 0, end: 3.5),
          createdAt: now,
          updatedAt: now,
          tracks: [
            Track(
              id: 'tr_main',
              name: 'Original Captions',
              language: 'en',
              style: const SubtitleStyle(
                preset: 'Bold',
                animation: 'Pop',
                font: 'Noto Sans',
                fontSize: 26,
                textColor: '#FFFFFF',
                bgColor: '#181124',
              ),
              captions: [
                CaptionCue(
                  id: 'c1',
                  start: 0.2,
                  end: 1.8,
                  text: 'Welcome to CaptionForge AI',
                  words: [
                    const WordTiming(start: 0.2, end: 0.7, text: 'Welcome'),
                    const WordTiming(start: 0.7, end: 0.9, text: 'to'),
                    const WordTiming(start: 0.9, end: 1.4, text: 'CaptionForge'),
                    const WordTiming(start: 1.4, end: 1.8, text: 'AI'),
                  ],
                ),
                CaptionCue(
                  id: 'c2',
                  start: 2.0,
                  end: 3.4,
                  text: 'ویرایشگر هوشمند زیرنویس ویدیو',
                  words: [
                    const WordTiming(start: 2.0, end: 2.5, text: 'ویرایشگر'),
                    const WordTiming(start: 2.5, end: 2.8, text: 'هوشمند'),
                    const WordTiming(start: 2.8, end: 3.1, text: 'زیرنویس'),
                    const WordTiming(start: 3.1, end: 3.4, text: 'ویدیو'),
                  ],
                ),
              ],
            ),
          ],
        );

        if (mounted) {
          await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => EditorScreen(
                initialProject: dummy,
                apiService: _api,
              ),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error opening sample: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showNewProjectOptions() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surfaceElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Create New Project',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 16),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.primaryPurple.withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.video_library, color: AppColors.primaryPurpleLight),
              ),
              title: const Text('Choose Video from Gallery / Files', style: TextStyle(fontWeight: FontWeight.w600)),
              subtitle: const Text('MP4, MOV, WebM (up to 500 MB)', style: TextStyle(fontSize: 12, color: AppColors.textMuted)),
              trailing: const Icon(Icons.chevron_right, color: AppColors.textSecondary),
              onTap: () {
                Navigator.pop(ctx);
                _pickAndImportVideo();
              },
            ),
            const Divider(color: AppColors.surfaceBorder),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.accentCyan.withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.play_circle_outline, color: AppColors.accentCyan),
              ),
              title: const Text('Open Sample Demo Project', style: TextStyle(fontWeight: FontWeight.w600)),
              subtitle: const Text('Instant preview with dual-language subtitle cues', style: TextStyle(fontSize: 12, color: AppColors.textMuted)),
              trailing: const Icon(Icons.chevron_right, color: AppColors.textSecondary),
              onTap: () {
                Navigator.pop(ctx);
                _createSampleProject();
              },
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _renameProject(String pid, String currentName) async {
    final controller = TextEditingController(text: currentName);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surfaceElevated,
        title: const Text('Rename Project', style: TextStyle(fontWeight: FontWeight.w700)),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(labelText: 'Project Name', border: OutlineInputBorder()),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Rename'),
          ),
        ],
      ),
    );

    if (confirmed == true && controller.text.trim().isNotEmpty) {
      try {
        final pr = await _api.getProject(pid);
        await _api.updateProject(pid, pr.copyWith(name: controller.text.trim()));
        _loadProjects();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Cannot rename project: $e')));
        }
      }
    }
  }

  Future<void> _duplicateProject(String pid) async {
    try {
      final pr = await _api.getProject(pid);
      final duplicated = pr.copyWith(name: '${pr.name} (Copy)');
      await _api.updateProject(pid, duplicated);
      _loadProjects();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Project duplicated successfully')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Duplicate error: $e')));
      }
    }
  }

  Future<void> _deleteProject(String pid, String name) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surfaceElevated,
        title: const Text('Delete Project', style: TextStyle(fontWeight: FontWeight.w700)),
        content: Text('Are you sure you want to delete "$name"? This cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.accentCoral),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        await _api.deleteProject(pid);
        _loadProjects();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Cannot delete project: $e')));
        }
      }
    }
  }

  Future<void> _showServerConfigDialog() async {
    final controller = TextEditingController(text: ApiConstants.baseUrl);
    String? testResult;
    bool isTesting = false;
    bool isSuccess = false;

    await showDialog(
      context: context,
      builder: (dCtx) => StatefulBuilder(
        builder: (dCtx, setDialogState) => AlertDialog(
          backgroundColor: AppColors.surfaceElevated,
          title: const Text('Backend Server Configuration', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 17)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Choose Environment Preset:', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: [
                    ActionChip(
                      label: const Text('Emulator (10.0.2.2)', style: TextStyle(fontSize: 11)),
                      backgroundColor: AppColors.surface,
                      onPressed: () {
                        controller.text = 'http://10.0.2.2:8000';
                        setDialogState(() => testResult = null);
                      },
                    ),
                    ActionChip(
                      label: const Text('USB ADB (127.0.0.1)', style: TextStyle(fontSize: 11)),
                      backgroundColor: AppColors.surface,
                      onPressed: () {
                        controller.text = 'http://127.0.0.1:8000';
                        setDialogState(() => testResult = null);
                      },
                    ),
                    ActionChip(
                      label: const Text('Local Wi-Fi', style: TextStyle(fontSize: 11)),
                      backgroundColor: AppColors.surface,
                      onPressed: () {
                        controller.text = 'http://192.168.1.100:8000';
                        setDialogState(() => testResult = null);
                      },
                    ),
                    ActionChip(
                      label: const Text('Production HTTPS', style: TextStyle(fontSize: 11)),
                      backgroundColor: AppColors.surface,
                      onPressed: () {
                        controller.text = 'https://api.captionforge.app';
                        setDialogState(() => testResult = null);
                      },
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: controller,
                  decoration: const InputDecoration(
                    labelText: 'Backend URL',
                    hintText: 'http://10.0.2.2:8000',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.dns, size: 20),
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    OutlinedButton.icon(
                      icon: isTesting
                          ? const SizedBox(width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.wifi_find, size: 16),
                      label: const Text('Test Connection'),
                      onPressed: isTesting
                          ? null
                          : () async {
                              setDialogState(() {
                                isTesting = true;
                                testResult = null;
                              });
                              final targetUrl = controller.text.trim();
                              final previousUrl = ApiConstants.baseUrl;
                              ApiConstants.setBaseUrl(targetUrl);
                              try {
                                final sw = Stopwatch()..start();
                                final ok = await _api.checkHealth();
                                sw.stop();
                                if (ok) {
                                  final cfg = await _api.fetchConfig();
                                  setDialogState(() {
                                    isTesting = false;
                                    isSuccess = true;
                                    testResult = 'Connected (${sw.elapsedMilliseconds}ms)! AI: ${cfg['transcription']}, Translation: ${cfg['translation']}';
                                  });
                                } else {
                                  throw Exception('Health check returned false');
                                }
                              } catch (e) {
                                ApiConstants.setBaseUrl(previousUrl);
                                setDialogState(() {
                                  isTesting = false;
                                  isSuccess = false;
                                  String tip = '';
                                  if (targetUrl.contains('127.0.0.1')) {
                                    tip = ' (Run "adb reverse tcp:8000 tcp:8000" on PC for USB phone)';
                                  } else if (targetUrl.contains('10.0.2.2')) {
                                    tip = ' (10.0.2.2 is valid inside Android Emulator only)';
                                  }
                                  testResult = 'Connection failed: $e$tip';
                                });
                              }
                            },
                    ),
                  ],
                ),
                if (testResult != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      testResult!,
                      style: TextStyle(
                        fontSize: 11,
                        color: isSuccess ? AppColors.accentGreen : AppColors.accentCoral,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dCtx),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () {
                final targetUrl = controller.text.trim();
                if (targetUrl.isNotEmpty) {
                  ApiConstants.setBaseUrl(targetUrl);
                  Navigator.pop(dCtx);
                  _loadProjects();
                  _loadBackendConfig();
                }
              },
              child: const Text('Save & Connect'),
            ),
          ],
        ),
      ),
    );
  }

  void _showSettingsModal() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surfaceElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'CaptionForge Settings & Capabilities',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.surfaceBorder),
              ),
              child: Column(
                children: [
                  _buildStatusRow(
                    'AI Speech Transcription',
                    _backendConfig?['transcription'] == 'faster-whisper' ? 'Ready (Faster-Whisper)' : 'Checking...',
                    _backendConfig?['transcription'] == 'faster-whisper',
                  ),
                  const Divider(color: AppColors.surfaceBorder),
                  _buildStatusRow(
                    'FFmpeg Video Renderer',
                    _backendConfig?['render'] == true ? 'Ready (libass + h264)' : 'Not available',
                    _backendConfig?['render'] == true,
                  ),
                  const Divider(color: AppColors.surfaceBorder),
                  _buildStatusRow(
                    'OpenCV Face Tracking',
                    _backendConfig?['faceTracking'] == true ? 'Ready (Haar Cascades)' : 'Not available',
                    _backendConfig?['faceTracking'] == true,
                  ),
                  const Divider(color: AppColors.surfaceBorder),
                  () {
                    final transRaw = (_backendConfig?['translation'] ?? '').toString();
                    final isReady = transRaw.startsWith('ready');
                    final label = isReady
                        ? (transRaw.contains('OpenAI') ? 'Ready (OpenAI)' : 'Ready (Offline Engine)')
                        : 'Set OPENAI_API_KEY';
                    return _buildStatusRow('AI Translation Engine', label, isReady);
                  }(),
                  const Divider(color: AppColors.surfaceBorder),
                  InkWell(
                    onTap: () {
                      Navigator.pop(ctx);
                      _showServerConfigDialog();
                    },
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text('Backend Server URL', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                          Row(
                            children: [
                              Text(
                                ApiConstants.baseUrl,
                                style: const TextStyle(fontSize: 11, color: AppColors.primaryPurpleLight, decoration: TextDecoration.underline),
                              ),
                              const SizedBox(width: 4),
                              const Icon(Icons.edit, size: 12, color: AppColors.primaryPurpleLight),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'CaptionForge v1.0.0+1 • Flutter 3.47 / Dart 3.13 / FastAPI',
              style: TextStyle(fontSize: 11, color: AppColors.textMuted),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusRow(String label, String value, bool isOk) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
          Row(
            children: [
              Icon(isOk ? Icons.check_circle : Icons.info_outline, size: 16, color: isOk ? Colors.greenAccent : Colors.amberAccent),
              const SizedBox(width: 6),
              Text(value, style: TextStyle(fontSize: 12, color: isOk ? Colors.greenAccent : Colors.amberAccent)),
            ],
          ),
        ],
      ),
    );
  }

  void _showProModal() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surfaceElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: const BoxDecoration(
                color: Color(0x338B5CF6),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.workspace_premium, color: AppColors.primaryPurple, size: 32),
            ),
            const SizedBox(height: 16),
            const Text(
              'CaptionForge Pro',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 8),
            const Text(
              'Unlimited 4K UHD Exports • Real-Time AI Speech Transcription • Multilingual Bilingual Subtitles • Advanced Face Tracking',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 13, color: AppColors.textSecondary, height: 1.4),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => Navigator.pop(ctx),
                child: const Text('Upgrade to Pro — \$9.99 / mo'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        titleSpacing: 16,
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 30,
              height: 30,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [AppColors.primaryPurple, AppColors.accentCyan],
                ),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(Icons.closed_caption, color: Colors.white, size: 18),
            ),
            const SizedBox(width: 8),
            const Flexible(
              child: Text(
                'CaptionForge',
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            tooltip: 'Settings & Status',
            onPressed: _showSettingsModal,
          ),
          IconButton(
            icon: const Icon(Icons.workspace_premium, color: AppColors.primaryPurpleLight),
            tooltip: 'Pro Membership',
            onPressed: _showProModal,
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _loadProjects,
          color: AppColors.primaryPurple,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // Premium banner teaser (matching IMG_9611)
              GestureDetector(
                onTap: _showProModal,
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF2E1065), Color(0xFF1E1B4B)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: AppColors.primaryPurple.withOpacity(0.5), width: 1.5),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.primaryPurple.withOpacity(0.2),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.auto_awesome, color: AppColors.primaryPurpleLight, size: 24),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: const [
                            Text(
                              'Auto AI Captions & Styles',
                              style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14, color: Colors.white),
                            ),
                            SizedBox(height: 2),
                            Text(
                              'Transcribe, translate, and animate viral captions in seconds',
                              style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                            ),
                          ],
                        ),
                      ),
                      const Icon(Icons.chevron_right, color: AppColors.textSecondary),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // CTA: New Project Button (Matching IMG_9611)
              SizedBox(
                height: 54,
                child: ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primaryPurple,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  ),
                  onPressed: _showNewProjectOptions,
                  icon: const Icon(Icons.add_circle, size: 22),
                  label: const Text(
                    'New Project',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // Recent Projects Section Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Recent Projects',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: Colors.white),
                  ),
                  Text(
                    '${_projects.length} clips',
                    style: const TextStyle(fontSize: 12, color: AppColors.textMuted),
                  ),
                ],
              ),
              const SizedBox(height: 12),

              // Projects List or Empty State
              if (_isLoading)
                const Center(
                  child: Padding(
                    padding: EdgeInsets.all(32),
                    child: CircularProgressIndicator(color: AppColors.primaryPurple),
                  ),
                )
              else if (_errorMessage != null)
                Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      children: [
                        const Icon(Icons.cloud_off, size: 40, color: AppColors.textMuted),
                        const SizedBox(height: 12),
                        Text(
                          'Backend offline: $_errorMessage',
                          textAlign: TextAlign.center,
                          style: const TextStyle(color: AppColors.textSecondary, fontSize: 12),
                        ),
                        const SizedBox(height: 14),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          alignment: WrapAlignment.center,
                          children: [
                            ElevatedButton.icon(
                              style: ElevatedButton.styleFrom(backgroundColor: AppColors.primaryPurple),
                              icon: const Icon(Icons.settings_ethernet, size: 16),
                              label: const Text('Configure Server'),
                              onPressed: _showServerConfigDialog,
                            ),
                            OutlinedButton.icon(
                              icon: const Icon(Icons.play_circle_outline, size: 16),
                              onPressed: _createSampleProject,
                              label: const Text('Demo Editor'),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                )
              else if (_projects.isEmpty)
                Center(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 40),
                    child: Column(
                      children: [
                        Icon(Icons.video_library_outlined, size: 48, color: AppColors.textMuted.withOpacity(0.5)),
                        const SizedBox(height: 12),
                        const Text(
                          'No projects yet',
                          style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: AppColors.textSecondary),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'Tap New Project to import your first video',
                          style: TextStyle(fontSize: 12, color: AppColors.textMuted),
                        ),
                      ],
                    ),
                  ),
                )
              else
                ..._projects.map((item) {
                  final pid = item['id'] as String? ?? '';
                  final name = item['name'] as String? ?? 'Untitled';
                  final dur = (item['duration'] as num?)?.toDouble() ?? 0.0;
                  final updated = item['updatedAt'] as String? ?? '';

                  return Card(
                    margin: const EdgeInsets.only(bottom: 12),
                    child: ListTile(
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                      leading: Container(
                        width: 48,
                        height: 64,
                        decoration: BoxDecoration(
                          color: AppColors.surfaceElevated,
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: AppColors.surfaceBorder),
                        ),
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            const Icon(Icons.movie, color: AppColors.primaryPurpleLight, size: 24),
                            Positioned(
                              bottom: 2,
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                                decoration: BoxDecoration(
                                  color: Colors.black.withOpacity(0.7),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Text(
                                  '${dur.toStringAsFixed(1)}s',
                                  style: const TextStyle(fontSize: 8, color: Colors.white),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      title: Text(
                        name,
                        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                      ),
                      subtitle: Text(
                        updated.isNotEmpty ? updated.substring(0, 10) : 'Recent',
                        style: const TextStyle(fontSize: 11, color: AppColors.textMuted),
                      ),
                      trailing: PopupMenuButton<String>(
                        icon: const Icon(Icons.more_vert, color: AppColors.textSecondary),
                        color: AppColors.surfaceElevated,
                        onSelected: (val) async {
                          if (val == 'open') {
                            _openProject(pid);
                          } else if (val == 'rename') {
                            _renameProject(pid, name);
                          } else if (val == 'duplicate') {
                            _duplicateProject(pid);
                          } else if (val == 'delete') {
                            _deleteProject(pid, name);
                          }
                        },
                        itemBuilder: (ctx) => [
                          const PopupMenuItem(value: 'open', child: Text('Open in Editor')),
                          const PopupMenuItem(value: 'rename', child: Text('Rename Project')),
                          const PopupMenuItem(value: 'duplicate', child: Text('Duplicate Project')),
                          const PopupMenuItem(value: 'delete', child: Text('Delete Project', style: TextStyle(color: AppColors.accentCoral))),
                        ],
                      ),
                      onTap: () => _openProject(pid),
                    ),
                  );
                }),
            ],
          ),
        ),
      ),
    );
  }
}
