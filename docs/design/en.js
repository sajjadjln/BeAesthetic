/* English for the mockups (screens.html, flows.html). The app itself uses string resources; this mirrors them.
   MockupI18n.set("en") turns every .phone left-to-right, translates its text and labels, uses Latin digits,
   puts Gregorian dates first, and mirrors what runs along time (inline right offsets, [data-mirror] charts).
   Lookup: the exact text first, then the text with each number replaced by {n} (values use {0}, {1}… in order).
   Anything still Persian is listed by MockupI18n.missing(). */
(function () {
  "use strict";
  var D = {
    // ----- dates (exact, Gregorian first) -----
    "۱۸ آبان · هفته ۵ از ۲۳ · فصل آبان": "9 Nov · 18 Aban · Week 5 of 23 · Aban",
    "۱۸ مهر تا ۲۹ اسفند ۱۴۰۵": "10 Oct 2026 – 20 Mar 2027",
    "۱۶ تا ۲۲ آبان": "7–13 Nov", "۹ تا ۱۵ آبان": "31 Oct – 6 Nov", "۲ تا ۸ آبان": "24–30 Oct",
    "۲۵ مهر تا ۱ آبان": "17–23 Oct", "۱۸ تا ۲۴ مهر": "10–16 Oct",
    "چک‌این ماهانه: جمعه ۲۰ آذر": "Monthly check-in: Fri 11 Dec",
    "جمعه ۱۵ آبان": "Fri 6 Nov", "جمعه ۸ آبان": "Fri 30 Oct", "جمعه ۱ آبان": "Fri 23 Oct", "جمعه ۱۷ مهر": "Fri 9 Oct",
    "جمعه ۱۵ آبان ۱۴۰۵": "Fri 6 Nov 2026",
    "جمعه ۱۵ آبان · ۴۸ مگ · ۹ عکس": "Fri 6 Nov · 48 MB · 9 photos",
    "۱۷ مهر ۱۴۰۵": "9 Oct 2026", "۲۹ اسفند ۱۴۰۵": "20 Mar 2027", "۱۷ مهر": "9 Oct", "۱۸ مهر": "10 Oct", "۲۰ آذر": "11 Dec",
    "۲۰ آذر · هفته ۹ از ۲۳ · فصل آذر": "11 Dec · 20 Azar · Week 9 of 23 · Azar",
    "۱ آذر · هفته ۷ از ۲۳ · فصل آذر": "22 Nov · 1 Azar · Week 7 of 23 · Azar",
    "۱ دی · هفته ۱۱ از ۲۳ · فصل دی": "22 Dec · 1 Dey · Week 11 of 23 · Dey",
    "۳ دی · هفته ۱۱ از ۲۳ · فصل دی": "24 Dec · 3 Dey · Week 11 of 23 · Dey",
    "کیلو · قبلی ۷۷٫۱ (جمعه ۱۳ آذر)": "kg · last 77.1 (Fri 4 Dec)",
    "دیروز · دوشنبه ۱۸ آبان": "Yesterday · Mon 9 Nov",
    "9 Nov 2026 · هفته ۵": "18 Aban 1405 · Week 5",
    "نوشته: ۱ آذر": "Written: 22 Nov", "باز می‌شه: ۱ دی": "Opens: 22 Dec",
    "بعد از مهر و موم، تا ۱ دی نه می‌شه خوندش نه عوضش کرد. فقط می‌شه پاکش کرد.": "Once sealed, it can't be read or changed until 22 Dec. It can only be deleted.",
    "یه نامه از ۱ آذر برات اومده": "A letter from 22 Nov is here", "از خودِ ۱ آذر": "From you, 22 Nov",
    "یه نامه برای اول دی": "A letter for 22 Dec", "بعدی: ۱ دی": "Next: 22 Dec",
    "۲۹ اسفند ۱۴۰۵ · خط پایان": "20 Mar 2027 · The finish",
    "رسیدی · ۱۹ آذر": "Reached · 10 Dec", "رسیدی · ۳ آذر": "Reached · 24 Nov",
    "شبح آبان": "Ghost: Aban", "مهر · روز صفر": "Mehr · Day 0", "نوروز ۱۴۰۶": "Nowruz 1406",
    "{n} مهر": "{0} Mehr", "{n} آبان": "{0} Aban", "{n} آذر": "{0} Azar", "{n} دی": "{0} Dey",

    // ----- app shell -----
    "پیکر": "Peykar", "امروز": "Today", "داشبورد": "Dashboard", "پیشرفت": "Progress", "هفته": "Week", "ناوبری": "Navigation",
    "تنظیمات": "Settings", "روز حداقلی": "Minimum day", "این هفته": "This week", "بخش روز": "Part of day",
    "صبح": "Morning", "ظهر": "Midday", "عصر": "Evening", "شب": "Night", "بستن": "Close", "بازگشت": "Back", "برگشت": "Back",
    "{n}٪": "{0}%", "از {n}": "of {0}", "روز {n} از {n}": "Day {0} of {1}", "{n} از {n}": "{0} of {1}", "هفته {n}": "Week {0}", "{n} روز": "{0} days", "روز {n}": "Day {0}",
    "شنبه": "Saturday", "یکشنبه": "Sunday", "دوشنبه": "Monday", "سه‌شنبه": "Tuesday", "چهارشنبه": "Wednesday",
    "پنج‌شنبه": "Thursday", "پنجشنبه": "Thursday", "جمعه": "Friday",
    "ش": "Sa", "ی": "Su", "د": "Mo", "س": "Tu", "چ": "We", "پ": "Th", "ج": "Fr",
    "مهر": "Mehr", "آبان": "Aban", "آذر": "Azar", "دی": "Dey", "بهمن": "Bahman", "اسفند": "Esfand", "مهر–آبان": "Mehr–Aban",

    // ----- Today -----
    "پوست": "Skin", "مهم": "Important", "مسواک": "Brush", "کراتین": "Creatine", "پروتئین": "Protein", "ویتامین D": "Vitamin D",
    "تمرین B": "Workout B", "ناخن": "Nails", "موعدش رسیده": "Due", "میان‌وعده": "Snack", "مسواک شب": "Night brush", "{n} شنا": "{0} push-ups",
    "صبح بخیر! اول یه لیوان آب.": "Morning! Glass of water first.",
    "{n} تا زدی. داره خوب پیش می‌ره!": "{0} done. Looking good!",
    "صبحت کامل شد! ایول.": "Morning done! Nice.", "ظهرت کامل شد! ایول.": "Midday done! Nice.",
    "عصرت کامل شد! ایول.": "Evening done! Nice.", "شبت کامل شد! ایول.": "Night done! Nice.",
    "امروز کامل شد! این بازوها مال خودته.": "Day complete! Those arms are yours.",
    "امروز سبک. همینم حسابه.": "Light day. It still counts.",
    "بازوهامو دیدی؟ مال تو هم داره می‌شه.": "Seen my arms? Yours are coming.",
    "آب خوردی؟": "Had water yet?", "دست نزن، دارم پمپ می‌کنم!": "Hands off, I'm pumping!",

    // ----- Dashboard -----
    "روز حضور": "Days shown up", "روز ویژه": "Special day", "روزهای ویژه": "Special days", "کل مسیر": "The whole program",
    "کمتر": "Less", "بیشتر": "More", "ویژه": "Special", "قبل از اپ": "Before the app", "هفته به هفته": "Week by week", "جاری": "Now",

    // ----- Progress -----
    "از روز صفر · {n} روز": "Since Day 0 · {0} days", "وزن، متر، {n} عکس": "Weight, tape, {0} photos",
    "بارفیکس": "Pull-ups", "شنا سوئدی": "Push-ups", "پلانک": "Plank", "ث": "s", "+{n}": "+{0}", "+{n}ث": "+{0}s", "از {n}ث": "from {0}s",
    "دور کمر": "Waist", "سانت": "cm", "−{n}": "−{0}", "رکورد": "Record", "وزن": "Weight", "{n} کیلو": "{0} kg",
    "عکس‌های ماهانه": "Monthly photos", "جلو": "Front", "بغل": "Side", "پشت": "Back",
    "بارفیکس: روز صفر {n}، هفته {n} {n}، هفته {n} {n}": "Pull-ups: Day 0 {0}, week {1} {2}, week {3} {4}",
    "روند وزن پنج هفته": "Weight over five weeks",

    // ----- Workout -----
    "پله‌ی {n} از {n} · بعدی: الماسی": "Rung {0} of {1} · next: diamond", "قبلی {n} · {n} · {n}": "Last {0} · {1} · {2}",
    "درجه سختی": "How hard", "حرکت {n} از {n}": "Exercise {0} of {1}", "یکی بیشتر": "One more", "یکی کمتر": "One less", "ثبت": "Log",
    "آسون": "Easy", "خیلی سخت": "Very hard",

    // ----- Week -----
    "مرور و برنامه‌ی هفته": "Review and plan", "مرور": "Review", "برنامه": "Plan",
    "بارفیکس {n} (قبلی {n})": "Pull-ups {0} (was {1})", "پوست شب {n} از {n}": "Night skin {0} of {1}", "{n} بار برگشتی": "Came back {0} times",
    "بارفیکس {n} تا! هفته‌ی خوبی بود.": "{0} pull-ups! Good week.", "حداکثر {n}": "Max {0}", "سقف پر است": "That's the limit", "سفر": "Travel",
    "میان‌وعده‌ها": "Snacks", "شروع هفته {n}": "Start week {0}", "چی آماده کنم؟": "What should I prep?",
    "میان‌وعده‌ی شنبه": "Saturday snack", "میان‌وعده‌ی یکشنبه": "Sunday snack", "میان‌وعده‌ی دوشنبه": "Monday snack",
    "میان‌وعده‌ی سه‌شنبه": "Tuesday snack", "میان‌وعده‌ی چهارشنبه": "Wednesday snack", "میان‌وعده‌ی پنج‌شنبه": "Thursday snack", "میان‌وعده‌ی جمعه": "Friday snack",
    "ماست یونانی، یک مشت گردو، کمی عسل.": "Greek yogurt, a handful of walnuts, a little honey.",
    "توپ انرژی جو دوسر: {n} پیمانه جو دوسر، {n} قاشق کره بادام‌زمینی، {n} قاشق عسل، کمی کاکائو. هم بزن، گلوله کن، {n} دقیقه یخچال.": "Oat energy balls:\n{0} cup oats, {1} spoons peanut butter, {2} spoon honey, a little cocoa. Stir, roll, {3} minutes in the fridge.",
    "توپ انرژی (از یکشنبه)": "Energy balls (from Sunday)", "{n} تخم‌مرغ آب‌پز و خیار": "{0} boiled eggs and a cucumber",
    "میوه و آجیل برای کوه": "Fruit and nuts for the hike",

    // ----- Settings, appearance -----
    "یادآورها سالم‌اند": "Reminders are healthy", "کارها": "Tasks", "یادآورها": "Reminders", "برنامه تمرین": "Workout plan",
    "پشتیبان": "Backup", "ظاهر و زبان": "Appearance & language", "گوشی": "Phone", "نسخه {n} · BeAesthetic": "Version {0} · پیکر",
    "رنگ": "Color", "سبز": "Green", "آبی": "Blue", "فیروزه‌ای": "Turquoise", "بنفش": "Violet", "نارنجی": "Orange",
    "{n} از {n} روز": "{0} of {1} days", "حالت": "Mode", "سیستم": "System", "تیره": "Dark", "روشن": "Light", "زبان": "Language",
    "فارسی": "فارسی", "ذخیره": "Save", "نمونه داشبورد": "Dashboard sample",

    // ----- Tasks, editor, reminders, plan -----
    "همه": "All", "یک‌باره": "One-off", "خرید جو دوسر و گردو": "Buy oats and walnuts", "پنج‌شنبه · عصر": "Thursday · Evening",
    "روتین پوست صبح": "Morning skincare", "هر روز": "Every day", "مسواک صبح": "Morning brush", "کراتین {n} گرم": "Creatine {0} g",
    "هر روز · زنگ {n}": "Every day · alarm {0}", "کوتاه کردن ناخن": "Trim nails", "هر {n} روز": "Every {0} days", "شامپو": "Shampoo",
    "کار جدید": "New task", "فیلتر": "Filter", "روزهای خاص": "Some days", "یک بار": "Once", "بعد از…": "After…", "ناهار": "Lunch",
    "زنگ": "Alarm", "ساعت": "Time", "نام": "Name", "مکمل": "Supplement", "دوش": "Shower", "قیچی": "Scissors", "خوراکی": "Food", "تکرار": "Repeat",
    "زنگ روز ش": "Alarm on Saturday", "زنگ روز ی": "Alarm on Sunday", "زنگ روز د": "Alarm on Monday", "زنگ روز س": "Alarm on Tuesday",
    "زنگ روز چ": "Alarm on Wednesday", "زنگ روز پ": "Alarm on Thursday", "زنگ روز ج": "Alarm on Friday",
    "آزمایش": "Test", "روزهای عادی": "Normal days", "زنگ‌های کارها": "Task alarms", "یادآور دوباره برای «مهم»": "Follow-up for “Important”",
    "{n} دقیقه": "{0} min", "مرور جمعه": "Friday review", "نوع روز": "Day type", "کوه": "Hike",
    "اسکوات": "Squat", "{n} × {n}–{n}": "{0} × {1}–{2}", "ثانیه": "Seconds", "ست": "Sets", "از": "From", "تا": "To",
    "لانج": "Lunge", "پل باسن": "Glute bridge", "ساق پا": "Calf raise", "{n} × {n}–{n}ث": "{0} × {1}–{2}s",
    "حرکت جدید": "New exercise", "نام تمرین": "Workout name", "تمرین A · پایین‌تنه": "Workout A · Lower body",

    // ----- Onboarding -----
    "سلام! من پیکرم.": "Hi! I'm Peykar.", "{n} هفته، با هم": "{0} weeks, together",
    "هر روز می‌گم چی کار کنی، به‌موقع یادت می‌ندازم، و همه‌چی رو روی گوشی خودت نگه می‌دارم.": "Every day I'll tell you what to do, remind you on time, and keep everything on your own phone.",
    "بزن بریم": "Let's go", "مسیرت": "Your program", "شروع و پایان برنامه": "Start and end", "روز صفر": "Day 0", "خط پایان": "Finish line",
    "{n} هفته · {n} فصل": "{0} weeks · {1} chapters", "شب نوروز": "Nowruz eve",
    "روز صفر قبل از نصب بوده؟ اشکالی نداره. اون روزها «قبل از اپ» حساب می‌شن، نه جای خالی.": "Day 0 was before you installed? That's fine. Those days count as “Before the app”, not as gaps.",
    "ادامه": "Continue", "یه اعلان برای هر بخش روز، نه برای هر کار": "One notification per part of the day, not per task",
    "ساعت خواب": "Bedtime", "یادآور شب نیم ساعت قبلشه": "The night reminder comes half an hour before",
    "هر وقت خواستی، از تنظیمات عوضشون کن.": "Change them in Settings any time.",
    "گوشی‌ات رو آماده کن": "Get your phone ready", "شیائومی اپ‌ها رو می‌بنده. این {n} تا جلوشو می‌گیره.": "Xiaomi closes apps. These {0} stop it.",
    "اعلان‌ها": "Notifications", "روشنه": "On", "زنگ دقیق": "Exact alarms", "اجرای خودکار": "Autostart", "Autostart · روشنش کن": "Turn it on",
    "باز کن": "Open", "باتری": "Battery", "«بدون محدودیت» رو بزن": "Pick “No restrictions”", "قفل در اپ‌های اخیر": "Lock in recent apps",
    "اپ رو پایین بکش و قفلش کن": "Pull the app down and lock it", "چطوری؟": "How?", "اعلان آزمایشی، یه دقیقه‌ی دیگه": "Test notification in a minute",
    "اینا روشن باشه، هیچ یادآوری گم نمی‌شه.": "With these on, no reminder gets lost.",
    "هیچ‌چی گم نمی‌شه، حتی اگه گوشی عوض بشه": "Nothing gets lost, even with a new phone", "پوشه‌ی پشتیبان": "Backup folder", "انتخاب شد": "Chosen",
    "پشتیبان خودکار": "Automatic backup", "آخر مرور جمعه و بعد از هر چک‌این": "After the Friday review and every check-in",
    "نگه‌داشتن": "Keep", "{n} تای آخر": "Last {0}",
    "این پوشه رو با Syncthing به کامپیوترت وصل کن تا یه نسخه هم اونجا باشه. اپ اینترنت نداره و چیزی رو جایی نمی‌فرسته.": "Sync this folder to your computer with Syncthing so there's a copy there too. The app has no internet and sends nothing anywhere.",
    "نقطه‌ی شروع، برای اینکه پیشرفت دیده بشه": "Where you started, so progress shows", "کیلو": "kg", "{n} اندازه‌ی متر": "{0} tape measurements",
    "کمر {n} · سینه {n} · …": "Waist {0} · Chest {1} · …", "تست‌های قدرت": "Strength tests", "بارفیکس {n} · شنا {n} · …": "Pull-ups {0} · Push-ups {1} · …",
    "{n} عکس روز صفر": "{0} Day 0 photos", "پشت · از گالری": "Back · from gallery", "تمرین‌های قبل از نصب": "Workouts before install",
    "اختیاری · {n} جلسه اضافه شد": "Optional · {0} sessions added", "برنامه‌ی تمرین": "Workout plan", "متن برنامه‌ات رو اینجا بچسبون": "Paste your plan here",
    "شنبه - تمرین A اسکات {n}×{n}-{n} شنا سوئدی {n}×{n}-{n} پلانک {n}×{n}ثانیه دوشنبه - تمرین B …": "Saturday - Workout A\nSquat {0}×{1}-{2}\nPush-ups {3}×{4}-{5}\nPlank {6}×{7}s\nMonday - Workout B …",
    "اینو فهمیدم": "Here's what I understood", "تمرین A": "Workout A", "اسکات · شنا سوئدی · پلانک": "Squat · Push-ups · Plank", "{n} حرکت": "{0} exercises",
    "بارفیکس · دیپ · آویزان": "Pull-ups · Dips · Dead hang", "تمرین C": "Workout C", "لانج · پل باسن · شنا الماسی": "Lunge · Glute bridge · Diamond push-ups",
    "بدون ثبت ست": "No sets logged", "درسته، ادامه": "Looks right, continue", "خودم دستی می‌سازم": "I'll build it by hand", "متن برنامه": "Plan text",
    "برای چی؟": "What for?", "یه جمله برای خودت، و چند تا هدف واقعی": "A sentence for yourself, and a few real goals",
    "من کسی‌ام که هر روز سر قولش با خودش هست.": "I'm someone who keeps the promise to myself, every day.",
    "هدف‌ها · {n} تا انتخاب شد": "Goals · {0} chosen", "{n} بارفیکس کامل": "{0} strict pull-ups", "دور کمر −{n} سانت": "Waist −{0} cm",
    "{n} دقیقه و نیم پلانک": "{0}.5 min plank", "{n} شنا پشت سر هم": "{0} push-ups in a row", "{n} روز حضور": "{0} days shown up",
    "شنا آرچر": "Archer push-ups", "{n} دقیقه آویزان": "{0} min dead hang", "هدف خودم": "My own goal", "عالیه! بریم سراغ امروز.": "Great! On to today.",
    "تموم": "Done", "جمله‌ی من": "My sentence",

    // ----- Notifications, phone -----
    "پیکر · صبح": "BeAesthetic · Morning", "الان": "now", "صبح · {n} کار": "Morning · {0} tasks", "پوست · مسواک · کراتین · اسکین‌کر": "Skin · Brush · Creatine · Skincare",
    "بعد از مسواک": "after brushing", "پیکر · مهم": "BeAesthetic · Important", "{n} دقیقه پیش": "{0} min ago", "پوست صبح": "Morning skin",
    "هنوز وقتش هست.": "There's still time.", "انجام شد": "Done", "پیکر · زنگ کار": "BeAesthetic · Task alarm", "با صبحونه": "With breakfast",
    "پیکر · چک‌این": "BeAesthetic · Check-in", "جمعه {n}": "Fri {0}", "امروز چک‌این ماهانه‌ست": "Monthly check-in today",
    "قبل از صبحونه: وزن، متر، {n} عکس · {n} دقیقه": "Before breakfast: weight, tape, {0} photos · {1} min",
    "{n} از {n} به‌موقع": "{0} of {1} on time", "یکی {n} دقیقه دیر رسید: سه‌شنبه، ظهر. احتمالاً صرفه‌جویی باتری.": "One came {0} min late: Tuesday, midday. Probably battery saver.",
    "دوباره روی «صرفه‌جویی» رفته": "Back on “Battery saver”", "اعلان آزمایشی": "Test notification",
    "روی صفحه‌ی امروز این‌طوری خبر می‌ده": "This is how Today tells you", "یه یادآور دیر رسید": "A reminder came late", "درستش کن": "Fix it",
    "یادآورهای این هفته": "This week's reminders",

    // ----- Check-in -----
    "چک‌این آذر": "Azar check-in", "وزن، {n} اندازه، {n} عکس · حدود {n} دقیقه": "Weight, {0} measurements, {1} photos · about {2} min",
    "قبل از صبحونه": "Before breakfast", "همون نور": "Same light", "همون لباس": "Same clothes", "شروع": "Start",
    "امروز روز عکسه! همون جا، همون نور.": "Photo day! Same spot, same light.", "متر": "Tape", "عکس": "Photos", "وزن امروز": "Today's weight",
    "صبح، ناشتا، بعد از دستشویی": "Morning, before eating, after the bathroom", "بعدی": "Next", "{n} اندازه": "{0} measurements",
    "متر نرم، روی پوست، بدون فشار": "Soft tape, on skin, no squeezing", "هم‌سطح ناف": "At the navel", "قبلی {n}": "last {0}",
    "دور سینه": "Chest", "زیر بغل": "Under the armpits", "دور بازو": "Arm", "راست، منقبض": "Right, flexed", "دور ران": "Thigh",
    "راست، وسط": "Right, middle", "دور شانه": "Shoulders", "پهن‌ترین جا": "Widest point",
    "هر عدد با آخری پر شده؛ فقط اونایی که فرق کردن رو عوض کن.": "Each number starts at the last one; change only what changed.",
    "جلو · {n} از {n}": "Front · {0} of {1}", "خودت رو با خط‌ها جفت کن": "Line yourself up with the outline", "همون جا": "Same spot",
    "همون فاصله": "Same distance", "بی فلش": "No flash", "{n}ث": "{0}s", "ماه قبل": "Last month",
    "کمر −{n} از روز صفر": "Waist −{0} since Day 0", "شانه +{n}": "Shoulders +{0}", "وزن −{n}": "Weight −{0}",
    "واای! روز صفرو ببین، الانو ببین.": "Whoa! Look at Day 0, look at now.", "مقایسه": "Compare",

    // ----- Strength test -----
    "تست قدرت · هفته {n}": "Strength test · week {0}", "{n} تست، با {n} دقیقه استراحت بین‌شون · حدود {n} دقیقه": "{0} tests, {1} min rest between · about {2} min",
    "قبلی {n}ث": "last {0}s", "آویزان": "Dead hang", "اسکات یک‌پا": "Single-leg squat", "بارفیکس {n} تا! بعدی شنا.": "{0} pull-ups! Push-ups next.",
    "بعدی: شنا سوئدی": "Next: push-ups", "حدس بزن چند تا می‌زنی؟": "Guess how many you'll do?", "اختیاری. با آخری پر شده.": "Optional. Starts at your last result.",
    "دفعه‌ی قبل {n} · روز صفر {n}": "Last time {0} · Day 0 {1}", "حدسم همینه، برو": "That's my guess, go", "بی‌حدس": "No guess",
    "حدس: {n}ث": "Guess: {0}s", "قبلی {n} · رد شد": "Last {0} · passed", "خط کوچیک روی حلقه = رکورد قبلی": "The small mark on the ring = your last result",
    "حدس": "Guess", "واقعی": "Actual", "از حدست بهتر": "Beat your guess", "+{n} از روز صفر": "+{0} since Day 0",
    "{n} تا! از حدست بهتر.": "{0}! You beat your guess.", "اگه کمتر بشه: فقط دو عدد، کنار هم، بی‌رنگ. همین.": "If it's lower: just the two numbers, side by side, no color. That's all.",
    "بعدی: پلانک": "Next: plank",

    // ----- Day detail, backup -----
    "{n} از {n} · سطح {n} از {n}": "{0} of {1} · level {2} of {3}", "پروتئین ×{n}": "Protein ×{0}", "تمرین B · رکورد بارفیکس": "Workout B · pull-up record",
    "روز ویژه بود؟": "Was it a special day?", "مریض": "Sick", "استراحت": "Off", "ویرایش تیک‌های دیروز": "Edit yesterday's ticks",
    "آخرین پشتیبان: {n} روز پیش": "Last backup: {0} days ago", "همین الان پشتیبان بگیر": "Back up now", "پوشه": "Folder", "عوض کن": "Change",
    "خودکار": "Automatic", "بعد از مرور هفته": "After the weekly review", "{n} مگ": "{0} MB", "بازگردانی از فایل": "Restore from a file",
    "بازگردانی این فایل؟": "Restore this file?", "تاریخ": "Date", "نسخه‌ی اپ": "App version", "روزهای ثبت‌شده": "Days logged", "تمرین": "Workouts",
    "{n} جلسه": "{0} sessions", "اندازه‌ها": "Measurements",
    "اول از همه‌ی داده‌های فعلی یه پشتیبان می‌گیرم، بعد جایگزین می‌کنم. اگه چیزی وسطش خراب شد، همه‌چی همون‌طور که بود می‌مونه.": "First I back up everything you have now, then I replace it. If anything fails halfway, everything stays as it was.",
    "جایگزین کن": "Replace", "نه": "No",

    // ----- Chapters, letters -----
    "فصل آذر! هدبندمو بستم.": "Azar chapter! Headband on.", "فصل ② آذر شروع شد": "Chapter ② Azar has started",
    "{n} روز، یه شروع تازه. هیچ عددی صفر نمی‌شه.": "{0} days, a fresh start. No number goes back to zero.", "بعداً": "Later",
    "نامه به خودم": "Letter to myself", "مهر و موم کن": "Seal it", "متن نامه": "Letter text",
    "سلام خودم، الان اول آذره. بارفیکس {n} تاست و پلانک یه دقیقه‌ی کامل نمی‌شه. اگه داری اینو می‌خونی یعنی یه ماه دیگه هم رفتی. یادته مهر چقدر سخت بود از جات پاشی؟ یه چیز: هفته‌ای که مریض شدی رو یادت نره؛ برگشتی. همین مهمه.": "Hi me,\nIt's the start of Azar. Pull-ups are at {0} and I can't hold a full minute of plank.\nIf you're reading this, you kept going for another month. Remember how hard it was to get up in Mehr?\nOne thing: don't forget the week you were sick. You came back. That's what counts.",
    "سلام خودم،": "Hi me,", "الان اول آذره. بارفیکس {n} تاست و پلانک یه دقیقه‌ی کامل نمی‌شه.": "It's the start of Azar. Pull-ups are at {0} and I can't hold a full minute of plank.",
    "اگه داری اینو می‌خونی یعنی یه ماه دیگه هم رفتی. یادته مهر چقدر سخت بود از جات پاشی؟": "If you're reading this, you kept going for another month. Remember how hard it was to get up in Mehr?",
    "یه چیز: هفته‌ای که مریض شدی رو یادت نره؛ برگشتی. همین مهمه.": "One thing: don't forget the week you were sick. You came back. That's what counts.",
    "بزن تا باز بشه": "Tap to open", "بارفیکس امروز: {n}": "Pull-ups today: {0}", "پلانک: {n}": "Plank: {0}",
    "نامه داری! از خودت.": "You've got a letter! From you.", "باز کردن نامه": "Open the letter",

    // ----- Recaps, finish -----
    "یلدا · نصف راه": "Yalda · Halfway", "از {n} روز، اومدی.": "Of {0} days, you showed up.", "نصف راهو اومدیم! یلدا مبارک.": "Halfway there! Happy Yalda.",
    "بزن برای بعدی": "Tap for next", "از روز صفر تا امروز": "From Day 0 to today", "شنا تا حالا": "push-ups so far", "جلسه تمرین": "workouts",
    "رسیدی: {n} بارفیکس کامل": "Reached: {0} strict pull-ups", "رسیدی: {n} دقیقه پلانک": "Reached: {0} min plank", "روز صفر و حالا": "Day 0 and now",
    "«من کسی‌ام که هر روز سر قولش با خودش هست.»": "“I'm someone who keeps the promise to myself, every day.”", "جمله‌ی خودت، از روز اول": "Your own sentence, from day one",
    "{n} هفته! تو انجامش دادی.": "{0} weeks! You did it.", "از {n} روز، اومدی. فردا نوروزه.": "Of {0} days, you showed up. Tomorrow is Nowruz.",
    "بعدش چی؟": "What's next?", "هر کدوم، همه‌ی تاریخچه‌ات می‌مونه": "Either way, all your history stays",
    "کارها و یادآورها همون‌طور می‌مونن. تمرین اختیاری، بدون فصل و شمارش معکوس.": "Tasks and reminders stay as they are. Workouts optional, no chapters or countdown.",
    "برنامه‌ی تازه": "New program", "تاریخ شروع و پایان جدید. کارها و برنامه‌ی تمرین با خودت میاد.": "New start and end dates. Your tasks and workout plan come along.",
    "من که هستم. تو بگو.": "I'm in. You choose.",

    // ----- Offers, goals, progress additions -----
    "مسواک شب دیگه عادت شده": "Night brushing is a habit now",
    "{n} هفته‌ست سر جاشه. یادآورش رو خاموش کنم؟ تو امروز می‌مونه.": "It's held for {0} weeks. Turn off its reminder? It stays on Today.",
    "خاموش کن": "Turn off", "بمونه": "Keep it", "اینو دیگه خودت بلدی!": "You've got this one!", "هدف‌ها": "Goals",
    "{n} دقیقه پلانک": "{0} min plank", "{n} از {n}ث": "{0} of {1}s", "پله‌ی {n} از {n} · الان پله‌ی {n}": "Rung {0} of {1} · now rung {2}",
    "هدف جدید": "New goal", "از چی خونده بشه؟": "Measured by", "شنا سوئدی · تست": "Push-ups · test", "≥ {n}": "≥ {0}", "اسم هدف": "Goal name",
    "مسیر": "The program", "{n} روز تا نوروز": "{0} days to Nowruz", "یلدا": "Yalda", "پله‌های شنا": "Push-up ladder", "{n} تا تا حالا": "{0} so far",
    "زانو": "Knee", "معمولی": "Standard", "الماسی": "Diamond", "آرچر": "Archer", "پله‌های بارفیکس": "Pull-up ladder", "منفی": "Negative",
    "با کش": "Band", "کامل": "Strict", "جلسه": "Workouts", "پلانک، کل": "Plank, total", "{n} نامه‌ی مهر و موم": "{0} sealed letters",
    "{n} از {n} · بعدی: دور کمر −{n}": "{0} of {1} · next: waist −{2}"
  };

  var FA = "۰۱۲۳۴۵۶۷۸۹", AR = /[؀-ٟ٭-ۯۺ-ۿ]/;
  var NUM = /[0-9۰-۹]+(?:[٫٬.,:][0-9۰-۹]+)*/g;
  var miss = {};
  function norm(s) { return s.replace(/\s+/g, " ").trim(); }
  function lat(s) { return s.replace(/[۰-۹]/g, function (c) { return FA.indexOf(c); }).replace(/٫/g, ".").replace(/٬/g, ",").replace(/٪/g, "%"); }
  function tr(raw) {
    var k = norm(raw);
    if (!k) return null;
    if (D.hasOwnProperty(k)) return D[k];
    var nums = [], t = k.replace(NUM, function (m) { nums.push(lat(m)); return "{n}"; });
    if (D.hasOwnProperty(t)) return D[t].replace(/\{(\d)\}/g, function (m, d) { return nums[+d]; });
    if (!AR.test(k)) return lat(k);
    miss[k] = 1;
    return null;
  }

  var lang = "fa", ATTRS = ["aria-label", "placeholder", "title"];
  function trText(n) {
    if (n.parentNode && n.parentNode.closest && n.parentNode.closest("script,style,[data-en]")) return;
    if (n.__en !== undefined && n.data === n.__en) return;
    n.__fa = n.data;
    var g = n.parentNode && n.parentNode.getAttribute && n.parentNode.getAttribute("data-g");
    var v = g && !AR.test(n.data) ? g : tr(n.data);
    if (v === null) return;
    var lead = n.data.match(/^\s*/)[0], trail = n.data.match(/\s*$/)[0];
    n.__en = lead + v + trail;
    n.data = n.__en;
  }
  function trEl(el) {
    ATTRS.forEach(function (a) {
      var v = el.getAttribute(a);
      if (v === null || !AR.test(v)) return;
      var t = tr(v); if (t === null) return;
      el["__fa_" + a] = v; el.setAttribute(a, t);
    });
    if ((el.tagName === "TEXTAREA" || (el.tagName === "INPUT" && el.type !== "range")) && AR.test(el.value)) {
      var t = tr(el.value); if (t !== null) { el.__faVal = el.value; el.value = t; }
    }
    if (el.hasAttribute("data-en") && el.__faHTML === undefined) { el.__faHTML = el.innerHTML; el.textContent = el.getAttribute("data-en"); }
    var st = el.getAttribute("style");
    if (st && /(^|;)\s*(right|text-align:\s*right)/.test(st) && el.__faStyle === undefined) {
      el.__faStyle = st;
      el.setAttribute("style", st.replace(/(^|;)(\s*)right:/g, "$1$2left:").replace(/text-align:\s*right/g, "text-align:left"));
    }
    if (el.hasAttribute("data-mirror") && el.__faMirror === undefined) {
      var w = +el.getAttribute("viewBox").split(/\s+/)[2];
      el.__faMirror = el.innerHTML;
      Array.prototype.forEach.call(el.children, function (c) {
        if (c.tagName === "text") c.setAttribute("x", w - +c.getAttribute("x"));
        else c.setAttribute("transform", "matrix(-1 0 0 1 " + w + " 0)");
      });
    }
  }
  function walk(root) {
    if (root.nodeType === 3) { trText(root); return; }
    if (root.nodeType !== 1) return;
    trEl(root);
    root.querySelectorAll("*").forEach(trEl);
    var w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT), n, list = [];
    while ((n = w.nextNode())) list.push(n);
    list.forEach(trText);
  }
  function unwalk(root) {
    root.querySelectorAll("*").forEach(function (el) {
      if (el.__faHTML !== undefined) { el.innerHTML = el.__faHTML; delete el.__faHTML; }
      if (el.__faMirror !== undefined) { el.innerHTML = el.__faMirror; delete el.__faMirror; }
      if (el.__faStyle !== undefined) { el.setAttribute("style", el.__faStyle); delete el.__faStyle; }
      ATTRS.forEach(function (a) { if (el["__fa_" + a] !== undefined) { el.setAttribute(a, el["__fa_" + a]); delete el["__fa_" + a]; } });
      if (el.__faVal !== undefined) { el.value = el.__faVal; delete el.__faVal; }
    });
    var w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT), n;
    while ((n = w.nextNode())) if (n.__fa !== undefined && n.data === n.__en) { n.data = n.__fa; delete n.__fa; delete n.__en; }
  }

  var obs = new MutationObserver(function (recs) {
    if (lang !== "en") return;
    recs.forEach(function (r) {
      if (r.type === "childList") r.addedNodes.forEach(walk);
      else if (r.type === "characterData") trText(r.target);
      else if (r.type === "attributes" && r.target.getAttribute(r.attributeName) !== null && AR.test(r.target.getAttribute(r.attributeName))) trEl(r.target);
    });
  });

  function phones() { return document.querySelectorAll(".phone"); }
  function set(l) {
    if (l === lang) return;
    lang = l;
    phones().forEach(function (ph) {
      if (ph.__dir === undefined) { ph.__dir = ph.getAttribute("dir") || ""; ph.__lang = ph.getAttribute("lang") || ""; }
      if (ph.__dir !== "rtl") return; // phones that are LTR by design (none yet) stay as they are
      if (l === "en") { ph.setAttribute("dir", "ltr"); ph.setAttribute("lang", "en"); walk(ph); obs.observe(ph, { subtree: true, childList: true, characterData: true, attributes: true, attributeFilter: ATTRS }); }
      else { ph.setAttribute("dir", "rtl"); ph.setAttribute("lang", "fa"); unwalk(ph); }
    });
    if (l === "fa") obs.disconnect();
  }
  window.MockupI18n = { set: set, lang: function () { return lang; }, missing: function () { return Object.keys(miss); } };
})();
