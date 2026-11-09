import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../../models/project.dart';

class ApiService {
  final http.Client _client;

  ApiService({http.Client? client}) : _client = client ?? http.Client();

  Future<bool> checkHealth() async {
    try {
      final res = await _client.get(Uri.parse(ApiConstants.health)).timeout(const Duration(seconds: 4));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['ok'] == true;
      }
      return false;
    } catch (_) {
      return false;
    }
  }

  Future<Map<String, dynamic>> fetchConfig() async {
    final res = await _client.get(Uri.parse(ApiConstants.config));
    if (res.statusCode == 200) {
      return jsonDecode(res.body) as Map<String, dynamic>;
    }
    throw Exception('Failed to fetch backend configuration (${res.statusCode})');
  }

  Future<List<Map<String, dynamic>>> getProjects() async {
    final res = await _client.get(Uri.parse(ApiConstants.projects));
    if (res.statusCode == 200) {
      final list = jsonDecode(res.body) as List<dynamic>;
      return list.map((e) => e as Map<String, dynamic>).toList();
    }
    throw Exception('Failed to load projects list (${res.statusCode})');
  }

  Future<Project> getProject(String pid) async {
    final res = await _client.get(Uri.parse(ApiConstants.project(pid)));
    if (res.statusCode == 200) {
      return Project.fromJson(jsonDecode(utf8.decode(res.bodyBytes)));
    }
    throw Exception('Failed to load project $pid (${res.statusCode})');
  }

  Future<Project> createProject({
    required Uint8List bytes,
    required String filename,
  }) async {
    final uri = Uri.parse(ApiConstants.projects);
    final req = http.MultipartRequest('POST', uri);
    req.files.add(http.MultipartFile.fromBytes('file', bytes, filename: filename));

    final streamedRes = await req.send();
    final res = await http.Response.fromStream(streamedRes);

    if (res.statusCode == 200) {
      return Project.fromJson(jsonDecode(utf8.decode(res.bodyBytes)));
    }
    throw Exception('Failed to create project: ${res.body}');
  }

  Future<Project> updateProject(String pid, Project project) async {
    final res = await _client.put(
      Uri.parse(ApiConstants.project(pid)),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'name': project.name,
        'trim': project.trim.toJson(),
        'tracks': project.tracks.map((t) => t.toJson()).toList(),
        'overlays': project.overlays.map((o) => o.toJson()).toList(),
        'audioClips': project.audioClips.map((a) => a.toJson()).toList(),
        'customFonts': project.customFonts,
        'effects': project.effects.toJson(),
        'resolution': project.resolution,
        'aspectRatio': project.aspectRatio,
      }),
    );
    if (res.statusCode == 200) {
      return Project.fromJson(jsonDecode(utf8.decode(res.bodyBytes)));
    }
    throw Exception('Failed to update project: ${res.body}');
  }

  Future<Map<String, dynamic>> getSfxCatalog() async {
    final res = await _client.get(Uri.parse(ApiConstants.sfx));
    if (res.statusCode == 200) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    }
    throw Exception('Failed to load SFX catalog (${res.statusCode})');
  }

  Future<List<Map<String, dynamic>>> getFonts() async {
    final res = await _client.get(Uri.parse(ApiConstants.fonts));
    if (res.statusCode == 200) {
      final data = jsonDecode(utf8.decode(res.bodyBytes));
      return (data['fonts'] as List<dynamic>).map((e) => e as Map<String, dynamic>).toList();
    }
    throw Exception('Failed to load fonts library (${res.statusCode})');
  }

  Future<Map<String, dynamic>> uploadCustomFont(Uint8List fontBytes, String filename) async {
    final uri = Uri.parse(ApiConstants.fonts);
    final req = http.MultipartRequest('POST', uri);
    req.files.add(http.MultipartFile.fromBytes('file', fontBytes, filename: filename));
    final streamedRes = await req.send();
    final res = await http.Response.fromStream(streamedRes);
    if (res.statusCode == 200) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    }
    throw Exception('Failed to upload custom font: ${res.body}');
  }

  Future<List<Map<String, dynamic>>> trackMotion(
    String pid, {
    String trackType = 'face',
    double start = 0.0,
    double end = 5.0,
  }) async {
    final res = await _client.post(
      Uri.parse(ApiConstants.projectTrack(pid)),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'trackType': trackType, 'start': start, 'end': end}),
    );
    if (res.statusCode == 200) {
      final data = jsonDecode(utf8.decode(res.bodyBytes));
      return (data['points'] as List<dynamic>).map((e) => e as Map<String, dynamic>).toList();
    }
    throw Exception('Failed to track motion: ${res.body}');
  }

  Future<void> deleteProject(String pid) async {
    final res = await _client.delete(Uri.parse(ApiConstants.project(pid)));
    if (res.statusCode != 200) {
      throw Exception('Failed to delete project ($pid)');
    }
  }

  Future<Map<String, dynamic>> importSrt(String pid, String srtContent) async {
    final uri = Uri.parse(ApiConstants.projectSrt(pid));
    final req = http.MultipartRequest('POST', uri);
    req.files.add(http.MultipartFile.fromString('file', srtContent, filename: 'subtitles.srt'));
    final streamedRes = await req.send();
    final res = await http.Response.fromStream(streamedRes);
    if (res.statusCode == 200) {
      return jsonDecode(utf8.decode(res.bodyBytes));
    }
    throw Exception('Failed to import SRT: ${res.body}');
  }

  Future<String> exportSrt(String pid, [String trackId = '']) async {
    final res = await _client.get(Uri.parse(ApiConstants.projectSrt(pid, trackId)));
    if (res.statusCode == 200) {
      return utf8.decode(res.bodyBytes);
    }
    throw Exception('Failed to export SRT: ${res.body}');
  }

  Future<String> transcribe(String pid, {String? language, String model = 'small'}) async {
    final res = await _client.post(
      Uri.parse(ApiConstants.projectTranscribe(pid)),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'language': language, 'model': model}),
    );
    if (res.statusCode == 200) {
      final data = jsonDecode(res.body);
      return data['jobId'] as String;
    }
    throw Exception('Transcription failed to start: ${res.body}');
  }

  Future<String> translate(
    String pid, {
    required String trackId,
    required String language,
    String mode = 'Simultaneous',
    String? provider,
  }) async {
    final payload = <String, dynamic>{
      'trackId': trackId,
      'language': language,
      'mode': mode,
    };
    if (provider != null && provider.isNotEmpty) {
      payload['provider'] = provider;
    }
    final res = await _client.post(
      Uri.parse(ApiConstants.projectTranslate(pid)),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(payload),
    );
    if (res.statusCode == 200) {
      final data = jsonDecode(res.body);
      return data['jobId'] as String;
    }
    throw Exception('Translation failed to start: ${res.body}');
  }

  Future<String> exportVideo(String pid, int resolution, [String? aspectRatio]) async {
    final payload = <String, dynamic>{'resolution': resolution};
    if (aspectRatio != null) {
      payload['aspectRatio'] = aspectRatio;
    }
    final res = await _client.post(
      Uri.parse(ApiConstants.projectExport(pid)),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(payload),
    );
    if (res.statusCode == 200) {
      final data = jsonDecode(res.body);
      return data['jobId'] as String;
    }
    throw Exception('Export failed to start: ${res.body}');
  }

  Future<Map<String, dynamic>> pollJob(
    String jobId, {
    Duration pollInterval = const Duration(milliseconds: 300),
    int maxAttempts = 120,
    void Function(String message)? onProgress,
  }) async {
    for (int i = 0; i < maxAttempts; i++) {
      final res = await _client.get(Uri.parse(ApiConstants.job(jobId)));
      if (res.statusCode == 200) {
        final data = jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
        final status = data['status'] as String?;
        if (onProgress != null && data['message'] != null) {
          onProgress(data['message'].toString());
        }
        if (status == 'complete') {
          return data;
        } else if (status == 'error') {
          throw Exception(data['message'] ?? 'Background job error');
        }
      }
      await Future.delayed(pollInterval);
    }
    throw TimeoutException('Background job timed out after $maxAttempts checks');
  }

  String getDownloadUrl(String pid) => ApiConstants.projectDownload(pid);
}
