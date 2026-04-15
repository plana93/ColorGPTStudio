Perfetto — ora lo strutturiamo **come un vero documento di progettazione serio**, diviso in:

1. 🧠 **Design ad alto livello (prodotto + UX + architettura logica)**
2. ⚙️ **Specifiche tecniche (implementazione reale Android)**
3. 🧰 **Selezione ragionata delle librerie dalla lista (non a caso)**

---

# 🧠 1. DESIGN AD ALTO LIVELLO

## 🎯 Visione prodotto

L’app non è solo un “color picker”.

👉 È un **tool professionale di analisi colore su immagini** orientato a:

* designer
* architetti
* interior designer
* artigiani e falegnami curiosi che vogliono sperimentare e documentare soluzioni colore

### Concetto chiave:

> “Trasformare immagini reali in dati cromatici strutturati, annotati e comunicabili”

L’obiettivo non è solo “leggere un codice colore”, ma:

* capire come i colori convivono in un ambiente reale
* annotare significato, materiali, finiture e contesto
* comunicare decisioni e alternative a clienti, colleghi o fornitori

---

## 🧩 Modello concettuale

### Entità principali

**Project (Scheda)**

* contenitore logico di un lavoro reale (es. “Cucina Rossi”)
* può avere più immagini (prima/dopo, varianti, dettagli)
* ha una nota globale (es. stile, vincoli, budget, cliente)

**Image**

* immagine caricata o scattata
* contiene:
  * punti colore manuali
  * palette automatica
  * note contestuali (es. “foto scattata in luce serale”)

**ColorPoint**

* punto scelto dall’utente
* coordinate (x, y) sull’immagine
* colore estratto (RGB, HEX, eventualmente codifiche aggiuntive)
* nota semantica e/o tag “da cantiere”
* campo “prodotto usato / codice vernice / fornitore”

**Palette (macro colori)**

* generata automaticamente
* top N colori dominanti (es. 5)
* usata per:
  * confrontare alternative (Palette A vs B)
  * creare schede riassuntive stampabili

---

## 🔄 Flussi utente principali

All'avvio, l'app presenta una scelta chiara e immediata:

1.  **Analisi Rapida**: Per un'analisi "one-shot" su una singola immagine.
2.  **Nuovo Progetto**: Per avviare un lavoro strutturato con più immagini e note.

### A. Flusso "Analisi Rapida" (ex "Modalità Veloce")

Pensato per un uso estemporaneo, non persistente.

1.  **Scelta Sorgente**: L'utente sceglie tra "Scatta Foto" o "Carica da Galleria".
2.  **Analisi Interattiva**:
    *   L'immagine viene caricata.
    *   L'utente può fare tap per estrarre colori, zoomare e vedere i codici.
3.  **Fine Sessione**: Uscendo dalla schermata, i dati vengono persi. Non c'è salvataggio né cronologia. Questo flusso è volutamente semplice e senza persistenza.

### B. Flusso "Progetto" (Modalità Principale)

1.  **Creazione Progetto**:
    *   L'utente sceglie "Nuovo Progetto".
    *   Inserisce un nome per il progetto (es. "Cucina Rossi").
    *   Viene creata una cartella dedicata nell'area privata dell'app.
2.  **Inserimento Immagini**:
    *   L'utente aggiunge immagini (da camera o galleria) al progetto.
    *   Ogni immagine viene copiata a risoluzione piena nella cartella del progetto.
    *   Possibilità di marcare le immagini (es. "Stato Attuale", "Proposta A").
3.  **Analisi e Arricchimento**:
    *   L'utente seleziona un'immagine dal progetto per analizzarla.
    *   **Analisi Interattiva**: tap per colore, drag, zoom.
    *   **Arricchimento Semantico**: Aggiunge note e tag ai `ColorPoint`, alle `Image` e al `Project`.
4.  **Confronto e Revisione**:
    *   Vista split-screen o swipe per confrontare immagini diverse dello stesso progetto.
    *   Confronto tra le palette generate per diverse immagini.
