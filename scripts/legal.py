"""
Single source for Knurl's imprint and privacy policy (German and English).

    python scripts/legal.py

writes the in-app texts (res/values*/legal.xml) and the web pages (public/impressum.html,
public/datenschutz.html, public/en/imprint.html, public/en/privacy.html). Change CONTROLLER here, then re-run.
"""
import html
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

CONTROLLER = {
    "name": "David Büchel",
    "street": "Graf-Gessler-Str. 2",
    "city": "50679 Köln",
    "country_de": "Deutschland",
    "country_en": "Germany",
    "email": "davidb92@googlemail.com",
}
UPDATED_DE = "20. November 2026"
UPDATED_EN = "20 November 2026"

c = CONTROLLER

IMPRINT_DE = f"""Angaben gemäß § 5 DDG

{c['name']}
{c['street']}
{c['city']}
{c['country_de']}

Kontakt
E-Mail: {c['email']}

Verantwortlich für den Inhalt nach § 18 Abs. 2 MStV
{c['name']}, Anschrift wie oben

Verbraucherstreitbeilegung
Wir sind nicht bereit und nicht verpflichtet, an Streitbeilegungsverfahren vor einer Verbraucherschlichtungsstelle teilzunehmen."""

IMPRINT_EN = f"""Information according to § 5 DDG (German Digital Services Act)

{c['name']}
{c['street']}
{c['city']}
{c['country_en']}

Contact
E-mail: {c['email']}

Responsible for content according to § 18 (2) MStV
{c['name']}, address as above

Consumer dispute resolution
We are neither willing nor obliged to take part in dispute resolution proceedings before a consumer arbitration board."""

