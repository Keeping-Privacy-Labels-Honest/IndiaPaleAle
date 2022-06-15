# IndiaPaleAle
This tool takes care of downloading 3u app ranking as well as the privacy labels from apple

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