5.  **Export e Condivisione**:
    *   Genera un report testuale o una scheda PDF/Immagine del progetto.
    *   Condivide l'output via mail, WhatsApp, ecc.

---

## 🧠 Pattern UX importanti

### 1. Direct manipulation

*   niente menu complessi
*   tutto succede “sulla foto”:
    *   tap per creare punto
    *   drag per spostarlo
    *   tap lungo per aprire note/tag

### 2. Feedback immediato

*   appena tocchi → colore + codici (HEX, RGB di default).
*   un'icona o un pulsante "+" permette di visualizzare formati aggiuntivi (HSL, HSV, LAB, CMYK) per non affollare l'interfaccia.
*   anteprima colore grande, ben leggibile anche in cantiere.

### 3. Persistenza invisibile (Solo in Modalità Progetto)

*   tutto salvato automaticamente all'interno della cartella di progetto:
    *   punti, note, tag.
    *   un file `project_data.json` accompagna le immagini.

### 4. Editing fluido

*   punti trascinabili
*   note inline, senza cambiare schermata
*   i "tag da cantiere" sono stringhe di testo libero per massima flessibilità.

### 5. Aiuto decisionale (attivato dall'utente)

*   **Suggerimenti**: L'utente può cliccare su un'icona "lampadina" (?) che mostra una lista di suggerimenti pre-impostati (es. "Controlla contrasto", "Valuta saturazione"). Non sono automatici.
*   **Template di Palette**: L'app legge da una cartella interna dei file JSON che definiscono i template (es. `cucina_rustica.json`). L'utente può selezionare un template per vedere una palette di riferimento e le relative indicazioni (es. "colore principale", "colore d'accento").

---

## 📤 Export: concetto avanzato

Non è un semplice export.

👉 È una **serializzazione narrativa + operativa del progetto**

Contiene:

*   struttura (project → images → color points → palette)
*   semantica (note, tag, tipo di ambiente, template usato)
*   dati colore (codici)
*   **lista materiali minima**:
    *   per ogni colore: “Prodotto / codice vernice / fornitore”

Formati:

*   **Testo Strutturato (Markdown)**: Facilmente incollabile e leggibile.
*   **Cartella di Progetto (ZIP)**: L'intera cartella del progetto (immagini + file JSON) può essere zippata e condivisa per un backup completo o per passarla a un altro utente dell'app.
*   **Scheda PDF/Immagine**: Con layout personalizzabile (opzione per v2) per la condivisione con i clienti.

---

# ⚙️ 2. SPECIFICHE TECNICHE

## 🏗 Architettura

Consigliata:

👉 **MVVM + Clean Architecture**

Layer:

*   UI (Jetpack Compose)
*   ViewModel
*   UseCase (application layer)
*   Repository
*   Data (DB + File System)

---

## 🧵 Concorrenza

Usa:

*   **kotlinx.coroutines**

Per:

*   caricamento immagini
*   analisi colori (es. K-means in background)
*   operazioni su DB e File System (lettura/scrittura JSON, copia immagini)

---

## 🖼 Gestione immagini

### Caricamento

*   URI → Bitmap

Libreria:

*   **Coil**

---

## 🎯 Color picking

### Metodo base:

```kotlin
bitmap.getPixel(x, y)
```

### Problemi reali:

*   scaling immagine
*   coordinate trasformate
*   precisione zoom

👉 devi gestire:

*   matrix transformation
*   mapping touch → pixel reale

### Rappresentazione Colore

*   **Default**: Mostra sempre HEX e RGB.
*   **On-demand**: Un'interfaccia espandibile (es. un popup o una sezione che si apre) mostra i valori calcolati per HSL, HSV, LAB, CMYK.
*   **Luce Ambientale**: Non viene gestita in automatico. L'utente può usare il campo note dell'entità `Image` per descrivere le condizioni di luce (es. "luce naturale fredda", "luce artificiale calda").

