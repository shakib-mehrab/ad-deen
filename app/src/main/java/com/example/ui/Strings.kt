package com.example.ui

object Strings {
    fun t(key: String, lang: String): String {
        val isBn = lang == "bn"
        return when (key) {
            "app_title" -> if (isBn) "আদ-দ্বীন" else "Ad-Deen"
            "nav_home" -> if (isBn) "হোম" else "Home"
            "nav_prayer" -> if (isBn) "ওয়াক্ত" else "Wakt"
            "nav_tracker" -> if (isBn) "আমল" else "Tracker"
            "nav_azkhar" -> if (isBn) "আযকার" else "Azkhar"
            "nav_more" -> if (isBn) "আরও" else "More"

            // Home
            "current_prayer" -> if (isBn) "ওয়াক্ত" else "Wakt"
            "next_prayer" -> if (isBn) "পরবর্তী ওয়াক্ত" else "Next Wakt"
            "time_remaining" -> if (isBn) "বাকি সময়" else "Time Remaining"
            "today_score" -> if (isBn) "আজকের আমল স্কোর" else "Today's Worship Score"
            "perfect_day" -> if (isBn) "পারফেক্ট ডে" else "Perfect Day"
            "not_perfect" -> if (isBn) "চলমান আমল" else "In Progress"
            "current_streak" -> if (isBn) "বর্তমান স্ট্রিক" else "Current Streak"
            "longest_streak" -> if (isBn) "সর্বোচ্চ স্ট্রিক" else "Best Streak"
            "days" -> if (isBn) "দিন" else "Days"
            "quick_azkhar" -> if (isBn) "আজকের জরুরি আযকার" else "Today's Azkhar Routines"
            "wake_azkhar" -> if (isBn) "ঘুম থেকে ওঠার দুআ (৬টি)" else "Wake Azkhar (6)"
            "morning_azkhar" -> if (isBn) "সকালের আযকার (১৫টি)" else "Morning Azkhar"
            "evening_azkhar" -> if (isBn) "সন্ধ্যার আযকার (১৫টি)" else "Evening Azkhar"
            "sleep_azkhar" -> if (isBn) "ঘুমানোর আযকার (১১টি)" else "Sleep Azkhar"
            "daily_zikr" -> if (isBn) "দৈনিক যিকির ও তাসবীহ" else "Daily Zikr"
            "names_99" -> if (isBn) "আসমাউল হুসনা (৯৯ নাম)" else "99 Names of Allah"

            // Prayer Times & Forbidden Times
            "calculation_method" -> if (isBn) "হিসাব পদ্ধতি" else "Calculation Method"
            "location" -> if (isBn) "অবস্থান" else "Location"
            "change_location" -> if (isBn) "অবস্থান পরিবর্তন" else "Change Location"
            "start_alarm" -> if (isBn) "শুরুর অ্যালার্ম" else "Start Alarm"
            "end_warning_15m" -> if (isBn) "১৫ মিনিট আগের সতর্কবার্তা" else "15m End Warning"
            "sunrise" -> if (isBn) "সূর্যোদয়" else "Sunrise"
            "sunset" -> if (isBn) "সূর্যাস্ত" else "Sunset"
            "midnight" -> if (isBn) "ইসলামিক মধ্যরাত" else "Islamic Midnight"
            "forbidden_times" -> if (isBn) "নামাজের নিষিদ্ধ সময় (মাকরুহ ওয়াক্ত)" else "Forbidden Prayer Times"
            "forbidden_active_warning" -> if (isBn) "বর্তমানে নামাজের নিষিদ্ধ সময় চলছে — সালাত আদায় নিষেধ" else "Forbidden Prayer Time Active — Prayers Prohibited"

            // Tracker
            "daily_worship_tracker" -> if (isBn) "দৈনিক আমল ট্র্যাকার" else "Daily Worship Tracker"
            "future_date_locked" -> if (isBn) "ভবিষ্যতের তারিখের আমল এখনও রেকর্ড করা সম্ভব নয়" else "Future dates cannot be tracked yet"
            "fard_prayers" -> if (isBn) "ফরজ সালাত (সর্বোচ্চ ৬০)" else "5 Daily Fard Prayers (Max 60 pts)"
            "nafl_prayers" -> if (isBn) "নফল ও তাহাজ্জুদ" else "Nafl, Witr & Tahajjud"
            "azkhar_sections" -> if (isBn) "আযকার ও যিকির শাখা (+১৫)" else "Azkhar Sections (15 pts)"
            "status_jamat" -> if (isBn) "জামাতে (+১২)" else "Jamat (+12)"
            "status_alone" -> if (isBn) "একাকী (+৭)" else "Alone (+7)"
            "status_qaza" -> if (isBn) "ক্বাযা (০)" else "Qaza (0)"
            "status_missed" -> if (isBn) "ছুটে গেছে (০)" else "Missed (0)"
            "witr_title" -> if (isBn) "বিতর সালাত (+১০)" else "Witr Prayer (+10)"
            "tahajjud_title" -> if (isBn) "তাহাজ্জুদ সালাত (+১০)" else "Tahajjud Prayer (+10)"
            "duha_title" -> if (isBn) "দুহা / চাশত (+৫)" else "Duha Prayer (+5)"
            "calendar_view" -> if (isBn) "মাসিক ক্যালেন্ডার ও স্ট্যাটস" else "Monthly Calendar & History"
            "score_history" -> if (isBn) "স্কোর অগ্রগতি" else "Score Trends"

            // Azkhar
            "tap_to_count" -> if (isBn) "গণনার জন্য ট্যাপ করুন" else "Tap to Count"
            "completed" -> if (isBn) "সম্পন্ন হয়েছে" else "Completed"
            "reset" -> if (isBn) "রিসেট" else "Reset"
            "add_custom_zikr" -> if (isBn) "নতুন যিকির যোগ করুন" else "Add Custom Zikr"
            "zikr_title" -> if (isBn) "যিকিরের বাক্য" else "Zikr Text"
            "target_count" -> if (isBn) "লক্ষ্যমাত্রা (টার্গেট)" else "Target Count"
            "save" -> if (isBn) "সংরক্ষণ করুন" else "Save"
            "cancel" -> if (isBn) "বাতিল" else "Cancel"
            "delete" -> if (isBn) "মুছুন" else "Delete"

            // Reminders
            "istigfar_reminders" -> if (isBn) "ইস্তিগফার স্মরণিকা" else "Istigfar Reminders"
            "durud_reminders" -> if (isBn) "দরূদ শরীফ স্মরণিকা" else "Durud Reminders"
            "reminder_mode" -> if (isBn) "রিমাইন্ডার মোড" else "Reminder Mode"
            "quiet_hours" -> if (isBn) "নিস্তব্ধ সময় (Quiet Hours)" else "Quiet Hours"
            "quiet_hours_note" -> if (isBn) "নিস্তব্ধ সময়ে শুধু ইস্তিগফার ও দরূদের নোটিফিকেশন বন্ধ থাকবে। নামাজের অ্যালার্ম বাজবে।" else "Quiet hours only suppress Istigfar/Durud reminders. Prayer alarms always ring."

            // Quran
            "quran_browser" -> if (isBn) "আল-কুরআন ওয়েব ব্রাউজার" else "Al-Quran Browser"

            // Settings
            "settings" -> if (isBn) "সেটিংস" else "Settings"
            "language" -> if (isBn) "ভাষা / Language" else "Language / ভাষা"
            "english" -> "English"
            "bangla" -> "বাংলা"
            "calc_method_label" -> if (isBn) "নামাজের সময় নির্ধারণ পদ্ধতি" else "Calculation Method"
            "location_mode" -> if (isBn) "অবস্থান নির্ধারণের মাধ্যম" else "Location Mode"
            "gps_mode" -> if (isBn) "স্বয়ংক্রিয় জিপিএস (GPS)" else "GPS Auto-Detect"
            "district_mode" -> if (isBn) "বাংলাদেশের ৬৪ জেলা থেকে নির্বাচন" else "Select from 64 Districts (BD)"
            "exact_alarm_permission" -> if (isBn) "সঠিক সময়ে অ্যালার্মের অনুমতি" else "Exact Alarm Permission"
            "battery_optimization" -> if (isBn) "ব্যাটারি অপটিমাইজেশন বন্ধ করুন" else "Disable Battery Optimization"
            "battery_opt_desc" -> if (isBn) "ব্যাকগ্রাউন্ডে নামাজের সময়সূচী ও অ্যালার্ম নিরবচ্ছিন্ন রাখতে ব্যাটারি অপটিমাইজেশন শিথিল করা প্রয়োজন।" else "Allows seamless prayer alarms and reminders to fire accurately in the background."

            // Account & Backup
            "account" -> if (isBn) "অ্যাকাউন্ট ও ক্লাউড সিঙ্ক" else "Account & Cloud Sync"
            "signed_in_as" -> if (isBn) "লগইন আছেন:" else "Signed in as:"
            "guest_mode" -> if (isBn) "সাইন ইন করা নেই" else "Not Signed In"
            "sign_in" -> if (isBn) "সাইন ইন / নিবন্ধন" else "Sign In / Register"
            "sign_out" -> if (isBn) "লগআউট" else "Sign Out"
            "delete_account" -> if (isBn) "অ্যাকাউন্ট ও সব ডাটা মুছে ফেলুন" else "Delete Account & Data"
            "delete_account_confirm" -> if (isBn) "আপনি কি নিশ্চিতভাবে আপনার অ্যাকাউন্ট ও ক্লাউডে সংরক্ষিত সমস্ত আমল ডাটা স্থায়ীভাবে মুছে ফেলতে চান?" else "Are you sure you want to permanently delete your account and all tracked data from the cloud?"
            "cloud_sync" -> if (isBn) "ব্যাকআপ ও সিঙ্ক" else "Backup & Sync"
            "sync_now" -> if (isBn) "এখনই ক্লাউডে সিঙ্ক করুন" else "Sync to Cloud Now"
            "last_synced" -> if (isBn) "সর্বশেষ সিঙ্ক:" else "Last Synced:"
            "never_synced" -> if (isBn) "এখনও সিঙ্ক করা হয়নি" else "Never synced"
            "backup_google_drive" -> if (isBn) "গুগল ড্রাইভ বা ফাইলে ব্যাকআপ" else "Backup to Google Drive / File"
            "backup_desc" -> if (isBn) "আপনার সকল আমল, নামাজের রেকর্ড ও যিকির JSON ব্যাকআপ ফাইলে সংরক্ষণ করুন" else "Export your prayer tracking, scores, and zikr into a backup file"
            "restore_backup" -> if (isBn) "ব্যাকআপ ফাইল থেকে রিস্টোর" else "Restore from Backup"
            "restore_desc" -> if (isBn) "গুগল ড্রাইভ বা মেমরি থেকে পূর্বে সংরক্ষিত ব্যাকআপ ফাইল সিলেক্ট করুন" else "Import an existing backup file from Google Drive or storage"
            "share_backup" -> if (isBn) "গুগল ড্রাইভ / অ্যাপে শেয়ার" else "Share / Save to Google Drive"
            "save_file" -> if (isBn) "ফাইল সরাসরি সেভ করুন" else "Save File to Storage"
            "firebase_connected" -> if (isBn) "ফায়ারবেস ক্লাউড কানেক্টেড" else "Firebase Cloud Connected"
            "local_mode_notice" -> if (isBn) "লোকাল প্রডাকশন মোড • নিরাপদ অফলাইন স্টোরেজ" else "Local Production Mode • Offline Ready"

            else -> key
        }
    }
}
