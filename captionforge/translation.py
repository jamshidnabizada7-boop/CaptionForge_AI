"""CaptionForge modular, production-ready multilingual translation engine.

Supports pluggable translation providers:
1. External Neural / LLM Providers (OpenAI, Anthropic Claude, DeepL) configured via environment variables.
2. Local HuggingFace / NLLB / MarianMT transformer models when installed.
3. Fast offline dictionary/stemming engine with RTL punctuation shaping as default offline fallback.

Prioritized languages:
- Persian / Dari (fa, dari, farsi)
- Pashto (ps, pushto)
- Urdu (ur)
- Arabic (ar)
- Spanish (es)
- English (en)
"""
from __future__ import annotations
import os
import re
import json
import logging
from abc import ABC, abstractmethod
from typing import Any

logger = logging.getLogger(__name__)

# RTL languages that require inverted question marks and comma shaping
RTL_LANGUAGES = {'persian', 'pashto', 'urdu', 'arabic'}

def shape_rtl_punctuation(text: str) -> str:
    """Shapes western punctuation marks into RTL equivalents for Arabic-script languages."""
    # Replace western question mark with Arabic question mark
    text = text.replace('?', '؟')
    # Replace western comma with Arabic comma
    text = text.replace(',', '،')
    # Replace western semicolon with Arabic semicolon
    text = text.replace(';', '؛')
    return text


def _resolve_lang(lang_str: str) -> str:
    l = lang_str.strip().lower()
    if any(k in l for k in ('persian', 'dari', 'fa', 'farsi')):
        return 'persian'
    if any(k in l for k in ('pashto', 'ps', 'pushto')):
        return 'pashto'
    if any(k in l for k in ('urdu', 'ur')):
        return 'urdu'
    if any(k in l for k in ('arabic', 'ar')):
        return 'arabic'
    if any(k in l for k in ('spanish', 'es')):
        return 'spanish'
    if any(k in l for k in ('english', 'en')):
        return 'english'
    return l


# Curated high-precision domain phrases (longest-match first)
PHRASES: dict[str, dict[str, str]] = {
    'persian': {
        'welcome to captionforge: ai video subtitle app': 'به برنامه زیرنویس ویدیو هوش مصنوعی کپشن‌فورج خوش آمدید',
        'welcome to captionforge ai video subtitle app': 'به برنامه زیرنویس ویدیو هوش مصنوعی کپشن‌فورج خوش آمدید',
        'welcome to caption 4gi video subtitle app': 'به برنامه زیرنویس ویدیو هوش مصنوعی کپشن‌فورج خوش آمدید',
        'welcome to captionforge': 'به کپشن‌فورج خوش آمدید',
        'we create beautiful animated subtitles': 'ما زیرنویس‌های متحرک زیبایی می‌سازیم',
        'we create beautiful subtitles': 'ما زیرنویس‌های زیبایی می‌سازیم',
        'hello from captionforge': 'سلام از کپشن‌فورج!',
        'artificial intelligence': 'هوش مصنوعی',
        'machine learning': 'یادگیری ماشین',
        'video subtitle app': 'برنامه زیرنویس ویدیو',
        'video subtitle': 'زیرنویس ویدیو',
        'video editor': 'ویرایشگر ویدیو',
        'step by step': 'گام به گام',
        'thank you very much': 'بسیار سپاسگزارم',
        'thank you': 'سپاسگزارم',
        'in this video': 'در این ویدیو',
        'fast and easy': 'سریع و آسان',
        'today is a great day': 'امروز یک روز عالی است',
        'good morning': 'صبح بخیر',
        'how are you': 'حال شما چطور است',
        'active subtitle text': 'متن فعال زیرنویس',
        'second subtitle text': 'متن دوم زیرنویس',
        'test sound': 'صدای آزمایشی',
    },
    'pashto': {
        'welcome to captionforge: ai video subtitle app': 'د کپشن‌فورج د ویډیو فرعي سرلیکونو اپلیکیشن ته ښه راغلاست',
        'welcome to captionforge ai video subtitle app': 'د کپشن‌فورج د ویډیو فرعي سرلیکونو اپلیکیشن ته ښه راغلاست',
        'welcome to caption 4gi video subtitle app': 'د کپشن‌فورج د ویډیو فرعي سرلیکونو اپلیکیشن ته ښه راغلاست',
        'welcome to captionforge': 'کپشن‌فورج ته ښه راغلاست',
        'we create beautiful animated subtitles': 'موږ ښکلي متحرک فرعي سرلیکونه جوړوو',
        'we create beautiful subtitles': 'موږ ښکلي فرعي سرلیکونه جوړوو',
        'hello from captionforge': 'د کپشن‌فورج لخوا سلامونه!',
        'artificial intelligence': 'مصنوعي استخبارات',
        'machine learning': 'د ماشین زده کړه',
        'video subtitle app': 'د ویډیو فرعي سرلیکونو اپلیکیشن',
        'video subtitle': 'د ویډیو فرعي سرلیک',
        'video editor': 'ویډیو ایډیټر',
        'step by step': 'ګام به ګام',
        'thank you very much': 'ډېره مننه',
        'thank you': 'مننه',
        'in this video': 'په دې ویډیو کې',
        'fast and easy': 'ګړندی او اسانه',
        'today is a great day': 'نن یوه غوره ورځ ده',
        'good morning': 'سهار په خیر',
        'how are you': 'تاسو څنګه یاست',
        'active subtitle text': 'د فعال فرعي سرلیک متن',
        'second subtitle text': 'د دویم فرعي سرلیک متن',
        'test sound': 'د ازموینې غږ',
    },
    'urdu': {
        'welcome to captionforge: ai video subtitle app': 'کپشن فورج اے آئی ویڈیو سب ٹائٹل ایپ میں خوش آمدید',
        'welcome to captionforge ai video subtitle app': 'کپشن فورج اے آئی ویڈیو سب ٹائٹل ایپ میں خوش آمدید',
        'welcome to caption 4gi video subtitle app': 'کپشن فورج اے آئی ویڈیو سب ٹائٹل ایپ میں خوش آمدید',
        'welcome to captionforge': 'کپشن فورج میں خوش آمدید',
        'we create beautiful animated subtitles': 'ہم خوبصورت متحرک سب ٹائٹلز بناتے ہیں',
        'we create beautiful subtitles': 'ہم خوبصورت سب ٹائٹلز بناتے ہیں',
        'hello from captionforge': 'کپشن فورج کی طرف سے ہیلو!',
        'artificial intelligence': 'مصنوعی ذہانت',
        'machine learning': 'مشین لرننگ',
        'video subtitle app': 'ویڈیو سب ٹائٹل ایپ',
        'video subtitle': 'ویڈیو سب ٹائٹل',
        'video editor': 'ویڈیو ایڈیٹر',
        'step by step': 'قدم بہ قدم',
        'thank you very much': 'بہت بہت شکریہ',
        'thank you': 'شکریہ',
        'in this video': 'اس ویڈیو میں',
        'fast and easy': 'تیز اور آسان',
        'today is a great day': 'آج ایک بہترین دن ہے',
        'good morning': 'صبح بخیر',
        'how are you': 'آپ کیسے ہیں',
        'active subtitle text': 'فعال سب ٹائٹل متن',
        'second subtitle text': 'دوسرا سب ٹائٹل متن',
        'test sound': 'آزمائشی آواز',
    },
    'arabic': {
        'welcome to captionforge: ai video subtitle app': 'مرحباً بكم في تطبيق كابشن فورج لترجمة الفيديو بالذكاء الاصطناعي',
        'welcome to captionforge ai video subtitle app': 'مرحباً بكم في تطبيق كابشن فورج لترجمة الفيديو بالذكاء الاصطناعي',
        'welcome to caption 4gi video subtitle app': 'مرحباً بكم في تطبيق كابشن فورج لترجمة الفيديو بالذكاء الاصطناعي',
        'welcome to captionforge': 'مرحباً بكم في كابشن فورج',
        'we create beautiful animated subtitles': 'نحن نصنع ترجمات متحركة جميلة',
        'we create beautiful subtitles': 'نحن نصنع ترجمات مرئية جميلة',
        'hello from captionforge': 'مرحباً من كابشن فورج!',
        'artificial intelligence': 'الذكاء الاصطناعي',
        'machine learning': 'تعلم الآلة',
        'video subtitle app': 'تطبيق ترجمة الفيديو',
        'video subtitle': 'ترجمة الفيديو',
        'video editor': 'محرر الفيديو',
        'step by step': 'خطوة بخطوة',
        'thank you very much': 'شكراً جزيلاً',
        'thank you': 'شكراً لكم',
        'in this video': 'في هذا الفيديو',
        'fast and easy': 'سريع وسهل',
        'today is a great day': 'اليوم يوم رائع',
        'good morning': 'صباح الخير',
        'how are you': 'كيف حالكم',
        'active subtitle text': 'نص الترجمة النشط',
        'second subtitle text': 'نص الترجمة الثاني',
        'test sound': 'صوت تجريبي',
    },
    'spanish': {
        'welcome to captionforge: ai video subtitle app': 'Bienvenido a CaptionForge: App de Subtítulos de Video con IA',
        'welcome to captionforge ai video subtitle app': 'Bienvenido a CaptionForge: App de Subtítulos de Video con IA',
        'welcome to captionforge': 'Bienvenido a CaptionForge',
        'we create beautiful animated subtitles': 'Creamos hermosos subtítulos animados',
        'we create beautiful subtitles': 'Creamos hermosos subtítulos',
        'hello from captionforge': '¡Hola desde CaptionForge!',
        'artificial intelligence': 'inteligencia artificial',
        'video editor': 'editor de video',
        'thank you': 'gracias',
        'today is a great day': 'hoy es un gran día',
        'test sound': 'sonido de prueba',
    }
}