---

## 🎨 Estrazione palette

### Metodo consigliato:

*   **K-means clustering**

### Dettagli Implementativi:

*   **Filtro Pre-Clustering**: Prima di eseguire l'algoritmo, i pixel dell'immagine vengono filtrati per escludere i colori "sporchi". Criteri di massima:
    *   Escludere pixel con luminosità < 15% o > 95%.
    *   Escludere pixel con saturazione < 10%.
    *   Questi valori saranno configurabili.
*   **Selezione Area (Post MVP)**: La possibilità per l'utente di disegnare un'area sull'immagine da cui estrarre la palette è una feature complessa. Verrà considerata per una versione successiva. Per la v1, l'analisi avviene sull'intera immagine filtrata.

Opzioni:

*   manuale (più controllo)
*   tramite OpenCV

---

## 🧠 Overlay interattivo

Tecnologie:

*   Jetpack Compose Canvas
*   oppure Custom View

Funzioni:

*   disegno marker
*   frecce
*   zoom lens

---

## 💾 Persistenza

La persistenza è gestita a due livelli: un database per l'accesso rapido ai metadati e il file system per la portabilità e il backup.

### Database:

👉 **Room Database** (per indicizzare progetti e metadati)

#### Tabelle:

*   **projects**: `id`, `name`, `description`, `folder_path`
*   **images**: `id`, `project_id`, `original_uri`, `file_name`, `notes`
*   **color_points**: `id`, `image_id`, `x`, `y`, `hex_code`, `note`, `material_info`
*   **palette_colors**: `id`, `project_id`, `hex_code` (La palette è un aggregato a livello di Progetto)

### File System:

*   Alla creazione di un progetto, viene creata una cartella in `context.getFilesDir()/projects/project_name`.
*   Questa cartella contiene:
    *   Le copie a **risoluzione piena** di tutte le immagini.
    *   Un file `project_data.json` che contiene una serializzazione di tutte le note, i tag, i punti colore e le palette, per un backup e ripristino facilitato.

---

## 🗂 Storage immagini

*   **Copia Locale Obbligatoria**: Per ogni immagine aggiunta a un progetto, una copia a risoluzione piena viene salvata nella cartella del progetto.
*   **Indipendenza dalla Galleria**: L'app fa affidamento solo sulla sua copia locale. Se l'utente cancella l'immagine originale dalla galleria, l'app continua a funzionare senza problemi.
*   **Backup Semplificato**: L'utente può fare il backup di un intero progetto semplicemente copiando la cartella corrispondente.

---

## 📋 Clipboard export

Android:

*   ClipboardManager

---

## 🔄 Serializzazione

Formato:

*   **JSON (kotlinx.serialization)**: Usato per il file `project_data.json` all'interno di ogni cartella di progetto. Contiene tutti i metadati, le note e i punti colore.
*   **Markdown**: Usato per l'export testuale verso la clipboard.

---

# 🧰 3. LIBRERIE UTILI (SELEZIONE RAGIONATA)

Ora la parte più importante: **filtriamo davvero quella lista enorme**.

---

## 🧠 CORE ARCHITECTURE

### ✔️ koin

* semplice
* perfetto per MVP

Alternativa:

* Hilt (non nella lista)

---

### ✔️ FlowMVI

👉 SOLO se vuoi qualcosa di avanzato
(altrimenti MVVM basta)

---

## 🧵 ASYNC / FLOW

### ✔️ kotlinx.coroutines

ESSENZIALE

---

### ✔️ FlowBinding

* utile per eventi UI reattivi

---

## 🖼 IMMAGINI

### ✔️ Coil

* perfetto con Compose

---

### ✔️ EasyImage

* semplifica camera/galleria

---

### ✔️ ZoomLayout

🔥 molto utile per il tuo caso

* zoom + pan già pronti

---

## 🧠 IMAGE PROCESSING

### ✔️ OpenCV

* clustering colori
* preprocessing

