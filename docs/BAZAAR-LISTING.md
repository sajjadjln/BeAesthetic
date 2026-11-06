# «فانوس» on Cafe Bazaar: the listing

_Written 2026-11-06 on branch `claude/bazaar-finish-a0e614` (from `bazaar`), for the first upload (B8, D217). Rules and limits come from `docs/research/bazaar-developer-guide.md` §3 and §16. Paste the Persian text into the panel as it is; the character counts were checked with a script._

## 1. Basic info («اطلاعات پایه»)

| Field | Value | Rule |
|---|---|---|
| Developer title («عنوان توسعه‌دهنده»), Persian | **دانی دورکار** (11 chars) | ≤32 chars, unique; the same name the app shows in «درباره» |
| Developer title, English | **Donnie Doorkar** (14 chars) | ≤32 chars; should match the English name in «پروفایل عمومی» |
| Type | برنامه (app) | |
| Category | **ابزارها** | one only; Bazaar has no «بهره‌وری» (D217) |
| Pricing | رایگان (free), no in-app purchase | can't change after publishing |
| Package | `app.lantern` | permanent; check it is free on the panel |

## 2. App info («اطلاعات برنامه»)

**«نام فارسی برنامه»:** `فانوس`

It must equal the launcher label (`app_name` in the Bazaar build, tested by `BazaarStringsTest.theLauncherSaysFanus`). No keywords in the name.

**«توضیحات کوتاه فارسی»** (≤60 chars; this one is 48):

```
لیست کارهای روزانه با یادآور مطمئن، بدون اینترنت
```

**«توضیحات فارسی»** (≤800 chars; this one is 784). Only what 1.0 does: no goals, no plugins, no "soon".

```
فانوس هر روز بهت می‌گه الان نوبت چه کاریه.

• کارهات رو بساز: یک بار، هر روز، روزهای خاص هفته، یا «هر وقت».
• روز چهار بخش داره: صبح، ظهر، عصر و شب، هر کدوم با یادآور خودش. هر کار هم می‌تونه زنگ خودش رو داشته باشه.
• یادآورها سر دقیقه می‌رسن، حتی بعد از خاموش و روشن کردن گوشی. «رسیدن یادآورها» نشون می‌ده چندتاشون به‌موقع رسیدن.
• با یه بار زدن تیک بزن. روزی که جا بمونه فقط یه خونه‌ی خاکستریه؛ نه شمارنده‌ای صفر می‌شه، نه سرزنشی.
• یه همراه انتخاب کن: آریاسب شوالیه، دراکولا، زال جادوگر یا وایولت و گربه‌ش. یا ظاهر ساده، بدون شخصیت.
• سابقه‌ی روزها توی یه جدول.
• ویجت امروز برای صفحه‌ی اصلی.
• پشتیبان خودکار هفتگی توی پوشه‌ای که خودت انتخاب می‌کنی.
• تاریخ شمسی، هفته از شنبه.

فانوس اجازه‌ی اینترنت نداره؛ نه حساب کاربری، نه تبلیغ، نه آمارگیری. همه‌چیز فقط روی گوشی خودت می‌مونه.
```

English name and description are optional; leave them empty (the Bazaar build is Persian only, D207).

## 3. Support («اطلاعات پشتیبانی»)

