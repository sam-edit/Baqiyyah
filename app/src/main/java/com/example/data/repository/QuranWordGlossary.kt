package com.example.data.repository

object QuranWordGlossary {
    private val normalizedGlossary = mapOf(
        "بسم" to Pair("نام سے", "In the name"),
        "الله" to Pair("اللہ کے", "of Allah"),
        "الرحمن" to Pair("نہایت مہربان", "the Entirely Merciful"),
        "الرحيم" to Pair("نہایت رحم والا", "the Especially Merciful"),
        "الحمد" to Pair("سب تعریفیں", "[All] praise"),
        "لله" to Pair("اللہ کے لیے", "is [due] to Allah"),
        "رب" to Pair("پروردگار", "Lord"),
        "العالمين" to Pair("تمام جہانوں کا", "of the worlds"),
        "مالك" to Pair("مالک", "Sovereign"),
        "يوم" to Pair("دن کا", "Day"),
        "الدين" to Pair("جزا و سزا کے", "of Recompense"),
        "اياك" to Pair("ہم صرف تیری ہی", "You [alone]"),
        "نعبد" to Pair("عبادت کرتے ہیں", "we worship"),
        "واياك" to Pair("اور تجھ ہی سے", "and You [alone]"),
        "نستعين" to Pair("ہم مدد مانگتے ہیں", "we ask for help"),
        "اهدنا" to Pair("ہمیں دکھا", "Guide us"),
        "الصراط" to Pair("سیدھا راستہ", "to the path"),
        "المستقيم" to Pair("سیدھا", "the straight"),
        "صراط" to Pair("راستہ", "The path"),
        "الذين" to Pair("ان لوگوں کا", "of those"),
        "انعمت" to Pair("تو نے انعام کیا", "You have bestowed favor"),
        "عليهم" to Pair("جن پر", "upon them"),
        "غير" to Pair("نہ کہ", "not"),
        "المغضوب" to Pair("غضب کیے گئے", "those who evoked anger"),
        "ولا" to Pair("اور نہ ہی", "nor"),
        "الضالين" to Pair("گمراہوں کا", "those who are astray"),
        "قل" to Pair("کہہ دیجیے", "Say"),
        "هو" to Pair("وہ", "He is"),
        "احد" to Pair("ایک ہے", "One"),
        "الصمد" to Pair("بے نیاز", "the Eternal Refuge"),
        "لم" to Pair("نہ", "Neither"),
        "يلد" to Pair("اس کی اولاد ہے", "did He beget"),
        "ولم" to Pair("اور نہ", "nor"),
        "يولد" to Pair("وہ پیدا کیا گیا", "was He begotten"),
        "يكن" to Pair("ہے", "is"),
        "له" to Pair("اس کے لیے", "to Him"),
        "كفوا" to Pair("برابر", "equivalent"),
        "اعوذ" to Pair("میں پناہ مانگتا ہوں", "I seek refuge"),
        "برب" to Pair("رب کی", "in the Lord"),
        "الفلق" to Pair("صبح کے", "of daybreak"),
        "من" to Pair("سے", "From"),
        "شر" to Pair("شر کے", "the evil"),
        "ما" to Pair("جو", "of that which"),
        "خلق" to Pair("اس نے پیدا کیا", "He created"),
        "غاسق" to Pair("اندھیری رات", "darkness"),
        "اذا" to Pair("جب", "when"),
        "وقب" to Pair("وہ چھا جائے", "it settles"),
        "النفاثات" to Pair("پھونکنے والیوں کے", "the blowers"),
        "في" to Pair("میں", "in"),
        "العقد" to Pair("گرہوں", "knots"),
        "حاسد" to Pair("حسد کرنے والے کے", "an envier"),
        "حسد" to Pair("وہ حسد کرے", "he envies"),
        "الناس" to Pair("لوگوں کے", "mankind"),
        "ملك" to Pair("بادشاہ", "Sovereign"),
        "اله" to Pair("معبود", "God"),
        "الوسواس" to Pair("وسوسہ ڈالنے والے کے", "the whisperer"),
        "الخناس" to Pair("پیچھے ہٹ جانے والے", "the retreating"),
        "الذي" to Pair("جو", "Who"),
        "يوسوس" to Pair("وسوسہ ڈالتا ہے", "whispers"),
        "صدور" to Pair("سینوں میں", "into breasts"),
        "الجنة" to Pair("جنات میں سے", "the jinn"),
        "والناس" to Pair("اور انسانوں میں سے", "and mankind"),
        "الكافرون" to Pair("اے کافرو", "disbelievers"),
        "لا" to Pair("نہیں", "Not"),
        "اعبد" to Pair("میں عبادت کرتا", "I worship"),
        "تعبدون" to Pair("تم پوجتے ہو", "you worship"),
        "انتم" to Pair("تم", "you are"),
        "عابدون" to Pair("عبادت کرنے والے", "worshippers"),
        "انا" to Pair("میں", "I will"),
        "عابد" to Pair("عبادت کرنے والا", "a worshipper"),
        "عبدتم" to Pair("تم نے عبادت کی", "you worshipped"),
        "لكم" to Pair("تمہارے لیے", "For you"),
        "دينكم" to Pair("تمہارا دین ہے", "is your religion"),
        "ولي" to Pair("اور میرے لیے", "and for me"),
        "دين" to Pair("میرا دین", "is my religion"),
        "انا" to Pair("بے شک ہم نے", "Indeed, We"),
        "اعطيناك" to Pair("عطا فرمایا آپ کو", "granted you"),
        "الكوثر" to Pair("کوثر", "Al-Kawthar"),
        "فصل" to Pair("پس نماز پڑھیں", "So pray"),
        "لربك" to Pair("اپنے رب کے لیے", "to your Lord"),
        "وانحر" to Pair("اور قربانی کیجیے", "and sacrifice"),
        "شانئك" to Pair("آپ کا دشمن", "your enemy"),
        "الابتر" to Pair("بے نام و نشان ہے", "cut off"),
        "اذا" to Pair("جب", "When"),
        "جاء" to Pair("آ جائے", "has come"),
        "نصر" to Pair("مدد", "victory"),
        "والفتح" to Pair("اور فتح", "and the conquest"),
        "ورايت" to Pair("اور آپ دیکھ لیں", "and you see"),
        "يدخلون" to Pair("داخل ہو رہے ہیں", "entering"),
        "افواجا" to Pair("فوج در فوج", "in multitudes"),
        "فسبح" to Pair("پس تسبیح کیجیے", "Then glorify"),
        "بحمد" to Pair("حمد کے ساتھ", "with praise"),
        "واستغفره" to Pair("اور مغفرت مانگیے", "and ask His forgiveness"),
        "انه" to Pair("بے شک وہ", "Indeed He"),
        "كان" to Pair("ہے", "is"),
        "توابا" to Pair("توبہ قبول فرمانے والا", "Accepting of repentance"),
        "والعصر" to Pair("زمانے کی قسم", "By time"),
        "ان" to Pair("بے شک", "Indeed"),
        "الانسان" to Pair("انسان", "mankind"),
        "لفي" to Pair("یقیناً میں ہے", "is in"),
        "خسر" to Pair("خسارے", "loss"),
        "الا" to Pair("سوائے", "Except"),
        "امنوا" to Pair("وہ ایمان لائے", "those who believe"),
        "وعملوا" to Pair("اور انہوں نے کیے", "and do"),
        "الصالحات" to Pair("نیک اعمال", "righteous deeds"),
        "وتواصوا" to Pair("اور نصیحت کی", "and advise"),
        "بالحق" to Pair("حق بات کی", "each other to truth"),
        "وبالصبر" to Pair("اور صبر کی", "and to patience"),
        "ذلك" to Pair("وہ", "That"),
        "هذا" to Pair("یہ", "This"),
        "هؤلاء" to Pair("یہ لوگ", "These"),
        "تلك" to Pair("وہ", "That"),
        "التي" to Pair("جو", "Which"),
        "عن" to Pair("سے", "From"),
        "على" to Pair("پر", "Upon"),
        "الى" to Pair("کی طرف", "To"),
        "حتى" to Pair("یہاں تک کہ", "Until"),
        "ثم" to Pair("پھر", "Then"),
        "او" to Pair("یا", "Or"),
        "ام" to Pair("یا", "Or"),
        "بل" to Pair("بلکہ", "Rather"),
        "لكن" to Pair("لیکن", "But"),
        "انما" to Pair("سوائے اس کے نہیں / صرف", "Only"),
        "كانوا" to Pair("وہ تھے", "they were"),
        "يكون" to Pair("وہ ہوتا ہے", "will be"),
        "قال" to Pair("اس نے کہا", "He said"),
        "قالوا" to Pair("انہوں نے کہا", "They said"),
        "قلنا" to Pair("ہم نے کہا", "We said"),
        "يقول" to Pair("وہ کہتا ہے", "he says"),
        "يقولون" to Pair("وہ کہتے ہیں", "they say"),
        "يعلم" to Pair("وہ جانتا ہے", "He knows"),
        "يعلمون" to Pair("وہ جانتے ہیں", "they know"),
        "تعلمون" to Pair("تم جانتے ہو", "you know"),
        "انزل" to Pair("نازل کیا", "sent down"),
        "انزلنا" to Pair("ہم نے نازل کیا", "We sent down"),
        "اتقوا" to Pair("تم ڈرو", "fear"),
        "متقين" to Pair("پرہیزگار", "the righteous"),
        "مؤمنين" to Pair("ایمان والے", "believers"),
        "كافرين" to Pair("کافر", "disbelievers"),
        "صادقين" to Pair("سچے", "truthful"),
        "ظالمين" to Pair("ظالم", "wrongdoers"),
        "جنة" to Pair("جنت", "Paradise"),
        "جنات" to Pair("باغات", "Gardens"),
        "نار" to Pair("آگ", "Fire"),
        "عذاب" to Pair("عذاب", "punishment"),
        "رحمة" to Pair("رحمت", "mercy"),
        "حق" to Pair("حق / سچ", "truth"),
        "باطل" to Pair("باطل", "falsehood"),
        "سماء" to Pair("آسمان", "sky"),
        "سموات" to Pair("آسمانوں", "heavens"),
        "ارض" to Pair("زمین", "earth"),
        "شمس" to Pair("سورج", "sun"),
        "قمر" to Pair("چاند", "moon"),
        "ليل" to Pair("رات", "night"),
        "نهار" to Pair("دن", "day"),
        "رسول" to Pair("رسول", "Messenger"),
        "رسل" to Pair("رسولوں", "messengers"),
        "نبي" to Pair("نبی", "Prophet"),
        "كتاب" to Pair("کتاب", "Book"),
        "اية" to Pair("آیت / نشانی", "sign/verse"),
        "ايات" to Pair("آیات / نشانیاں", "signs/verses"),
        "ملائكة" to Pair("فرشتے", "angels"),
        "نفس" to Pair("جان / نفس", "soul"),
        "انفسهم" to Pair("ان کی جانیں", "themselves"),
        "قلوب" to Pair("دل", "hearts"),
        "قلوبهم" to Pair("ان کے دل", "their hearts"),
        "ابصار" to Pair("آنکھیں / بینائی", "vision"),
        "سمع" to Pair("سماعت / سننا", "hearing"),
        "قوم" to Pair("قوم", "people"),
        "اهل" to Pair("اہل / والے", "people of"),
        "خير" to Pair("بہتر / بھلائی", "good"),
        "عليم" to Pair("جاننے والا", "All-Knowing"),
        "حكيم" to Pair("حکمت والا", "All-Wise"),
        "غفور" to Pair("بخشنے والا", "Forgiving"),
        "قدير" to Pair("قدرت والا", "Competent"),
        "سميع" to Pair("سننے والا", "All-Hearing"),
        "بصير" to Pair("دیکھنے والا", "All-Seeing"),
        "عزيز" to Pair("غالب / زبردست", "Almighty"),
        "كبير" to Pair("بڑا", "Great"),
        "عظيم" to Pair("عظمت والا", "Magnificent"),
        "خبير" to Pair("باخبر", "All-Aware")
    )