PRIVACY_DE = f"""Stand: {UPDATED_DE}

1. Verantwortlicher
{c['name']}, {c['street']}, {c['city']}, {c['country_de']}. E-Mail: {c['email']}

2. Worum es geht
Knurl ist eine App zum Protokollieren von Krafttraining. Wir verarbeiten nur, was die App dafür braucht. Es gibt keine Werbung, kein Tracking und keine Weitergabe an Dritte zu Werbezwecken.

3. Konto
Beim ersten Start legt die App ein anonymes Konto bei Firebase Authentication (Google Ireland Ltd. / Google LLC) an. Es besteht nur aus einer zufälligen Kennung. Wenn du dein Konto sicherst, speichern wir zusätzlich deine E-Mail-Adresse oder die Kennung deines Google-Kontos (Name und E-Mail aus dem Google-Login). Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO (Vertrag, Bereitstellung der App).

4. Trainingsdaten
In Cloud Firestore (Rechenzentrum Frankfurt, europe-west3) speichern wir unter deinem Konto: Trainings mit Übungen, Sätzen, Gewichten, Wiederholungen und Notizen, deine Pläne, Einstellungen, Favoriten, eigene Übungen mit deinen Hinweisen und, wenn du eines hinzufügst, ein kleines Foto dazu (etwa 480 × 480 Pixel), sowie, wenn du es einträgst, dein Körpergewicht. Nur du kannst diese Daten lesen. Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO. Körpergewicht kann ein Gesundheitsdatum sein; du trägst es freiwillig ein (Art. 9 Abs. 2 lit. a DSGVO) und kannst es jederzeit löschen. Fotos wählst du selbst aus oder nimmst sie auf; die App verkleinert sie auf dem Gerät, bevor sie gespeichert werden, und löscht sie mit der Übung oder dem Konto.

5. Benachrichtigungen
Für Push-Nachrichten speichern wir pro Gerät ein Firebase-Cloud-Messaging-Token, die Plattform und die Sprache. Erinnerungen und der Pausen-Alarm laufen lokal auf deinem Gerät. Jede Art von Benachrichtigung lässt sich in den Einstellungen einzeln abschalten. Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO.

6. Absturzberichte
Nur wenn du sie in den Einstellungen einschaltest, sendet Firebase Crashlytics bei einem Absturz technische Angaben (Gerätemodell, Android-Version, App-Version, Fehlerverlauf). Rechtsgrundlage: Einwilligung, Art. 6 Abs. 1 lit. a DSGVO. Du kannst sie jederzeit in den Einstellungen widerrufen.

7. Health Connect
Nur wenn du es in den Einstellungen unter Verbindungen einschaltest und Android die Berechtigung erteilst, tauscht Knurl Daten mit Health Connect auf deinem Gerät aus: Knurl schreibt beendete Trainings (Zeitraum, Art, Name, geschätzte kcal), liest die Herzfrequenz während eines Trainings und gleicht dein Körpergewicht in beide Richtungen ab. Die Herzfrequenz wird nur auf dem Gerät für die Anzeige und die kcal-Schätzung genutzt und nicht in die Cloud übertragen; aus Health Connect übernommene Gewichte werden wie selbst eingetragene in deinem Konto gespeichert. Für die kcal aus der Herzfrequenz kannst du freiwillig Geburtsjahr und Geschlecht angeben; sie liegen in deinen Einstellungen. Rechtsgrundlage: deine Einwilligung, Art. 6 Abs. 1 lit. a und Art. 9 Abs. 2 lit. a DSGVO. Du kannst sie jederzeit in Knurl oder in Health Connect widerrufen. Daten aus Health Connect werden nicht für Werbung genutzt und nicht an Dritte weitergegeben.

8. Übungsbilder und Webseiten
Übungsfotos und diese Seiten liefert Firebase Hosting aus. Dabei verarbeitet Google technisch notwendig deine IP-Adresse. Rechtsgrundlage: Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse an einer funktionierenden Auslieferung).

9. Empfänger und Drittlandübermittlung
Auftragsverarbeiter ist Google (Firebase). Daten liegen in der EU (Frankfurt); einzelne Dienste von Google können Daten in die USA übermitteln. Grundlage dafür sind der EU-US Data Privacy Framework, unter dem Google zertifiziert ist, und die Standardvertragsklauseln der EU-Kommission.

10. Speicherdauer
Wir speichern deine Daten, solange dein Konto besteht. Wenn du dein Konto in der App löschst, werden alle Trainingsdaten und das Konto sofort gelöscht; Sicherungen bei Google werden innerhalb der dort üblichen Fristen überschrieben. Absturzberichte löscht Crashlytics nach 90 Tagen.

11. Deine Rechte
Du hast das Recht auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Datenübertragbarkeit und Widerspruch (Art. 15–21 DSGVO) sowie auf Widerruf einer Einwilligung. Export (JSON, CSV) und Löschung kannst du direkt in der App unter Profil → Konto & Daten auslösen. Außerdem kannst du dich bei einer Datenschutz-Aufsichtsbehörde beschweren, zum Beispiel in dem Bundesland, in dem du wohnst.

12. Kontakt
Fragen zum Datenschutz: {c['email']}"""