---

## 💾 DATABASE

### ✔️ Room Database

(non nella lista ma standard)

Alternativa dalla lista:

* DBFlow
* sqldelight

👉 consiglio: Room > tutto

---

## 🧪 DEBUG / QUALITÀ

### ✔️ leakcanary

* fondamentale con immagini

---

### ✔️ timber

* logging serio

---

### ✔️ detekt

* qualità codice

---

## 🎨 UI / UX

### ✔️ accompanist

* utilities Compose

---

### ✔️ Balloon

🔥 perfetto per:

* note sui colori
* overlay parlanti

---

### ✔️ TransformationLayout

* animazioni tra elementi

---

## 🧠 BONUS UTILI

### ✔️ android-upload-service

(se in futuro vuoi cloud)

---

### ✔️ Store

* caching immagini/dati

---

# ⚠️ COSA NON USARE (per ora)

Evita:

* RxJava → overkill
* framework troppo complessi
* librerie UI pesanti

👉 mantieni:

> semplice + modulare

---

# 🧱 ARCHITETTURA FINALE CONSIGLIATA

Stack ideale:

* Kotlin
* Compose
* Room
* Coroutines
* Coil
* OpenCV
* Koin

---

# 🚀 CONCLUSIONE

Hai in mano un’app che in realtà è:

👉 metà **tool di analisi visiva**
👉 metà **strumento documentale per progetti**

Se fatta bene può diventare:

* prodotto reale
* app premium

---

# 👉 Se vuoi fare il prossimo step

Posso costruirti:

1. 📁 struttura progetto Android completa
2. 🧱 schema DB pronto
3. 🎯 primo screen funzionante (color picker)
4. 🧠 algoritmo palette già scritto

Dal punto di vista di utilità “pratica”, soprattutto per un falegname curioso, direi che sei molto coperto sul lato pro, ma ti mancano un paio di pezzi “terra‑terra”:

1. Modalità veloce / light
Per l’utente non designer:

“Scatta → tocca → vedi il colore” senza creare un progetto obbligatorio
Solo dopo, opzionale: “Vuoi salvarlo in una scheda progetto?”
2. Linguaggio e preset “da cantiere”
Il falegname non ragiona in “palette concettuali”, ma in:

Materiale + finitura: legno chiaro/scuro, laccato, opaco/lucido
Zone tipiche: mobile, anta, top, parete dietro, pavimento
Tag veloci predefiniti tipo: “pensile”, “piano lavoro”, “parete fondo”
3. Confronto prima/dopo / alternative
Per sperimentare:

Vista split-screen o swipe tra 2 immagini (stato attuale vs proposta)
Confronto tra 2 palette: “Versione A” vs “Versione B”
4. Aiuto decisionale minimo
Non serve una AI, ma:

Semplici suggerimenti tipo:
“Questo colore è molto saturo, di solito sui mobili grandi stanca”
“Palette con forte contrasto, adatta a pezzi singoli, non a tutta la stanza”
Piccoli template: “cucina rustica chiara / moderna scura / laboratorio”
5. Output “stampabile / condivisibile”
Oltre al testo:

Scheda riassuntiva in immagine/PDF con:
foto + marker
blocchi colore con codici
note essenziali
Pensata per: mandarla su WhatsApp al cliente o stamparla in falegnameria
6. Flusso “lista materiali”
Un minimo ponte con la realtà:

Per ogni colore: campo libero “Prodotto usato / codice vernice / fornitore”
Export che sembri quasi una lista materiali da portare in negozio
Se vuoi, nel prossimo passo posso aiutarti a formalizzare una “User Story” specifica per il falegname curioso (scenario completo dall’apertura app all’export).

---

# Librerie Esterne Fondamentali

Per accelerare lo sviluppo e garantire funzionalità robuste, verranno integrate le seguenti librerie esterne, scelte dopo un'attenta analisi.

