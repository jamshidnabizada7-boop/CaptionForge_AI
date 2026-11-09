---
name: mobile-app-testing
description: Use this skill when authoring, maintaining, and executing automated tests for Flutter mobile applications and backend services in CaptionForge.
---

# Mobile App Testing Guide for CaptionForge

This guide establishes the testing hierarchy, test design patterns, and edge case matrices for CaptionForge.

## Testing Pyramid

1. **Unit Tests**:
   - Model serialization/deserialization (`Project.fromJson`, `CaptionCue.copyWith`).
   - Time calculations, formatting (`Duration` <-> `00:01:23.456`).
   - Undo/Redo command stack mechanics.
2. **Widget Tests**:
   - Verification of UI elements: Home screen, project list, editor shell, bottom tool trays.
   - Timeline interaction: Cue selection, drag handle movements, scrubbing.
   - Subtitle overlay rendering and responsive viewport sizing.
3. **Integration Tests**:
   - End-to-end import -> trim -> AI transcribe -> style edit -> export flow.
   - Inter-process communication between Flutter client and FastAPI backend.

## Writing Robust Flutter Widget Tests

```dart
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

void main() {
  testWidgets('Selecting a caption cue highlights it in timeline and preview', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(
          home: CaptionEditorScreen(),
        ),
      ),
    );

    // Verify initial idle state
    expect(find.byType(CaptionTimelineView), findsOneWidget);

    // Tap on first caption block
    final cueFinder = find.text('Welcome to CaptionForge');
    expect(cueFinder, findsOneWidget);
    await tester.tap(cueFinder);
    await tester.pumpAndSettle();

    // Verify editing tray appears with style controls
    expect(find.text('Edit Caption'), findsOneWidget);
  });
}
```

## Critical Edge Cases Matrix

| Edge Case | Test Strategy | Expected Behavior |
| :--- | :--- | :--- |
| **Zero-length / empty video** | Mock picker returns 0s file | Show error banner; reject import gracefully |
| **Silent audio stream** | Input video without audio | ASR returns empty transcript; prompt user to enter text or SRT |
| **Overlapping cue times** | Import SRT with identical start times | Auto-resolve or visually warn user in timeline |
| **Complex RTL text** | Persian, Arabic, Urdu with numbers | Correct visual shaping without inverted numbers or reversed words |
| **Network interruption during render** | Disconnect Wi-Fi while polling `/api/jobs` | Exponential backoff retry with UI status 'Reconnecting...' |
| **Rapid button double-taps** | Tap 'Export' 5 times in 100ms | Debounce prevents duplicate background render jobs |

## Testing Commands

```bash
# Run all Flutter tests
flutter test

# Run tests with coverage output
flutter test --coverage

# Run specific test file
flutter test test/features/timeline/timeline_widget_test.dart

# Run backend pytest suite
pytest -v tests/
```

## Verification Checklist

- [ ] All unit and widget tests pass with zero failures.
- [ ] Golden tests match expected UI themes and font renderings.
- [ ] No unhandled exceptions or memory leaks on widget disposal.
- [ ] Backend smoke tests verify API contract matches Flutter models.