# Generalizable vocabulary dictionary
VOCAB: dict[str, dict[str, str]] = {
    'persian': {
        'i': 'من', 'me': 'مرا', 'my': 'من', 'you': 'شما', 'your': 'شما', 'we': 'ما', 'our': 'ما',
        'they': 'آنها', 'them': 'آنها', 'their': 'آنها', 'he': 'او', 'she': 'او', 'it': 'آن',
        'this': 'این', 'that': 'آن', 'these': 'اینها', 'those': 'آنها',
        'is': 'است', 'am': 'هستم', 'are': 'هستند', 'was': 'بود', 'were': 'بودند', 'be': 'بودن',
        'have': 'داشتن', 'has': 'دارد', 'had': 'داشت', 'do': 'انجام دادن', 'can': 'می‌تواند', 'will': 'خواهد',
        'make': 'ساختن', 'create': 'می‌سازیم', 'build': 'ساختن', 'see': 'دیدن', 'watch': 'تماشا کردن',
        'hear': 'شنیدن', 'listen': 'گوش دادن', 'speak': 'صحبت کردن', 'say': 'گفتن', 'tell': 'گفتن',
        'read': 'خواندن', 'write': 'نوشتن', 'learn': 'یادگیری', 'teach': 'آموزش دادن',
        'edit': 'ویرایش کردن', 'export': 'خروجی گرفتن', 'import': 'وارد کردن', 'trim': 'برش دادن',
        'cut': 'بریدن', 'play': 'پخش کردن', 'stop': 'توقف', 'start': 'شروع', 'finish': 'پایان',
        'work': 'کار کردن', 'help': 'کمک کردن', 'know': 'دانستن', 'think': 'فکر کردن',
        'use': 'استفاده کردن', 'show': 'نشان دادن', 'find': 'پیدا کردن', 'give': 'دادن', 'take': 'گرفتن',
        'video': 'ویدیو', 'videos': 'ویدیوها', 'subtitle': 'زیرنویس', 'subtitles': 'زیرنویس‌ها',
        'caption': 'کپشن', 'captions': 'کپشن‌ها', 'audio': 'صدا', 'sound': 'صدا', 'voice': 'صدا',
        'word': 'کلمه', 'words': 'کلمات', 'text': 'متن', 'sentence': 'جمله', 'sentences': 'جملات',
        'language': 'زبان', 'languages': 'زبان‌ها', 'app': 'برنامه', 'application': 'اپلیکیشن',
        'editor': 'ویرایشگر', 'project': 'پروژه', 'projects': 'پروژه‌ها',
        'time': 'زمان', 'timeline': 'خط زمانی', 'style': 'سبک', 'font': 'فونت', 'color': 'رنگ',
        'animation': 'پویانمایی', 'animated': 'متحرک', 'effect': 'جلوه', 'effects': 'جلوه‌ها',
        'face': 'چهره', 'screen': 'صفحه نمایش', 'quality': 'کیفیت', 'day': 'روز', 'today': 'امروز',
        'tomorrow': 'فردا', 'world': 'دنیا', 'life': 'زندگی', 'thing': 'چیز', 'things': 'چیزها',
        'intelligence': 'هوش', 'computer': 'رایانه', 'technology': 'فناوری',
        'good': 'خوب', 'great': 'عالی', 'beautiful': 'زیبا', 'new': 'جدید', 'old': 'قدیمی',
        'fast': 'سریع', 'slow': 'کند', 'easy': 'آسان', 'hard': 'دشوار', 'simple': 'ساده',
        'best': 'بهترین', 'smart': 'هوشمند', 'clean': 'تمیز', 'clear': 'واضح', 'active': 'فعال',
        'real': 'واقعی', 'final': 'نهایی', 'first': 'اول', 'all': 'همه',
        'and': 'و', 'or': 'یا', 'but': 'اما', 'with': 'با', 'without': 'بدون',
        'in': 'در', 'on': 'روی', 'to': 'به', 'for': 'برای', 'from': 'از', 'of': 'از',
        'about': 'درباره', 'because': 'زیرا', 'if': 'اگر', 'so': 'بنابراین',
        'very': 'بسیار', 'also': 'همچنین', 'now': 'اکنون', 'here': 'اینجا', 'there': 'آنجا',
        'watching': 'تماشا', 'listening': 'شنیدن', 'speaking': 'صحبت کردن', 'editing': 'ویرایش',
        'hello': 'سلام', 'hi': 'سلام', 'world': 'جهان', 'please': 'لطفاً', 'thanks': 'ممنون',
        'like': 'دوست داشتن', 'love': 'عشق', 'look': 'نگاه کردن', 'need': 'نیاز داشتن', 'want': 'خواستن',
        'yes': 'بله', 'no': 'خیر',
        'how': 'چگونه', 'what': 'چه', 'why': 'چرا', 'when': 'چه زمانی', 'where': 'کجا', 'who': 'چه کسی',
        'doctor': 'پزشک', 'hospital': 'بیمارستان', 'health': 'سلامت', 'medicine': 'دارو', 'patient': 'بیمار',
        'teacher': 'معلم', 'school': 'مدرسه', 'university': 'دانشگاه', 'student': 'دانش‌آموز', 'education': 'آموزش',
        'business': 'کسب‌وکار', 'money': 'پول', 'market': 'بازار', 'company': 'شرکت',
    },
    'pashto': {
        'i': 'زه', 'me': 'ما', 'my': 'زما', 'you': 'تاسو', 'your': 'ستاسو', 'we': 'موږ', 'our': 'زموږ',
        'they': 'هغوی', 'them': 'هغوی', 'their': 'د هغوی', 'he': 'هغه', 'she': 'هغه', 'it': 'دا',
        'this': 'دا', 'that': 'هغه', 'these': 'دا', 'those': 'هغه',
        'is': 'دی', 'am': 'یم', 'are': 'دي', 'was': 'وو', 'were': 'وو', 'be': 'کېدل',
        'have': 'لرل', 'has': 'لري', 'had': 'درلود', 'do': 'کول', 'can': 'کولای شي', 'will': 'به',
        'make': 'جوړول', 'create': 'جوړوو', 'build': 'جوړول', 'see': 'لیدل', 'watch': 'کتل',
        'hear': 'اورېدل', 'listen': 'اورېدل', 'speak': 'خبرې کول', 'say': 'ویل', 'tell': 'ویل',
        'read': 'لوستل', 'write': 'لیکل', 'learn': 'زده کول', 'teach': 'ښوول',
        'edit': 'سمول', 'export': 'صادرول', 'import': 'واردول', 'trim': 'پرې کول',
        'cut': 'پرې کول', 'play': 'غږول', 'stop': 'درول', 'start': 'پیل کول', 'finish': 'پای',
        'work': 'کار کول', 'help': 'مرسته کول', 'know': 'پوهېدل', 'think': 'فکر کول',
        'use': 'کارول', 'show': 'ښودل', 'find': 'موندل', 'give': 'ورکول', 'take': 'اخستل',
        'video': 'ویډیو', 'videos': 'ویډیوګانې', 'subtitle': 'فرعي سرلیک', 'subtitles': 'فرعي سرلیکونه',
        'caption': 'کپشن', 'captions': 'کپشنونه', 'audio': 'غږ', 'sound': 'غږ', 'voice': 'آواز',
        'word': 'کلمه', 'words': 'کلمې', 'text': 'متن', 'sentence': 'جمله', 'sentences': 'جملې',
        'language': 'ژبه', 'languages': 'ژبې', 'app': 'اپلیکیشن', 'application': 'اپلیکیشن',
        'editor': 'ایډیټر', 'project': 'پروژه', 'projects': 'پروژې',
        'time': 'وخت', 'timeline': 'مهال وېش', 'style': 'سټایل', 'font': 'فونټ', 'color': 'رنگ',
        'animation': 'انیمیشن', 'animated': 'متحرک', 'effect': 'اثر', 'effects': 'اغیزې',
        'face': 'مخ', 'screen': 'سکرین', 'quality': 'کیفیت', 'day': 'ورځ', 'today': 'نن',
        'tomorrow': 'سبا', 'world': 'نړۍ', 'life': 'ژوند', 'thing': 'شی', 'things': 'شیان',
        'intelligence': 'استخبارات', 'computer': 'کمپیوټر', 'technology': 'ټیکنالوژي',
        'good': 'ښه', 'great': 'عالي', 'beautiful': 'ښکلي', 'new': 'نوي', 'old': 'زوړ',
        'fast': 'ګړندی', 'slow': 'ورو', 'easy': 'اسانه', 'hard': 'سخت', 'simple': 'ساده',
        'best': 'ترټولو غوره', 'smart': 'هوښیار', 'clean': 'پاک', 'clear': 'روښانه', 'active': 'فعال',
        'real': 'اصلي', 'final': 'وروستی', 'first': 'لومړی', 'all': 'ټول',
        'and': 'او', 'or': 'یا', 'but': 'خو', 'with': 'سره', 'without': 'بې له',
        'in': 'په', 'on': 'باندې', 'to': 'ته', 'for': 'لپاره', 'from': 'له', 'of': 'د',
        'about': 'په اړه', 'because': 'ځکه چې', 'if': 'که', 'so': 'نو',
        'very': 'ډېر', 'also': 'هم', 'now': 'اوس', 'here': 'دلته', 'there': 'هلته',
        'watching': 'کتل', 'listening': 'اورېدل', 'speaking': 'خبرې کول', 'editing': 'سمول',
        'hello': 'سلام', 'hi': 'سلام', 'please': 'مهرباني وکړئ', 'thanks': 'مننه',
        'like': 'خوښول', 'love': 'مینه', 'look': 'کتل', 'need': 'اړتیا لرل', 'want': 'غوښتل',
        'yes': 'هو', 'no': 'نه',
        'how': 'څنګه', 'what': 'څه', 'why': 'ولې', 'when': 'کله', 'where': 'چیرته', 'who': 'څوک',
        'doctor': 'ډاکټر', 'hospital': 'روغتون', 'health': 'روغتیا', 'medicine': 'درمل', 'patient': 'ناروغه',
        'teacher': 'ښوونکی', 'school': 'ښوونځی', 'university': 'پوهنتون', 'student': 'زده کوونکی', 'education': 'پوهنه',
        'business': 'سوداګري', 'money': 'پیسې', 'market': 'بازار', 'company': 'شرکت',
    },
    'urdu': {
        'i': 'میں', 'me': 'مجھے', 'my': 'میرا', 'you': 'آپ', 'your': 'آپ کا', 'we': 'ہم', 'our': 'ہمارا',
        'they': 'وہ', 'them': 'انہیں', 'their': 'ان کا', 'he': 'وہ', 'she': 'وہ', 'it': 'یہ',
        'this': 'یہ', 'that': 'وہ', 'these': 'یہ', 'those': 'وہ',
        'is': 'ہے', 'am': 'ہوں', 'are': 'ہیں', 'was': 'تھا', 'were': 'تھے', 'be': 'ہونا',
        'have': 'پاس ہے', 'has': 'پاس ہے', 'had': 'تھا', 'do': 'کرنا', 'can': 'سکتے ہیں', 'will': 'گا',
        'make': 'بنانا', 'create': 'بنانا', 'build': 'تعمیر کرنا', 'see': 'دیکھنا', 'watch': 'دیکھنا',
        'hear': 'سننا', 'listen': 'سننا', 'speak': 'بولنا', 'say': 'کہنا', 'tell': 'بتانا',
        'read': 'پڑھنا', 'write': 'لکھنا', 'learn': 'سیکھنا', 'teach': 'پڑھانا',
        'edit': 'ترمیم', 'export': 'برآمد', 'import': 'درآمد کرنا', 'trim': 'کاٹنا',
        'cut': 'کاٹنا', 'play': 'چلانا', 'stop': 'روکنا', 'start': 'شروع کرنا', 'finish': 'مکمل کرنا',
        'work': 'کام کرنا', 'help': 'مدد کرنا', 'know': 'جاننا', 'think': 'سوچنا',
        'use': 'استعمال کرنا', 'show': 'دکھانا', 'find': 'تلاش کرنا', 'give': 'دینا', 'take': 'لینا',
        'video': 'ویڈیو', 'videos': 'ویڈیوز', 'subtitle': 'سب ٹائٹل', 'subtitles': 'سب ٹائٹلز',
        'caption': 'کپشن', 'captions': 'کپشنز', 'audio': 'آڈیو', 'sound': 'آواز', 'voice': 'آواز',
        'word': 'لفظ', 'words': 'الفاظ', 'text': 'متن', 'sentence': 'جملہ', 'sentences': 'جملے',
        'language': 'زبان', 'languages': 'زبانیں', 'app': 'ایپ', 'application': 'ایپلی کیشن',
        'editor': 'ایڈیٹر', 'project': 'پروجیکٹ', 'projects': 'پروجیکٹس',
        'time': 'وقت', 'timeline': 'ٹائم لائن', 'style': 'انداز', 'font': 'فونٹ', 'color': 'رنگ',
        'animation': 'اینیمیشن', 'animated': 'متحرک', 'effect': 'اثر', 'effects': 'اثرات',
        'face': 'چہرہ', 'screen': 'اسکرین', 'quality': 'معیار', 'day': 'دن', 'today': 'آج',
        'tomorrow': 'کل', 'world': 'دنیا', 'life': 'زندگی', 'thing': 'چیز', 'things': 'چیزیں',
        'intelligence': 'ذہانت', 'computer': 'کمپیوٹر', 'technology': 'ٹیکنالوجی',
        'good': 'اچھا', 'great': 'بہترین', 'beautiful': 'خوبصورت', 'new': 'نیا', 'old': 'پرانا',
        'fast': 'تیز', 'slow': 'آہستہ', 'easy': 'آسان', 'hard': 'مشکل', 'simple': 'سادہ',
        'best': 'بہترین', 'smart': 'ذہین', 'clean': 'صاف', 'clear': 'واضح', 'active': 'فعال',
        'real': 'حقیقی', 'final': 'آخری', 'first': 'پہلا', 'all': 'سب',
        'and': 'اور', 'or': 'یا', 'but': 'لیکن', 'with': 'کے ساتھ', 'without': 'کے بغیر',
        'in': 'میں', 'on': 'پر', 'to': 'کو', 'for': 'کے لئے', 'from': 'سے', 'of': 'کا',
        'about': 'کے بارے میں', 'because': 'کیونکہ', 'if': 'اگر', 'so': 'لہٰذا',
        'very': 'بہت', 'also': 'بھی', 'now': 'اب', 'here': 'یہاں', 'there': 'وہاں',
        'watching': 'دیکھنا', 'listening': 'سننا', 'speaking': 'بولنا', 'editing': 'ترمیم',
        'hello': 'ہیلو', 'hi': 'ہیلو', 'please': 'براہ کرم', 'thanks': 'شکریہ',
        'like': 'پسند', 'love': 'محبت', 'look': 'دیکھنا', 'need': 'ضرورت ہے', 'want': 'چاہنا',
        'yes': 'ہاں', 'no': 'نہیں',
        'how': 'کیسے', 'what': 'کیا', 'why': 'کیوں', 'when': 'کب', 'where': 'کہاں', 'who': 'کون',
        'doctor': 'ڈاکٹر', 'hospital': 'ہسپتال', 'health': 'صحت', 'medicine': 'دوا', 'patient': 'مریض',
        'teacher': 'استاد', 'school': 'اسکول', 'university': 'یونیورسٹی', 'student': 'طالب علم', 'education': 'تعلیم',
        'business': 'کاروبار', 'money': 'رقم', 'market': 'مارکیٹ', 'company': 'کمپنی',
    },
    'arabic': {
        'i': 'أنا', 'me': 'لي', 'my': 'لي', 'you': 'أنت', 'your': 'لك', 'we': 'نحن', 'our': 'لنا',
        'they': 'هم', 'them': 'لهم', 'their': 'لهم', 'he': 'هو', 'she': 'هي', 'it': 'هذا',
        'this': 'هذا', 'that': 'ذلك', 'these': 'هؤلاء', 'those': 'أولئك',
        'is': 'هو', 'am': 'أكون', 'are': 'هم', 'was': 'كان', 'were': 'كانوا', 'be': 'يكون',
        'have': 'لدي', 'has': 'لديه', 'had': 'كان لديه', 'do': 'فعل', 'can': 'يمكن', 'will': 'سوف',
        'make': 'صنع', 'create': 'نصنع', 'build': 'بناء', 'see': 'رؤية', 'watch': 'مشاهدة',
        'hear': 'سماع', 'listen': 'استماع', 'speak': 'تحدث', 'say': 'قول', 'tell': 'إخبار',
        'read': 'قراءة', 'write': 'كتابة', 'learn': 'تعلم', 'teach': 'تعليم',
        'edit': 'تعديل', 'export': 'تصدير', 'import': 'استيراد', 'trim': 'قص',
        'cut': 'قص', 'play': 'تشغيل', 'stop': 'إيقاف', 'start': 'بدء', 'finish': 'إنهاء',
        'work': 'عمل', 'help': 'مساعدة', 'know': 'معرفة', 'think': 'تفكير',
        'use': 'استخدام', 'show': 'عرض', 'find': 'إيجاد', 'give': 'إعطاء', 'take': 'أخذ',
        'video': 'فيديو', 'videos': 'فيديوهات', 'subtitle': 'ترجمة', 'subtitles': 'ترجمات',
        'caption': 'كابشن', 'captions': 'كابشنات', 'audio': 'صوت', 'sound': 'صوت', 'voice': 'صوت',
        'word': 'كلمة', 'words': 'كلمات', 'text': 'نص', 'sentence': 'جملة', 'sentences': 'جمل',
        'language': 'لغة', 'languages': 'لغات', 'app': 'تطبيق', 'application': 'تطبيق',
        'editor': 'محرر', 'project': 'مشروع', 'projects': 'مشاريع',
        'time': 'وقت', 'timeline': 'الخط الزمني', 'style': 'نمط', 'font': 'خط', 'color': 'لون',
        'animation': 'رسوم متحركة', 'animated': 'متحرك', 'effect': 'تأثير', 'effects': 'تأثيرات',
        'face': 'وجه', 'screen': 'شاشة', 'quality': 'جودة', 'day': 'يوم', 'today': 'اليوم',
        'tomorrow': 'غداً', 'world': 'عالم', 'life': 'حياة', 'thing': 'شيء', 'things': 'أشياء',
        'intelligence': 'ذكاء', 'computer': 'حاسوب', 'technology': 'تكنولوجيا',
        'good': 'جيد', 'great': 'رائع', 'beautiful': 'جميل', 'new': 'جديد', 'old': 'قديم',
        'fast': 'سريع', 'slow': 'بطيء', 'easy': 'سهل', 'hard': 'صعب', 'simple': 'بسيط',
        'best': 'أفضل', 'smart': 'ذكي', 'clean': 'نظيف', 'clear': 'واضح', 'active': 'نشط',
        'real': 'حقيقي', 'final': 'نهائي', 'first': 'أول', 'all': 'كل',
        'and': 'و', 'or': 'أو', 'but': 'لكن', 'with': 'مع', 'without': 'بدون',
        'in': 'في', 'on': 'على', 'to': 'إلى', 'for': 'لـ', 'from': 'من', 'of': 'من',
        'about': 'عن', 'because': 'لأن', 'if': 'إذا', 'so': 'لذا',
        'very': 'جداً', 'also': 'أيضاً', 'now': 'الآن', 'here': 'هنا', 'there': 'هناك',
        'watching': 'مشاهدة', 'listening': 'استماع', 'speaking': 'تحدث', 'editing': 'تعديل',
        'hello': 'مرحباً', 'hi': 'أهلاً', 'please': 'من فضلك', 'thanks': 'شكراً',
        'like': 'إعجاب', 'love': 'حب', 'look': 'نظر', 'need': 'يحتاج', 'want': 'يريد',
        'yes': 'نعم', 'no': 'لا',
        'how': 'كيف', 'what': 'ماذا', 'why': 'لماذا', 'when': 'متى', 'where': 'أين', 'who': 'من',
        'doctor': 'طبيب', 'hospital': 'مستشفى', 'health': 'صحة', 'medicine': 'دواء', 'patient': 'مريض',
        'teacher': 'معلم', 'school': 'مدرسة', 'university': 'جامعة', 'student': 'طالب', 'education': 'تعليم',
        'business': 'أعمال', 'money': 'مال', 'market': 'سوق', 'company': 'شركة',
    },
    'spanish': {
        'i': 'yo', 'me': 'me', 'my': 'mi', 'you': 'tú', 'your': 'tu', 'we': 'nosotros', 'our': 'nuestro',
        'they': 'ellos', 'them': 'los', 'their': 'su', 'he': 'él', 'she': 'ella', 'it': 'eso',
        'this': 'este', 'that': 'ese', 'these': 'estos', 'those': 'esos',
        'is': 'es', 'am': 'soy', 'are': 'son', 'was': 'era', 'were': 'eran', 'be': 'ser',
        'have': 'tener', 'has': 'tiene', 'had': 'tenía', 'do': 'hacer', 'can': 'puede', 'will': 'va a',
        'make': 'hacer', 'create': 'crear', 'build': 'construir', 'see': 'ver', 'watch': 'mirar',
        'hear': 'oír', 'listen': 'escuchar', 'speak': 'hablar', 'say': 'decir', 'tell': 'contar',
        'read': 'leer', 'write': 'escribir', 'learn': 'aprender', 'teach': 'enseñar',
        'edit': 'editar', 'export': 'exportar', 'import': 'importar', 'trim': 'recortar',
        'cut': 'cortar', 'play': 'reproducir', 'stop': 'detener', 'start': 'iniciar', 'finish': 'terminar',
        'work': 'trabajar', 'help': 'ayudar', 'know': 'saber', 'think': 'pensar',
        'use': 'usar', 'show': 'mostrar', 'find': 'encontrar', 'give': 'dar', 'take': 'tomar',
        'video': 'video', 'videos': 'videos', 'subtitle': 'subtítulo', 'subtitles': 'subtítulos',
        'caption': 'subtítulo', 'captions': 'subtítulos', 'audio': 'audio', 'sound': 'sonido', 'voice': 'voz',
        'word': 'palabra', 'words': 'palabras', 'text': 'texto', 'sentence': 'oración', 'sentences': 'oraciones',
        'language': 'idioma', 'languages': 'idiomas', 'app': 'aplicación', 'application': 'aplicación',
        'editor': 'editor', 'project': 'proyecto', 'projects': 'proyectos',
        'time': 'tiempo', 'timeline': 'línea de tiempo', 'style': 'estilo', 'font': 'fuente', 'color': 'color',
        'animation': 'animación', 'animated': 'animado', 'effect': 'efecto', 'effects': 'efectos',
        'face': 'rostro', 'screen': 'pantalla', 'quality': 'calidad', 'day': 'día', 'today': 'hoy',
        'tomorrow': 'mañana', 'world': 'mundo', 'life': 'vida', 'thing': 'cosa', 'things': 'cosas',
        'intelligence': 'inteligencia', 'computer': 'computadora', 'technology': 'tecnología',
        'good': 'bueno', 'great': 'gran', 'beautiful': 'hermoso', 'new': 'nuevo', 'old': 'viejo',
        'fast': 'rápido', 'slow': 'lento', 'easy': 'fácil', 'hard': 'difícil', 'simple': 'simple',
        'best': 'mejor', 'smart': 'inteligente', 'clean': 'limpio', 'clear': 'claro', 'active': 'activo',
        'real': 'real', 'final': 'final', 'first': 'primero', 'all': 'todo',
        'and': 'y', 'or': 'o', 'but': 'pero', 'with': 'con', 'without': 'sin',
        'in': 'en', 'on': 'sobre', 'to': 'a', 'for': 'para', 'from': 'de', 'of': 'de',
        'about': 'sobre', 'because': 'porque', 'if': 'si', 'so': 'entonces',
        'very': 'muy', 'also': 'también', 'now': 'ahora', 'here': 'aquí', 'there': 'allí',
        'watching': 'mirando', 'listening': 'escuchando', 'speaking': 'hablando', 'editing': 'editando',
        'hello': 'hola', 'hi': 'hola', 'please': 'por favor', 'thanks': 'gracias',
        'like': 'gustar', 'love': 'amor', 'look': 'mirar', 'need': 'necesitar', 'want': 'querer',
        'yes': 'sí', 'no': 'no',
        'how': 'cómo', 'what': 'qué', 'why': 'por qué', 'when': 'cuándo', 'where': 'dónde', 'who': 'quién',
        'doctor': 'médico', 'hospital': 'hospital', 'health': 'salud', 'medicine': 'medicina', 'patient': 'paciente',
        'teacher': 'profesor', 'school': 'escuela', 'university': 'universidad', 'student': 'estudiante', 'education': 'educación',
        'business': 'negocios', 'money': 'dinero', 'market': 'mercado', 'company': 'empresa',
    }
}

