BookNLP
=======

BookNLP is a natural language processing pipeline that scales to books and other long documents (in English), including:

* Part-of-speech tagging (Stanford)
* Dependency parsing (MaltParser)
* Named entity recognition (Stanford)
* Character name clustering (e.g., "Tom", "Tom Sawyer", "Mr. Sawyer", "Thomas Sawyer" -> TOM_SAWYER)
* Quotation speaker identification
* Pronominal coreference resolution

This pipeline is described in the following paper; please cite if you write a research paper using this software:

David Bamman, Ted Underwood and Noah Smith, "A Bayesian Mixed Effects Model of Literary Character," ACL 2014.


How To Run
=======

####Preliminaries

Download external jars (which are sadly too big for GitHub's 100MB file size limit)

* Download and unzip stanford-core-nlp-full-2015-09.zip from http://stanfordnlp.github.io/CoreNLP/
* copy stanford-corenlp-full-2014-01-04/stanford-corenlp-3.6.0-models.jar to the lib/ folder in the current working directory


####Example

From the command line, run the following:

    ./runjava novels/BookNLP -doc data/originalTexts/dickens.oliver.pg730.txt -printHTML -p data/output/dickens -tok data/tokens/dickens.oliver.tokens -f

(On a 2.6 GHz MBP, this takes about 3.5 minutes)

This runs the bookNLP pipeline on "Oliver Twist" in the data/originalTexts directory and writes the processed document to data/tokens/dickens.oliver.tokens, along with diagnostic info to data/output/dickens.  To run on your own texts, change the following:

* data/originalTexts/dickens.oliver.pg730.txt -> the path to the input book you want to process.
* data/tokens/dickens.oliver.tokens -> the path to the file where you want the processed text to be stored.
* data/output/dickens -> the path to the output directory you want to write any other diagnostics to.

####Flags

######Required

-doc <text> : original text to process

-tok <file> : file path to save processed tokens to (or read them from, if it already exists)

-p : the directory to write all diagnostic files to.  Creates the directory if it does not already exist.


######Optional

-id : a unique book ID for this book (output files include this in the filename)

-printHTML	: also print the text as an HTML file with character aliases, coref and speaker ID annotated

-f : force the (slower) syntactic processing of the original text file, even if the <file> in the -tok flag exists (if the -tok <file> exists, the process that would parse the original text to create it is skipped)


####Output

The main output here is data/tokens/dickens.oliver.tokens, which contains the original book, one token per line, with part of speech, syntax, NER, coreference and other annotations.  The (tab-separated) format is:

1. Paragraph id
2. Sentence id
3. Token id
4. Byte start
5. Byte end
6. Whitespace following the token (useful for pretty-printing the original text)
7. Syntactic head id (-1 for the sentence root)
8. Original token
9. Normalized token (for quotes etc.)
10. Lemma
11. Penn Treebank POS tag
12. NER tag (PERSON, NUMBER, DATE, DURATION, MISC, TIME, LOCATION, ORDINAL, MONEY, ORGANIZATION, SET, O)
13. Stanford basic dependency label
14. Within-quotation flag
15. Character id (all coreferent tokens share the same character id)

The data/output/dickens folder will now contain:

* dickens.oliver.twist.html (described above)
* dickens.oliver.twist.book (a representation of all of the characters' features, in JSON)




Modifying the code
================

With apache ant installed, running `ant` compiles everything.


Training coreference
====================

Coreference only needs to be trained when there's new training data (or new feature ideas: current features are based on syntactic tree distance, linear distance, POS identity, gender matching, quotation scope and salience).

####Data

Coreference annotated data is located in the coref/ directory. 

annotatedData.txt contains coreference annotations, in the (tab-separated) format:

1. book ID
2. anaphor token ID
3. antecendent token ID

bookIDs are mapped to their respective token files in docPaths.txt.  All of these token files are located in finalTokenData/.  These tokens files are all read-only -- since the annotations are keyed to specific token IDs in those files, we want to make sure they stay permanent.

####Training a model

Given the coref/ folder above, train new coreference weights with:

    ./runjava novels.training/TrainCoref -training coref/annotatedData.txt -o coref/weights.txt

-training specifies the input training file

-o specifies the output file to write the trained weights to

Two parameters control the amount of regularization in the model (higher regularization dampens the impact of any single feature, and L1 regularization removes features from the model; both help prevent overfitting to training data.)

-l1 specifies the L1 regularization parameter (higher = more weights end up driven to 0). Default = 2

-l2 specifies the L2 regularization parameter (higher = weights shrink faster). Default = .1

To use the newly trained weights in the pipeline above, copy them to files/coref.weights or specify them on the novels.BookNLP command line with the -w flag.


