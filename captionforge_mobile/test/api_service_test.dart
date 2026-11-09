import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:captionforge_mobile/core/services/api_service.dart';
import 'package:captionforge_mobile/models/project.dart';

void main() {
  group('ApiService Integration & Contract Tests', () {
    test('checkHealth returns true when backend responds with ok: true', () async {
      final mockClient = MockClient((request) async {
        if (request.url.path == '/health') {
          return http.Response(jsonEncode({'ok': true}), 200);
        }
        return http.Response('Not Found', 404);
      });

      final api = ApiService(client: mockClient);
      final healthy = await api.checkHealth();
      expect(healthy, true);
    });

    test('getProjects returns parsed projects list', () async {
      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/projects') {
          return http.Response(
            jsonEncode([
              {
                'id': 'p1',
                'name': 'Sample Project',
                'duration': 3.5,
                'source': 'sample.mp4',
                'updatedAt': '2026-11-09T10:00:00Z',
              }
            ]),
            200,
          );
        }
        return http.Response('Not Found', 404);
      });

      final api = ApiService(client: mockClient);
      final projects = await api.getProjects();
      expect(projects.length, 1);
      expect(projects.first['name'], 'Sample Project');
    });

    test('exportVideo initiates job and pollJob awaits completion', () async {
      int pollCount = 0;
      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/projects/p1/export' && request.method == 'POST') {
          return http.Response(jsonEncode({'jobId': 'job_123'}), 200);
        }
        if (request.url.path == '/api/jobs/job_123') {
          pollCount++;
          if (pollCount < 2) {
            return http.Response(
              jsonEncode({'id': 'job_123', 'status': 'running', 'message': 'Rendering...'}),
              200,
            );
          }
          return http.Response(
            jsonEncode({
              'id': 'job_123',
              'status': 'complete',
              'result': {'download': '/api/projects/p1/download', 'duration': 3.5}
            }),
            200,
          );
        }
        return http.Response('Not Found', 404);
      });

      final api = ApiService(client: mockClient);
      final jobId = await api.exportVideo('p1', 720);
      expect(jobId, 'job_123');

      final result = await api.pollJob(jobId, pollInterval: const Duration(milliseconds: 10));
      expect(result['status'], 'complete');
      expect(result['result']['download'], '/api/projects/p1/download');
    });

    test('updateProject sends correct JSON schema and receives updated project', () async {
      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/projects/p1' && request.method == 'PUT') {
          final body = jsonDecode(request.body);
          expect(body.containsKey('name'), true);
          expect(body.containsKey('tracks'), true);
          expect(body.containsKey('effects'), true);
          expect(body.containsKey('aspectRatio'), true);
          return http.Response(
            jsonEncode({
              'id': 'p1',
              'name': body['name'],
              'source': 'original.mp4',
              'duration': 4.0,
              'width': 720,
              'height': 1280,
              'hasAudio': true,
              'trim': {'start': 0.0, 'end': 4.0},
              'resolution': 720,
              'effects': body['effects'],
              'tracks': body['tracks'],
              'createdAt': '2026-11-09T00:00:00Z',
              'updatedAt': '2026-11-09T00:01:00Z',
            }),
            200,
          );
        }
        return http.Response('Not Found', 404);
      });

      final api = ApiService(client: mockClient);
      const project = Project(
        id: 'p1',
        name: 'Updated Name',
        source: 'original.mp4',
        duration: 4.0,
        width: 720,
        height: 1280,
        hasAudio: true,
        trim: VideoTrim(start: 0, end: 4.0),
        createdAt: '2026-11-09T00:00:00Z',
        updatedAt: '2026-11-09T00:00:00Z',
      );

      final updated = await api.updateProject('p1', project);
      expect(updated.name, 'Updated Name');
    });
  });
}
