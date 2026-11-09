import sys
from pathlib import Path
TEST_DIR = Path(__file__).resolve().parent
ROOT = TEST_DIR.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import pytest
from translation import translate_text, _resolve_lang

def test_language_resolution():
    assert _resolve_lang('fa') == 'persian'
    assert _resolve_lang('Persian') == 'persian'
    assert _resolve_lang('Dari') == 'persian'
    assert _resolve_lang('ps') == 'pashto'
    assert _resolve_lang('Pashto') == 'pashto'
    assert _resolve_lang('ur') == 'urdu'
    assert _resolve_lang('Urdu') == 'urdu'
    assert _resolve_lang('ar') == 'arabic'
    assert _resolve_lang('es') == 'spanish'
    assert _resolve_lang('en') == 'english'

def test_workflow_sample_phrases():
    # Persian
    fa_t1 = translate_text('Welcome to CaptionForge: AI Video Subtitle App', 'fa')
    assert 'خوش آمدید' in fa_t1
    assert 'کپشن‌فورج' in fa_t1

    fa_t2 = translate_text('We create beautiful animated subtitles.', 'fa')
    assert 'می‌سازیم' in fa_t2

    # Pashto
    ps_t1 = translate_text('Welcome to CaptionForge: AI Video Subtitle App', 'ps')
    assert 'ښه راغلاست' in ps_t1
    assert 'کپشن‌فورج' in ps_t1

    ps_t2 = translate_text('We create beautiful animated subtitles.', 'ps')
    assert 'جوړوو' in ps_t2

    # Urdu
    ur_t1 = translate_text('Welcome to CaptionForge: AI Video Subtitle App', 'ur')
    assert 'خوش آمدید' in ur_t1
    assert 'کپشن فورج' in ur_t1

    ur_t2 = translate_text('We create beautiful animated subtitles.', 'ur')
    assert 'سب ٹائٹلز' in ur_t2 or 'بناتے ہیں' in ur_t2

    # Arabic
    ar_t1 = translate_text('Welcome to CaptionForge: AI Video Subtitle App', 'ar')
    assert 'مرحباً' in ar_t1 or 'كابشن فورج' in ar_t1

def test_unseen_novel_sentences():
    # Test novel sentence not in any sample phrase list
    novel_sentence = 'Today is a great day to learn and create new videos with artificial intelligence.'

    # Urdu unseen translation
    ur_out = translate_text(novel_sentence, 'ur')
    assert 'آج' in ur_out
    assert 'بہترین' in ur_out or 'دن' in ur_out
    assert 'ویڈیوز' in ur_out
    assert 'مصنوعی ذہانت' in ur_out
    assert '__' not in ur_out

    # Persian unseen translation
    fa_out = translate_text(novel_sentence, 'fa')
    assert 'امروز' in fa_out
    assert 'روز' in fa_out
    assert 'ویدیوها' in fa_out
    assert 'هوش مصنوعی' in fa_out
    assert '__' not in fa_out

    # Pashto unseen translation
    ps_out = translate_text(novel_sentence, 'ps')
    assert 'نن' in ps_out
    assert 'ورځ' in ps_out
    assert 'ویډیوګانې' in ps_out
    assert 'مصنوعي استخبارات' in ps_out
    assert '__' not in ps_out

    # Second unseen sentence with question mark
    q_sentence = 'How can I edit and export my project?'
    ur_q = translate_text(q_sentence, 'ur')
    assert '؟' in ur_q
    assert 'ترمیم' in ur_q or 'برآمد' in ur_q
    assert '__' not in ur_q

    # Third test: phrase with question mark
    q_phrase = 'Do you like artificial intelligence?'
    fa_q = translate_text(q_phrase, 'fa')
    assert 'هوش مصنوعی' in fa_q
    assert '؟' in fa_q
    assert '__' not in fa_q

    # Fourth test: conversational unseen sentence
    conv_sentence = 'Hello world, please help me edit this video.'
    fa_conv = translate_text(conv_sentence, 'fa')
    assert 'سلام' in fa_conv
    assert 'جهان' in fa_conv or 'دنیا' in fa_conv
    assert 'لطفاً' in fa_conv
    assert '__' not in fa_conv

