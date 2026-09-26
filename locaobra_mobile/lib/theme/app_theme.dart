// ============================================
// LOCAOBRA — DESIGN SYSTEM (mobile)
// Espelha 1:1 os tokens de locaobra_web/src/styles/global.css
// Paleta: aço + âmbar de obra. Tipografia técnica.
// ============================================
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Cores — mesmos valores hex das CSS variables do web.
class AppColors {
  AppColors._();

  // Cor primária — âmbar de obra
  static const Color primary = Color(0xFFFF6600);
  static const Color primaryDark = Color(0xFFAF4600);
  static const Color primaryLight = Color(0xFFFF934B);
  // --secondary-blue: rgba(217,130,15,0.10) — tinta usada em fundos leves
  static const Color primaryTint = Color(0x1AD9820F);

  // Aço/marinho — navegação, headers técnicos
  static const Color steel900 = Color(0xFF17232F);
  static const Color steel800 = Color(0xFF1E2E3D);
  static const Color steel700 = Color(0xFF2A4157);
  static const Color steel600 = Color(0xFF3A5670);
  static const Color steel100 = Color(0xFFE9EDF1);

  // Apoio
  static const Color success = Color(0xFF1F9D6B);
  static const Color successLight = Color(0xFF3DBE8A);
  static const Color successBg = Color(0xFFE4F6EE);
  static const Color warning = Color(0xFFD9A441);
  static const Color warningBg = Color(0xFFFAF0DC);
  static const Color error = Color(0xFFDC4C4C);
  static const Color errorLight = Color(0xFFE97070);
  static const Color errorBg = Color(0xFFFCE9E9);
  static const Color info = Color(0xFF3574B8);

  // Neutras
  static const Color white = Color(0xFFFFFFFF);
  static const Color gray50 = Color(0xFFF9FAFB);
  static const Color gray100 = Color(0xFFF3F4F6);
  static const Color gray200 = Color(0xFFE5E7EB);
  static const Color gray300 = Color(0xFFD1D5DB);
  static const Color gray400 = Color(0xFF9CA3AF);
  static const Color gray500 = Color(0xFF6B7280);
  static const Color gray600 = Color(0xFF4B5563);
  static const Color gray700 = Color(0xFF374151);
  static const Color gray800 = Color(0xFF1F2937);
  static const Color gray900 = Color(0xFF111827);

  // Aliases (iguais ao "compatibilidade com os módulos administrativos" do web)
  static const Color bgPrimary = Color(0xFFF4F5F7);
  static const Color bgSecondary = white;
  static const Color textPrimary = Color(0xFF1B2530);
  static const Color textSecondary = Color(0xFF667181);
  static const Color borderColor = Color(0xFFE4E7EB);
}

/// Espaçamentos — mesma escala compacta do web (rem -> dp, base 16px).
class AppSpacing {
  AppSpacing._();
  static const double s1 = 4;
  static const double s2 = 8;
  static const double s3 = 10;
  static const double s4 = 14;
  static const double s5 = 18;
  static const double s6 = 20;
  static const double s8 = 28;
  static const double s10 = 34;
  static const double s12 = 40;
  static const double s16 = 52;
  static const double s20 = 64;
}

/// Raios de borda — "menos bolha, mais técnico", igual ao web.
class AppRadius {
  AppRadius._();
  static const double sm = 2;
  static const double md = 5;
  static const double lg = 7;
  static const double xl = 10;
  static const double xl2 = 14;
  static const double full = 999;
}

/// Sombras equivalentes às var(--shadow-*) do web (rgba(23,35,47,...)).
class AppShadows {
  AppShadows._();
  static List<BoxShadow> sm = [
    BoxShadow(color: AppColors.steel900.withOpacity(0.06), blurRadius: 2, offset: const Offset(0, 1)),
  ];
  static List<BoxShadow> md = [
    BoxShadow(color: AppColors.steel900.withOpacity(0.10), blurRadius: 8, offset: const Offset(0, 4)),
    BoxShadow(color: AppColors.steel900.withOpacity(0.06), blurRadius: 4, offset: const Offset(0, 2)),
  ];
  static List<BoxShadow> lg = [
    BoxShadow(color: AppColors.steel900.withOpacity(0.12), blurRadius: 20, offset: const Offset(0, 10)),
  ];
}

/// Escala tipográfica — mesmos tamanhos do web (--text-xs a --text-5xl).
/// Fontes: Inter (texto) e Space Grotesk (títulos), igual ao @import do global.css.
class AppText {
  AppText._();

  static TextStyle _base({
    required double size,
    FontWeight weight = FontWeight.w400,
    Color color = AppColors.textSecondary,
    double? height,
    double? letterSpacing,
  }) =>
      GoogleFonts.inter(
        fontSize: size,
        fontWeight: weight,
        color: color,
        height: height,
        letterSpacing: letterSpacing,
      );

  static TextStyle _heading({
    required double size,
    FontWeight weight = FontWeight.w600,
    Color color = AppColors.gray900,
  }) =>
      GoogleFonts.spaceGrotesk(
        fontSize: size,
        fontWeight: weight,
        color: color,
        letterSpacing: -0.2,
        height: 1.2,
      );