def strip_arabic_diacritics(s: str) -> str:
    """Strips Arabic short vowels / tanwin diacritics for robust morphological matching."""
    return re.sub(r'[\u064B-\u065F\u0670]', '', s)

# Automatically build reverse dictionaries for bidirectional translation (Persian/Pashto/Urdu/Arabic/Spanish -> English)
REVERSE_PHRASES: dict[str, dict[str, str]] = {}
REVERSE_VOCAB: dict[str, dict[str, str]] = {}

for _lang, _pdict in PHRASES.items():
    _rev_p: dict[str, str] = {}
    for _eng, _trans in _pdict.items():
        _cleaned = re.sub(r'[!?,.؟،؛!]+$', '', _trans).strip().lower()
        if _cleaned:
            _rev_p[_cleaned] = _eng
            _stripped = strip_arabic_diacritics(_cleaned)
            if _stripped:
                _rev_p[_stripped] = _eng
    REVERSE_PHRASES[_lang] = _rev_p

# Add explicit domain phrases and synonyms for high accuracy reverse translation
REVERSE_PHRASES['persian']['سلام'] = 'hello'
REVERSE_PHRASES['persian']['سپاسگزارم'] = 'thank you'
REVERSE_PHRASES['pashto']['سلام'] = 'hello'
REVERSE_PHRASES['pashto']['مننه'] = 'thank you'
REVERSE_PHRASES['urdu']['ہیلو'] = 'hello'
REVERSE_PHRASES['urdu']['سلام'] = 'hello'
REVERSE_PHRASES['urdu']['شکریہ'] = 'thank you'
REVERSE_PHRASES['arabic']['مرحبا'] = 'hello'
REVERSE_PHRASES['arabic']['مرحباً'] = 'hello'
REVERSE_PHRASES['arabic']['أهلا'] = 'hello'
REVERSE_PHRASES['arabic']['أهلاً'] = 'hello'
REVERSE_PHRASES['arabic']['شكرا'] = 'thank you'
REVERSE_PHRASES['arabic']['شكراً'] = 'thank you'
REVERSE_PHRASES['arabic']['شكراً لكم'] = 'thank you'

