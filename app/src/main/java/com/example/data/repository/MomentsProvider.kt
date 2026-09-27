package com.example.data.repository

import android.content.Context
import com.example.domain.model.Ayah
import com.example.domain.model.DuaItem
import com.example.domain.model.Hadith
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

object MomentsProvider {

    val inspiringAyahs: List<Ayah> = listOf(
        Ayah(
            number = 255,
            surahNumber = 2,
            numberInSurah = 255,
            textArabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ",
            textTranslation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep.",
            urduTranslation = "اللہ وہ معبود برحق ہے جس کے سوا کوئی معبود نہیں، وہ ہمیشہ زندہ رہنے والا، سب کا نگہبان ہے، نہ اسے اونگھ آتی ہے نہ نیند۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3"
        ),
        Ayah(
            number = 286,
            surahNumber = 2,
            numberInSurah = 286,
            textArabic = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا ۚ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ",
            textTranslation = "Allah does not burden a soul beyond that it can bear. It will have [the consequence of] what [good] it has gained, and it will bear [the consequence of] what [evil] it has earned.",
            urduTranslation = "اللہ کسی جان پر اس کی طاقت سے زیادہ بوجھ نہیں ڈالتا، جو نیکی اس نے کمائی وہ اس کے لیے ہے اور جو گناہ اس نے کمایا وہ اسی کے سر ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/293.mp3"
        ),
        Ayah(
            number = 152,
            surahNumber = 2,
            numberInSurah = 152,
            textArabic = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            textTranslation = "So remember Me; I will remember you. And be grateful to Me and do not deny Me.",
            urduTranslation = "پس تم مجھے یاد رکھو، میں تمہیں یاد رکھوں گا اور میرا شکر ادا کرو اور میری ناشکری نہ کرو۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/159.mp3"
        ),
        Ayah(
            number = 186,
            surahNumber = 2,
            numberInSurah = 186,
            textArabic = "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
            textTranslation = "And when My servants ask you concerning Me, indeed I am near. I respond to the invocation of the supplicant when he calls upon Me.",
            urduTranslation = "اور جب میرے بندے آپ سے میرے متعلق پوچھیں تو یقیناً میں قریب ہوں، پکارنے والے کی دعا قبول کرتا ہوں جب وہ مجھے پکارے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/193.mp3"
        ),
        Ayah(
            number = 139,
            surahNumber = 3,
            numberInSurah = 139,
            textArabic = "وَلَا تَهِنُوا وَلَا تَحْزَنُوا وَأَنتُمُ الْأَعْلَوْنَ إِن كُنتُم مُّؤْمِنِينَ",
            textTranslation = "So do not weaken and do not grieve, and you will be superior if you are [true] believers.",
            urduTranslation = "اور تم نہ سست پڑو اور نہ غمگین ہو اور تم ہی غالب رہو گے اگر تم مومن ہو۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/432.mp3"
        ),
        Ayah(
            number = 53,
            surahNumber = 39,
            numberInSurah = 53,
            textArabic = "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا",
            textTranslation = "Say, 'O My servants who have transgressed against themselves, do not despair of the mercy of Allah. Indeed, Allah forgives all sins.'",
            urduTranslation = "کہہ دیجیے کہ اے میرے بندو جنہوں نے اپنی جانوں پر زیادتی کی ہے! اللہ کی رحمت سے ناامید نہ ہو، بے شک اللہ تمام گناہ بخش دیتا ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/4111.mp3"
        ),
        Ayah(
            number = 28,
            surahNumber = 13,
            numberInSurah = 28,
            textArabic = "الَّذِينَ آمَنُوا وَتَطْمَئِنُّ قُلُوبُهُم بِذِكْرِ اللَّهِ ۗ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            textTranslation = "Those who have believed and whose hearts are assured by the remembrance of Allah. Unquestionably, by the remembrance of Allah hearts are assured.",
            urduTranslation = "وہ لوگ جو ایمان لائے اور جن کے دل اللہ کے ذکر سے مطمئن ہوتے ہیں، سن لو! اللہ کے ذکر ہی سے دلوں کو اطمینان نصیب ہوتا ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1735.mp3"
        ),
        Ayah(
            number = 5,
            surahNumber = 94,
            numberInSurah = 5,
            textArabic = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا • إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            textTranslation = "For indeed, with hardship [will be] ease. Indeed, with hardship [will be] ease.",
            urduTranslation = "پس یقیناً مشکل کے ساتھ آسانی ہے، بے شک مشکل کے ساتھ آسانی ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6095.mp3"
        ),
        Ayah(
            number = 3,
            surahNumber = 65,
            numberInSurah = 3,
            textArabic = "وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ۚ وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
            textTranslation = "And will provide for him from where he does not expect. And whoever relies upon Allah - then He is sufficient for him.",
            urduTranslation = "اور اسے وہاں سے رزق دے گا جہاں سے گمان بھی نہ ہو، اور جو اللہ پر بھروسہ کرے تو وہ اس کے لیے کافی ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/5244.mp3"
        ),
        Ayah(
            number = 7,
            surahNumber = 14,
            numberInSurah = 7,
            textArabic = "لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ ۖ وَلَئِن كَفَرْتُمْ إِنَّ عَذَابِي لَشَدِيدٌ",
            textTranslation = "If you are grateful, I will surely increase you [in favor]; but if you deny, indeed, My punishment is severe.",
            urduTranslation = "اگر تم شکر ادا کرو گے تو میں تمہیں ضرور زیادہ دوں گا، اور اگر تم ناشکری کرو گے تو یقیناً میرا عذاب بہت سخت ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1757.mp3"
        ),
        Ayah(
            number = 87,
            surahNumber = 21,
            numberInSurah = 87,
            textArabic = "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
            textTranslation = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            urduTranslation = "تیرے سوا کوئی معبود نہیں، تو پاک ہے، بے شک میں ہی قصورواروں میں سے تھا۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/2570.mp3"
        ),
        Ayah(
            number = 24,
            surahNumber = 28,
            numberInSurah = 24,
            textArabic = "رَبِّ إِنِّي لِمَا أَنزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ",
            textTranslation = "My Lord, indeed I am, for whatever good You would send down to me, in need.",
            urduTranslation = "اے میرے رب! جو بھلائی بھی تو میری طرف نازل فرمائے میں اس کا محتاج ہوں۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/3276.mp3"
        ),
        Ayah(
            number = 114,
            surahNumber = 20,
            numberInSurah = 114,
            textArabic = "وَقُل رَّبِّ زِدْنِي عِلْمًا",
            textTranslation = "And say, 'My Lord, increase me in knowledge.'",
            urduTranslation = "اور دعا کیجیے کہ اے میرے پروردگار! میرے علم میں اضافہ فرما۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/2462.mp3"
        ),
        Ayah(
            number = 2,
            surahNumber = 67,
            numberInSurah = 2,
            textArabic = "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا ۚ وَهُوَ الْعَزِيزُ الْغَفُورُ",
            textTranslation = "[He] who created death and life to test you [as to] which of you is best in deed - and He is the Exalted in Might, the Forgiving.",
            urduTranslation = "جس نے موت اور زندگی کو پیدا کیا تاکہ تمہیں آزمائے کہ تم میں سے کون اچھے عمل کرتا ہے، اور وہ زبردست بخشنے والا ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/5263.mp3"
        ),
        Ayah(
            number = 22,
            surahNumber = 59,
            numberInSurah = 22,
            textArabic = "هُوَ اللَّهُ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ ۖ عَالِمُ الْغَيْبِ وَالشَّهَادَةِ ۖ هُوَ الرَّحْمَٰنُ الرَّحِيمُ",
            textTranslation = "He is Allah, other than whom there is no deity, Knower of the unseen and the witnessed. He is the Entirely Merciful, the Especially Merciful.",
            urduTranslation = "وہی اللہ ہے جس کے سوا کوئی معبود نہیں، غائب اور ظاہر کا جاننے والا، وہی بڑا مہربان نہایت رحم کرنے والا ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/5148.mp3"
        )
    )