def test_provider_architecture():
    from translation import get_provider_info, get_active_provider, get_registered_providers
    info = get_provider_info()
    assert 'active' in info
    assert 'available_providers' in info
    assert 'offline' in info['available_providers']
    assert info['offlineFallbackReady'] is True
    assert len(info['supported_languages']) >= 5

    active = get_active_provider()
    assert active is not None
    assert active.is_available is True

    providers = get_registered_providers()
    assert 'openai' in providers
    assert 'anthropic' in providers
    assert 'deepl' in providers
    assert 'offline' in providers

def test_batch_translation_and_rtl_shaping():
    from translation import translate_batch, shape_rtl_punctuation
    # Test RTL punctuation shaping directly
    assert shape_rtl_punctuation("Where is the hospital? Doctor, please help.") == "Where is the hospital؟ Doctor، please help."

    cues = [
        "Welcome to CaptionForge: AI Video Subtitle App",
        "How can I edit and export my project?",
        "Today is a great day to learn and create new videos with artificial intelligence."
    ]
    # Urdu batch
    ur_batch = translate_batch(cues, 'ur')
    assert len(ur_batch) == 3
    assert 'خوش آمدید' in ur_batch[0]
    assert '؟' in ur_batch[1]
    assert 'آج' in ur_batch[2]

    # Persian batch
    fa_batch = translate_batch(cues, 'fa')
    assert len(fa_batch) == 3
    assert 'خوش آمدید' in fa_batch[0]
    assert '؟' in fa_batch[1]
    assert 'امروز' in fa_batch[2]

def test_multi_domain_unseen_paragraphs():
    # Medical domain
    med_sentence = "The doctor and hospital need medicine for every patient."
    fa_med = translate_text(med_sentence, 'fa')
    assert 'پزشک' in fa_med or 'بیمارستان' in fa_med or 'دارو' in fa_med

    # Education domain
    edu_sentence = "The teacher and student love education at the university."
    ur_edu = translate_text(edu_sentence, 'ur')
    assert 'استاد' in ur_edu or 'طالب علم' in ur_edu or 'تعلیم' in ur_edu

    # Business domain
    biz_sentence = "Our business and company make money in the market."
    ps_biz = translate_text(biz_sentence, 'ps')
    assert 'سوداګري' in ps_biz or 'شرکت' in ps_biz or 'پیسې' in ps_biz

def test_bidirectional_translations():
    # Persian -> English
    fa_hello = translate_text('سلام', 'en', source_lang='fa')
    assert any(w in fa_hello.lower() for w in ('hello', 'hi'))

    fa_thanks = translate_text('سپاسگزارم', 'en', source_lang='fa')
    assert 'thank you' in fa_thanks.lower()

    fa_phrase = translate_text('ما زیرنویس‌های زیبایی می‌سازیم', 'en', source_lang='fa')
    assert 'subtitles' in fa_phrase.lower()

    # Pashto -> English
    ps_thanks = translate_text('مننه', 'en', source_lang='ps')
    assert 'thank you' in ps_thanks.lower()

    ps_phrase = translate_text('د کپشن‌فورج لخوا سلامونه!', 'en', source_lang='ps')
    assert 'captionforge' in ps_phrase.lower()

    # Urdu -> English
    ur_thanks = translate_text('شکریہ', 'en', source_lang='ur')
    assert 'thank you' in ur_thanks.lower()

    ur_phrase = translate_text('کپشن فورج میں خوش آمدید', 'en', source_lang='ur')
    assert 'captionforge' in ur_phrase.lower() or 'welcome' in ur_phrase.lower()

    # Arabic -> English
    ar_thanks = translate_text('شكراً', 'en', source_lang='ar')
    assert 'thank you' in ar_thanks.lower()

    ar_phrase = translate_text('مرحباً بكم في كابشن فورج', 'en', source_lang='ar')
    assert 'captionforge' in ar_phrase.lower() or 'welcome' in ar_phrase.lower()

    # Unseen compound reverse translation
    unseen_fa = translate_text('پزشک و بیمارستان', 'en', source_lang='fa')
    assert 'doctor' in unseen_fa.lower()
    assert 'hospital' in unseen_fa.lower()


