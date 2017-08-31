from google_ngram_downloader import readline_google_store
import google_ngram_downloader
import sys

_tags = [ "_NOUN", "_VERB", "_ADJ",  "_ADV",  "_PRON", "_DET",  "_ADP",  "_NUM",  "_CONJ", "_PRT",  "_ROOT_", "_START_", "_END_",  "_NOUN_", "_VERB_", "_ADJ_",  "_ADV_",  "_PRON_", "_DET_",  "_ADP_",  "_NUM_",  "_CON    J_", "_PRT_", ]

def remove_tags(s):
    """Remove google tags."""
    for tag in _tags:
	if '_' not in s:
	    return s
        s = s.replace(tag, '')
    return s

with open(sys.argv[1]) as f:
    needed_ngrams = f.read().splitlines()

needed_ngrams.sort()

n=int(sys.argv[2])
#make a set of all indices that need to be downloaded
needed_indices = set([x[:min(n,2)].lower() for x in needed_ngrams])
#print "Needed indices:",needed_indices

#create a map with each needed ngram a key associated with a 0 value
ngram_counts_dict = {}
for needed_ngram in needed_ngrams:
	ngram_counts_dict[needed_ngram] = 0

#print ngram_counts_dict

#for each indices, iterate over all entries for the index (lines aren't sorted)
for index in needed_indices:
	fname, url, records = next(readline_google_store(ngram_len=n, indices=(index if n ==     1 else [index])))
	for record in records:
	#add counts for matching terms
		record_ngram = remove_tags(record.ngram)
		if record_ngram in ngram_counts_dict:
			ngram_counts_dict[record_ngram] += record.match_count
 
#print counts
for ngram,ngram_counts in ngram_counts_dict.iteritems():
	print ngram, ngram_counts