    val inspiringHadiths: List<Hadith> = listOf(
        Hadith(
            id = 1,
            bookSlug = "sahih-bukhari",
            bookName = "Sahih al-Bukhari",
            chapterNumber = 1,
            chapterNameArabic = "كتاب بدء الوحي",
            chapterNameUrdu = "وحی کا باب",
            hadithNumber = "1",
            arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى.",
            urduTranslation = "اعمال کا دارومدار صرف نیتوں پر ہے اور ہر انسان کے لیے وہی ہے جس کی اس نے نیت کی۔",
            englishTranslation = "Actions are judged by intentions, and everyone will get what was intended."
        ),
        Hadith(
            id = 2,
            bookSlug = "sahih-bukhari",
            bookName = "Sahih al-Bukhari",
            chapterNumber = 2,
            chapterNameArabic = "كتاب الإيمان",
            chapterNameUrdu = "ایمان کا باب",
            hadithNumber = "13",
            arabicText = "لاَ يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ.",
            urduTranslation = "تم میں سے کوئی شخص اس وقت تک مومن نہیں ہو سکتا جب تک اپنے بھائی کے لیے بھی وہی پسند نہ کرے جو اپنے لیے پسند کرتا ہے۔",
            englishTranslation = "None of you will believe until you love for your brother what you love for yourself."
        ),
        Hadith(
            id = 3,
            bookSlug = "sahih-bukhari",
            bookName = "Sahih al-Bukhari",
            chapterNumber = 61,
            chapterNameArabic = "فضائل القرآن",
            chapterNameUrdu = "قرآن کی فضیلت",
            hadithNumber = "5027",
            arabicText = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ.",
            urduTranslation = "تم میں سے سب سے بہترین شخص وہ ہے جو قرآن سیکھے اور دوسروں کو سکھائے۔",
            englishTranslation = "The best among you are those who learn the Quran and teach it."
        ),
        Hadith(
            id = 4,
            bookSlug = "sahih-muslim",
            bookName = "Sahih Muslim",
            chapterNumber = 35,
            chapterNameArabic = "كتاب البر والصلة",
            chapterNameUrdu = "حسن سلوک اور صلہ رحمی",
            hadithNumber = "2564",
            arabicText = "إِنَّ اللَّهَ لاَ يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ، وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ.",
            urduTranslation = "اللہ تعالیٰ تمہاری صورتوں اور مال و دولت کو نہیں دیکھتا، بلکہ وہ تمہارے دلوں اور اعمال کو دیکھتا ہے۔",
            englishTranslation = "Allah does not look at your appearance or your wealth, but He looks at your hearts and your deeds."
        ),
        Hadith(
            id = 5,
            bookSlug = "sahih-muslim",
            bookName = "Sahih Muslim",
            chapterNumber = 2,
            chapterNameArabic = "كتاب الطهارة",
            chapterNameUrdu = "طہارت و پاکیزگی کا باب",
            hadithNumber = "223",
            arabicText = "الطُّهُورُ شَطْرُ الإِيمَانِ، وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ.",
            urduTranslation = "پاکیزگی نصف ایمان ہے، اور الحمد للہ کہنا ترازو کو نیکیوں سے بھر دیتا ہے۔",
            englishTranslation = "Purity is half of faith, and saying 'Al-hamdulillah' fills the scales of good deeds."
        ),
        Hadith(
            id = 6,
            bookSlug = "jami-at-tirmidhi",
            bookName = "Jami` at-Tirmidhi",
            chapterNumber = 27,
            chapterNameArabic = "كتاب البر والصلة",
            chapterNameUrdu = "نیکی کا باب",
            hadithNumber = "1956",
            arabicText = "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ.",
            urduTranslation = "اپنے مسلمان بھائی کے سامنے تمہارا مسکرانا بھی تمہارے لیے صدقہ ہے۔",
            englishTranslation = "Smiling in the face of your brother is an act of charity for you."
        ),
        Hadith(
            id = 7,
            bookSlug = "sahih-bukhari",
            bookName = "Sahih al-Bukhari",
            chapterNumber = 78,
            chapterNameArabic = "كتاب الأدب",
            chapterNameUrdu = "اخلاق و ادب",
            hadithNumber = "6011",
            arabicText = "الْمُسْلِمُ مَنْ سَلِمَ الْمُسْلِمُونَ مِنْ لِسَانِهِ وَيَدِهِ.",
            urduTranslation = "سچا مسلمان وہ ہے جس کی زبان اور ہاتھ کی تکلیف سے دوسرے مسلمان محفوظ رہیں۔",
            englishTranslation = "A true Muslim is the one from whose tongue and hands other Muslims are safe."
        ),
        Hadith(
            id = 8,
            bookSlug = "sunan-abi-dawud",
            bookName = "Sunan Abi Dawud",
            chapterNumber = 43,
            chapterNameArabic = "كتاب الأدب",
            chapterNameUrdu = "حسن اخلاق",
            hadithNumber = "4941",
            arabicText = "الرَّاحِمُونَ يَرْحَمُهُمُ الرَّحْمَٰنُ، ارْحَمُوا مَنْ فِي الأَرْضِ يَرْحَمْكُمْ مَنْ فِي السَّمَاءِ.",
            urduTranslation = "رحم کرنے والوں پر رحمن رحم فرماتا ہے، تم زمین والوں پر رحم کرو، آسمان والا تم پر رحم فرمائے گا۔",
            englishTranslation = "The merciful will be shown mercy by the Most Merciful. Be merciful to those on earth, and the One in the heavens will be merciful to you."
        ),
        Hadith(
            id = 9,
            bookSlug = "jami-at-tirmidhi",
            bookName = "Jami` at-Tirmidhi",
            chapterNumber = 37,
            chapterNameArabic = "كتاب صفة القيامة",
            chapterNameUrdu = "تقویٰ و حسن اخلاق",
            hadithNumber = "2518",
            arabicText = "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ.",
            urduTranslation = "تم جہاں کہیں بھی رہو اللہ سے ڈرو، اور گناہ کے بعد نیکی کرو وہ اسے مٹا دے گی، اور لوگوں کے ساتھ حسن اخلاق سے پیش آؤ۔",
            englishTranslation = "Fear Allah wherever you may be, follow up an evil deed with a good one which will wipe it out, and behave well towards the people."
        ),
        Hadith(
            id = 10,
            bookSlug = "sahih-bukhari",
            bookName = "Sahih al-Bukhari",
            chapterNumber = 80,
            chapterNameArabic = "كتاب الدعوات",
            chapterNameUrdu = "دعائیں اور ذکر",
            hadithNumber = "6407",
            arabicText = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَٰنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ.",
            urduTranslation = "دو کلمے زبان پر بہت ہلکے، میزان میں بہت بھاری اور رحمن کو بہت پیارے ہیں: سبحان اللہ وبحمدہ، سبحان اللہ العظیم۔",
            englishTranslation = "Two words are light on the tongue, heavy in the balance, and beloved to the Most Merciful: Subhan Allahi wa bi-hamdih, Subhan Allahil-Azeem."
        )
    )

