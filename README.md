# IndiaPaleAle
This tool takes care of downloading 3u app ranking as well as the privacy labels from apple.

You can get a list of app ids via the `3u` action, or by using the `meta` action on a set of IPAs. 
This list of app ids is then used in the `privacyLabels` action or if you want to use the jsons obtained via `3u` in the `get-privacy-labels` subaction to download the corresponding set of privacy labels.

Our obtained set of privacy labels can be accessed [here](https://github.com/Keeping-Privacy-Labels-Honest/privacyLabels).

## Build

```
sbt stage
```

### Dependencies

```
- OpenJDK 18.0.1.1 (or better)
- sbt 1.6.2
```

## Usage

```
$> ./run.sh -h
usage: IPA {-h} 3u meta privacyLabels

utility to analyze and work with information contained in .ipa files

          -h/--help prints this help message

          3u interaction with the 3u web API

          meta extract the app meta data of the given file/or files in given folder

          privacyLabels actions related to privacy labels
```

You have three different features here:
1. interacting with the 3u API endpoint to retrieve app meta information
2. extract meta information of an IPA or a folder full of IPAs
3. download privacy labels

### 3u
```
./run.sh 3u -h
usage: ... 3u {-h}  statistics get-privacy-labels download

interaction with the 3u web API

          -h/--help prints this help message

           

          statistics get statistics on the downloaded app list

          get-privacy-labels download the privacy labels

          download download json files for category and page
 ```

#### statistics

```
./run.sh 3u statistics <folder>
```

This will read in all .json in the given folder (downloaded via `download` action) and give basic statistics on how many apps are contained and how many unique apps a re contained.

#### get-privacy-labels

```
./run.sh 3u get-privacy-labels <folder>
```

This will read in all .json in the given folder (downloaded via `download` action) and download the given privacy labels.

#### download

```
./run.sh 3u download <category> <until> <folder>
```

This will download all the apps (as json) of `category` (can be a single number, csv, or `all`) until page number `until` storing results in 

### meta

```
./run.sh meta <elements> <path> [-e,--header]
```

Takes a csv list of `elements` naming the different attributes of an IPA (example see `runMetaDataExtraction.sh`) and goes through all IPA listed at `path` and prints out the retrieved elements. If `--header` or `-e` is provided the first line is a header line containing the order of the extracted elements.

### privacyLabels

```
./run.sh privacyLabels download <csv> <out>
```

Downloads the privacy labels contained in the `csv`, the csv needs to have a header line and contain at least the row `itemId`. Easiest way to get that csv is to use the `meta` action on a set of IPAs combined with the `-e`  flag.