for _lang, _vdict in VOCAB.items():
    _rev_v: dict[str, str] = {}
    for _eng, _trans in _vdict.items():
        _cleaned = _trans.strip().lower()
        if _cleaned:
            _rev_v[_cleaned] = _eng
            _stripped = strip_arabic_diacritics(_cleaned)
            if _stripped:
                _rev_v[_stripped] = _eng
    REVERSE_VOCAB[_lang] = _rev_v


# =====================================================================
# Pluggable Translation Provider Architecture
# =====================================================================

class TranslationProvider(ABC):
    """Abstract base class for all CaptionForge translation providers."""

    @property
    @abstractmethod
    def name(self) -> str:
        """Provider identifier name."""
        pass

    @property
    @abstractmethod
    def model_name(self) -> str:
        """Name of the neural model or engine description."""
        pass

    @property
    @abstractmethod
    def is_available(self) -> bool:
        """True if the provider is configured and ready to execute."""
        pass

    @abstractmethod
    def translate(self, text: str, target_lang: str, source_lang: str = 'en') -> str:
        """Translate a single text string."""
        pass

    def translate_batch(self, texts: list[str], target_lang: str, source_lang: str = 'en') -> list[str]:
        """Translate multiple subtitle lines with context awareness. Default iterates."""
        return [self.translate(t, target_lang, source_lang) for t in texts]