PRIVACY_EN = f"""Last updated: {UPDATED_EN}

1. Controller
{c['name']}, {c['street']}, {c['city']}, {c['country_en']}. E-mail: {c['email']}

2. What this is about
Knurl is an app for logging strength training. We only process what the app needs for that. There are no ads, no tracking and no sharing with third parties for advertising.

3. Account
On first start the app creates an anonymous account with Firebase Authentication (Google Ireland Ltd. / Google LLC). It is just a random identifier. If you secure your account, we also store your e-mail address or your Google account identifier (name and e-mail from Google sign-in). Legal basis: Art. 6 (1) (b) GDPR (contract, providing the app).

4. Training data
In Cloud Firestore (data centre Frankfurt, europe-west3) we store under your account: workouts with exercises, sets, weights, reps and notes, your routines, settings, favourites, your own exercises with your cues and, if you add one, a small photo for each (about 480 × 480 pixels), and, if you enter it, your body weight. Only you can read this data. Legal basis: Art. 6 (1) (b) GDPR. Body weight may be health data; you enter it voluntarily (Art. 9 (2) (a) GDPR) and can delete it at any time. You pick or take photos yourself; the app shrinks them on the device before they are stored and deletes them with the exercise or the account.

5. Notifications
For push messages we store a Firebase Cloud Messaging token, the platform and the language per device. Reminders and the rest alarm run locally on your phone. Every kind of notification can be switched off in the settings. Legal basis: Art. 6 (1) (b) GDPR.

6. Crash reports
Only if you switch them on in the settings, Firebase Crashlytics sends technical details when the app crashes (device model, Android version, app version, stack trace). Legal basis: consent, Art. 6 (1) (a) GDPR. You can withdraw it in the settings at any time.

7. Health Connect
Only if you switch it on in the settings under Connections and Android grants the permission, Knurl exchanges data with Health Connect on your phone: it writes finished workouts (time span, type, name, estimated kcal), reads your heart rate during a workout and syncs your body weight both ways. Heart rate is only used on the phone for display and the kcal estimate and is never uploaded; weights taken from Health Connect are stored in your account like ones you entered. For kcal from heart rate you may add your year of birth and sex; they are kept in your settings. Legal basis: your consent, Art. 6 (1) (a) and Art. 9 (2) (a) GDPR. You can withdraw it at any time in Knurl or in Health Connect. Data from Health Connect is never used for advertising and never shared with third parties.

8. Exercise photos and web pages
Exercise photos and these pages are served by Firebase Hosting, which technically processes your IP address. Legal basis: Art. 6 (1) (f) GDPR (legitimate interest in working delivery).

9. Recipients and transfers outside the EU
Our processor is Google (Firebase). Data is stored in the EU (Frankfurt); some Google services may transfer data to the USA, based on the EU-US Data Privacy Framework, under which Google is certified, and the European Commission's standard contractual clauses.

10. Retention
We keep your data as long as your account exists. When you delete your account in the app, all training data and the account are deleted at once; Google's backups are overwritten within its usual periods. Crashlytics deletes crash reports after 90 days.

11. Your rights
You have the right of access, rectification, erasure, restriction, data portability and objection (Art. 15–21 GDPR) and to withdraw consent. You can export (JSON, CSV) and delete your data directly in the app under Profile → Account & data. You can also lodge a complaint with a data protection supervisory authority.

12. Contact
Privacy questions: {c['email']}"""


def android_escape(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")


def write_xml(folder, imprint, privacy):
    path = os.path.join(ROOT, "app", "src", "main", "res", folder, "legal.xml")
    with open(path, "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by scripts/legal.py. Edit there, not here. -->\n<resources>\n')
        f.write(f'    <string name="legal_imprint_body">{android_escape(imprint)}</string>\n')
        f.write(f'    <string name="legal_privacy_body">{android_escape(privacy)}</string>\n')
        f.write("</resources>\n")


def page(title, text, lang):
    blocks = []
    for para in text.split("\n\n"):
        lines = [html.escape(l) for l in para.split("\n")]
        if len(lines) > 1 and len(lines[0]) < 70:
            blocks.append(f"<h2>{lines[0]}</h2><p>{'<br>'.join(lines[1:])}</p>")
        else:
            blocks.append(f"<p>{'<br>'.join(lines)}</p>")
    return f"""<!doctype html><html lang="{lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)} · Knurl</title>
<style>body{{font:16px/1.6 system-ui,-apple-system,"Segoe UI",sans-serif;max-width:720px;margin:0 auto;padding:32px 20px 64px;color:#11151C;background:#F3F5F8}}
h1{{font-size:30px;margin:0 0 20px}}h2{{font-size:18px;margin:28px 0 6px}}p{{margin:0 0 12px}}a{{color:#2448E8}}
@media (prefers-color-scheme:dark){{body{{color:#EEF1F5;background:#0C0F14}}a{{color:#7C93FF}}}}</style></head>
<body><h1>{html.escape(title)}</h1>{''.join(blocks)}</body></html>
"""


def write_page(rel, title, text, lang):
    path = os.path.join(ROOT, "public", rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w", encoding="utf-8").write(page(title, text, lang))


if __name__ == "__main__":
    write_xml("values", IMPRINT_EN, PRIVACY_EN)
    write_xml("values-de", IMPRINT_DE, PRIVACY_DE)
    write_page("impressum.html", "Impressum", IMPRINT_DE, "de")
    write_page("datenschutz.html", "Datenschutzerklärung", PRIVACY_DE, "de")
    write_page("en/imprint.html", "Imprint", IMPRINT_EN, "en")
    write_page("en/privacy.html", "Privacy policy", PRIVACY_EN, "en")
    left = sum(v.startswith("[") for v in CONTROLLER.values())
    print("written; placeholders left:", left)