    val inspiringWords: List<AuthoritativeContentProvider.WordOfTheMoment> = listOf(
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "تَقْوَى",
            urduMeaning = "اللہ کا خوف، پرہیزگاری اور گناہوں سے بچنا",
            englishMeaning = "Piety, God-consciousness, righteousness",
            pronunciation = "Taqwa",
            context = "Mentioned over 250 times in the Quran as the core of faith."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "إِحْسَان",
            urduMeaning = "کمال اخلاص، اللہ کی ایسی عبادت گویا تم اسے دیکھ رہے ہو",
            englishMeaning = "Spiritual excellence, benevolence and beauty of conduct",
            pronunciation = "Ihsan",
            context = "Described by the Prophet ﷺ as worshiping Allah as if you see Him."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "صَبْر",
            urduMeaning = "استقامت، شکوے کے بغیر مصیبت پر ثابت قدمی",
            englishMeaning = "Patience, steadfastness, perseverance",
            pronunciation = "Sabr",
            context = "Allah declares: 'Indeed, Allah is with those who are patient' (2:153)."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "شُكْر",
            urduMeaning = "نعمتوں پر زبان، دل اور عمل سے احسان مندی",
            englishMeaning = "Gratitude, thankfulness to the Creator",
            pronunciation = "Shukr",
            context = "Allah promises: 'If you are grateful, I will surely increase you' (14:7)."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "تَوَكُّل",
            urduMeaning = "تدبیر کے ساتھ نتیجہ اللہ کے سپرد کر دینا",
            englishMeaning = "Complete reliance and trust upon Allah",
            pronunciation = "Tawakkul",
            context = "The Quran states: 'And whoever relies upon Allah, He is sufficient for him' (65:3)."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "إِخْلَاص",
            urduMeaning = "ریاکاری سے پاک خالص اللہ کی رضا کے لیے عمل",
            englishMeaning = "Purity of intention and sincere devotion",
            pronunciation = "Ikhlas",
            context = "The foundation of all accepted deeds in Islamic theology."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "رَحْمَة",
            urduMeaning = "عظیم شفقت، بخشش، کرم اور عنایت",
            englishMeaning = "Divine mercy, compassion and tenderness",
            pronunciation = "Rahmah",
            context = "Allah says: 'My mercy encompasses all things' (7:156)."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "سَكِينَة",
            urduMeaning = "قلبی سکون، اطمینان اور الٰہی تسکین",
            englishMeaning = "Inner tranquility, serenity and divine peace",
            pronunciation = "Sakinah",
            context = "Sent down into the hearts of believers during distress (48:4)."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "حِكْمَة",
            urduMeaning = "دانائی، بصیرت اور حق بات کو موقع پر کہنا",
            englishMeaning = "Wisdom, deep insight and sound discernment",
            pronunciation = "Hikmah",
            context = "Quran states: 'Whoever is granted wisdom has been given much good' (2:269)."
        ),
        AuthoritativeContentProvider.WordOfTheMoment(
            arabicWord = "تَوْبَة",
            urduMeaning = "گناہ پر ندامت کے ساتھ اللہ کی طرف پلٹنا",
            englishMeaning = "Repentance, turning back to Allah with remorse",
            pronunciation = "Tawbah",
            context = "The gateway to divine forgiveness and cleansing of sins."
        )
    )

    val inspiringDuas: List<DuaItem> = listOf(
        DuaItem(
            id = "dua_moment_1",
            category = "Daily",
            title = "Dua for Goodness in Both Worlds",
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar",
            englishTranslation = "Our Lord, give us in this world that which is good and in the Hereafter that which is good and protect us from the punishment of the Fire.",
            urduTranslation = "اے ہمارے رب! ہمیں دنیا میں بھی بھلائی عطا فرما اور آخرت میں بھی بھلائی عطا فرما اور ہمیں آگ کے عذاب سے بچا۔",
            reference = "Surah Al-Baqarah (2:201)"
        ),
        DuaItem(
            id = "dua_moment_2",
            category = "Knowledge",
            title = "Dua for Increase in Knowledge & Wisdom",
            arabicText = "رَّبِّ زِدْنِي عِلْمًا",
            transliteration = "Rabbi zidni 'ilma",
            englishTranslation = "My Lord, increase me in knowledge.",
            urduTranslation = "اے میرے رب! میرے علم میں اضافہ فرما۔",
            reference = "Surah Ta-Ha (20:114)"
        ),
        DuaItem(
            id = "dua_moment_3",
            category = "Forgiveness",
            title = "Dua of Prophet Yunus (A.S)",
            arabicText = "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
            transliteration = "La ilaha illa anta subhanaka inni kuntu minaz-zalimin",
            englishTranslation = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            urduTranslation = "تیرے سوا کوئی معبود نہیں، تو پاک ہے، بے شک میں ہی قصورواروں میں سے تھا۔",
            reference = "Surah Al-Anbiya (21:87)"
        ),
        DuaItem(
            id = "dua_moment_4",
            category = "Ease",
            title = "Dua for Opening of the Heart & Ease",
            arabicText = "رَبِّ اشْرَحْ لِي صَدْرِي • وَيَسِّرْ لِي أَمْرِي • وَاحْلُلْ عُقْدَةً مِّن لِّسَانِي • يَفْقَهُوا قَوْلِي",
            transliteration = "Rabbish-rah li sadri, wa yassir li amri, wahlul 'uqdatam-mil-lisani, yafqahu qawli",
            englishTranslation = "My Lord, expand for me my breast [with assurance] and ease for me my task and untie the knot from my tongue that they may understand my speech.",
            urduTranslation = "اے میرے پروردگار! میرے لیے میرا سینہ کھول دے، اور میرا کام آسان کر دے، اور میری زبان کی گرہ کھول دے تاکہ لوگ میری بات سمجھ سکیں۔",
            reference = "Surah Ta-Ha (20:25-28)"
        ),
        DuaItem(
            id = "dua_moment_5",
            category = "Protection",
            title = "Dua for Reliance on Allah",
            arabicText = "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
            transliteration = "Hasbunallahu wa ni'mal wakeel",
            englishTranslation = "Sufficient for us is Allah, and [He is] the best Disposer of affairs.",
            urduTranslation = "ہمیں اللہ کافی ہے اور وہ بہترین کارساز ہے۔",
            reference = "Surah Ali 'Imran (3:173)"
        ),
        DuaItem(
            id = "dua_moment_6",
            category = "Steadfastness",
            title = "Dua for Steadfast Faith",
            arabicText = "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً ۚ إِنَّكَ أَنتَ الْوَهَّابُ",
            transliteration = "Rabbana la tuzigh qulubana ba'da idh hadaytana wa hab lana mil-ladunka rahmah, innaka antal-Wahhab",
            englishTranslation = "Our Lord, let not our hearts deviate after You have guided us and grant us from Yourself mercy. Indeed, You are the Bestower.",
            urduTranslation = "اے ہمارے رب! ہمارے دلوں کو ٹیڑھا نہ کر بعد اس کے کہ تو نے ہمیں ہدایت دی اور ہمیں اپنے پاس سے رحمت عطا فرما، یقیناً تو ہی بہت عطا فرمانے والا ہے۔",
            reference = "Surah Ali 'Imran (3:8)"
        ),
        DuaItem(
            id = "dua_moment_7",
            category = "Parents",
            title = "Dua for Parents",
            arabicText = "رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            transliteration = "Rabbir-hamhuma kama rabbayani sagheera",
            englishTranslation = "My Lord, have mercy upon them as they brought me up [when I was] small.",
            urduTranslation = "اے میرے رب! ان دونوں (والدین) پر رحم فرما جیسا کہ انہوں نے بچپن میں مجھے پالا۔",
            reference = "Surah Al-Isra (17:24)"
        ),
        DuaItem(
            id = "dua_moment_8",
            category = "Daily",
            title = "Master Supplication for Forgiveness (Sayyid-ul-Istighfar)",
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            transliteration = "Allahumma Anta Rabbi la ilaha illa Anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bini'matika 'alayya, wa abu'u laka bidhanbi faghfir li fa innahu la yaghfirudh-dhunuba illa Anta",
            englishTranslation = "O Allah, You are my Lord, none has the right to be worshiped but You. You created me and I am Your servant, and I am faithful to my covenant and my promise as much as I can.",
            urduTranslation = "اے اللہ! تو میرا رب ہے، تیرے سوا کوئی معبود نہیں، تو نے مجھے پیدا کیا اور میں تیرا بندہ ہوں اور تیرے عہد اور وعدے پر قائم ہوں جہاں تک طاقت ہے۔ اپنے گناہوں کا اعتراف کرتا ہوں، مجھے معاف فرما دے کیونکہ تیرے سوا کوئی گناہوں کو معاف نہیں کر سکتا۔",
            reference = "Sahih al-Bukhari (6306)"
        )
    )

    @Volatile
    private var cachedDownloadedHadiths: List<Hadith>? = null
    @Volatile
    private var lastHadithScanTime: Long = 0

    fun invalidateHadithCache() {
        cachedDownloadedHadiths = null
        lastHadithScanTime = 0
    }

    fun getRandomAyah(context: Context?, excludeGlobalNumber: Int? = null): Ayah {
        // Strictly pick a random ayah across all 114 Surahs from OfflineQuranProvider
        try {
            if (context != null) {
                return OfflineQuranProvider.getRandomAyah(context, excludeGlobalNumber)
            }
        } catch (_: Throwable) {}

        val pool = if (excludeGlobalNumber != null && inspiringAyahs.size > 1) {
            inspiringAyahs.filter { it.number != excludeGlobalNumber }.ifEmpty { inspiringAyahs }
        } else {
            inspiringAyahs
        }
        return pool.random()
    }

    fun getRandomHadith(context: Context?, excludeId: Long? = null): Hadith {
        // Strictly pick from downloaded hadith books
        try {
            val now = System.currentTimeMillis()
            // Reuse cached hadiths if scanned in the last 60 seconds
            var hadithList = cachedDownloadedHadiths
            if (hadithList == null || (now - lastHadithScanTime) > 60_000L) {
                val hadithsDir = context?.let { File(it.filesDir, "hadiths") }
                if (hadithsDir != null && hadithsDir.exists()) {
                    val files = hadithsDir.listFiles { _, name -> 
                        (name.endsWith("_urdu.json") || name.endsWith("_urd.json") || name.endsWith("_eng.json") || name.endsWith(".json")) &&
                        !name.endsWith("_ara.json") && File(hadithsDir, name).length() > 5000
                    }
                    if (!files.isNullOrEmpty()) {
                        val collected = ArrayList<Hadith>(200)
                        for (file in files) {
                            try {
                                val root = JSONObject(file.readText(Charsets.UTF_8))
                                val hadithsArray = root.optJSONArray("hadiths") ?: continue
                                val count = hadithsArray.length()
                                if (count == 0) continue

                                val bookSlug = file.name.substringBefore("_")
                                val bookName = AuthoritativeContentProvider.hadithBooks.firstOrNull { it.slug == bookSlug }?.englishName ?: "Hadith"
                                
                                val araFile = File(hadithsDir, "${bookSlug}_ara.json").takeIf { it.exists() && it.length() > 5000 }
                                val araArray = if (araFile != null) {
                                    try { JSONObject(araFile.readText(Charsets.UTF_8)).optJSONArray("hadiths") } catch (_: Throwable) { null }
                                } else null

                                val step = if (count > 200) count / 100 else 1
                                for (idx in 0 until count step step) {
                                    val hObj = hadithsArray.getJSONObject(idx)
                                    val text = hObj.optString("text", "").trim()
                                    if (text.isNotBlank()) {
                                        val hNum = hObj.optString("hadithnumber").ifBlank {
                                            hObj.optString("hadithNumber", (idx + 1).toString())
                                        }.removeSuffix(".0").trim()

                                        var arabicText = hObj.optString("arabic", "")
                                        if (arabicText.isBlank() && araArray != null && idx < araArray.length()) {
                                            arabicText = araArray.getJSONObject(idx).optString("text", "")
                                        }
                                        if (arabicText.isBlank()) {
                                            arabicText = "قال رسول الله ﷺ: $text"
                                        }

                                        collected.add(
                                            Hadith(
                                                id = (bookSlug.hashCode().toLong() shl 20) + idx,
                                                bookSlug = bookSlug,
                                                bookName = bookName,
                                                chapterNumber = 1,
                                                chapterNameArabic = "كتاب الحديث",
                                                chapterNameUrdu = "حدیث شریف",
                                                hadithNumber = hNum,
                                                arabicText = arabicText,
                                                urduTranslation = text,
                                                englishTranslation = text,
                                                activeTranslation = text
                                            )
                                        )
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                        if (collected.isNotEmpty()) {
                            hadithList = collected
                            cachedDownloadedHadiths = collected
                            lastHadithScanTime = now
                        }
                    }
                }
            }

            if (!hadithList.isNullOrEmpty()) {
                val pool = if (excludeId != null && hadithList.size > 1) {
                    hadithList.filter { it.id != excludeId }.ifEmpty { hadithList }
                } else {
                    hadithList
                }
                return pool.random()
            }
        } catch (_: Throwable) {
            // fallback
        }

        val pool = if (excludeId != null && inspiringHadiths.size > 1) {
            inspiringHadiths.filter { it.id != excludeId }.ifEmpty { inspiringHadiths }
        } else {
            inspiringHadiths
        }
        return pool.random()
    }

    fun getRandomWord(excludeWord: String? = null): AuthoritativeContentProvider.WordOfTheMoment {
        val pool = if (excludeWord != null && inspiringWords.size > 1) {
            inspiringWords.filter { it.arabicWord != excludeWord }.ifEmpty { inspiringWords }
        } else {
            inspiringWords
        }
        return pool.random()
    }

    fun getRandomDua(excludeId: String? = null): DuaItem {
        val pool = if (excludeId != null && inspiringDuas.size > 1) {
            inspiringDuas.filter { it.id != excludeId }.ifEmpty { inspiringDuas }
        } else {
            inspiringDuas
        }
        return pool.random()
    }
}
