# MIRCV Project
## Introduction
In this project, we have developed a search engine for text retrieval, leveraging an inverted index structure based on the MSMARCO Passages collection. The goal is to efficiently process queries and rank relevant documents for users. Developed by Acampora Vittoria, Laporta Daniele and Zingariello Francesco.

MSMARCO Passages Collection: [TREC-Deep-Learning-2020](https://microsoft.github.io/msmarco/TREC-Deep-Learning-2020)

## Project Overview

### 1. Main Modules
Our project is divided into three main modules:

 - Indexing: Builds the inverted index data structure and writes it to disk.
 - Query: Processes user queries, allowing customization of query parameters (scoring function, query type).
 - Evaluation: Loads queries from a file, generates query results, and evaluates performance using the trec_eval tool.

### 2. Main Functions and Classes
 - MainIndexing: Main class for the indexing module, responsible for building the inverted index.
 - Compressor: Handles compression and decompression of a list of integers using Variable Byte Encoding.
 - Merger: Facilitates merging of block files generated during the SPIMI algorithm in the indexing phase.
 - MainQueries: Main class for processing user queries, interacting with the inverted index.
 - MaxScore: Implements the scoring process based on the DAAT algorithm for conjunctive and disjunctive queries.
 - MainEvaluation: Main class for query evaluation, loading queries from a file, generating query result files, and evaluating them using trec_eval.
 - Parameters: Interface that contains all constants and paths.

## Usage
To build the inverted index and write it to disk: mainIndexing.java

To process a user query and retrieve relevant documents: mainQueries.java

To evaluate query performance: mainEvaluation.java.


