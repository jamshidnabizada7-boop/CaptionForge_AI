import 'dart:io' show Platform;
import 'package:flutter/foundation.dart';

class ApiConstants {
  static String get defaultBaseUrl {
    if (kIsWeb) return 'http://127.0.0.1:8000';
    try {
      if (Platform.isAndroid) {
        return 'http://10.0.2.2:8000';
      }
    } catch (_) {}
    return 'http://127.0.0.1:8000';
  }

  static String baseUrl = defaultBaseUrl;

  static void setBaseUrl(String url) {
    String clean = url.trim();
    if (clean.endsWith('/')) {
      clean = clean.substring(0, clean.length - 1);
    }
    baseUrl = clean;
  }

  static String get health => '$baseUrl/health';
  static String get config => '$baseUrl/api/config';
  static String get projects => '$baseUrl/api/projects';
  static String project(String pid) => '$baseUrl/api/projects/$pid';
  static String projectVideo(String pid) => '$baseUrl/api/projects/$pid/video';
  static String projectSrt(String pid, [String trackId = '']) =>
      '$baseUrl/api/projects/$pid/srt${trackId.isNotEmpty ? '?track=$trackId' : ''}';
  static String projectTranscribe(String pid) =>
      '$baseUrl/api/projects/$pid/transcribe';
  static String projectTranslate(String pid) =>
      '$baseUrl/api/projects/$pid/translate';
  static String projectExport(String pid) =>
      '$baseUrl/api/projects/$pid/export';
  static String projectDownload(String pid) =>
      '$baseUrl/api/projects/$pid/download';
  static String job(String jid) => '$baseUrl/api/jobs/$jid';
  static String get sfx => '$baseUrl/api/sfx';
  static String get fonts => '$baseUrl/api/fonts';
  static String projectTrack(String pid) => '$baseUrl/api/projects/$pid/track';
  static String projectAudio(String pid) => '$baseUrl/api/projects/$pid/audio';
}