1.  **`coil-kt/coil`**:
    *   **Scopo**: Caricamento delle immagini.
    *   **Motivazione**: È la libreria standard de-facto per il caricamento di immagini in Jetpack Compose. È performante, scritta in Kotlin, basata su Coroutines e si integra perfettamente con l'ecosistema moderno di Android.

2.  **`InsertKoinIO/koin`**:
    *   **Scopo**: Dependency Injection.
    *   **Motivazione**: Per un MVP, Koin offre una curva di apprendimento più dolce e una configurazione più semplice rispetto a Dagger/Hilt, pur fornendo tutti gli strumenti necessari per una buona architettura.

3.  **`opencv/opencv`**:
    *   **Scopo**: Analisi dell'immagine (estrazione palette).
    *   **Motivazione**: OpenCV è lo standard industriale per la computer vision. La sua implementazione dell'algoritmo K-means è affidabile e performante, ideale per estrarre la palette di colori dominante da un'immagine in modo efficiente.

4.  **`burhanrashid52/PhotoEditor`**:
    *   **Scopo**: Editing e annotazione dell'immagine.
    *   **Motivazione**: Questa libreria è una scoperta chiave. Fornisce quasi tutte le funzionalità di interazione con l'immagine di cui abbiamo bisogno:
        *   Disegno a mano libera e forme geometriche.
        *   Aggiunta di testo personalizzabile.
        *   Aggiunta di "Sticker" (che useremo per implementare i nostri "punti colore").
        *   Gestione di zoom e rotazione (Pinch & Zoom) delle annotazioni.
        *   Funzioni di Undo/Redo.
        *   È scritta in Kotlin e usa Coroutines, integrandosi perfettamente nel nostro stack.
        *   L'adozione di questa libreria eviterà di dover scrivere da zero codice complesso e prono a errori per la gestione del canvas.

5.  **`skydoves/balloon`**:
    *   **Scopo**: Tooltip e popup informativi.
    *   **Motivazione**: Ideale per mostrare informazioni contestuali in modo non invasivo, come i dettagli di un colore quando l'utente tocca un punto sulla palette, senza interrompere il flusso di lavoro.

6.  **`iamageo/zoomlayout`**:
    *   **Scopo**: Zoom dell'interfaccia.
    *   **Motivazione**: Sebbene `PhotoEditor` gestisca lo zoom dell'immagine, `ZoomLayout` potrebbe essere utile per rendere zoomabili altre parti dell'interfaccia, come le palette di colori generate o le viste di confronto.

7.  **`JaCobrT/Spotlight`**: Per l'onboarding e i tutorial.

### Filosofia di Design: "Studio Digitale"

L'app deve evocare la sensazione di uno **studio di design professionale**: pulito, organizzato e focalizzato sullo strumento principale, il colore. Deve essere un ambiente che ispira creatività ma che al tempo stesso trasmette precisione e affidabilità, come un vero strumento di lavoro.

#### 1. Palette Colori dell'Interfaccia

*   **Tema Neutro**: L'interfaccia utilizzerà un tema prevalentemente neutro (es. diverse tonalità di grigio scuro per la modalità "dark" e di grigio chiaro per la "light"). Questo per un motivo fondamentale: **l'interfaccia non deve mai competere con i colori analizzati**. I colori dell'utente devono essere i protagonisti assoluti.
*   **Colore Accento**: Useremo un singolo colore di accento, vibrante ma non invadente (es. un blu elettrico o un arancione), per tutti gli elementi interattivi: pulsanti, icone attive, slider. Questo crea una guida visiva chiara per l'utente su "cosa si può fare".

#### 2. Tipografia

*   **Leggibilità**: Sceglieremo un font pulito e moderno (come *Roboto*, *Inter* o *Montserrat*) che garantisca massima leggibilità sia per i testi lunghi (note) sia per i dati numerici (valori RGB/HEX).
*   **Gerarchia Visiva**: Useremo pesi e dimensioni diverse del font per creare una chiara gerarchia: titoli di progetto, nomi delle immagini, valori dei colori e note avranno stili distinti per essere immediatamente riconoscibili.