| Field | Value |
|---|---|
| Support email (required) | **donniedoorkar@gmail.com**, the same address as `about_contact` (the contact row in «درباره») |
| Phone | leave empty unless you want calls |
| Website | optional; leave empty for 1.0 (Bazaar doesn't require a privacy-policy URL; the in-app note covers it, guide §16.2 item 13) |

The guide says to use a support email **different from the developer account email** (the one you log in with, which can never change). So log in to the panel with another address and keep `donniedoorkar@gmail.com` for support, or the other way round, but not both the same.

## 4. Images

| Asset | Spec | What to use |
|---|---|---|
| Icon | PNG, 512×512 (up to 3000×3000), ≤3 MB, **square, no rounded corners, no shadow** (Bazaar adds both). Must match the launcher icon. Padding if the shape touches the edges. | Export the adaptive launcher lantern (foreground on its background) flat at 512×512 |
| Screenshots | 1–12, any size, ≤1 MB each; 5–6 good ones. Real app only, fit for age 4+. | Today (list), Today (parts of the day), the character pick, the new-task sheet, History, «رسیدن یادآورها». **Not** the Goals tab (D217). |
| Header image («تصویر سر صفحه») | PNG, ≥720×288, **5:2**, ≤1 MB | The splash lantern and the name on the night sky |
| Promo screenshots | JPG, ≥1152×648, 16:9, ≤1 MB. Optional. | Skip for 1.0 |
| Video | Aparat link, ≤10 min. Optional. | Skip |

## 5. Notes for the reviewer («توضیحات بیشتر برای بررسی‌کننده»)

Paste as is. Upload the signed art letter (§6) as the attached file.

```
سلام. فانوس یه برنامه‌ی لیست کارهای روزانه با یادآوره که کاملاً آفلاین کار می‌کنه. اجازه‌ی INTERNET نداره و هیچ اطلاعاتی از گوشی بیرون نمی‌فرسته. حساب کاربری، تبلیغ و کیت آمارگیری نداره.

دلیل هر دسترسی:
• POST_NOTIFICATIONS: نشون دادن یادآورها. در شروع برنامه پرسیده می‌شه و اگه کاربر رد کنه، برنامه بدون اعلان کار می‌کنه.
• USE_EXACT_ALARM و SCHEDULE_EXACT_ALARM: یادآورها و زنگ کارها باید سر همون دقیقه‌ای که کاربر تعیین کرده برسن. SCHEDULE_EXACT_ALARM جایگزین برای نسخه‌های اندروید ۱۲ است.
• RECEIVE_BOOT_COMPLETED: بعد از خاموش و روشن شدن گوشی، یادآورها دوباره تنظیم بشن.
• REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: خیلی از گوشی‌ها (شیائومی، سامسونگ، هواوی) برای صرفه‌جویی باتری برنامه رو می‌بندن و یادآور نمی‌رسه. پنجره‌ی سیستم فقط وقتی باز می‌شه که خود کاربر توی «تنظیمات ← رسیدن یادآورها» روی ردیف باتری بزنه.
• VIBRATE: لرزش هنگام یادآور.
• WAKE_LOCK، FOREGROUND_SERVICE و ACCESS_NETWORK_STATE: این سه تا رو کتابخانه‌ی WorkManager اندروید خودش اضافه می‌کنه (برای کار پس‌زمینه‌ی تنظیم دوباره‌ی یادآورها و پشتیبان هفتگی). برنامه اجازه‌ی INTERNET نداره و از شبکه هیچ استفاده‌ای نمی‌کنه.

بررسی به‌روزرسانی فقط با سرویس خود اپ بازار روی گوشی حرف می‌زنه (IUpdateCheckService) و امتیازدهی با ACTION_EDIT روی bazaar://details?id=app.lantern باز می‌شه.

بخش «هدف‌ها»: این تب عمداً با برچسب «در دست ساخت» گذاشته شده و توضیح می‌ده که این بخش در نسخه‌های بعد میاد. بقیه‌ی برنامه (امروز، کارها، یادآورها، سابقه، ویجت، پشتیبان، تنظیمات) کامل است. در توضیحات و تصویرهای صفحه‌ی برنامه به این بخش اشاره نشده.

تصویرهای شخصیت‌ها با اجازه‌ی کتبی صاحب اثر استفاده شده‌اند؛ نامه‌ی اجازه پیوست است. تنها فونت برنامه «استعداد» با مجوز SIL OFL 1.1 است و متن مجوزها در «تنظیمات ← درباره ← مجوزها» هست.

راه تماس: donniedoorkar@gmail.com (در برنامه: تنظیمات ← درباره ← تماس با سازنده).
```

Check the list against the merged release manifest before each upload (`app/build/intermediates/merged_manifests/bazaarRelease/.../AndroidManifest.xml`). On 0.9.11 it has exactly the nine permissions above, plus `app.lantern.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which AndroidX Core declares for the app itself (signature-level, not shown to users); add a line for it if the reviewer asks.

## 6. Permission letter for the character art

Bazaar's copyright rule (guide §9): using someone else's work needs a written confirmation **sent to Bazaar**, naming **the developer email used on Bazaar** and **the package name**. From a private person it must also carry their **ID details**, with **images of their ID documents** attached. From a company: on letterhead with date, stamp and the manager's signature, or sent from an address on its own domain. A forged letter means removal.

What it covers: the four characters (knight, vampire, wizard, goth woman with her cat), each in three moods, as shipped in `app/src/main/res/drawable-nodpi/char_*.jpg`, and their use in the store screenshots. **Fonts:** the APK ships only Estedad (SIL OFL 1.1, licence text in «مجوزها»); no per-character font is in the APK, so no font letter is needed. If a font is added later, it needs its own licence or letter.

If the art is yours (you drew it, or you own it outright), no letter: say so in the reviewer notes instead («تصویرهای شخصیت‌ها کار خود توسعه‌دهنده است») and change `licenses_art_note` in the app to match.

Fill the brackets, print, sign, scan as PDF with the ID copies, and upload it at the last step of the panel.

```
به نام خدا

تاریخ: [تاریخ شمسی]

به: کافه‌بازار، واحد بررسی برنامه‌ها

موضوع: اجازه‌ی استفاده از تصویرهای شخصیت‌ها در برنامه‌ی «فانوس»

اینجانب [نام و نام خانوادگی صاحب اثر]، فرزند [نام پدر]، به کد ملی [کد ملی]، شماره‌ی شناسنامه‌ی [شماره]، ساکن [نشانی]، تلفن [شماره‌ی تماس]، صاحب حقوق مادی و معنوی تصویرهای چهار شخصیت (شوالیه، خون‌آشام، جادوگر و دختر گوتیک با گربه‌اش) هستم.

به این وسیله به «دانی دورکار»، توسعه‌دهنده‌ی کافه‌بازار با ایمیل [ایمیل حساب توسعه‌دهنده در بازار]، اجازه می‌دهم این تصویرها را در برنامه‌ی «فانوس» با نام بسته‌ی app.lantern و در تصویرهای صفحه‌ی این برنامه در کافه‌بازار، به صورت رایگان و بدون محدودیت زمانی، منتشر و استفاده کند.

تصویر کارت ملی و شناسنامه‌ی اینجانب پیوست این نامه است.

نام و نام خانوادگی: [ ]
امضا: [ ]
```

Attach: front and back of the national card, and the ID-booklet page with the photo.

## 7. Release steps (summary)

1. Account (you): individual, national card, a mobile in your own name, two-step login, the 400,000-rial yearly subscription. The account email is permanent.
2. New app → upload the signed APK from `FLAVOR=bazaar scripts/release.sh`.
3. Fill §1–§4. Release group **«آلفا»** with the testers' group (≤100 mobile numbers; they log in to Bazaar with that number). Publish mode **«انتشار دستی»**.
4. Paste §5, attach §6, «درخواست انتشار برنامه». Review takes up to 3 working days; at most 10 publish requests a day.
5. After the beta (B7): switch the release from alpha to «عمومی». It can't go back to alpha.
6. No staged rollout: it needs 5,000 active installs.

## Character counts

Checked with Python `len()` on 2026-11-06: short description 48, description 784. Re-count after any edit; the limit is 800.