  // Corpo
  static TextStyle xs({Color? color, FontWeight? weight}) =>
      _base(size: 11, color: color ?? AppColors.textSecondary, weight: weight ?? FontWeight.w400);
  static TextStyle sm({Color? color, FontWeight? weight}) =>
      _base(size: 12, color: color ?? AppColors.textSecondary, weight: weight ?? FontWeight.w400);
  static TextStyle base({Color? color, FontWeight? weight}) =>
      _base(size: 13, color: color ?? AppColors.textPrimary, weight: weight ?? FontWeight.w400, height: 1.55);
  static TextStyle lg({Color? color, FontWeight? weight}) =>
      _base(size: 15, color: color ?? AppColors.textPrimary, weight: weight ?? FontWeight.w500);
  static TextStyle xl({Color? color, FontWeight? weight}) =>
      _base(size: 17, color: color ?? AppColors.textPrimary, weight: weight ?? FontWeight.w600);

  // Títulos (Space Grotesk)
  static TextStyle h1({Color? color}) => _heading(size: 30, color: color ?? AppColors.gray900);
  static TextStyle h2({Color? color}) => _heading(size: 24, color: color ?? AppColors.gray900);
  static TextStyle h3({Color? color}) => _heading(size: 20, color: color ?? AppColors.gray900);
  static TextStyle h4({Color? color}) => _heading(size: 17, color: color ?? AppColors.gray900);
  static TextStyle h5({Color? color}) => _heading(size: 15, color: color ?? AppColors.gray900);

  // Rótulos / preços / mono
  static TextStyle mono({double size = 13, Color? color, FontWeight? weight}) => GoogleFonts.jetBrainsMono(
        fontSize: size,
        fontWeight: weight ?? FontWeight.w600,
        color: color ?? AppColors.textPrimary,
      );
}

/// ThemeData central do app — aplica a paleta e a tipografia globalmente,
/// para que cada tela herde o mesmo visual do web sem repetir estilo.
class AppTheme {
  AppTheme._();

  static ThemeData get light {
    final base = ThemeData.light(useMaterial3: true);

    return base.copyWith(
      scaffoldBackgroundColor: AppColors.bgPrimary,
      primaryColor: AppColors.primary,
      colorScheme: base.colorScheme.copyWith(
        primary: AppColors.primary,
        secondary: AppColors.steel700,
        error: AppColors.error,
        surface: AppColors.white,
      ),
      primaryTextTheme: base.primaryTextTheme.apply(
        fontFamily: GoogleFonts.inter().fontFamily,
      ),
      textTheme: base.textTheme.apply(
        bodyColor: AppColors.textPrimary,
        displayColor: AppColors.gray900,
        fontFamily: GoogleFonts.inter().fontFamily,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        centerTitle: false,
        surfaceTintColor: Colors.transparent,
        titleTextStyle: AppText.h4(color: AppColors.textPrimary),
        iconTheme: const IconThemeData(color: AppColors.textPrimary),
      ),
      cardTheme: CardThemeData(
        color: AppColors.white,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.xl),
        ),
      ),
      dividerTheme: const DividerThemeData(color: AppColors.gray200, thickness: 1, space: 1),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: AppColors.white,
          disabledBackgroundColor: AppColors.primary.withOpacity(0.5),
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.s6, vertical: AppSpacing.s3),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.lg)),
          textStyle: AppText.base(color: AppColors.white, weight: FontWeight.w600),
          elevation: 0,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.primary,
          side: const BorderSide(color: AppColors.primary),
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.s6, vertical: AppSpacing.s3),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.lg)),
          textStyle: AppText.base(color: AppColors.primary, weight: FontWeight.w600),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.primary,
          textStyle: AppText.base(color: AppColors.primary, weight: FontWeight.w600),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.white,
        contentPadding: const EdgeInsets.symmetric(horizontal: AppSpacing.s4, vertical: AppSpacing.s3),
        hintStyle: AppText.base(color: AppColors.gray400),
        labelStyle: AppText.sm(color: AppColors.gray700, weight: FontWeight.w500),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          borderSide: const BorderSide(color: AppColors.gray300),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          borderSide: const BorderSide(color: AppColors.gray300),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          borderSide: const BorderSide(color: AppColors.error),
        ),
      ),
      chipTheme: base.chipTheme.copyWith(
        backgroundColor: AppColors.gray100,
        labelStyle: AppText.sm(color: AppColors.textPrimary),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.full)),
        side: BorderSide.none,
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: AppColors.white,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: AppColors.gray500,
        showUnselectedLabels: true,
        type: BottomNavigationBarType.fixed,
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(color: AppColors.primary),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppColors.steel900,
        contentTextStyle: AppText.base(color: AppColors.white),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.lg)),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: AppColors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.xl2)),
        titleTextStyle: AppText.h4(color: AppColors.gray900),
        contentTextStyle: AppText.base(color: AppColors.textSecondary),
      ),
    );
  }
}