    private fun stripDiacritics(input: String): String {
        return input
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
            .replace("ٱ", "ا")
            .replace("آ", "ا")
            .replace("إ", "ا")
            .replace("أ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .trim()
    }

    fun lookup(arabicWord: String): Pair<String, String>? {
        val clean = stripDiacritics(arabicWord)
        normalizedGlossary[clean]?.let { return it }

        // Strip standard attachable prefixes (waw, fa, ba, lam, al)
        val prefixes = listOf(
            "وال" to Pair("اور ", "and the "),
            "فال" to Pair("پس ", "so the "),
            "بال" to Pair("ساتھ ", "with the "),
            "لل" to Pair("کے لیے ", "for the "),
            "ال" to Pair("", "the "),
            "و" to Pair("اور ", "and "),
            "ف" to Pair("پس ", "so "),
            "ب" to Pair("ساتھ ", "with "),
            "ل" to Pair("کے لیے ", "for ")
        )

        for ((pfx, pfxTrans) in prefixes) {
            if (clean.length > pfx.length + 2 && clean.startsWith(pfx)) {
                val stem = clean.substring(pfx.length)
                val stemMatch = normalizedGlossary[stem]
                if (stemMatch != null) {
                    val urdu = if (pfxTrans.first.isNotBlank()) "${pfxTrans.first}${stemMatch.first}" else stemMatch.first
                    val eng = if (pfxTrans.second.isNotBlank()) "${pfxTrans.second}${stemMatch.second}" else stemMatch.second
                    return Pair(urdu, eng)
                }
            }
        }

        // Also check if word has trailing pronominal suffix (hum, ha, kum, hu, na)
        val suffixes = listOf(
            "هم" to Pair(" ان کے", " their"),
            "كم" to Pair(" تمہارے", " your"),
            "نا" to Pair(" ہمارے", " our"),
            "ها" to Pair(" اس کے", " its"),
            "ه" to Pair(" اس کا", " his")
        )
        for ((sfx, sfxTrans) in suffixes) {
            if (clean.length > sfx.length + 2 && clean.endsWith(sfx)) {
                val stem = clean.substring(0, clean.length - sfx.length)
                val stemMatch = normalizedGlossary[stem]
                if (stemMatch != null) {
                    return Pair("${stemMatch.first}${sfxTrans.first}", "${sfxTrans.second} ${stemMatch.second}".trim())
                }
            }
        }

        return null
    }
}
