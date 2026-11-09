import 'package:flutter/material.dart';

class AppColors {
  // Deep dark-purple and black theme
  static const Color background = Color(0xFF0D0B14);
  static const Color surface = Color(0xFF161224);
  static const Color surfaceElevated = Color(0xFF201B34);
  static const Color surfaceBorder = Color(0xFF2E264A);

  // Vibrant purple accents
  static const Color primaryPurple = Color(0xFF8B5CF6);
  static const Color primaryPurpleLight = Color(0xFFA78BFA);
  static const Color primaryPurpleDark = Color(0xFF6D28D9);

  // Functional accents
  static const Color accentCyan = Color(0xFF06B6D4);
  static const Color accentGreen = Color(0xFF10B981);
  static const Color accentAmber = Color(0xFFF59E0B);
  static const Color accentCoral = Color(0xFFF43F5E);

  // Subtitle timeline track colors
  static const List<Color> trackColors = [
    Color(0xFF8B5CF6), // Track 1: Purple
    Color(0xFF06B6D4), // Track 2: Cyan
    Color(0xFFF59E0B), // Track 3: Amber
    Color(0xFF10B981), // Track 4: Emerald
    Color(0xFFEC4899), // Track 5: Pink
  ];

  // Text colors
  static const Color textPrimary = Color(0xFFF8FAFC);
  static const Color textSecondary = Color(0xFF94A3B8);
  static const Color textMuted = Color(0xFF64748B);
}

class AppTheme {
  static ThemeData get darkTheme {
    return ThemeData(
      brightness: Brightness.dark,
      scaffoldBackgroundColor: AppColors.background,
      primaryColor: AppColors.primaryPurple,
      colorScheme: const ColorScheme.dark(
        primary: AppColors.primaryPurple,
        secondary: AppColors.primaryPurpleLight,
        surface: AppColors.surface,
        error: AppColors.accentCoral,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.background,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 18,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.2,
        ),
        iconTheme: IconThemeData(color: AppColors.textPrimary),
      ),
      cardTheme: CardThemeData(
        color: AppColors.surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: AppColors.surfaceBorder, width: 1),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.primaryPurple,
          foregroundColor: Colors.white,
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: const TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.1,
          ),
        ),
      ),
      sliderTheme: const SliderThemeData(
        activeTrackColor: AppColors.primaryPurple,
        inactiveTrackColor: AppColors.surfaceBorder,
        thumbColor: Colors.white,
        overlayColor: Color(0x338B5CF6),
        trackHeight: 4,
      ),
      dividerTheme: const DividerThemeData(
        color: AppColors.surfaceBorder,
        thickness: 1,
        space: 1,
      ),
    );
  }
}