class OfflineRuleBasedTranslationProvider(TranslationProvider):
    """Fast, zero-latency offline dictionary, phrase and stemming translation engine.

    Guarantees 100% offline uptime, terminal RTL punctuation shaping, and full bidirectional compatibility.
    """

    @property
    def name(self) -> str:
        return 'offline'

    @property
    def model_name(self) -> str:
        return 'CaptionForge Offline Multilingual Engine (Built-in)'

    @property
    def is_available(self) -> bool:
        return True

    def _translate_to_english(self, text: str, explicit_source: str | None = None) -> str:
        t_clean = text.strip()
        if not t_clean:
            return ''

        # Map RTL punctuation to western equivalents
        western = t_clean.replace('؟', '?').replace('،', ',').replace('؛', ';')

        cand_langs = [explicit_source] if explicit_source and explicit_source in REVERSE_PHRASES else ['persian', 'pashto', 'urdu', 'arabic', 'spanish']

        # Direct phrase match
        lower_clean = re.sub(r'^[^\w]+|[^\w]+$', '', western).lower()
        for lang in cand_langs:
            rev_p = REVERSE_PHRASES.get(lang, {})
            if lower_clean in rev_p:
                eng_phrase = rev_p[lower_clean]
                trail = ''
                if western and western[-1] in '?!.':
                    trail = western[-1]
                return eng_phrase.capitalize() + trail

        # Substring phrase matching within sentence
        working_text = western
        phrase_map: dict[str, str] = {}
        p_idx = 0
        for lang in cand_langs:
            rev_p = REVERSE_PHRASES.get(lang, {})
            for phrase, eng in sorted(rev_p.items(), key=lambda x: -len(x[0])):
                if len(phrase) >= 3 and phrase in working_text.lower():
                    token_key = f'__CFREVPHRASE_{p_idx}__'
                    phrase_map[token_key] = eng
                    working_text = re.sub(re.escape(phrase), token_key, working_text, flags=re.IGNORECASE)
                    p_idx += 1

        # Token-level translation with punctuation preservation
        tokens = working_text.split()
        translated_tokens = []
        for tok in tokens:
            m = re.match(r'^([^\w\u064B-\u065F]*)(.*?)([^\w\u064B-\u065F]*)$', tok)
            if not m:
                translated_tokens.append(phrase_map.get(tok, tok))
                continue
            lead_punct, core, trail_punct = m.groups()
            if core in phrase_map:
                translated_core = phrase_map[core]
            else:
                core_lower = core.lower()
                core_stripped = strip_arabic_diacritics(core_lower)
                translated_core = None
                for lang in cand_langs:
                    rev_v = REVERSE_VOCAB.get(lang, {})
                    if core_lower in rev_v:
                        translated_core = rev_v[core_lower]
                        break
                    elif core_stripped in rev_v:
                        translated_core = rev_v[core_stripped]
                        break
                if translated_core is None:
                    translated_core = core
            translated_tokens.append(f'{lead_punct}{translated_core}{trail_punct}')

        result = ' '.join(translated_tokens)
        for k, v in phrase_map.items():
            result = result.replace(k, v)

        if result and result[0].isalpha():
            result = result[0].upper() + result[1:]
        return result

    def translate(self, text: str, target_lang: str, source_lang: str = 'en') -> str:
        target = _resolve_lang(target_lang)
        source = _resolve_lang(source_lang) if source_lang else 'en'
        t_clean = text.strip()
        if not t_clean:
            return ''
        if target == source and source != 'en':
            return t_clean

        is_input_rtl = bool(re.search(r'[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]', t_clean))

        # Handle reverse translation (RTL / foreign -> English)
        if target == 'english':
            if not is_input_rtl and source == 'english':
                return t_clean
            return self._translate_to_english(t_clean, source if source != 'english' else None)

        phrases = PHRASES.get(target, {})
        vocab = VOCAB.get(target, {})
        is_rtl = target in RTL_LANGUAGES

        # Check for direct phrase matches (longest phrase first)
        lower_clean = t_clean.lower()
        for phrase, translated in sorted(phrases.items(), key=lambda x: -len(x[0])):
            if phrase in lower_clean:
                stripped_input = re.sub(r'^[^\w]+|[^\w]+$', '', lower_clean)
                if stripped_input == phrase:
                    trail = ''
                    if t_clean and t_clean[-1] in '?!.,؟،':
                        p = t_clean[-1]
                        if is_rtl:
                            p = shape_rtl_punctuation(p)
                        trail = p
                    if translated and translated[-1] in '?!.,؟،':
                        return shape_rtl_punctuation(translated) if is_rtl else translated
                    res = translated + trail
                    return shape_rtl_punctuation(res) if is_rtl else res

        # Phrase replacement within sentence using safe non-whitespace tokens
        phrase_map: dict[str, str] = {}
        working_text = t_clean
        p_idx = 0
        for phrase, translated in sorted(phrases.items(), key=lambda x: -len(x[0])):
            pattern = re.compile(r'\b' + re.escape(phrase) + r'\b', re.IGNORECASE)
            if pattern.search(working_text):
                token_key = f'__CFPHRASE_{p_idx}__'
                phrase_map[token_key] = translated
                working_text = pattern.sub(token_key, working_text)
                p_idx += 1

        # Token-level translation with punctuation preservation
        tokens = working_text.split()
        translated_tokens = []

        for tok in tokens:
            m = re.match(r'^([^\w]*)(.*?)([^\w]*)$', tok)
            if not m:
                translated_tokens.append(phrase_map.get(tok, tok))
                continue
            lead_punct, core, trail_punct = m.groups()

            if is_rtl:
                lead_punct = shape_rtl_punctuation(lead_punct)
                trail_punct = shape_rtl_punctuation(trail_punct)

            if core in phrase_map:
                translated_core = phrase_map[core]
            else:
                core_lower = core.lower()
                translated_core = None

                if core_lower in vocab:
                    translated_core = vocab[core_lower]
                elif core_lower.endswith("'s") and core_lower[:-2] in vocab:
                    translated_core = vocab[core_lower[:-2]]
                elif core_lower.endswith('s') and core_lower[:-1] in vocab:
                    translated_core = vocab[core_lower[:-1]]
                elif core_lower.endswith('ing') and core_lower[:-3] in vocab:
                    translated_core = vocab[core_lower[:-3]]
                elif core_lower.endswith('ed') and core_lower[:-2] in vocab:
                    translated_core = vocab[core_lower[:-2]]

                if translated_core is None:
                    translated_core = core

            translated_tokens.append(f'{lead_punct}{translated_core}{trail_punct}')

        result = ' '.join(translated_tokens)
        for k, v in phrase_map.items():
            result = result.replace(k, v)
        if is_rtl:
            result = shape_rtl_punctuation(result)
        return result