#### 3. Iconografia

*   **Stile Coerente**: Tutte le icone (salva, aggiungi, cancella, etc.) seguiranno uno stile unico e minimalista (es. "outline" o "filled", ma non un mix). Questo contribuisce a un aspetto pulito e professionale.
*   **Intuitività**: Le icone saranno universalmente riconoscibili per ridurre il carico cognitivo. Un'icona a forma di "più" (+) significherà sempre "aggiungi", un cestino significherà sempre "elimina".

#### 4. Animazioni e Transizioni (Micro-interazioni)

Le animazioni non saranno puramente decorative, ma funzionali a migliorare l'esperienza.

*   **Transizioni fluide**: Passaggi tra schermate (es. dalla lista progetti all'analisi di un'immagine) saranno fluidi e contestuali (es. l'immagine del progetto si "espande" per riempire lo schermo). La libreria `TransformationLayout` che abbiamo identificato è perfetta per questo.
*   **Feedback Tattile**: Ogni azione importante (aggiungere un punto colore, salvare una nota) sarà accompagnata da un leggero feedback visivo (es. un'onda che si espande dal punto toccato) o una sottile animazione dell'icona. Questo conferma all'utente che il suo comando è stato ricevuto.
*   **Animazioni "Intelligenti"**: Quando si estrae una palette, i colori potrebbero apparire con una piccola animazione in sequenza, guidando l'occhio dell'utente e rendendo il processo più dinamico e meno "statico".

#### 5. Onboarding e Guida Utente

*   **Primo Avvio Guidato**: Utilizzeremo la libreria `Spotlight` per creare un tour guidato al primo avvio. Metteremo in evidenza i 3-4 pulsanti/funzioni chiave ("Aggiungi Immagine", "Estrai Palette", "Aggiungi Punto Colore") per rendere l'utente subito operativo.
*   **Tooltip Contestuali**: Per le funzioni più avanzate, useremo la libreria `Balloon` per mostrare piccoli tooltip informativi quando l'utente tiene premuto a lungo su un'icona, senza interrompere il suo lavoro.

## Struttura dei Dati e Persistenza

### Database:

👉 **Room Database** (per indicizzare progetti e metadati)

#### Tabelle:

*   **projects**: `id`, `name`, `description`, `folder_path`
*   **images**: `id`, `project_id`, `original_uri`, `file_name`, `notes`
*   **color_points**: `id`, `image_id`, `x`, `y`, `hex_code`, `note`, `material_info`
*   **palette_colors**: `id`, `project_id`, `hex_code` (La palette è un aggregato a livello di Progetto)

### File System:

*   Alla creazione di un progetto, viene creata una cartella in `context.getFilesDir()/projects/project_name`.
*   Questa cartella contiene:
    *   Le copie a **risoluzione piena** di tutte le immagini.
    *   Un file `project_data.json` che contiene una serializzazione di tutte le note, i tag, i punti colore e le palette, per un backup e ripristino facilitato.

---

## 🗂 Storage immagini

*   **Copia Locale Obbligatoria**: Per ogni immagine aggiunta a un progetto, una copia a risoluzione piena viene salvata nella cartella del progetto.
*   **Indipendenza dalla Galleria**: L'app fa affidamento solo sulla sua copia locale. Se l'utente cancella l'immagine originale dalla galleria, l'app continua a funzionare senza problemi.
*   **Backup Semplificato**: L'utente può fare il backup di un intero progetto semplicemente copiando la cartella corrispondente.

---

## 📋 Clipboard export

Android:

*   ClipboardManager

---

## 🔄 Serializzazione

Formato:

*   **JSON (kotlinx.serialization)**: Usato per il file `project_data.json` all'interno di ogni cartella di progetto. Contiene tutti i metadati, le note e i punti colore.
*   **Markdown**: Usato per l'export testuale verso la clipboard.

---

