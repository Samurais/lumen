# Lumen Quran Skill

## Installation

1. Inside `C:\Users\<username>\git` folder, clone [Sanad project](https://github.com/soluvas/sanad) from https://github.com/soluvas/sanad.git .
2. Inside `C:\Users\<username>\git` folder, clone [Sanad-Quran project](https://github.com/soluvas/sanad-quran) from https://github.com/soluvas/sanad-quran.git .
3. Install [PostgreSQL 9.5+](http://www.postgresql.org/).
4. In PgAdmin, create database `lumen_lumen_dev` with UTF-8 encoding (if not exists).
5. Run `C:\Users\<username>\git\sanad\export\sanad.schema.sql` in `lumen_lumen_dev` database. You can use PgAdmin.
6. In `C:\Users\<username>\git\sanad-quran` folder, import data using `psql` and `COPY` (PostgreSQL server's) or `\copy` (locally) command:

        \copy sanad.quranchapter from 'quranchapter.tsv' (format csv, delimiter E'\t', header true, escape E'\\', encoding 'UTF-8')
        \copy sanad.literal from 'literal-quran.tsv' (format csv, delimiter E'\t', header true, escape E'\\', encoding 'UTF-8')
        \copy sanad.quranverse from 'quranverse.tsv' (format csv, delimiter E'\t', header true, escape E'\\', encoding 'UTF-8')
        \copy sanad.transliteration from 'transliteration-quran.tsv' (format csv, delimiter E'\t', header true, escape E'\\', encoding 'UTF-8')
        \copy sanad.spellingproperty from 'spellingproperty-quran.tsv' (format csv, delimiter E'\t', header true, escape E'\\', encoding 'UTF-8')
        \copy sanad.authenticityproperty from 'authenticityproperty-quran.tsv' (format csv, delimiter E'\t', header true, escape E'\\', encoding 'UTF-8')
        \copy sanad.successionproperty from 'successionproperty-quran.tsv' (format csv, delimiter E'\t', header true, escape E'\\', encoding 'UTF-8')

7. Download [versebyversequran_alafasy_ogg.zip](https://drive.google.com/file/d/0B9dx38a6NVxKZjlfcWR6aWVod28/view?usp=sharing) (1.24 GB) and extract to `D:\` (will create `versebyversequran_alafasy_ogg` subfolder)