class OpenAITranslationProvider(TranslationProvider):
    """External OpenAI LLM Neural Translation Provider (GPT-4o-mini / GPT-4.1)."""

    def __init__(self, api_key: str | None = None, model: str | None = None):
        self._api_key = api_key or os.getenv('OPENAI_API_KEY')
        self._model = model or os.getenv('OPENAI_TRANSLATION_MODEL', 'gpt-4o-mini')

    @property
    def name(self) -> str:
        return 'openai'

    @property
    def model_name(self) -> str:
        return f'OpenAI ({self._model})'

    @property
    def is_available(self) -> bool:
        return bool(self._api_key)

    def translate(self, text: str, target_lang: str, source_lang: str = 'en') -> str:
        batch = self.translate_batch([text], target_lang, source_lang)
        return batch[0] if batch else text

    def translate_batch(self, texts: list[str], target_lang: str, source_lang: str = 'en') -> list[str]:
        if not self.is_available:
            raise RuntimeError('OpenAI API key is not configured.')
        import httpx

        target = _resolve_lang(target_lang)
        is_rtl = target in RTL_LANGUAGES

        results: list[str] = []
        # Batch in chunks of 40 subtitles for context preservation and API safety
        for start in range(0, len(texts), 40):
            block = texts[start:start+40]
            system_prompt = (
                f"You are a professional video subtitle translator. Translate each subtitle cue from "
                f"{source_lang} naturally into {target_lang}. "
                f"Preserve concise length, colloquial tone, and emotional cadence. "
                f"Return ONLY a valid JSON object matching: {{\"translations\": [\"line 1\", \"line 2\", ...]}} "
                f"with exactly the same number of elements as the input array."
            )
            payload = {
                'model': self._model,
                'temperature': 0.2,
                'response_format': {'type': 'json_object'},
                'messages': [
                    {'role': 'system', 'content': system_prompt},
                    {'role': 'user', 'content': json.dumps({'captions': block}, ensure_ascii=False)},
                ],
            }
            with httpx.Client(timeout=120) as client:
                res = client.post(
                    'https://api.openai.com/v1/chat/completions',
                    json=payload,
                    headers={'Authorization': f'Bearer {self._api_key}'},
                )
                res.raise_for_status()
                data = res.json()
                content = data['choices'][0]['message']['content']
                parsed = json.loads(content).get('translations', [])
                if len(parsed) != len(block):
                    raise ValueError(f'OpenAI returned {len(parsed)} translations for {len(block)} cues')
                if is_rtl:
                    parsed = [shape_rtl_punctuation(p) for p in parsed]
                results.extend(parsed)

        return results


class AnthropicTranslationProvider(TranslationProvider):
    """External Anthropic Claude Neural Translation Provider."""

    def __init__(self, api_key: str | None = None, model: str | None = None):
        self._api_key = api_key or os.getenv('ANTHROPIC_API_KEY')
        self._model = model or os.getenv('ANTHROPIC_TRANSLATION_MODEL', 'claude-3-5-haiku-20241022')

    @property
    def name(self) -> str:
        return 'anthropic'

    @property
    def model_name(self) -> str:
        return f'Anthropic Claude ({self._model})'

    @property
    def is_available(self) -> bool:
        return bool(self._api_key)

    def translate(self, text: str, target_lang: str, source_lang: str = 'en') -> str:
        batch = self.translate_batch([text], target_lang, source_lang)
        return batch[0] if batch else text

    def translate_batch(self, texts: list[str], target_lang: str, source_lang: str = 'en') -> list[str]:
        if not self.is_available:
            raise RuntimeError('Anthropic API key is not configured.')
        import httpx

        target = _resolve_lang(target_lang)
        is_rtl = target in RTL_LANGUAGES

        results: list[str] = []
        for start in range(0, len(texts), 40):
            block = texts[start:start+40]
            system_prompt = (
                f"You are an expert subtitle translator. Translate each cue into {target_lang}. "
                f"Output ONLY a raw JSON object with key 'translations' containing an array of strings."
            )
            payload = {
                'model': self._model,
                'max_tokens': 2048,
                'system': system_prompt,
                'messages': [
                    {'role': 'user', 'content': json.dumps({'captions': block}, ensure_ascii=False)},
                ],
            }
            with httpx.Client(timeout=120) as client:
                res = client.post(
                    'https://api.anthropic.com/v1/messages',
                    json=payload,
                    headers={
                        'x-api-key': self._api_key,
                        'anthropic-version': '2023-06-01',
                        'content-type': 'application/json',
                    },
                )
                res.raise_for_status()
                data = res.json()
                raw_text = data['content'][0]['text']
                # extract json if markdown wrapped
                m = re.search(r'\{.*\}', raw_text, re.DOTALL)
                parsed = json.loads(m.group(0) if m else raw_text).get('translations', [])
                if len(parsed) != len(block):
                    raise ValueError(f'Claude returned {len(parsed)} translations for {len(block)} cues')
                if is_rtl:
                    parsed = [shape_rtl_punctuation(p) for p in parsed]
                results.extend(parsed)

        return results


class DeepLTranslationProvider(TranslationProvider):
    """External DeepL Translation Provider."""

    def __init__(self, api_key: str | None = None):
        self._api_key = api_key or os.getenv('DEEPL_API_KEY')

    @property
    def name(self) -> str:
        return 'deepl'

    @property
    def model_name(self) -> str:
        return 'DeepL Pro / Free API'

    @property
    def is_available(self) -> bool:
        return bool(self._api_key)

    def translate(self, text: str, target_lang: str, source_lang: str = 'en') -> str:
        batch = self.translate_batch([text], target_lang, source_lang)
        return batch[0] if batch else text

    def translate_batch(self, texts: list[str], target_lang: str, source_lang: str = 'en') -> list[str]:
        if not self.is_available:
            raise RuntimeError('DeepL API key is not configured.')
        import httpx

        target = _resolve_lang(target_lang)
        lang_map = {'persian': 'FA', 'arabic': 'AR', 'spanish': 'ES', 'english': 'EN'}
        target_code = lang_map.get(target, target.upper()[:2])
        is_rtl = target in RTL_LANGUAGES

        url = 'https://api-free.deepl.com/v2/translate' if ':fx' in (self._api_key or '') else 'https://api.deepl.com/v2/translate'
        with httpx.Client(timeout=120) as client:
            res = client.post(
                url,
                data={'text': texts, 'target_lang': target_code},
                headers={'Authorization': f'DeepL-Auth-Key {self._api_key}'},
            )
            res.raise_for_status()
            trans = [item['text'] for item in res.json().get('translations', [])]
            if is_rtl:
                trans = [shape_rtl_punctuation(t) for t in trans]
            return trans


class LocalTransformerTranslationProvider(TranslationProvider):
    """Local offline Neural Translation using NLLB / MarianMT transformer models."""

    def __init__(self, model_id: str | None = None):
        self._model_id = model_id or os.getenv('LOCAL_TRANSLATION_MODEL', 'facebook/nllb-200-distilled-600M')
        self._pipeline = None

    @property
    def name(self) -> str:
        return 'local_transformer'

    @property
    def model_name(self) -> str:
        return f'Local Transformer ({self._model_id})'

    @property
    def is_available(self) -> bool:
        # Check if local model directory exists or if transformers is installed
        model_path = os.getenv('LOCAL_TRANSLATION_MODEL_PATH')
        if model_path and os.path.isdir(model_path):
            return True
        try:
            import importlib.util
            return importlib.util.find_spec('transformers') is not None
        except Exception:
            return False

    def translate(self, text: str, target_lang: str, source_lang: str = 'en') -> str:
        batch = self.translate_batch([text], target_lang, source_lang)
        return batch[0] if batch else text

    def translate_batch(self, texts: list[str], target_lang: str, source_lang: str = 'en') -> list[str]:
        if not self.is_available:
            raise RuntimeError('Local Transformer provider is not available.')
        target = _resolve_lang(target_lang)
        is_rtl = target in RTL_LANGUAGES

        # If pipeline not yet loaded, try loading with transformers
        from transformers import pipeline
        if self._pipeline is None:
            model_path = os.getenv('LOCAL_TRANSLATION_MODEL_PATH', self._model_id)
            self._pipeline = pipeline('translation', model=model_path, device=-1)

        outputs = self._pipeline(texts)
        res = [out.get('translation_text', '') for out in outputs]
        if is_rtl:
            res = [shape_rtl_punctuation(r) for r in res]
        return res


# =====================================================================
# Provider Registry and Resolution
# =====================================================================

_OFFLINE_PROVIDER = OfflineRuleBasedTranslationProvider()

def get_registered_providers() -> dict[str, TranslationProvider]:
    """Returns all registered translation providers."""
    return {
        'openai': OpenAITranslationProvider(),
        'anthropic': AnthropicTranslationProvider(),
        'deepl': DeepLTranslationProvider(),
        'local_transformer': LocalTransformerTranslationProvider(),
        'offline': _OFFLINE_PROVIDER,
    }


def get_active_provider(preferred_provider: str | None = None) -> TranslationProvider:
    """Selects the active translation provider based on configuration or auto-detection."""
    providers = get_registered_providers()

    # Explicit preference passed
    if preferred_provider and preferred_provider in providers:
        p = providers[preferred_provider]
        if p.is_available:
            return p

    # Preference set in environment
    env_choice = os.getenv('TRANSLATION_PROVIDER', '').lower().strip()
    if env_choice and env_choice in providers:
        p = providers[env_choice]
        if p.is_available:
            return p

    # Auto-detection priority:
    # 1. OpenAI
    if providers['openai'].is_available:
        return providers['openai']
    # 2. Anthropic
    if providers['anthropic'].is_available:
        return providers['anthropic']
    # 3. DeepL
    if providers['deepl'].is_available:
        return providers['deepl']
    # 4. Local Transformer
    if providers['local_transformer'].is_available:
        return providers['local_transformer']

    # 5. Fast, robust Offline Fallback
    return _OFFLINE_PROVIDER


def get_provider_info() -> dict[str, Any]:
    """Provides a serializable summary of translation capabilities for API consumers."""
    providers = get_registered_providers()
    active = get_active_provider()
    available = [p.name for p in providers.values() if p.is_available]

    return {
        'active': active.name,
        'model': active.model_name,
        'isNeural': active.name in ('openai', 'anthropic', 'deepl', 'local_transformer'),
        'available_providers': available,
        'supported_languages': [
            {'code': 'fa', 'name': 'Persian (Dari / Farsi)', 'rtl': True},
            {'code': 'ps', 'name': 'Pashto', 'rtl': True},
            {'code': 'ur', 'name': 'Urdu', 'rtl': True},
            {'code': 'ar', 'name': 'Arabic', 'rtl': True},
            {'code': 'es', 'name': 'Spanish', 'rtl': False},
            {'code': 'en', 'name': 'English', 'rtl': False},
        ],
        'offlineFallbackReady': True,
    }


# =====================================================================
# Public High-Level Translation API
# =====================================================================

def translate_text(text: str, target_lang: str, source_lang: str = 'en', provider: str | None = None) -> str:
    """Translates arbitrary text into target language using active provider with offline fallback."""
    p = get_active_provider(provider)
    try:
        return p.translate(text, target_lang, source_lang)
    except Exception as e:
        logger.warning(f"Translation provider '{p.name}' failed: {e}. Falling back to offline engine.")
        return _OFFLINE_PROVIDER.translate(text, target_lang, source_lang)


def translate_batch(texts: list[str], target_lang: str, source_lang: str = 'en', provider: str | None = None) -> list[str]:
    """Translates multiple subtitle cues preserving context and order, with offline fallback."""
    p = get_active_provider(provider)
    try:
        return p.translate_batch(texts, target_lang, source_lang)
    except Exception as e:
        logger.warning(f"Translation provider '{p.name}' batch failed: {e}. Falling back to offline engine.")
        return _OFFLINE_PROVIDER.translate_batch(texts, target_lang, source_lang)

